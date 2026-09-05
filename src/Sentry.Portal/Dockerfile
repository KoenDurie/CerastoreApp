FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build
WORKDIR /src
COPY src/Sentry.Portal/Sentry.Portal.csproj src/Sentry.Portal/
RUN dotnet restore src/Sentry.Portal/Sentry.Portal.csproj
COPY src/Sentry.Portal/ src/Sentry.Portal/
RUN dotnet publish src/Sentry.Portal/Sentry.Portal.csproj -c Release -o /app/publish

FROM mcr.microsoft.com/dotnet/aspnet:8.0
WORKDIR /app
COPY --from=build /app/publish .
ENV ASPNETCORE_URLS=http://+:8080
EXPOSE 8080
ENTRYPOINT ["dotnet", "Sentry.Portal.dll"]
