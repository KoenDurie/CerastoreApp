using Microsoft.AspNetCore.Identity.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore;
using Sentry.Portal.Data.Entities;

namespace Sentry.Portal.Data;

public sealed class SentryDbContext : IdentityDbContext<ApplicationUser>
{
    public SentryDbContext(DbContextOptions<SentryDbContext> options) : base(options)
    {
    }

    public DbSet<Tenant> Tenants => Set<Tenant>();
    public DbSet<Site> Sites => Set<Site>();
    public DbSet<Asset> Assets => Set<Asset>();
    public DbSet<AlarmEvent> AlarmEvents => Set<AlarmEvent>();
    public DbSet<AssetTelemetry> AssetTelemetry => Set<AssetTelemetry>();
    public DbSet<AlertRule> AlertRules => Set<AlertRule>();
    public DbSet<AlertContact> AlertContacts => Set<AlertContact>();
    public DbSet<NotificationLog> NotificationLogs => Set<NotificationLog>();

    protected override void OnModelCreating(ModelBuilder builder)
    {
        base.OnModelCreating(builder);

        builder.Entity<Tenant>(e =>
        {
            e.HasIndex(x => x.Slug).IsUnique();
            e.Property(x => x.Slug).HasMaxLength(64).IsRequired();
            e.Property(x => x.Name).HasMaxLength(200).IsRequired();
        });

        builder.Entity<Site>(e =>
        {
            e.HasIndex(x => x.ApiKey).IsUnique();
            e.Property(x => x.ApiKey).HasMaxLength(128).IsRequired();
            e.HasOne(x => x.Tenant).WithMany(t => t.Sites).HasForeignKey(x => x.TenantId);
        });

        builder.Entity<Asset>(e =>
        {
            e.HasIndex(x => new { x.TenantId, x.ExternalId }).IsUnique();
            e.HasOne(x => x.Tenant).WithMany(t => t.Assets).HasForeignKey(x => x.TenantId);
        });

        builder.Entity<AlarmEvent>(e =>
        {
            e.ToTable("alarm_events");
            e.HasIndex(x => new { x.TenantId, x.SiteId, x.ExternalId }).IsUnique();
            e.HasIndex(x => new { x.TenantId, x.StartedAt });
            e.HasIndex(x => new { x.TenantId, x.State });
            e.HasIndex(x => new { x.TenantId, x.AssetLabel, x.StartedAt });
            e.Property(x => x.ExternalId).HasMaxLength(200).IsRequired();
            e.Property(x => x.Description).HasMaxLength(1000).IsRequired();
        });

        builder.Entity<AssetTelemetry>(e =>
        {
            e.ToTable("asset_telemetry");
            e.HasIndex(x => new { x.TenantId, x.AssetLabel, x.RecordedAt });
        });

        builder.Entity<ApplicationUser>(e =>
        {
            e.HasOne(x => x.Tenant).WithMany(t => t.Users).HasForeignKey(x => x.TenantId);
        });
    }
}
