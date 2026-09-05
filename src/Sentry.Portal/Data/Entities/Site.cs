namespace Sentry.Portal.Data.Entities;

public sealed class Site
{
    public Guid Id { get; set; }
    public Guid TenantId { get; set; }
    public Tenant Tenant { get; set; } = null!;
    public string Name { get; set; } = "";
    public string ApiKey { get; set; } = "";
    public string SourceKind { get; set; } = "Unknown";
    public DateTimeOffset? LastHeartbeatAt { get; set; }
}
