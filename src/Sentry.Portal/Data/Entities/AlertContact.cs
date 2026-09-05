namespace Sentry.Portal.Data.Entities;

public sealed class AlertContact
{
    public Guid Id { get; set; }
    public Guid TenantId { get; set; }
    public Tenant Tenant { get; set; } = null!;
    public string Name { get; set; } = "";
    public string PhoneE164 { get; set; } = "";
    public bool SmsEnabled { get; set; } = true;
}
