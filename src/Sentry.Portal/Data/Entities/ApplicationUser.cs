using Microsoft.AspNetCore.Identity;

namespace Sentry.Portal.Data.Entities;

public sealed class ApplicationUser : IdentityUser
{
    public Guid? TenantId { get; set; }
    public Tenant? Tenant { get; set; }
    public bool IsPlatformAdmin { get; set; }
}
