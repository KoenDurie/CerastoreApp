namespace Sentry.Portal.Data.Entities;

public sealed class Asset
{
    public Guid Id { get; set; }
    public Guid TenantId { get; set; }
    public Tenant Tenant { get; set; } = null!;
    public Guid? SiteId { get; set; }
    public Site? Site { get; set; }
    public string ExternalId { get; set; } = "";
    public string Label { get; set; } = "";
    public string Kind { get; set; } = "agv";
}
