using Microsoft.AspNetCore.Mvc;
using Sentry.Portal.Services;

namespace Sentry.Portal.Api;

public static class IngestEndpoints
{
    public static IEndpointRouteBuilder MapIngestApi(this IEndpointRouteBuilder app)
    {
        var group = app.MapGroup("/api/ingest");

        group.MapPost("/alarms", async (
            AlarmBatchRequest body,
            HttpContext http,
            IngestService ingest,
            CancellationToken cancellationToken) =>
        {
            var site = await ingest.FindSiteByApiKeyAsync(ReadApiKey(http), cancellationToken);
            if (site is null)
            {
                return Results.Unauthorized();
            }

            var result = await ingest.UpsertAlarmsAsync(site, body.Alarms, cancellationToken);
            return Results.Ok(result);
        });

        group.MapPost("/telemetry", async (
            TelemetryBatchRequest body,
            HttpContext http,
            IngestService ingest,
            CancellationToken cancellationToken) =>
        {
            var site = await ingest.FindSiteByApiKeyAsync(ReadApiKey(http), cancellationToken);
            if (site is null)
            {
                return Results.Unauthorized();
            }

            var result = await ingest.AddTelemetryAsync(site, body.Readings, cancellationToken);
            return Results.Ok(result);
        });

        group.MapPost("/heartbeat", async (
            [FromBody] HeartbeatRequest? _,
            HttpContext http,
            IngestService ingest,
            CancellationToken cancellationToken) =>
        {
            var site = await ingest.FindSiteByApiKeyAsync(ReadApiKey(http), cancellationToken);
            if (site is null)
            {
                return Results.Unauthorized();
            }

            await ingest.HeartbeatAsync(site, cancellationToken);
            return Results.Ok(new { ok = true, site = site.Name, at = site.LastHeartbeatAt });
        });

        return app;
    }

    private static string? ReadApiKey(HttpContext http)
        => http.Request.Headers["X-Api-Key"].FirstOrDefault()
           ?? http.Request.Headers.Authorization.FirstOrDefault()?.Replace("Bearer ", "", StringComparison.OrdinalIgnoreCase);
}
