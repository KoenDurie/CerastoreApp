using Microsoft.Data.SqlClient;

namespace Sentry.Collector.Adapters;

/// <summary>
/// Read-only SQL tegen de fabrieksdatabase (GLA/JBT). Queries komen uit config,
/// niet uit Cerastore. Geen writes.
/// </summary>
public sealed class SqlAdapter(CollectorOptions options, ILogger<SqlAdapter> logger) : IFactoryAdapter
{
    public async Task<IReadOnlyList<TelemetryReading>> ReadTelemetryAsync(CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(options.Sql.ConnectionString) || string.IsNullOrWhiteSpace(options.Sql.TelemetryQuery))
        {
            logger.LogWarning("SQL-telemetry overgeslagen: connection string of query ontbreekt.");
            return [];
        }

        var rows = new List<TelemetryReading>();
        await using var connection = new SqlConnection(options.Sql.ConnectionString);
        await connection.OpenAsync(cancellationToken);
        await using var command = new SqlCommand(options.Sql.TelemetryQuery, connection);
        await using var reader = await command.ExecuteReaderAsync(cancellationToken);
        while (await reader.ReadAsync(cancellationToken))
        {
            rows.Add(new TelemetryReading(
                ReadString(reader, "AssetLabel") ?? "unknown",
                ReadDouble(reader, "BatteryPercent"),
                ReadString(reader, "Status"),
                DateTimeOffset.UtcNow));
        }

        return rows;
    }

    public async Task<IReadOnlyList<AlarmReading>> ReadAlarmsAsync(CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(options.Sql.ConnectionString) || string.IsNullOrWhiteSpace(options.Sql.AlarmQuery))
        {
            return [];
        }

        var rows = new List<AlarmReading>();
        await using var connection = new SqlConnection(options.Sql.ConnectionString);
        await connection.OpenAsync(cancellationToken);
        await using var command = new SqlCommand(options.Sql.AlarmQuery, connection);
        await using var reader = await command.ExecuteReaderAsync(cancellationToken);
        while (await reader.ReadAsync(cancellationToken))
        {
            var externalId = ReadString(reader, "ExternalId") ?? Guid.NewGuid().ToString("N");
            rows.Add(new AlarmReading(
                externalId,
                ReadString(reader, "Source") ?? options.Mode,
                ReadString(reader, "Zone"),
                ReadString(reader, "AssetLabel"),
                ReadString(reader, "Code"),
                ReadString(reader, "Description") ?? externalId,
                ReadString(reader, "State") ?? "active",
                ReadTime(reader, "StartedAt") ?? DateTimeOffset.UtcNow,
                ReadTime(reader, "EndedAt")));
        }

        return rows;
    }

    private static string? ReadString(SqlDataReader reader, string column)
    {
        var i = IndexOf(reader, column);
        return i < 0 || reader.IsDBNull(i) ? null : reader.GetValue(i)?.ToString();
    }

    private static double? ReadDouble(SqlDataReader reader, string column)
    {
        var i = IndexOf(reader, column);
        if (i < 0 || reader.IsDBNull(i))
        {
            return null;
        }

        return Convert.ToDouble(reader.GetValue(i));
    }

    private static DateTimeOffset? ReadTime(SqlDataReader reader, string column)
    {
        var i = IndexOf(reader, column);
        if (i < 0 || reader.IsDBNull(i))
        {
            return null;
        }

        var value = reader.GetValue(i);
        return value switch
        {
            DateTimeOffset dto => dto,
            DateTime dt => new DateTimeOffset(DateTime.SpecifyKind(dt, DateTimeKind.Utc)),
            _ => DateTimeOffset.TryParse(value.ToString(), out var parsed) ? parsed : null
        };
    }

    private static int IndexOf(SqlDataReader reader, string column)
    {
        for (var i = 0; i < reader.FieldCount; i++)
        {
            if (string.Equals(reader.GetName(i), column, StringComparison.OrdinalIgnoreCase))
            {
                return i;
            }
        }

        return -1;
    }
}
