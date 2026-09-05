namespace Sentry.Portal.Data.Entities;

public sealed class Tenant
{
    public Guid Id { get; set; }
    public string Slug { get; set; } = "";
    public string Name { get; set; } = "";
    public string TimeZoneId { get; set; } = "Europe/Brussels";
    public bool FeatureAlarms { get; set; }
    public bool FeatureBattery { get; set; }
    public bool FeatureMobileApp { get; set; }

    public ICollection<Site> Sites { get; set; } = new List<Site>();
    public ICollection<Asset> Assets { get; set; } = new List<Asset>();
    public ICollection<ApplicationUser> Users { get; set; } = new List<ApplicationUser>();
}
