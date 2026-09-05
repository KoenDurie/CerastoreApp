namespace Sentry.Portal.Data;

public static class SeedIds
{
    public static readonly Guid Stubbe = Guid.Parse("11111111-1111-1111-1111-111111111111");
    public static readonly Guid Stt = Guid.Parse("22222222-2222-2222-2222-222222222222");
    public static readonly Guid Weerts = Guid.Parse("33333333-3333-3333-3333-333333333333");

    public static readonly Guid StubbeSite = Guid.Parse("11111111-0000-0000-0000-000000000001");
    public static readonly Guid SttSite = Guid.Parse("22222222-0000-0000-0000-000000000001");
    public static readonly Guid WeertsSite = Guid.Parse("33333333-0000-0000-0000-000000000001");

    public const string StubbeApiKey = "sentry-dev-stubbe-site";
    public const string SttApiKey = "sentry-dev-stt-site";
    public const string WeertsApiKey = "sentry-dev-weerts-site";
}
