using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging.Abstractions;
using Sentry.Portal.Data;
using Sentry.Portal.Data.Entities;
using Sentry.Portal.Services;

namespace Sentry.Portal.Tests;

internal sealed class TestHost : IDisposable
{
    private readonly string _path = Path.Combine(Path.GetTempPath(), $"sentry-test-{Guid.NewGuid():N}.db");

    public SentryDbContext Db { get; }
    public IngestService Ingest { get; }

    public TestHost()
    {
        var options = new DbContextOptionsBuilder<SentryDbContext>()
            .UseSqlite($"Data Source={_path}")
            .Options;
        Db = new SentryDbContext(options);
        Db.Database.EnsureCreated();

        Db.Tenants.AddRange(
            new Tenant { Id = SeedIds.Stubbe, Slug = "stubbe", Name = "Stubbe", FeatureAlarms = true, FeatureBattery = true },
            new Tenant { Id = SeedIds.Stt, Slug = "stt", Name = "STT", FeatureAlarms = true });
        Db.Sites.AddRange(
            new Site { Id = SeedIds.StubbeSite, TenantId = SeedIds.Stubbe, Name = "Stubbe", ApiKey = SeedIds.StubbeApiKey, SourceKind = "JBT" },
            new Site { Id = SeedIds.SttSite, TenantId = SeedIds.Stt, Name = "STT", ApiKey = SeedIds.SttApiKey, SourceKind = "GLA" });
        Db.SaveChanges();

        var config = new ConfigurationBuilder().AddInMemoryCollection().Build();
        var sms = new SmsAlertService(Db, config, NullLogger<SmsAlertService>.Instance);
        Ingest = new IngestService(Db, sms);
    }

    public void Dispose()
    {
        Db.Dispose();
        try
        {
            File.Delete(_path);
        }
        catch
        {
            // best-effort cleanup
        }
    }
}
