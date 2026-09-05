# Sentry

Nieuw overlay-portaal voor fabrieksalarmen, batterijen en rapporten. **Geen WCS.** Bestaande programma’s (Cerastore-server/client, ShuttleMonitor, AlarmAnomaly.Web, de huidige Android-app) blijven onaangeroerd.

Naam is tijdelijk (botst met sentry.io).

## Solution

- `Sentry.Portal` — Blazor Server (.NET 8), login, tenant uit hostname
- `Sentry.Collector` — nieuwe Windows-worker, outbound HTTPS, optioneel read-only SQL
- `Sentry.Portal.Tests` — anomalie-score, ingest-upsert, tenant-isolatie

## Lokaal (zonder Docker)

```bash
dotnet run --project src/Sentry.Portal
```

Open http://localhost:5252. Standaardtenant is Stubbe (`Sentry:DefaultTenantSlug`). Andere klant: header `X-Tenant: stt` of `weerts`.

Demo-accounts (wachtwoord `SentryDev!234`):

| E-mail | Tenant |
| --- | --- |
| admin@stubbe.local | Stubbe (alarmen + batterij) |
| admin@stt.local | STT (alarmen) |
| admin@weerts.local | Weerts (batterij + SMS) |
| platform@sentry.local | alle hosts |

Collector (mock, geen fabrieks-SQL):

```bash
dotnet run --project src/Sentry.Collector
```

## Ingest

| Endpoint | Header |
| --- | --- |
| `POST /api/ingest/alarms` | `X-Api-Key` |
| `POST /api/ingest/telemetry` | `X-Api-Key` |
| `POST /api/ingest/heartbeat` | `X-Api-Key` |

Dev-keys: `sentry-dev-stubbe-site`, `sentry-dev-stt-site`, `sentry-dev-weerts-site`.

Upsert-sleutel: `(tenant_id, site_id, external_id)`.

Anomalie: `score = (recentCount - baselineAvg) / (stdDev + 1)`, drempel `3.0`.

## Docker / Lightsail

`docker compose up --build` start PostgreSQL, het portaal op :8080 en Caddy op :80 (`stubbe.localhost`, …). Zie [deploy/lightsail.md](deploy/lightsail.md). SNS-topic is optioneel; zonder topic worden SMS-alerts alleen gelogd.

## Wat dit project niet doet

- Geen wijzigingen in Cerastore, MQTT of WPF
- Geen IAM-keys in de collector
- Geen STT-inventaris, Conv4 of analoge graphic in v1
- Geen Android/FCM in deze slice
