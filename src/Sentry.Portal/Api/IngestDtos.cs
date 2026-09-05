namespace Sentry.Portal.Api;

public sealed class AlarmBatchRequest
{
    public List<AlarmIngestItem> Alarms { get; set; } = [];
}

public sealed class AlarmIngestItem
{
    public string ExternalId { get; set; } = "";
    public string Source { get; set; } = "";
    public string? Zone { get; set; }
    public string? AssetLabel { get; set; }
    public string? Code { get; set; }
    public string Description { get; set; } = "";
    public string? Details { get; set; }
    public string? Severity { get; set; }
    public string State { get; set; } = "active";
    public DateTimeOffset StartedAt { get; set; }
    public DateTimeOffset? EndedAt { get; set; }
    public string? PayloadJson { get; set; }
}

public sealed class TelemetryBatchRequest
{
    public List<TelemetryIngestItem> Readings { get; set; } = [];
}

public sealed class TelemetryIngestItem
{
    public string AssetLabel { get; set; } = "";
    public double? BatteryPercent { get; set; }
    public string? Status { get; set; }
    public DateTimeOffset RecordedAt { get; set; }
    public string? PayloadJson { get; set; }
}

public sealed class HeartbeatRequest
{
    public string? CollectorVersion { get; set; }
}

public sealed class IngestResult
{
    public int Accepted { get; set; }
    public int Updated { get; set; }
}
