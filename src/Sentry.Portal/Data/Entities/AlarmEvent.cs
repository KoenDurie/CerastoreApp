namespace Sentry.Portal.Data.Entities;

public enum AlarmState
{
    Active = 0,
    Cleared = 1,
    Resolved = 2
}

public sealed class AlarmEvent
{
    public Guid Id { get; set; }
    public Guid TenantId { get; set; }
    public Tenant Tenant { get; set; } = null!;
    public Guid SiteId { get; set; }
    public Site Site { get; set; } = null!;
    public string ExternalId { get; set; } = "";
    public string Source { get; set; } = "";
    public string? Zone { get; set; }
    public string? AssetLabel { get; set; }
    public string? Code { get; set; }
    public string Description { get; set; } = "";
    public string? Details { get; set; }
    public string? Severity { get; set; }
    public AlarmState State { get; set; }
    public DateTimeOffset StartedAt { get; set; }
    public DateTimeOffset? EndedAt { get; set; }
    public DateTimeOffset? ConfirmedAt { get; set; }
    public string? PayloadJson { get; set; }
}
