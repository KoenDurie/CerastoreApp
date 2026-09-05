namespace Sentry.Portal.Services;

public static class TenantHost
{
    public static string? SlugFromHost(string? host)
    {
        if (string.IsNullOrWhiteSpace(host))
        {
            return null;
        }

        var name = host.Split(':')[0].ToLowerInvariant();
        if (name is "localhost" or "127.0.0.1" or "::1")
        {
            return null;
        }

        var first = name.Split('.')[0];
        return string.IsNullOrWhiteSpace(first) ? null : first;
    }
}
