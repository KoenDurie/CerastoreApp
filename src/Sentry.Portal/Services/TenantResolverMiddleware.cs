using Microsoft.EntityFrameworkCore;
using Sentry.Portal.Data;

namespace Sentry.Portal.Services;

public sealed class TenantResolverMiddleware(RequestDelegate next)
{
    public async Task InvokeAsync(HttpContext context, TenantContext tenantContext, SentryDbContext db, IConfiguration config)
    {
        var slug = context.Request.Headers["X-Tenant"].FirstOrDefault()
            ?? TenantHost.SlugFromHost(context.Request.Host.Host)
            ?? config["Sentry:DefaultTenantSlug"];

        if (!string.IsNullOrWhiteSpace(slug))
        {
            tenantContext.Current = await db.Tenants.AsNoTracking()
                .FirstOrDefaultAsync(t => t.Slug == slug.ToLowerInvariant());
        }

        await next(context);
    }
}
