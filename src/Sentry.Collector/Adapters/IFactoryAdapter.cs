namespace Sentry.Collector.Adapters;

public interface IFactoryAdapter
{
    Task<IReadOnlyList<TelemetryReading>> ReadTelemetryAsync(CancellationToken cancellationToken);
    Task<IReadOnlyList<AlarmReading>> ReadAlarmsAsync(CancellationToken cancellationToken);
}
