using Sentry.Portal.Data.Entities;
using Sentry.Portal.Services;

namespace Sentry.Portal.Tests;

public class AnomalyScorerTests
{
    [Fact]
    public void Score_UsesDocumentedFormula()
    {
        var score = AnomalyScorer.Score(recentCount: 11, baselineAvg: 2, stdDev: 0);
        Assert.Equal(9.0, score, 5);
        Assert.True(score > AnomalyScorer.Threshold);
    }

    [Fact]
    public void BaselineStats_ConstantSeries_HasZeroStdDev()
    {
        var (mean, std) = AnomalyScorer.BaselineStats([2, 2, 2, 2]);
        Assert.Equal(2, mean, 5);
        Assert.Equal(0, std, 5);
    }

    [Fact]
    public void ScoreByAsset_FlagsSpikeToday()
    {
        var now = new DateTime(2026, 9, 5, 15, 0, 0, DateTimeKind.Utc);
        var events = new List<AlarmEvent>();
        for (var d = 14; d >= 1; d--)
        {
            events.Add(new AlarmEvent
            {
                AssetLabel = "AGV-04",
                StartedAt = now.Date.AddDays(-d).AddHours(8)
            });
        }

        for (var i = 0; i < 10; i++)
        {
            events.Add(new AlarmEvent
            {
                AssetLabel = "AGV-04",
                StartedAt = now.Date.AddHours(7 + i)
            });
        }

        var rows = AnomalyScorer.ScoreByAsset(events, now, recentDays: 1, baselineDays: 14);
        var agv = Assert.Single(rows, r => r.AssetLabel == "AGV-04");
        Assert.Equal(10, agv.RecentCount);
        Assert.True(agv.IsAnomaly);
        Assert.True(agv.Score > 3.0);
    }
}
