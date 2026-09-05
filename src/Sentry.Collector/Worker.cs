using Microsoft.Extensions.Options;
using Sentry.Collector.Adapters;

namespace Sentry.Collector;

public sealed class Worker(
    ILogger<Worker> logger,
    IOptions<CollectorOptions> options,
    PortalClient portal,
    IFactoryAdapter adapter) : BackgroundService
{
    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        var interval = TimeSpan.FromSeconds(Math.Max(5, options.Value.PollIntervalSeconds));
        logger.LogInformation("Sentry collector start. Mode={Mode} Portal={Portal}", options.Value.Mode, options.Value.PortalUrl);

        while (!stoppingToken.IsCancellationRequested)
        {
            try
            {
                await portal.HeartbeatAsync(stoppingToken);
                var telemetry = await adapter.ReadTelemetryAsync(stoppingToken);
                await portal.PostTelemetryAsync(telemetry, stoppingToken);
                var alarms = await adapter.ReadAlarmsAsync(stoppingToken);
                await portal.PostAlarmsAsync(alarms, stoppingToken);
                logger.LogInformation("Cycle ok. telemetry={Telemetry} alarms={Alarms}", telemetry.Count, alarms.Count);
            }
            catch (Exception ex) when (!stoppingToken.IsCancellationRequested)
            {
                logger.LogWarning(ex, "Collector-cycle mislukt; opnieuw over {Interval}", interval);
            }

            await Task.Delay(interval, stoppingToken);
        }
    }
}
