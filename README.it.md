<p align="center">
  <a href="README.md">English</a> · <a href="README.zh-CN.md">简体中文</a> · <a href="README.zh-TW.md">繁體中文</a> · <a href="README.ja.md">日本語</a> · <a href="README.ko.md">한국어</a><br>
  <a href="README.de.md">Deutsch</a> · <a href="README.es.md">Español</a> · <a href="README.fr.md">Français</a> · <a href="README.pt-BR.md">Português (Brasil)</a> · <a href="README.vi.md">Tiếng Việt</a> · <a href="README.id.md">Bahasa Indonesia</a> · <strong>Italiano</strong>
</p>

<p align="center"><img src="assets/wifi-spectrum.png" width="112" alt="Icona di Wi-Fi Spectrum"></p>
<h1 align="center">Analizzatore Wi-Fi professionale per Android</h1>
<p align="center">Wi-Fi Spectrum (Wi-Fi 頻譜分析儀) · 2,4 / 5 / 6 GHz · 20–320 MHz · Wi-Fi 7 MLO</p>
<p align="center">
  <a href="https://github.com/tonykfr1234/android_wifi-spectrum/actions/workflows/security.yml"><img src="https://github.com/tonykfr1234/android_wifi-spectrum/actions/workflows/security.yml/badge.svg" alt="CI"></a>
  <img src="https://img.shields.io/badge/platform-Android%208%2B-3DDC84" alt="Android 8+">
  <img src="https://img.shields.io/badge/Kotlin-2.2-7F52FF" alt="Kotlin 2.2">
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4" alt="Jetpack Compose">
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-green" alt="Licenza MIT"></a>
</p>
<p align="center">
  <a href="#quick-start">Avvio rapido</a> · <a href="#workflow">Funzionamento</a> · <a href="#features">Funzionalità</a> · <a href="#demo">Demo</a> · <a href="#faq">FAQ</a> · <a href="#documentation">Documentazione</a>
</p>

```bash
./gradlew testDebugUnitTest assembleDebug
```

---

Versione 1.0: tutte e tre le bande, ogni larghezza di canale fino a 320 MHz e multi-link Wi-Fi 7 (MLO) raggruppato per indirizzo MLD.

Wi-Fi Spectrum analizza gli access point vicini su 2,4, 5 e 6 GHz e mostra dove ciascuno trasmette davvero: il canale primario, l'intero blocco da 20/40/80/160/80+80/320 MHz e tutti i canali coperti. Vista spettro, andamento del segnale e valutazione dei canali aiutano a scegliere un canale libero; la vista MLO raggruppa i link di ogni router Wi-Fi 7.

Apple non consente alle app iOS di analizzare le reti Wi-Fi vicine, quindi queste funzioni sono solo per Android. La versione iOS, [IOS_wifi-spectrum](https://github.com/tonykfr1234/ios_wifi-spectrum), offre ciò che iOS permette.

Le 12 lingue del README traducono la documentazione. L'interfaccia dell'app è attualmente in cinese tradizionale.

<a id="quick-start"></a>
## Avvio rapido

1. Scarica `wifi-spectrum-debug-apk` dall'ultima [esecuzione riuscita di GitHub Actions](https://github.com/tonykfr1234/android_wifi-spectrum/actions) oppure compila con `./gradlew assembleDebug` (JDK 17+ e Android SDK).
2. Installa `app-debug.apk` su uno smartphone Android 8+. I 6 GHz richiedono Wi-Fi 6E; 320 MHz e MLO richiedono Android 13+ con Wi-Fi 7.
3. Apri l'app e consenti la posizione **precisa** e, su Android 13+, «Dispositivi Wi-Fi nelle vicinanze». Senza questi permessi Android non restituisce risultati.
4. Tieni attiva la localizzazione. L'app esegue una scansione ogni 30 secondi quando è visibile; tocca aggiorna per scansionare subito.
5. Nessuno smartphone adatto? Usa la modalità demo per esplorare un ambiente Wi-Fi 7 sintetico.

**Pubblicare su Google Play?** Segui [docs/PUBLISHING.zh-TW.md](docs/PUBLISHING.zh-TW.md) ([riepilogo in inglese](docs/PUBLISHING.md)): cambia l'ID dell'applicazione, crea una chiave di caricamento fuori dal progetto, genera l'App Bundle firmato e completa il test chiuso della Play Console.

<a id="workflow"></a>
## Funzionamento

```text
App visibile, ogni 30 secondi (Android consente 4 scansioni in primo piano ogni 2 minuti)
  → WifiManager.startScan(); le richieste limitate riutilizzano i risultati in cache
  → ripulire ogni ScanResult: SSID, BSSID, banda, canale primario
  → blocco occupato da larghezza e frequenza centrale (20–320 MHz, 80+80)
  → raggruppare i link Wi-Fi 7 per indirizzo MLD; produttore cercato offline
  → aggiornare elenco, spettro, andamento su 60 scansioni, valutazione e MLO
```

La scansione si ferma quando l'app lascia lo schermo; nulla gira in background. Se il driver omette la frequenza centrale o riporta il centro a 160 MHz in `centerFreq1`, il blocco viene ricavato dal piano dei canali standard. I campi MLO richiedono Android 13; il segnale per link del router connesso, Android 14.

<a id="features"></a>
## Funzionalità

| Funzione | Comportamento |
| --- | --- |
| Bande | 2,4, 5 (incl. UNII-4) e 6 GHz; avvisi di limitazione, Wi-Fi spento e posizione disattivata |
| Larghezza di canale | 20, 40, 80, 160, 80+80 e 320 MHz; intervallo occupato, canale centrale e canali coperti |
| Wi-Fi 7 MLO | Indirizzo MLD, ID dei link e link affiliati; link attivi e RSSI per link della connessione |
| Spettro | Una parabola per AP; panoramica dell'intera banda con zoom; link del router connesso in grassetto |
| Andamento | RSSI dei 10 AP più forti su 60 scansioni; interruzioni quando un AP mancava |
| Valutazione dei canali | Pesata per segnale e sovrapposizione; solo 1/6/11 a 2,4 GHz; preferenza per non DFS; PSC a 6 GHz |
| Dettagli | Produttore dal registro IEEE, rilevamento MAC casuali, sicurezza incl. WPA3 SAE-EXT e OWE, stima della distanza |
| Privacy | Nessun accesso alla rete, pubblicità o analisi; i risultati restano in memoria |

La distanza è una stima in spazio libero; i muri la accorciano. Il contenuto delle scansioni dipende dal chip Wi-Fi e dal driver; verifica i valori a 6 GHz, 320 MHz e MLO con il tuo hardware prima di farci affidamento.

<a id="demo"></a>
## Demo

Questa demo muta di 40 secondi usa **la vera interfaccia dell'app guidata dall'ambiente sintetico integrato**: un appartamento a Taipei con un router Wi-Fi 7 (tre link MLO) e vicini affollati, con sottotitoli bilingui. Non è una scansione reale e non dice nulla della radio di uno smartphone specifico.

![Presentazione dell'app con dati sintetici](docs/media/wifi-spectrum-demo.gif)

[Apri o scarica la demo MP4](docs/media/wifi-spectrum-demo.mp4)

| Spettro a 2,4 GHz | 6 GHz con 320 MHz | Link MLO Wi-Fi 7 |
| --- | --- | --- |
| ![Spettro a 2,4 GHz](docs/media/spectrum-24ghz.png) | ![6 GHz con 320 MHz](docs/media/spectrum-6ghz.png) | ![Link MLO Wi-Fi 7](docs/media/mlo-links.png) |

## Ultime modifiche

Prima versione: scansione su tre bande, larghezze da 20 a 320 MHz, Wi-Fi 7 MLO, spettro, andamento, valutazione dei canali, risorse per Google Play e guida alla pubblicazione, verificata con 214 test offline su Android 8–16. Vedi il [registro delle modifiche](CHANGELOG.md).

<a id="controls"></a>
## Comandi

| Comando | Azione |
| --- | --- |
| Aggiorna | Scansiona ora |
| Pausa / Riproduci | Ferma o riprendi la scansione automatica |
| ⋮ → intervallo | 3, 5, 10, 30 o 60 secondi; sotto i 30 s Android può limitare |
| ⋮ → modalità demo | Passa tra scansione reale e dati sintetici |
| Chip banda / larghezza / Wi-Fi 7 | Filtra l'elenco; il chip della larghezza alterna ≥40/80/160/320 MHz |
| Zoom (5/6 GHz) | Alterna panoramica della banda e vista ravvicinata scorrevole |
| Tocca un AP | Mostra intervallo occupato, canali coperti, sicurezza e link MLO |

<a id="faq"></a>
## FAQ

**Perché serve la posizione?** Android tratta i risultati delle scansioni Wi-Fi come dati di posizione. Senza posizione precisa (e «Dispositivi Wi-Fi nelle vicinanze» su Android 13+) il sistema non restituisce nulla. L'app non legge le coordinate GPS.

**Perché i 6 GHz sono vuoti?** Lo smartphone deve supportare il Wi-Fi 6E e un router a 6 GHz deve essere a portata.

**Perché non compaiono MLO o 320 MHz?** Servono uno smartphone Wi-Fi 7 con Android 13 o successivo e un router Wi-Fi 7 con MLO attivo.

**Perché dice che la scansione è limitata?** Android consente quattro scansioni in primo piano ogni due minuti. Mantieni l'intervallo di 30 secondi oppure, durante i test, disattiva la limitazione della ricerca Wi-Fi nelle Opzioni sviluppatore.

**Esiste una versione iOS?** Le API di Apple non espongono le reti vicine, quindi iOS non può fare queste scansioni. Vedi [IOS_wifi-spectrum](https://github.com/tonykfr1234/ios_wifi-spectrum) per ciò che iOS supporta.

<a id="docs"></a>
<a id="documentation"></a>
## Documentazione

| Argomento | Documento |
| --- | --- |
| Uso quotidiano | [繁中使用指南](docs/USER_GUIDE.zh-TW.md) · [English User Guide](docs/USER_GUIDE.md) |
| Funzionamento interno | [Architettura](docs/ARCHITECTURE.md) |
| Pubblicazione | [Guida Google Play (繁中)](docs/PUBLISHING.zh-TW.md) · [Riepilogo in inglese](docs/PUBLISHING.md) · [Informativa sulla privacy](docs/PRIVACY.md) |
| Versioni e sicurezza | [Registro delle modifiche](CHANGELOG.md) · [Sicurezza](SECURITY.md) · [Contribuire](CONTRIBUTING.md) |

Il codice è in `app/src/main/java`, i test in `app/src/test`, gli strumenti in `scripts/` e le immagini per Google Play in `store/`. I 214 test girano sulla JVM senza smartphone; verifica i valori reali a 6 GHz, 320 MHz e MLO con il tuo hardware.

## Sicurezza

Non fare mai commit di keystore, `keystore.properties`, `local.properties`, chiavi API o scansioni esportate di reti altrui. Prima del push, esegui lo stesso scanner usato da GitHub Actions:

```bash
python scripts/security_scan.py .
```

## Licenza

[MIT](LICENSE).

### 2026-10-20 · 1.0.0

首次發布：2.4／5／6 GHz 掃描、20–320 MHz 頻寬、Wi-Fi 7 MLO、頻譜圖、趨勢與頻道評分；214 個離線測試涵蓋 Android 8–16。

First release: 2.4/5/6 GHz scanning, 20–320 MHz widths, Wi-Fi 7 MLO, spectrum, trend and channel rating; 214 offline tests cover Android 8–16.

[Guida pubblica all'uso (cinese tradizionale, testo semplice)](docs/使用說明.txt)
