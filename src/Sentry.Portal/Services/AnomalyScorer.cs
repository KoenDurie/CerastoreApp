using Sentry.Portal.Data.Entities;

namespace Sentry.Portal.Services;

public sealed record AnomalyRow(
    string AssetLabel,
    double RecentCount,
    double BaselineAvg,
    double StdDev,
    double Score,
    bool IsAnomaly);

public static class AnomalyScorer
{
    public const double Threshold = 3.0;

    public static double Score(double recentCount, double baselineAvg, double stdDev)
        => (recentCount - baselineAvg) / (stdDev + 1.0);

    public static (double Mean, double StdDev) BaselineStats(IReadOnlyList<double> dailyCounts)
    {
        if (dailyCounts.Count == 0)
        {
            return (0, 0);
        }

        var mean = dailyCounts.Average();
        var variance = dailyCounts.Sum(x => (x - mean) * (x - mean)) / dailyCounts.Count;
        return (mean, Math.Sqrt(variance));
    }

    public static IReadOnlyList<AnomalyRow> ScoreByAsset(
        IEnumerable<AlarmEvent> events,
        DateTimeOffset now,
        int recentDays = 1,
        int baselineDays = 14)
    {
        var recentFrom = now.UtcDateTime.Date.AddDays(1 - recentDays);
        var baselineFrom = recentFrom.AddDays(-baselineDays);
        var rows = new List<AnomalyRow>();

        foreach (var group in events.GroupBy(e => string.IsNullOrWhiteSpace(e.AssetLabel) ? "onbekend" : e.AssetLabel!))
        {
            var recentCount = group.Count(e => e.StartedAt.UtcDateTime >= recentFrom);
            var daily = new double[baselineDays];
            foreach (var ev in group)
            {
                var day = ev.StartedAt.UtcDateTime.Date;
                if (day >= baselineFrom && day < recentFrom)
                {
                    var idx = (int)(day - baselineFrom).TotalDays;
                    if (idx >= 0 && idx < daily.Length)
                    {
                        daily[idx] += 1;
                    }
                }
            }

            var (mean, std) = BaselineStats(daily);
            var score = Score(recentCount, mean, std);
            rows.Add(new AnomalyRow(group.Key, recentCount, mean, std, score, score > Threshold));
        }

        return rows.OrderByDescending(r => r.Score).ToList();
    }
}
