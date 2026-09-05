namespace Sentry.Portal.Data.Entities;

public sealed class AssetTelemetry
{
    public Guid Id { get; set; }
    public Guid TenantId { get; set; }
    public Tenant Tenant { get; set; } = null!;
    public Guid SiteId { get; set; }
    public Site Site { get; set; } = null!;
    public string AssetLabel { get; set; } = "";
    public double? BatteryPercent { get; set; }
    public string? Status { get; set; }
    public DateTimeOffset RecordedAt { get; set; }
    public string? PayloadJson { get; set; }
}
