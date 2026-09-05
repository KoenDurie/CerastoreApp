using System.Net.Http.Json;
using System.Text.Json;

namespace Sentry.Collector;

public sealed class PortalClient(HttpClient http, ILogger<PortalClient> logger)
{
    private static readonly JsonSerializerOptions Json = new() { PropertyNamingPolicy = JsonNamingPolicy.CamelCase };

    public async Task HeartbeatAsync(CancellationToken cancellationToken)
    {
        using var response = await http.PostAsJsonAsync("api/ingest/heartbeat", new { collectorVersion = "1.0" }, Json, cancellationToken);
        response.EnsureSuccessStatusCode();
    }

    public async Task PostTelemetryAsync(IReadOnlyList<TelemetryReading> readings, CancellationToken cancellationToken)
    {
        if (readings.Count == 0)
        {
            return;
        }

        using var response = await http.PostAsJsonAsync("api/ingest/telemetry", new { readings }, Json, cancellationToken);
        if (!response.IsSuccessStatusCode)
        {
            logger.LogWarning("Telemetry POST {Status}", response.StatusCode);
            response.EnsureSuccessStatusCode();
        }
    }

    public async Task PostAlarmsAsync(IReadOnlyList<AlarmReading> alarms, CancellationToken cancellationToken)
    {
        if (alarms.Count == 0)
        {
            return;
        }

        using var response = await http.PostAsJsonAsync("api/ingest/alarms", new { alarms }, Json, cancellationToken);
        if (!response.IsSuccessStatusCode)
        {
            logger.LogWarning("Alarm POST {Status}", response.StatusCode);
            response.EnsureSuccessStatusCode();
        }
    }
}

public sealed record TelemetryReading(string AssetLabel, double? BatteryPercent, string? Status, DateTimeOffset RecordedAt);

public sealed record AlarmReading(
    string ExternalId,
    string Source,
    string? Zone,
    string? AssetLabel,
    string? Code,
    string Description,
    string State,
    DateTimeOffset StartedAt,
    DateTimeOffset? EndedAt);
