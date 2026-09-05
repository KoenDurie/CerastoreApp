using Microsoft.EntityFrameworkCore;
using Sentry.Portal.Api;
using Sentry.Portal.Data;
using Sentry.Portal.Data.Entities;

namespace Sentry.Portal.Services;

public sealed class IngestService(SentryDbContext db, SmsAlertService sms)
{
    public async Task<Site?> FindSiteByApiKeyAsync(string? apiKey, CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrWhiteSpace(apiKey))
        {
            return null;
        }

        return await db.Sites.FirstOrDefaultAsync(s => s.ApiKey == apiKey, cancellationToken);
    }

    public async Task<IngestResult> UpsertAlarmsAsync(Site site, IEnumerable<AlarmIngestItem> items, CancellationToken cancellationToken = default)
    {
        var accepted = 0;
        var updated = 0;

        foreach (var item in items)
        {
            if (string.IsNullOrWhiteSpace(item.ExternalId))
            {
                continue;
            }

            var state = ParseState(item.State);
            var existing = await db.AlarmEvents.FirstOrDefaultAsync(
                e => e.TenantId == site.TenantId && e.SiteId == site.Id && e.ExternalId == item.ExternalId,
                cancellationToken);

            if (existing is null)
            {
                db.AlarmEvents.Add(new AlarmEvent
                {
                    Id = Guid.NewGuid(),
                    TenantId = site.TenantId,
                    SiteId = site.Id,
                    ExternalId = item.ExternalId.Trim(),
                    Source = item.Source,
                    Zone = item.Zone,
                    AssetLabel = item.AssetLabel,
                    Code = item.Code,
                    Description = string.IsNullOrWhiteSpace(item.Description) ? item.ExternalId : item.Description,
                    Details = item.Details,
                    Severity = item.Severity,
                    State = state,
                    StartedAt = item.StartedAt == default ? DateTimeOffset.UtcNow : item.StartedAt,
                    EndedAt = item.EndedAt,
                    PayloadJson = item.PayloadJson
                });
                accepted++;
            }
            else
            {
                existing.Source = item.Source;
                existing.Zone = item.Zone;
                existing.AssetLabel = item.AssetLabel;
                existing.Code = item.Code;
                existing.Description = string.IsNullOrWhiteSpace(item.Description) ? existing.Description : item.Description;
                existing.Details = item.Details;
                existing.Severity = item.Severity;
                existing.State = state;
                existing.EndedAt = item.EndedAt ?? (state == AlarmState.Active ? existing.EndedAt : DateTimeOffset.UtcNow);
                existing.PayloadJson = item.PayloadJson ?? existing.PayloadJson;
                updated++;
            }
        }

        await db.SaveChangesAsync(cancellationToken);
        return new IngestResult { Accepted = accepted, Updated = updated };
    }

    public async Task<IngestResult> AddTelemetryAsync(Site site, IEnumerable<TelemetryIngestItem> items, CancellationToken cancellationToken = default)
    {
        var accepted = 0;
        foreach (var item in items)
        {
            if (string.IsNullOrWhiteSpace(item.AssetLabel))
            {
                continue;
            }

            db.AssetTelemetry.Add(new AssetTelemetry
            {
                Id = Guid.NewGuid(),
                TenantId = site.TenantId,
                SiteId = site.Id,
                AssetLabel = item.AssetLabel.Trim(),
                BatteryPercent = item.BatteryPercent,
                Status = item.Status,
                RecordedAt = item.RecordedAt == default ? DateTimeOffset.UtcNow : item.RecordedAt,
                PayloadJson = item.PayloadJson
            });
            accepted++;
        }

        await db.SaveChangesAsync(cancellationToken);
        await sms.EvaluateBatteryAsync(site.TenantId, cancellationToken);
        return new IngestResult { Accepted = accepted };
    }

    public async Task HeartbeatAsync(Site site, CancellationToken cancellationToken = default)
    {
        site.LastHeartbeatAt = DateTimeOffset.UtcNow;
        await db.SaveChangesAsync(cancellationToken);
    }

    private static AlarmState ParseState(string? value)
        => value?.Trim().ToLowerInvariant() switch
        {
            "cleared" => AlarmState.Cleared,
            "resolved" => AlarmState.Resolved,
            _ => AlarmState.Active
        };
}
