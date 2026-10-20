<p align="center">
  <a href="README.md">English</a> · <a href="README.zh-CN.md">简体中文</a> · <a href="README.zh-TW.md">繁體中文</a> · <a href="README.ja.md">日本語</a> · <a href="README.ko.md">한국어</a><br>
  <a href="README.de.md">Deutsch</a> · <strong>Español</strong> · <a href="README.fr.md">Français</a> · <a href="README.pt-BR.md">Português (Brasil)</a> · <a href="README.vi.md">Tiếng Việt</a> · <a href="README.id.md">Bahasa Indonesia</a> · <a href="README.it.md">Italiano</a>
</p>

<p align="center"><img src="assets/wifi-spectrum.png" width="112" alt="Icono de Wi-Fi Spectrum"></p>
<h1 align="center">Analizador Wi-Fi profesional para Android</h1>
<p align="center">Wi-Fi Spectrum (Wi-Fi 頻譜分析儀) · 2,4 / 5 / 6 GHz · 20–320 MHz · Wi-Fi 7 MLO</p>
<p align="center">
  <a href="https://github.com/tonykfr1234/android_wifi-spectrum/actions/workflows/security.yml"><img src="https://github.com/tonykfr1234/android_wifi-spectrum/actions/workflows/security.yml/badge.svg" alt="CI"></a>
  <img src="https://img.shields.io/badge/platform-Android%208%2B-3DDC84" alt="Android 8+">
  <img src="https://img.shields.io/badge/Kotlin-2.2-7F52FF" alt="Kotlin 2.2">
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4" alt="Jetpack Compose">
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-green" alt="Licencia MIT"></a>
</p>
<p align="center">
  <a href="#quick-start">Inicio rápido</a> · <a href="#workflow">Flujo</a> · <a href="#features">Funciones</a> · <a href="#demo">Demostración</a> · <a href="#faq">Preguntas frecuentes</a> · <a href="#documentation">Documentación</a>
</p>

```bash
./gradlew testDebugUnitTest assembleDebug
```

---

Versión 1.0: las tres bandas, todos los anchos de canal hasta 320 MHz y multienlace Wi-Fi 7 (MLO) agrupado por dirección MLD.

Wi-Fi Spectrum escanea los puntos de acceso cercanos en 2,4, 5 y 6 GHz y muestra dónde transmite realmente cada uno: su canal principal, el bloque completo de 20/40/80/160/80+80/320 MHz y todos los canales que ocupa. La vista de espectro, la tendencia de señal y la valoración de canales ayudan a elegir un canal tranquilo; la vista MLO agrupa los enlaces de cada router Wi-Fi 7.

Apple no permite que las apps de iOS escaneen redes Wi-Fi cercanas, así que estas funciones son exclusivas de Android. La versión para iOS, [IOS_wifi-spectrum](https://github.com/tonykfr1234/ios_wifi-spectrum), ofrece lo que iOS permite.

Los 12 idiomas del README traducen la documentación. La interfaz de la app está actualmente en chino tradicional.

<a id="quick-start"></a>
## Inicio rápido

1. Descarga `wifi-spectrum-debug-apk` de la última [ejecución correcta de GitHub Actions](https://github.com/tonykfr1234/android_wifi-spectrum/actions) o compílala con `./gradlew assembleDebug` (JDK 17+ y Android SDK).
2. Instala `app-debug.apk` en un teléfono con Android 8+. 6 GHz requiere Wi-Fi 6E; 320 MHz y MLO requieren Android 13+ con Wi-Fi 7.
3. Abre la app y permite la ubicación **precisa** y, en Android 13+, «Dispositivos Wi-Fi cercanos». Sin ellos, Android no entrega resultados.
4. Mantén activada la ubicación. La app escanea cada 30 segundos mientras está visible; pulsa actualizar para escanear ya.
5. ¿No tienes un teléfono adecuado? Usa el modo demostración para explorar un entorno Wi-Fi 7 sintético.

**¿Publicar en Google Play?** Sigue [docs/PUBLISHING.zh-TW.md](docs/PUBLISHING.zh-TW.md) ([resumen en inglés](docs/PUBLISHING.md)): cambia el ID de aplicación, crea una clave de subida fuera del proyecto, genera el App Bundle firmado y completa la prueba cerrada de Play Console.

<a id="workflow"></a>
## Flujo

```text
App visible, cada 30 segundos (Android permite 4 escaneos en primer plano cada 2 minutos)
  → WifiManager.startScan(); las solicitudes limitadas reutilizan los resultados en caché
  → limpiar cada ScanResult: SSID, BSSID, banda, canal principal
  → bloque ocupado según ancho y frecuencia central (20–320 MHz, 80+80)
  → agrupar enlaces Wi-Fi 7 por dirección MLD; consultar el fabricante sin conexión
  → actualizar lista, espectro, tendencia de 60 escaneos, valoración y MLO
```

El escaneo se detiene cuando la app sale de la pantalla; nada se ejecuta en segundo plano. Si el controlador omite la frecuencia central o informa el centro de 160 MHz en `centerFreq1`, el bloque se deriva del plan de canales estándar. Los campos MLO requieren Android 13; la señal por enlace del router conectado, Android 14.

<a id="features"></a>
## Funciones

| Función | Comportamiento |
| --- | --- |
| Bandas | 2,4, 5 (incl. UNII-4) y 6 GHz; avisos de limitación, Wi-Fi apagado y ubicación apagada |
| Ancho de canal | 20, 40, 80, 160, 80+80 y 320 MHz; rango ocupado, canal central y canales cubiertos |
| Wi-Fi 7 MLO | Dirección MLD, ID de enlace y enlaces afiliados; enlaces activos y RSSI por enlace de la conexión |
| Espectro | Una parábola por AP; vista general de toda la banda con zoom; enlaces del router conectado en negrita |
| Tendencia | RSSI de los 10 AP más fuertes en 60 escaneos; huecos cuando un AP no apareció |
| Valoración de canales | Ponderada por señal y solapamiento; solo 1/6/11 en 2,4 GHz; preferencia por no DFS; PSC en 6 GHz |
| Detalles | Fabricante del registro IEEE, detección de MAC aleatorias, seguridad incl. WPA3 SAE-EXT y OWE, estimación de distancia |
| Privacidad | Sin acceso a red, anuncios ni analítica; los resultados quedan en memoria |

La distancia es una estimación en espacio libre; las paredes la acortan. El contenido del escaneo depende del chip Wi-Fi y del controlador; comprueba los valores de 6 GHz, 320 MHz y MLO con tu propio equipo antes de fiarte de ellos.

<a id="demo"></a>
## Demostración

Esta demostración muda de 40 segundos usa la **interfaz real de la app con el entorno sintético integrado**: un apartamento en Taipéi con un router Wi-Fi 7 (tres enlaces MLO) y vecinos saturados, con subtítulos bilingües en cada pantalla. No es un escaneo real ni dice nada de la radio de ningún teléfono concreto.

![Recorrido por la app con datos sintéticos](docs/media/wifi-spectrum-demo.gif)

[Abrir o descargar la demostración MP4](docs/media/wifi-spectrum-demo.mp4)

| Espectro de 2,4 GHz | 6 GHz con 320 MHz | Enlaces MLO de Wi-Fi 7 |
| --- | --- | --- |
| ![Espectro de 2,4 GHz](docs/media/spectrum-24ghz.png) | ![6 GHz con 320 MHz](docs/media/spectrum-6ghz.png) | ![Enlaces MLO de Wi-Fi 7](docs/media/mlo-links.png) |

## Últimos cambios

Primera versión: escaneo en tres bandas, anchos de 20–320 MHz, Wi-Fi 7 MLO, espectro, tendencia, valoración de canales, recursos para Google Play y guía de publicación, verificada con 214 pruebas sin conexión para Android 8–16. Consulta el [registro de cambios](CHANGELOG.md).

<a id="controls"></a>
## Controles

| Control | Acción |
| --- | --- |
| Actualizar | Escanear ahora |
| Pausa / Reproducir | Detener o reanudar el escaneo automático |
| ⋮ → intervalo | 3, 5, 10, 30 o 60 segundos; por debajo de 30 s Android puede limitar |
| ⋮ → modo demostración | Cambiar entre escaneo real y datos sintéticos |
| Chips de banda / ancho / Wi-Fi 7 | Filtrar la lista; el chip de ancho alterna ≥40/80/160/320 MHz |
| Zoom (5/6 GHz) | Alternar la vista general de la banda y un primer plano desplazable |
| Tocar un AP | Ver rango ocupado, canales cubiertos, seguridad y enlaces MLO |

<a id="faq"></a>
## Preguntas frecuentes

**¿Por qué necesita la ubicación?** Android trata los resultados del escaneo Wi-Fi como datos de ubicación. Sin ubicación precisa (y «Dispositivos Wi-Fi cercanos» en Android 13+) el sistema no devuelve resultados. La app no lee coordenadas GPS.

**¿Por qué 6 GHz está vacío?** El teléfono debe admitir Wi-Fi 6E y debe haber un router de 6 GHz al alcance.

**¿Por qué no aparece MLO ni 320 MHz?** Requieren un teléfono Wi-Fi 7 con Android 13 o posterior y un router Wi-Fi 7 con MLO activado.

**¿Por qué dice que el escaneo está limitado?** Android permite cuatro escaneos en primer plano cada dos minutos. Mantén el intervalo de 30 segundos o desactiva la limitación de búsqueda Wi-Fi en Opciones para desarrolladores al probar.

**¿Hay versión para iOS?** Las API de Apple no exponen redes cercanas, así que iOS no puede hacer estos escaneos. Consulta [IOS_wifi-spectrum](https://github.com/tonykfr1234/ios_wifi-spectrum) para ver lo que admite iOS.

<a id="docs"></a>
<a id="documentation"></a>
## Documentación

| Tema | Documento |
| --- | --- |
| Uso diario | [繁中使用指南](docs/USER_GUIDE.zh-TW.md) · [English User Guide](docs/USER_GUIDE.md) |
| Funcionamiento interno | [Arquitectura](docs/ARCHITECTURE.md) |
| Publicación | [Guía de Google Play (繁中)](docs/PUBLISHING.zh-TW.md) · [Resumen en inglés](docs/PUBLISHING.md) · [Política de privacidad](docs/PRIVACY.md) |
| Versiones y seguridad | [Registro de cambios](CHANGELOG.md) · [Seguridad](SECURITY.md) · [Contribuir](CONTRIBUTING.md) |

El código está en `app/src/main/java`, las pruebas en `app/src/test`, las herramientas en `scripts/` y las imágenes de Google Play en `store/`. Las 214 pruebas se ejecutan en la JVM sin teléfono; verifica los valores reales de 6 GHz, 320 MHz y MLO con tu propio equipo.

## Seguridad

Nunca subas almacenes de claves, `keystore.properties`, `local.properties`, claves de API ni escaneos exportados de redes ajenas. Antes de hacer push, ejecuta el mismo escáner que GitHub Actions:

```bash
python scripts/security_scan.py .
```

## Licencia

[MIT](LICENSE).

### 2026-10-20 · 1.0.0

首次發布：2.4／5／6 GHz 掃描、20–320 MHz 頻寬、Wi-Fi 7 MLO、頻譜圖、趨勢與頻道評分；214 個離線測試涵蓋 Android 8–16。

First release: 2.4/5/6 GHz scanning, 20–320 MHz widths, Wi-Fi 7 MLO, spectrum, trend and channel rating; 214 offline tests cover Android 8–16.

[Guía pública de uso (chino tradicional, texto plano)](docs/使用說明.txt)
