namespace Sentry.Collector;

public sealed class CollectorOptions
{
    public const string SectionName = "Collector";

    public string PortalUrl { get; set; } = "http://localhost:5252";
    public string ApiKey { get; set; } = "sentry-dev-stubbe-site";
    public int PollIntervalSeconds { get; set; } = 30;

    /// <summary>Mock (geen fabrieks-SQL), Gla of Jbt.</summary>
    public string Mode { get; set; } = "Mock";

    public SqlOptions Sql { get; set; } = new();
}

public sealed class SqlOptions
{
    public string ConnectionString { get; set; } = "";
    public string TelemetryQuery { get; set; } = "";
    public string AlarmQuery { get; set; } = "";
}
