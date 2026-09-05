using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using Sentry.Portal.Data.Entities;

namespace Sentry.Portal.Data;

public static class SeedData
{
    public const string DevPassword = "SentryDev!234";

    public static async Task EnsureAsync(IServiceProvider services, bool seedDemoData, CancellationToken cancellationToken = default)
    {
        using var scope = services.CreateScope();
        var db = scope.ServiceProvider.GetRequiredService<SentryDbContext>();
        await db.Database.EnsureCreatedAsync(cancellationToken);

        if (!await db.Tenants.AnyAsync(cancellationToken))
        {
            db.Tenants.AddRange(
                new Tenant
                {
                    Id = SeedIds.Stubbe,
                    Slug = "stubbe",
                    Name = "Stubbe",
                    FeatureAlarms = true,
                    FeatureBattery = true,
                    FeatureMobileApp = false
                },
                new Tenant
                {
                    Id = SeedIds.Stt,
                    Slug = "stt",
                    Name = "STT",
                    FeatureAlarms = true,
                    FeatureBattery = false,
                    FeatureMobileApp = false
                },
                new Tenant
                {
                    Id = SeedIds.Weerts,
                    Slug = "weerts",
                    Name = "Weerts",
                    FeatureAlarms = false,
                    FeatureBattery = true,
                    FeatureMobileApp = false
                });

            db.Sites.AddRange(
                new Site { Id = SeedIds.StubbeSite, TenantId = SeedIds.Stubbe, Name = "Stubbe fabriek", ApiKey = SeedIds.StubbeApiKey, SourceKind = "JBT" },
                new Site { Id = SeedIds.SttSite, TenantId = SeedIds.Stt, Name = "STT magazijn", ApiKey = SeedIds.SttApiKey, SourceKind = "GLA" },
                new Site { Id = SeedIds.WeertsSite, TenantId = SeedIds.Weerts, Name = "Weerts", ApiKey = SeedIds.WeertsApiKey, SourceKind = "GLA" });

            db.AlertRules.AddRange(
                new AlertRule { Id = Guid.Parse("11111111-aaaa-0000-0000-000000000001"), TenantId = SeedIds.Stubbe, Kind = "Battery", Threshold = 20, Hysteresis = 5, Enabled = true },
                new AlertRule { Id = Guid.Parse("33333333-aaaa-0000-0000-000000000001"), TenantId = SeedIds.Weerts, Kind = "Battery", Threshold = 20, Hysteresis = 5, Enabled = true });

            db.AlertContacts.Add(new AlertContact
            {
                Id = Guid.Parse("33333333-bbbb-0000-0000-000000000001"),
                TenantId = SeedIds.Weerts,
                Name = "Piket Weerts",
                PhoneE164 = "+32470000000",
                SmsEnabled = true
            });

            await db.SaveChangesAsync(cancellationToken);
        }

        var users = scope.ServiceProvider.GetRequiredService<UserManager<ApplicationUser>>();
        await EnsureUserAsync(users, "admin@stubbe.local", SeedIds.Stubbe, platformAdmin: false);
        await EnsureUserAsync(users, "admin@stt.local", SeedIds.Stt, platformAdmin: false);
        await EnsureUserAsync(users, "admin@weerts.local", SeedIds.Weerts, platformAdmin: false);
        await EnsureUserAsync(users, "platform@sentry.local", tenantId: null, platformAdmin: true);

        if (seedDemoData && !await db.AlarmEvents.AnyAsync(cancellationToken))
        {
            SeedDemoAlarms(db);
            SeedDemoTelemetry(db);
            await db.SaveChangesAsync(cancellationToken);
        }
    }

    private static async Task EnsureUserAsync(UserManager<ApplicationUser> users, string email, Guid? tenantId, bool platformAdmin)
    {
        if (await users.FindByEmailAsync(email) is not null)
        {
            return;
        }

        var user = new ApplicationUser
        {
            UserName = email,
            Email = email,
            EmailConfirmed = true,
            TenantId = tenantId,
            IsPlatformAdmin = platformAdmin
        };

        var result = await users.CreateAsync(user, DevPassword);
        if (!result.Succeeded)
        {
            throw new InvalidOperationException($"Kon gebruiker {email} niet aanmaken: {string.Join(", ", result.Errors.Select(e => e.Description))}");
        }
    }

    private static void SeedDemoAlarms(SentryDbContext db)
    {
        var now = DateTimeOffset.UtcNow;
        AddSeries(db, SeedIds.Stubbe, SeedIds.StubbeSite, "JBT", "AGV-04", "Conv4", "E-STOP", "E-stop ingedrukt", now, days: 18, daily: 2, extraToday: 9);
        AddSeries(db, SeedIds.Stubbe, SeedIds.StubbeSite, "JBT", "AGV-12", "Dock", "BLOCK", "Pad geblokkeerd", now, days: 18, daily: 1, extraToday: 0);
        AddSeries(db, SeedIds.Stt, SeedIds.SttSite, "MFCS", "Shuttle-B2", "HB", "TIMEOUT", "Shuttle timeout", now, days: 16, daily: 3, extraToday: 1);
        AddSeries(db, SeedIds.Stt, SeedIds.SttSite, "MFCS", "Kraan-1", "ML", "FAULT", "Kraan storing", now, days: 16, daily: 1, extraToday: 0);

        db.AlarmEvents.Add(new AlarmEvent
        {
            Id = Guid.Parse("11111111-cccc-0000-0000-000000000001"),
            TenantId = SeedIds.Stubbe,
            SiteId = SeedIds.StubbeSite,
            ExternalId = "live-agv04-estop",
            Source = "JBT",
            Zone = "Conv4",
            AssetLabel = "AGV-04",
            Code = "E-STOP",
            Description = "E-stop ingedrukt",
            Severity = "high",
            State = AlarmState.Active,
            StartedAt = now.AddMinutes(-12)
        });
    }

    private static void AddSeries(
        SentryDbContext db,
        Guid tenantId,
        Guid siteId,
        string source,
        string asset,
        string zone,
        string code,
        string description,
        DateTimeOffset now,
        int days,
        int daily,
        int extraToday)
    {
        var n = 0;
        for (var d = days; d >= 0; d--)
        {
            var count = daily + (d == 0 ? extraToday : 0);
            for (var i = 0; i < count; i++)
            {
                n++;
                var started = now.Date.AddDays(-d).AddHours(6 + i);
                db.AlarmEvents.Add(new AlarmEvent
                {
                    Id = Guid.NewGuid(),
                    TenantId = tenantId,
                    SiteId = siteId,
                    ExternalId = $"{tenantId:N}-{asset}-{code}-{n}",
                    Source = source,
                    Zone = zone,
                    AssetLabel = asset,
                    Code = code,
                    Description = description,
                    Severity = "medium",
                    State = AlarmState.Cleared,
                    StartedAt = new DateTimeOffset(started, TimeSpan.Zero),
                    EndedAt = new DateTimeOffset(started.AddMinutes(8 + i), TimeSpan.Zero)
                });
            }
        }
    }

    private static void SeedDemoTelemetry(SentryDbContext db)
    {
        var now = DateTimeOffset.UtcNow;
        var weerts = new (string Label, double Pct, string Status)[]
        {
            ("SH-01", 82, "idle"),
            ("SH-02", 61, "busy"),
            ("SH-03", 17, "idle"),
            ("SH-04", 44, "busy"),
            ("SH-05", 93, "idle")
        };

        foreach (var (label, pct, status) in weerts)
        {
            db.AssetTelemetry.Add(new AssetTelemetry
            {
                Id = Guid.NewGuid(),
                TenantId = SeedIds.Weerts,
                SiteId = SeedIds.WeertsSite,
                AssetLabel = label,
                BatteryPercent = pct,
                Status = status,
                RecordedAt = now.AddMinutes(-3)
            });
        }

        db.AssetTelemetry.Add(new AssetTelemetry
        {
            Id = Guid.NewGuid(),
            TenantId = SeedIds.Stubbe,
            SiteId = SeedIds.StubbeSite,
            AssetLabel = "AGV-04",
            BatteryPercent = 38,
            Status = "busy",
            RecordedAt = now.AddMinutes(-2)
        });
        db.AssetTelemetry.Add(new AssetTelemetry
        {
            Id = Guid.NewGuid(),
            TenantId = SeedIds.Stubbe,
            SiteId = SeedIds.StubbeSite,
            AssetLabel = "AGV-12",
            BatteryPercent = 71,
            Status = "idle",
            RecordedAt = now.AddMinutes(-2)
        });
    }
}
