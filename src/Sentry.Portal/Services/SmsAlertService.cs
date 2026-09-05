using Amazon;
using Amazon.SimpleNotificationService;
using Amazon.SimpleNotificationService.Model;
using Microsoft.EntityFrameworkCore;
using Sentry.Portal.Data;
using Sentry.Portal.Data.Entities;

namespace Sentry.Portal.Services;

public sealed class SmsAlertService(SentryDbContext db, IConfiguration config, ILogger<SmsAlertService> logger)
{
    public async Task EvaluateBatteryAsync(Guid tenantId, CancellationToken cancellationToken = default)
    {
        var rule = await db.AlertRules.FirstOrDefaultAsync(
            r => r.TenantId == tenantId && r.Kind == "Battery" && r.Enabled,
            cancellationToken);
        if (rule is null)
        {
            return;
        }

        var rows = await db.AssetTelemetry
            .Where(t => t.TenantId == tenantId)
            .ToListAsync(cancellationToken);
        var latest = rows
            .GroupBy(t => t.AssetLabel)
            .Select(g => g.OrderByDescending(x => x.RecordedAt).First())
            .ToList();

        var low = latest.Where(t => t.BatteryPercent is not null && t.BatteryPercent < rule.Threshold).ToList();
        var recoverAbove = rule.Threshold + rule.Hysteresis;

        if (low.Count > 0 && !rule.IsTriggered)
        {
            var message = $"Sentry batterij: {low.Count} asset(s) onder {rule.Threshold:0}% — "
                          + string.Join(", ", low.Select(l => $"{l.AssetLabel} {l.BatteryPercent:0}%"));
            await NotifyAsync(tenantId, message, cancellationToken);
            rule.IsTriggered = true;
            await db.SaveChangesAsync(cancellationToken);
        }
        else if (rule.IsTriggered && low.Count == 0 && latest.All(t => t.BatteryPercent is null || t.BatteryPercent >= recoverAbove))
        {
            rule.IsTriggered = false;
            await db.SaveChangesAsync(cancellationToken);
        }
    }

    private async Task NotifyAsync(Guid tenantId, string message, CancellationToken cancellationToken)
    {
        var contacts = await db.AlertContacts
            .Where(c => c.TenantId == tenantId && c.SmsEnabled)
            .ToListAsync(cancellationToken);

        foreach (var contact in contacts)
        {
            var status = await TrySendSmsAsync(contact.PhoneE164, message, cancellationToken);
            db.NotificationLogs.Add(new NotificationLog
            {
                Id = Guid.NewGuid(),
                TenantId = tenantId,
                Channel = "sms",
                Target = contact.PhoneE164,
                Message = message,
                SentAt = DateTimeOffset.UtcNow,
                Status = status
            });
        }

        await db.SaveChangesAsync(cancellationToken);
    }

    private async Task<string> TrySendSmsAsync(string phone, string message, CancellationToken cancellationToken)
    {
        var topic = config["Sentry:Sns:TopicArn"];
        if (string.IsNullOrWhiteSpace(topic))
        {
            logger.LogInformation("SMS (niet verzonden, geen SNS topic): {Phone} {Message}", phone, message);
            return "logged";
        }

        try
        {
            var region = config["Sentry:Sns:Region"] ?? "eu-west-1";
            using var sns = new AmazonSimpleNotificationServiceClient(RegionEndpoint.GetBySystemName(region));
            await sns.PublishAsync(new PublishRequest
            {
                TopicArn = topic,
                Message = $"{phone}: {message}"
            }, cancellationToken);
            return "sent";
        }
        catch (Exception ex)
        {
            logger.LogWarning(ex, "SNS SMS mislukt voor {Phone}", phone);
            return "failed";
        }
    }
}
