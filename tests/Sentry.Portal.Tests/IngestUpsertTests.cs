using Microsoft.EntityFrameworkCore;
using Sentry.Portal.Api;
using Sentry.Portal.Data;
using Sentry.Portal.Data.Entities;

namespace Sentry.Portal.Tests;

public class IngestUpsertTests
{
    [Fact]
    public async Task SameExternalId_UpdatesExistingRow()
    {
        using var host = new TestHost();
        var site = await host.Db.Sites.SingleAsync(s => s.Id == SeedIds.StubbeSite);

        var first = await host.Ingest.UpsertAlarmsAsync(site, new[]
        {
            new AlarmIngestItem
            {
                ExternalId = "agv-04-block",
                Source = "JBT",
                AssetLabel = "AGV-04",
                Description = "Pad geblokkeerd",
                State = "active",
                StartedAt = DateTimeOffset.UtcNow.AddMinutes(-10)
            }
        });

        var second = await host.Ingest.UpsertAlarmsAsync(site, new[]
        {
            new AlarmIngestItem
            {
                ExternalId = "agv-04-block",
                Source = "JBT",
                AssetLabel = "AGV-04",
                Description = "Pad vrij",
                State = "cleared",
                StartedAt = DateTimeOffset.UtcNow.AddMinutes(-10),
                EndedAt = DateTimeOffset.UtcNow
            }
        });

        Assert.Equal(1, first.Accepted);
        Assert.Equal(1, second.Updated);
        Assert.Equal(1, await host.Db.AlarmEvents.CountAsync());
        var row = await host.Db.AlarmEvents.SingleAsync();
        Assert.Equal(AlarmState.Cleared, row.State);
        Assert.Equal("Pad vrij", row.Description);
        Assert.NotNull(row.EndedAt);
    }
}
