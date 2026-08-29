# AGV Alarm

Android-app (Kotlin, Jetpack Compose, Material 3) voor **Koen Durie / KDR Engineering**.
De app houdt een MQTT-verbinding open en moet Koen op zijn telefoon verwittigen wanneer een Stubbe-AGV (JBT, site Stubbe Zonnebeke) in storing gaat.

AGV-telemetry zit **niet** in deze app (geen SQL/JDBC). Een sidecar op PC-KDR publiceert retained MQTT op `stubbe/agv/{id}/alarm` wanneer een AGV in storing is. De app toont raw MQTT én een **popup + heads-up** bij `alarm:true`.

Package: `be.kdr.agvalarm`

## Brokers

| Omgeving | Host | Poort | Auth |
| --- | --- | --- | --- |
| Stubbe (live, voorrang) | `10.0.0.20` | 1883 | anoniem, geen TLS, MQTT 3.1.1 |
| Thuis (Mosquitto 2.0.21 op PC-KDR) | `192.168.0.239` | 1883 | anoniem, geen TLS |

Keepalive 30 s, clean session, QoS 1, connect-timeout 10 s, automatische reconnect met backoff. ClientId: `KDR_Android_<shortDeviceId>`.

Thuis-Mosquitto luistert op `0.0.0.0:1883` (anoniem, Windows-firewallregel "Mosquitto MQTT 1883"). Als de thuisprobe toch faalt, toont de app een generieke hint (PC uit, verkeerde wifi, Mosquitto niet actief) — niet meer dat de listener alleen op localhost zou staan.

## Dual-broker (auto-switch)

- Thuis-wifi `telenet-7E9C4`: behandel als thuis; `10.0.0.20` wordt niet verwacht tenzij er een VPN actief is.
- Anders: TCP-probe naar `10.0.0.20:1883` (~1,5 s). Bereikbaar → Stubbe. Anders → `192.168.0.239:1883`.
- Optionele Stubbe-SSID in Instellingen (leeg tot die bekend is). Match → Stubbe.
- `ConnectivityManager.NetworkCallback` + Wi-Fi-listener, plus 5 s-probe zolang offline of op de thuisbroker.
- De app **joint geen wifi**.

## Topics (vandaag)

Standaard subscribe (QoS 1), geen publish:

- `inventory/#` — retained JSON `{locationName, productName, color, textColor}`
- `quality/status` — Fanuc **kwaliteitsrobot** (niet de AGV-vloot)
- `stubbe/agv/#` — sidecar AGV-alarmen: `stubbe/agv/{vehicleId}/alarm`

Niet abonneren als commander: `quality/robot/cmd` / `quality/robot/ack`. Extra topicfilter in Instellingen (leeg of `#` voor discovery).

Actieve AGV-alarm JSON (retain, QoS 1): `{"vehicleId":7,"alarm":true,"error":true,"state":"error","message":"AGV 7 in error."}` → heads-up + dialog **AGV 7 storing**. Clear (`alarm:false`) geeft geen melding, wel “OPGELOST” in de log.

`quality/status` met `error` / `robotInError` / `alarm` geeft **Kwaliteitsrobot storing** (geen AGV-dialog). Inventory never notifies.

In Instellingen: **Test AGV-melding** (fake AGV 99 / Testmelding).

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
2. **Thuis:** host staat op `192.168.0.239`. Verbind met wifi `telenet-7E9C4`. Als de broker onbereikbaar is: PC-KDR aan, Mosquitto actief, juiste wifi.
3. **Op Stubbe:** wifi + bereikbaar `10.0.0.20` → automatische switch.
4. Live log toont inventory-, quality- en later sidecar-AGV-berichten. **Test AGV-melding** in Instellingen toont de popup. Echte AGV-storingen komen als retained `stubbe/agv/{id}/alarm` (geen SQL in de app).

Geen SQL-credentials of andere geheimen in deze repo, alleen de LAN-hosts hierboven.
