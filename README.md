# AGV Alarm

Android-app (Kotlin, Jetpack Compose, Material 3) voor **Koen Durie / KDR Engineering**.
De app houdt een MQTT-verbinding open en moet Koen op zijn telefoon verwittigen wanneer een Stubbe-AGV (JBT, site Stubbe Zonnebeke) in storing gaat.

AGV-telemetry zit **niet** op MQTT (dat is SNMP/SQL). Alarmtopics voor de AGV-vloot bestaan nog niet. Deze versie verbindt al, blijft op de achtergrond verbonden, toont live MQTT-verkeer (`inventory/#`, `quality/status`), en is klaar voor toekomstige AGV-alarmtopics.

Package: `be.kdr.agvalarm`

## Brokers

| Omgeving | Host | Poort | Auth |
| --- | --- | --- | --- |
| Stubbe (live, voorrang) | `10.0.0.20` | 1883 | anoniem, geen TLS, MQTT 3.1.1 |
| Thuis (Mosquitto 2.0.21 op PC-KDR) | `192.168.0.239` | 1883 | anoniem, geen TLS |

Keepalive 30 s, clean session, QoS 1, connect-timeout 10 s, automatische reconnect met backoff. ClientId: `KDR_Android_<shortDeviceId>`.

**Thuis-Mosquitto bindt nu alleen localhost** (`127.0.0.1:1883`). De telefoon kan pas verbinden na `listener 1883 0.0.0.0` in Mosquitto én een Windows-firewallregel voor poort 1883. De app blijft `192.168.0.239:1883` als thuisbroker gebruiken en toont een Nederlandse hint als die probe faalt.

## Dual-broker (auto-switch)

- Thuis-wifi `telenet-7E9C4`: behandel als thuis; `10.0.0.20` wordt niet verwacht tenzij er een VPN actief is.
- Anders: TCP-probe naar `10.0.0.20:1883` (~1,5 s). Bereikbaar → Stubbe. Anders → `192.168.0.239:1883`.
- Optionele Stubbe-SSID in Instellingen (leeg tot die bekend is). Match → Stubbe.
- `ConnectivityManager.NetworkCallback` + Wi-Fi-listener, plus 5 s-probe zolang offline of op de thuisbroker.
- De app **joint geen wifi**.

## Topics (vandaag)

Standaard subscribe (QoS 1), geen publish:

- `inventory/#` — retained JSON `{locationName, productName, color, textColor}` (o.a. `inventory/SnijLijn`, `inventory/CONV1`, `inventory/401`)
- `quality/status` — Fanuc **kwaliteitsrobot** (niet de AGV-vloot). Velden o.a. `error`, `robotInError`, `robotActiveAlarmsSummaryDisplay`, `operationMode`

Niet abonneren als commander: `quality/robot/cmd` / `quality/robot/ack` (geen retain, geen publish). Extra topicfilter in Instellingen (leeg of `#` voor discovery).

`quality/status` met `error` / `robotInError` / `alarm` geeft een melding **Kwaliteitsrobot storing**. Toekomstige AGV-alarmtopics blijven via dezelfde classifier werken.

## Debug-APK bouwen

Vereisten: JDK 17, Android SDK 35.

```bash
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Eerste gebruik

1. Open **AGV Alarm**. De app vraagt om meldingen (Android 13+) en start een voorgronddienst.
2. **Thuis:** host staat op `192.168.0.239`. Open Mosquitto op `0.0.0.0:1883` en firewallpoort 1883, anders blijft de probe falen.
3. **Op Stubbe:** wifi + bereikbaar `10.0.0.20` → automatische switch.
4. Live log toont inventory- en quality-berichten. AGV-storingen komen later via MQTT (nu SNMP/SQL).

Geen SQL-credentials of andere geheimen in deze repo, alleen de LAN-hosts hierboven.
