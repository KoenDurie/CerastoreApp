namespace Sentry.Collector.Adapters;

public sealed class MockAdapter : IFactoryAdapter
{
    private readonly Random _random = new(42);

    public Task<IReadOnlyList<TelemetryReading>> ReadTelemetryAsync(CancellationToken cancellationToken)
    {
        var now = DateTimeOffset.UtcNow;
        IReadOnlyList<TelemetryReading> rows =
        [
            new("AGV-04", 30 + _random.NextDouble() * 20, "busy", now),
            new("AGV-12", 55 + _random.NextDouble() * 30, "idle", now),
            new("SH-03", 12 + _random.NextDouble() * 10, "idle", now)
        ];
        return Task.FromResult(rows);
    }

    public Task<IReadOnlyList<AlarmReading>> ReadAlarmsAsync(CancellationToken cancellationToken)
    {
        var now = DateTimeOffset.UtcNow;
        IReadOnlyList<AlarmReading> rows =
        [
            new("mock-agv04-block", "JBT", "Dock", "AGV-04", "BLOCK", "Pad geblokkeerd (mock)", "active", now.AddMinutes(-4), null)
        ];
        return Task.FromResult(rows);
    }
}
