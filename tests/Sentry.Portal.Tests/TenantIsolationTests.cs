using System.Net.Http.Json;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.EntityFrameworkCore;
using Sentry.Portal.Api;
using Sentry.Portal.Data;
using Sentry.Portal.Data.Entities;
using Sentry.Portal.Services;

namespace Sentry.Portal.Tests;

public class TenantIsolationTests
{
    [Fact]
    public async Task QueryService_DoesNotLeakOtherTenantAlarms()
    {
        using var host = new TestHost();
        var stubbe = await host.Db.Sites.SingleAsync(s => s.Id == SeedIds.StubbeSite);
        var stt = await host.Db.Sites.SingleAsync(s => s.Id == SeedIds.SttSite);

        await host.Ingest.UpsertAlarmsAsync(stubbe, new[]
        {
            new AlarmIngestItem { ExternalId = "s1", Source = "JBT", Description = "Stubbe only", AssetLabel = "AGV-04", State = "active", StartedAt = DateTimeOffset.UtcNow }
        });
        await host.Ingest.UpsertAlarmsAsync(stt, new[]
        {
            new AlarmIngestItem { ExternalId = "s1", Source = "MFCS", Description = "STT only", AssetLabel = "Shuttle-B2", State = "active", StartedAt = DateTimeOffset.UtcNow }
        });

        var query = new AlarmQueryService(host.Db);
        var stubbeRows = await query.ListAsync(SeedIds.Stubbe, AlarmState.Active, 50);
        var sttRows = await query.ListAsync(SeedIds.Stt, AlarmState.Active, 50);

        Assert.Single(stubbeRows);
        Assert.Equal("Stubbe only", stubbeRows[0].Description);
        Assert.DoesNotContain(stubbeRows, r => r.Description.Contains("STT"));
        Assert.Single(sttRows);
        Assert.Equal("STT only", sttRows[0].Description);
    }

    [Fact]
    public async Task HttpIngest_RejectsUnknownApiKey()
    {
        await using var factory = new SentryApiFactory();
        var client = factory.CreateClient();
        client.DefaultRequestHeaders.Add("X-Api-Key", "wrong-key");

        var response = await client.PostAsJsonAsync("/api/ingest/heartbeat", new { });
        Assert.Equal(System.Net.HttpStatusCode.Unauthorized, response.StatusCode);
    }

    [Fact]
    public async Task HttpIngest_AcceptsStubbeKey()
    {
        await using var factory = new SentryApiFactory();
        var client = factory.CreateClient();
        client.DefaultRequestHeaders.Add("X-Api-Key", SeedIds.StubbeApiKey);

        var response = await client.PostAsJsonAsync("/api/ingest/alarms", new AlarmBatchRequest
        {
            Alarms =
            [
                new AlarmIngestItem
                {
                    ExternalId = "http-1",
                    Source = "JBT",
                    Description = "via HTTP",
                    State = "active",
                    StartedAt = DateTimeOffset.UtcNow
                }
            ]
        });

        response.EnsureSuccessStatusCode();
        var body = await response.Content.ReadFromJsonAsync<IngestResult>();
        Assert.NotNull(body);
        Assert.Equal(1, body.Accepted);
    }
}

file sealed class SentryApiFactory : WebApplicationFactory<Program>
{
    private readonly string _dbPath = Path.Combine(Path.GetTempPath(), $"sentry-api-{Guid.NewGuid():N}.db");

    protected override void ConfigureWebHost(Microsoft.AspNetCore.Hosting.IWebHostBuilder builder)
    {
        builder.UseEnvironment("Testing");
        builder.UseSetting("Sentry:Database", "Sqlite");
        builder.UseSetting("ConnectionStrings:Default", $"Data Source={_dbPath}");
        builder.UseSetting("Sentry:DefaultTenantSlug", "stubbe");
        builder.UseSetting("Sentry:SeedDemoData", "false");
    }

    protected override void Dispose(bool disposing)
    {
        base.Dispose(disposing);
        try
        {
            File.Delete(_dbPath);
        }
        catch
        {
            // best-effort cleanup
        }
    }
}
