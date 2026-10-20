<p align="center">
  <a href="README.md">English</a> · <a href="README.zh-CN.md">简体中文</a> · <a href="README.zh-TW.md">繁體中文</a> · <a href="README.ja.md">日本語</a> · <a href="README.ko.md">한국어</a><br>
  <strong>Deutsch</strong> · <a href="README.es.md">Español</a> · <a href="README.fr.md">Français</a> · <a href="README.pt-BR.md">Português (Brasil)</a> · <a href="README.vi.md">Tiếng Việt</a> · <a href="README.id.md">Bahasa Indonesia</a> · <a href="README.it.md">Italiano</a>
</p>

<p align="center"><img src="assets/wifi-spectrum.png" width="112" alt="Wi-Fi-Spectrum-Symbol"></p>
<h1 align="center">Professioneller WLAN-Analysator für Android</h1>
<p align="center">Wi-Fi Spectrum (Wi-Fi 頻譜分析儀) · 2,4 / 5 / 6 GHz · 20–320 MHz · Wi-Fi 7 MLO</p>
<p align="center">
  <a href="https://github.com/tonykfr1234/android_wifi-spectrum/actions/workflows/security.yml"><img src="https://github.com/tonykfr1234/android_wifi-spectrum/actions/workflows/security.yml/badge.svg" alt="CI"></a>
  <img src="https://img.shields.io/badge/platform-Android%208%2B-3DDC84" alt="Android 8+">
  <img src="https://img.shields.io/badge/Kotlin-2.2-7F52FF" alt="Kotlin 2.2">
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4" alt="Jetpack Compose">
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-green" alt="MIT-Lizenz"></a>
</p>
<p align="center">
  <a href="#quick-start">Schnellstart</a> · <a href="#workflow">Ablauf</a> · <a href="#features">Funktionen</a> · <a href="#demo">Demo</a> · <a href="#faq">FAQ</a> · <a href="#documentation">Dokumentation</a>
</p>

```bash
./gradlew testDebugUnitTest assembleDebug
```

---

Version 1.0: alle drei Frequenzbänder, jede Kanalbreite bis 320 MHz und Wi-Fi-7-Multi-Link (MLO), gruppiert nach MLD-Adresse.

Wi-Fi Spectrum scannt Access Points in der Nähe auf 2,4, 5 und 6 GHz und zeigt, wo jeder tatsächlich sendet: den Primärkanal, den gesamten Block mit 20/40/80/160/80+80/320 MHz und alle überdeckten Kanäle. Spektrumansicht, Signalverlauf und Kanalbewertung helfen bei der Wahl eines ruhigen Kanals; die MLO-Ansicht fasst die Links jedes Wi-Fi-7-Routers zusammen.

Apple erlaubt iOS-Apps nicht, WLANs in der Nähe zu scannen. Diese Funktionen gibt es daher nur unter Android. Die iOS-Variante [IOS_wifi-spectrum](https://github.com/tonykfr1234/ios_wifi-spectrum) bietet, was iOS zulässt.

Die 12 README-Sprachen übersetzen die Dokumentation. Die App-Oberfläche ist derzeit in traditionellem Chinesisch.

<a id="quick-start"></a>
## Schnellstart

1. Lade `wifi-spectrum-debug-apk` aus dem letzten erfolgreichen [GitHub-Actions-Lauf](https://github.com/tonykfr1234/android_wifi-spectrum/actions) herunter oder baue sie mit `./gradlew assembleDebug` (JDK 17+ und Android SDK).
2. Installiere `app-debug.apk` auf einem Smartphone mit Android 8+. 6 GHz erfordert Wi-Fi 6E; 320 MHz und MLO erfordern Android 13+ mit Wi-Fi 7.
3. Öffne die App und erlaube den **genauen** Standort sowie ab Android 13 „WLAN-Geräte in der Nähe“. Ohne diese Rechte liefert Android keine Scanergebnisse.
4. Lass die Standortdienste eingeschaltet. Die App scannt alle 30 Sekunden, solange sie sichtbar ist; Aktualisieren scannt sofort.
5. Kein passendes Smartphone zur Hand? Im Demomodus lässt sich eine synthetische Wi-Fi-7-Umgebung erkunden.

**Veröffentlichung bei Google Play?** Folge [docs/PUBLISHING.zh-TW.md](docs/PUBLISHING.zh-TW.md) ([englische Zusammenfassung](docs/PUBLISHING.md)): Anwendungs-ID ändern, Upload-Schlüssel außerhalb des Projekts erstellen, signiertes App Bundle bauen und den geschlossenen Test in der Play Console abschließen.

<a id="workflow"></a>
## Ablauf

```text
App sichtbar, alle 30 Sekunden (Android erlaubt 4 Vordergrund-Scans pro 2 Minuten)
  → WifiManager.startScan(); gedrosselte Anfragen nutzen die zwischengespeicherten Ergebnisse
  → jedes ScanResult bereinigen: SSID, BSSID, Band, Primärkanal
  → belegten Block aus Breite und Mittenfrequenz berechnen (20–320 MHz, 80+80)
  → Wi-Fi-7-Links nach MLD-Adresse gruppieren; Hersteller offline nachschlagen
  → Liste, Spektrum, Verlauf über 60 Scans, Kanalbewertung und MLO aktualisieren
```

Der Scan stoppt, sobald die App den Bildschirm verlässt; im Hintergrund läuft nichts. Fehlt die Mittenfrequenz oder meldet der Treiber die 160-MHz-Mitte in `centerFreq1`, wird der Block aus dem Standard-Kanalplan abgeleitet. MLO-Felder benötigen Android 13, das Signal pro Link des verbundenen Routers Android 14.

<a id="features"></a>
## Funktionen

| Funktion | Verhalten |
| --- | --- |
| Bänder | 2,4, 5 (inkl. UNII-4) und 6 GHz; Hinweise bei Drosselung, ausgeschaltetem WLAN oder Standort |
| Kanalbreite | 20, 40, 80, 160, 80+80 und 320 MHz; belegter Bereich, Mittenkanal und überdeckte Kanäle |
| Wi-Fi 7 MLO | MLD-Adresse, Link-IDs und zugehörige Links; aktive Links und RSSI pro Link der Verbindung |
| Spektrum | Eine Parabel pro AP; Übersicht über das ganze Band mit Zoom; Links des verbundenen Routers fett |
| Verlauf | RSSI der 10 stärksten APs über 60 Scans; Lücken, wenn ein AP fehlte |
| Kanalbewertung | Gewichtet nach Signal und Überlappung; auf 2,4 GHz nur 1/6/11; Nicht-DFS bevorzugt; PSC auf 6 GHz |
| Details | Hersteller aus dem IEEE-Register, Erkennung zufälliger MACs, Verschlüsselung inkl. WPA3 SAE-EXT und OWE, Entfernungsschätzung |
| Datenschutz | Kein Netzwerkzugriff, keine Werbung, keine Analyse; Scanergebnisse bleiben im Speicher |

Die Entfernung ist eine Freiraumschätzung; Wände verkürzen sie. Scaninhalte hängen vom WLAN-Chip und Treiber des Geräts ab. Prüfe 6-GHz-, 320-MHz- und MLO-Werte mit eigener Hardware, bevor du dich darauf verlässt.

<a id="demo"></a>
## Demo

Diese 40-sekündige stumme Vorführung nutzt die **echte App-Oberfläche mit der eingebauten synthetischen Umgebung**: eine Wohnung in Taipeh mit Wi-Fi-7-Router (drei MLO-Links) und dicht belegter Nachbarschaft, mit zweisprachigen Untertiteln. Es ist kein echter Scan und sagt nichts über das Funkmodul eines bestimmten Geräts aus.

![Vorführung der App mit synthetischen Daten](docs/media/wifi-spectrum-demo.gif)

[MP4-Demo öffnen oder herunterladen](docs/media/wifi-spectrum-demo.mp4)

| 2,4-GHz-Spektrum | 6 GHz mit 320 MHz | Wi-Fi-7-MLO-Links |
| --- | --- | --- |
| ![2,4-GHz-Spektrum](docs/media/spectrum-24ghz.png) | ![6 GHz mit 320 MHz](docs/media/spectrum-6ghz.png) | ![Wi-Fi-7-MLO-Links](docs/media/mlo-links.png) |

## Neueste Änderungen

Erste Veröffentlichung: Scan auf drei Bändern, Kanalbreiten von 20–320 MHz, Wi-Fi 7 MLO, Spektrum, Verlauf, Kanalbewertung, Google-Play-Material und Veröffentlichungsanleitung, geprüft mit 214 Offline-Tests für Android 8–16. Siehe [Changelog](CHANGELOG.md).

<a id="controls"></a>
## Bedienung

| Bedienelement | Aktion |
| --- | --- |
| Aktualisieren | Sofort scannen |
| Pause / Start | Automatischen Scan anhalten oder fortsetzen |
| ⋮ → Intervall | 3, 5, 10, 30 oder 60 Sekunden; unter 30 s kann Android drosseln |
| ⋮ → Demomodus | Zwischen echtem Scan und synthetischen Daten wechseln |
| Band-/Breite-/Wi-Fi-7-Chips | Liste filtern; der Breiten-Chip wechselt ≥40/80/160/320 MHz |
| Zoom (5/6 GHz) | Zwischen Bandübersicht und scrollbarer Nahansicht wechseln |
| AP antippen | Belegten Bereich, überdeckte Kanäle, Verschlüsselung und MLO-Links anzeigen |

<a id="faq"></a>
## FAQ

**Warum braucht die App den Standort?** Android behandelt WLAN-Scanergebnisse als Standortdaten. Ohne genauen Standort (ab Android 13 auch „WLAN-Geräte in der Nähe“) liefert das System keine Ergebnisse. Die App liest keine GPS-Koordinaten.

**Warum ist 6 GHz leer?** Das Smartphone braucht Wi-Fi-6E-Unterstützung, und ein 6-GHz-Router muss in Reichweite sein.

**Warum kein MLO oder 320 MHz?** Nötig sind ein Wi-Fi-7-Smartphone mit Android 13 oder neuer und ein Wi-Fi-7-Router mit aktiviertem MLO.

**Warum heißt es, der Scan sei gedrosselt?** Android erlaubt vier Vordergrund-Scans alle zwei Minuten. Behalte das 30-Sekunden-Intervall bei oder deaktiviere beim Testen die WLAN-Scan-Drosselung in den Entwickleroptionen.

**Gibt es eine iOS-Version?** Apples Schnittstellen geben keine Netzwerke in der Nähe preis, daher sind diese Scans unter iOS unmöglich. Was iOS kann, zeigt [IOS_wifi-spectrum](https://github.com/tonykfr1234/ios_wifi-spectrum).

<a id="docs"></a>
<a id="documentation"></a>
## Dokumentation

| Thema | Dokument |
| --- | --- |
| Täglicher Betrieb | [繁中使用指南](docs/USER_GUIDE.zh-TW.md) · [English User Guide](docs/USER_GUIDE.md) |
| Interna | [Architektur](docs/ARCHITECTURE.md) |
| Veröffentlichung | [Google-Play-Anleitung (繁中)](docs/PUBLISHING.zh-TW.md) · [Englische Zusammenfassung](docs/PUBLISHING.md) · [Datenschutzerklärung](docs/PRIVACY.md) |
| Versionen und Sicherheit | [Changelog](CHANGELOG.md) · [Sicherheit](SECURITY.md) · [Mitwirken](CONTRIBUTING.md) |

Code liegt in `app/src/main/java`, Tests in `app/src/test`, Werkzeuge in `scripts/` und Google-Play-Bilder in `store/`. Die 214 Tests laufen ohne Smartphone auf der JVM; echte 6-GHz-, 320-MHz- und MLO-Werte musst du mit eigener Hardware prüfen.

## Sicherheit

Committe niemals Keystores, `keystore.properties`, `local.properties`, API-Schlüssel oder exportierte Scans fremder Netzwerke. Führe vor dem Push denselben Scanner wie GitHub Actions aus:

```bash
python scripts/security_scan.py .
```

## Lizenz

[MIT](LICENSE).

### 2026-10-20 · 1.0.0

首次發布：2.4／5／6 GHz 掃描、20–320 MHz 頻寬、Wi-Fi 7 MLO、頻譜圖、趨勢與頻道評分；214 個離線測試涵蓋 Android 8–16。

First release: 2.4/5/6 GHz scanning, 20–320 MHz widths, Wi-Fi 7 MLO, spectrum, trend and channel rating; 214 offline tests cover Android 8–16.

[Öffentliche Kurzanleitung (traditionelles Chinesisch, Text)](docs/使用說明.txt)
