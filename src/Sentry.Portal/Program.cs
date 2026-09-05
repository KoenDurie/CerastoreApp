using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using Sentry.Portal.Api;
using Sentry.Portal.Components;
using Sentry.Portal.Data;
using Sentry.Portal.Data.Entities;
using Sentry.Portal.Services;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddRazorComponents()
    .AddInteractiveServerComponents();
builder.Services.AddCascadingAuthenticationState();
builder.Services.AddHttpContextAccessor();
builder.Services.AddAntiforgery();

var database = builder.Configuration["Sentry:Database"] ?? "Sqlite";
var connectionString = builder.Configuration.GetConnectionString("Default")
                       ?? "Data Source=sentry.db";

builder.Services.AddDbContext<SentryDbContext>(options =>
{
    if (string.Equals(database, "Postgres", StringComparison.OrdinalIgnoreCase))
    {
        options.UseNpgsql(connectionString);
    }
    else
    {
        options.UseSqlite(connectionString);
    }
});

builder.Services.AddIdentity<ApplicationUser, IdentityRole>(options =>
    {
        options.Password.RequiredLength = 8;
        options.Password.RequireNonAlphanumeric = false;
        options.SignIn.RequireConfirmedAccount = false;
        options.User.RequireUniqueEmail = true;
    })
    .AddEntityFrameworkStores<SentryDbContext>()
    .AddDefaultTokenProviders();

builder.Services.ConfigureApplicationCookie(options =>
{
    options.LoginPath = "/login";
    options.AccessDeniedPath = "/login";
});

builder.Services.AddScoped<TenantContext>();
builder.Services.AddScoped<IngestService>();
builder.Services.AddScoped<AlarmQueryService>();
builder.Services.AddScoped<SmsAlertService>();

var app = builder.Build();

var seedDemo = app.Configuration.GetValue("Sentry:SeedDemoData", true);
await SeedData.EnsureAsync(app.Services, seedDemo);

if (!app.Environment.IsDevelopment())
{
    app.UseExceptionHandler("/Error", createScopeForErrors: true);
    app.UseHsts();
}

if (!app.Environment.IsEnvironment("Testing"))
{
    app.UseHttpsRedirection();
}

app.UseStaticFiles();
app.UseAuthentication();
app.UseAuthorization();
app.UseMiddleware<TenantResolverMiddleware>();
app.UseAntiforgery();

app.MapPost("/account/login", async (
    HttpContext http,
    SignInManager<ApplicationUser> signIn,
    UserManager<ApplicationUser> users,
    TenantContext tenant) =>
{
    var form = await http.Request.ReadFormAsync();
    var email = form["email"].ToString();
    var password = form["password"].ToString();
    var returnUrl = form["returnUrl"].ToString();
    if (string.IsNullOrWhiteSpace(returnUrl) || !returnUrl.StartsWith('/'))
    {
        returnUrl = "/";
    }

    var user = await users.FindByEmailAsync(email);
    if (user is null)
    {
        return Results.Redirect("/login?error=1");
    }

    if (!user.IsPlatformAdmin && tenant.Current is not null && user.TenantId != tenant.Current.Id)
    {
        return Results.Redirect("/login?error=tenant");
    }

    var result = await signIn.PasswordSignInAsync(user, password, isPersistent: true, lockoutOnFailure: false);
    return result.Succeeded
        ? Results.Redirect(returnUrl)
        : Results.Redirect("/login?error=1");
});

app.MapPost("/account/logout", async (SignInManager<ApplicationUser> signIn) =>
{
    await signIn.SignOutAsync();
    return Results.Redirect("/login");
});

app.MapIngestApi();

app.MapRazorComponents<App>()
    .AddInteractiveServerRenderMode();

app.Run();

public partial class Program;