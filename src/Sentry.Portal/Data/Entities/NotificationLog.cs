namespace Sentry.Portal.Data.Entities;

public sealed class NotificationLog
{
    public Guid Id { get; set; }
    public Guid TenantId { get; set; }
    public Tenant Tenant { get; set; } = null!;
    public string Channel { get; set; } = "sms";
    public string Target { get; set; } = "";
    public string Message { get; set; } = "";
    public DateTime SentAt { get; set; }
    public string Status { get; set; } = "logged";
}
