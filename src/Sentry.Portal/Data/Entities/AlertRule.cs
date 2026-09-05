namespace Sentry.Portal.Data.Entities;

public sealed class AlertRule
{
    public Guid Id { get; set; }
    public Guid TenantId { get; set; }
    public Tenant Tenant { get; set; } = null!;
    public string Kind { get; set; } = "Battery";
    public double Threshold { get; set; } = 20;
    public double Hysteresis { get; set; } = 5;
    public bool Enabled { get; set; } = true;
    public bool IsTriggered { get; set; }
}
