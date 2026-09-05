using Sentry.Portal.Data.Entities;

namespace Sentry.Portal.Services;

public sealed class TenantContext
{
    public Tenant? Current { get; set; }

    public Guid RequireTenantId()
        => Current?.Id ?? throw new InvalidOperationException("Geen tenant op deze host.");
}
