using Microsoft.EntityFrameworkCore;
using Sentry.Portal.Data;
using Sentry.Portal.Data.Entities;

namespace Sentry.Portal.Services;

public sealed class AlarmQueryService(SentryDbContext db)
{
    public IQueryable<AlarmEvent> ForTenant(Guid tenantId)
        => db.AlarmEvents.AsNoTracking().Where(e => e.TenantId == tenantId);

    public async Task<List<AlarmEvent>> ListAsync(Guid tenantId, AlarmState? state, int take, CancellationToken cancellationToken = default)
    {
        var q = ForTenant(tenantId);
        if (state is not null)
        {
            q = q.Where(e => e.State == state);
        }

        var rows = await q.ToListAsync(cancellationToken);
        return rows.OrderByDescending(e => e.StartedAt).Take(take).ToList();
    }

    public async Task<IReadOnlyList<AnomalyRow>> AnomaliesAsync(Guid tenantId, DateTime nowUtc, CancellationToken cancellationToken = default)
    {
        var from = nowUtc.AddDays(-30);
        var events = await ForTenant(tenantId)
            .Where(e => e.StartedAt >= from)
            .ToListAsync(cancellationToken);
        return AnomalyScorer.ScoreByAsset(events, nowUtc);
    }

    public async Task<IReadOnlyList<DailyRow>> DailyAsync(Guid tenantId, int days, CancellationToken cancellationToken = default)
    {
        var from = DateTime.UtcNow.AddDays(-days);
        var events = await ForTenant(tenantId)
            .Where(e => e.StartedAt >= from)
            .ToListAsync(cancellationToken);

        return events
            .GroupBy(e => e.StartedAt.Date)
            .OrderBy(g => g.Key)
            .Select(g => new DailyRow(DateOnly.FromDateTime(g.Key), g.Count(), g.Count(x => x.State == AlarmState.Active)))
            .ToList();
    }

    public async Task<IReadOnlyList<SummaryRow>> SummaryAsync(Guid tenantId, int days, CancellationToken cancellationToken = default)
    {
        var from = DateTime.UtcNow.AddDays(-days);
        var events = await ForTenant(tenantId)
            .Where(e => e.StartedAt >= from)
            .ToListAsync(cancellationToken);

        return events
            .GroupBy(e => e.AssetLabel ?? e.Description)
            .Select(g => new SummaryRow(g.Key ?? "onbekend", g.Count(), g.Count(x => x.State == AlarmState.Active), g.Max(x => x.StartedAt)))
            .OrderByDescending(r => r.Total)
            .ToList();
    }

    public async Task<IReadOnlyList<BatteryRow>> LatestBatteryAsync(Guid tenantId, CancellationToken cancellationToken = default)
    {
        var rows = await db.AssetTelemetry.AsNoTracking()
            .Where(t => t.TenantId == tenantId)
            .ToListAsync(cancellationToken);
        var latest = rows
            .GroupBy(t => t.AssetLabel)
            .Select(g => g.OrderByDescending(x => x.RecordedAt).First())
            .ToList();

        return latest
            .OrderBy(t => t.BatteryPercent)
            .Select(t => new BatteryRow(t.AssetLabel, t.BatteryPercent, t.Status, t.RecordedAt))
            .ToList();
    }
}

public sealed record DailyRow(DateOnly Day, int Total, int Active);
public sealed record SummaryRow(string AssetLabel, int Total, int Active, DateTime LastSeen);
public sealed record BatteryRow(string AssetLabel, double? BatteryPercent, string? Status, DateTime RecordedAt);
