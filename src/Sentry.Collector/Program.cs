using Microsoft.Extensions.Options;
using Sentry.Collector;
using Sentry.Collector.Adapters;

var builder = Host.CreateApplicationBuilder(args);

builder.Services.Configure<CollectorOptions>(builder.Configuration.GetSection(CollectorOptions.SectionName));

builder.Services.AddHttpClient<PortalClient>((sp, client) =>
{
    var cfg = sp.GetRequiredService<IOptions<CollectorOptions>>().Value;
    client.BaseAddress = new Uri(cfg.PortalUrl.TrimEnd('/') + "/");
    client.DefaultRequestHeaders.Remove("X-Api-Key");
    client.DefaultRequestHeaders.Add("X-Api-Key", cfg.ApiKey);
});

builder.Services.AddSingleton<IFactoryAdapter>(sp =>
{
    var cfg = sp.GetRequiredService<IOptions<CollectorOptions>>().Value;
    return string.Equals(cfg.Mode, "Mock", StringComparison.OrdinalIgnoreCase)
        ? new MockAdapter()
        : new SqlAdapter(cfg, sp.GetRequiredService<ILogger<SqlAdapter>>());
});

builder.Services.AddHostedService<Worker>();

var host = builder.Build();
host.Run();
