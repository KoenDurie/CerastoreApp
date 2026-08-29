# AGV Alarm

Android-app (Kotlin, Jetpack Compose, Material 3) voor **Koen Durie / KDR Engineering**.
De app houdt een MQTT-verbinding open en moet Koen op zijn telefoon verwittigen wanneer een Stubbe-AGV (JBT, site Stubbe Zonnebeke) in storing gaat.

Alarmtopics bestaan **nog niet** op de broker. Deze versie verbindt al, blijft op de achtergrond verbonden, toont live MQTT-verkeer, en is klaar om meldingen te sturen zodra alarmpayloads verschijnen.

Package: `be.kdr.agvalarm`

## Brokers

| Omgeving | Host | Poort |
| --- | --- | --- |
| Stubbe (live, voorrang) | `10.0.0.20` | 1883 |
| Thuis | `PC-KDR` (of een LAN-IP) | 1883 |

Standaard **anoniem**, geen TLS. Gebruikersnaam/wachtwoord in Instellingen worden alleen gebruikt als ze ingevuld zijn. Keepalive 30 s, automatische reconnect met backoff.

## Dual-broker (auto-switch)

Zodra de telefoon op het Stubbe-wifi zit en `10.0.0.20:1883` bereikbaar is, schakelt de app **direct** naar de live broker.

- `ConnectivityManager.NetworkCallback` + Wi-Fi-statelistener.
- Bij elke netwerkwijziging, en elke 5 seconden zolang de app offline is of op de thuisbroker zit: korte TCP-probe naar `10.0.0.20:1883` (timeout ~1,5 s).
- Bereikbaar → Stubbe. Anders → geconfigureerde thuisbroker.
- Optioneel: Stubbe-SSID in Instellingen. Komt die overeen met het huidige netwerk, dan is het Stubbe en wordt de thuisbroker overgeslagen.
- De app **joint geen wifi** (Android laat dat niet stilzwijgend toe). Als SSID + wachtwoord ingevuld zijn, registreert de app een `WifiNetworkSuggestion` zodat Android dat netwerk kan voorstellen.

In de UI: `Stubbe 10.0.0.20` of `Thuis …`, plus **Verbonden / Verbinden… / Offline**.

## Debug-APK bouwen

Vereisten: JDK 17, Android SDK 35.

```bash
./gradlew assembleDebug
```

De APK staat daarna op:

```
app/build/outputs/apk/debug/app-debug.apk
```

Installeren (USB-debugging of `adb` over netwerk):

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Of kopieer de APK naar de telefoon en installeer hem (bronnen buiten Play Store toestaan).

## Eerste gebruik

1. Open **AGV Alarm**. De app vraagt om meldingen (Android 13+) en start een voorgronddienst zodat MQTT blijft lopen met het scherm uit.
2. **Thuis:** als `PC-KDR` niet resolve’t (Android kent vaak geen NetBIOS-namen), zet in Instellingen het **LAN-IP van je PC** als thuis-host, poort 1883. Mosquitto moet luisteren op `0.0.0.0:1883` (niet alleen localhost) en de Windows-firewall moet poort 1883 toelaten.
3. **Op Stubbe:** verbind met het Stubbewifi. De app probeert `10.0.0.20` en schakelt automatisch.
4. Topicfilter staat standaard op `#` zodat bestaande topics al zichtbaar zijn.
5. Alarmtopics komen later. Berichten waarvan topic/payload op een storing lijkt (`alarm`, `error`, `fault`, `storing`, of JSON `state`/`status`/`severity`) geven een melding in het kanaal **AGV storing**.

## Wat de app doet

- HiveMQ MQTT-client, clientId `agv-alarm-<androidId>`.
- Subscribe na CONNACK, QoS 1.
- Persistente statusmelding: *AGV Alarm · Verbonden met Stubbe / Thuis / Offline*.
- Hoog-prioriteit kanaal **AGV storing** (geluid, tril, heads-up).
- Deduplicatie: zelfde topic + payload binnen 60 s = één melding.
- Instellingen via DataStore (blijven bewaard).

Geen SQL-credentials of andere geheimen in deze repo, alleen de LAN-hosts hierboven.
