<p align="center">
  <a href="README.md">English</a> · <a href="README.zh-CN.md">简体中文</a> · <a href="README.zh-TW.md">繁體中文</a> · <a href="README.ja.md">日本語</a> · <a href="README.ko.md">한국어</a><br>
  <a href="README.de.md">Deutsch</a> · <a href="README.es.md">Español</a> · <a href="README.fr.md">Français</a> · <a href="README.pt-BR.md">Português (Brasil)</a> · <a href="README.vi.md">Tiếng Việt</a> · <strong>Bahasa Indonesia</strong> · <a href="README.it.md">Italiano</a>
</p>

<p align="center"><img src="assets/wifi-spectrum.png" width="112" alt="Ikon Wi-Fi Spectrum"></p>
<h1 align="center">Penganalisis Wi-Fi Profesional untuk Android</h1>
<p align="center">Wi-Fi Spectrum (Wi-Fi 頻譜分析儀) · 2,4 / 5 / 6 GHz · 20–320 MHz · Wi-Fi 7 MLO</p>
<p align="center">
  <a href="https://github.com/tonykfr1234/android_wifi-spectrum/actions/workflows/security.yml"><img src="https://github.com/tonykfr1234/android_wifi-spectrum/actions/workflows/security.yml/badge.svg" alt="CI"></a>
  <img src="https://img.shields.io/badge/platform-Android%208%2B-3DDC84" alt="Android 8+">
  <img src="https://img.shields.io/badge/Kotlin-2.2-7F52FF" alt="Kotlin 2.2">
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4" alt="Jetpack Compose">
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-green" alt="Lisensi MIT"></a>
</p>
<p align="center">
  <a href="#quick-start">Mulai cepat</a> · <a href="#workflow">Alur kerja</a> · <a href="#features">Fitur</a> · <a href="#demo">Demo</a> · <a href="#faq">Tanya jawab</a> · <a href="#documentation">Dokumentasi</a>
</p>

```bash
./gradlew testDebugUnitTest assembleDebug
```

---

Versi 1.0: ketiga pita frekuensi, semua lebar kanal hingga 320 MHz, dan multi-link Wi-Fi 7 (MLO) yang dikelompokkan menurut alamat MLD.

Wi-Fi Spectrum memindai titik akses di sekitar pada 2,4, 5, dan 6 GHz dan menunjukkan di mana masing-masing benar-benar memancar: kanal utama, seluruh blok 20/40/80/160/80+80/320 MHz, dan setiap kanal yang tercakup. Tampilan spektrum, tren sinyal, dan penilaian kanal membantu memilih kanal yang sepi; tampilan MLO menggabungkan link dari setiap router Wi-Fi 7.

Apple tidak mengizinkan aplikasi iOS memindai jaringan Wi-Fi di sekitar, jadi fitur ini hanya ada di Android. Versi iOS, [IOS_wifi-spectrum](https://github.com/tonykfr1234/ios_wifi-spectrum), menyediakan apa yang diizinkan iOS.

README dalam 12 bahasa adalah terjemahan dokumentasi. Antarmuka aplikasi saat ini memakai bahasa Tionghoa tradisional.

<a id="quick-start"></a>
## Mulai cepat

1. Unduh `wifi-spectrum-debug-apk` dari [GitHub Actions](https://github.com/tonykfr1234/android_wifi-spectrum/actions) terakhir yang berhasil, atau build dengan `./gradlew assembleDebug` (JDK 17+ dan Android SDK).
2. Pasang `app-debug.apk` di ponsel Android 8+. 6 GHz membutuhkan Wi-Fi 6E; 320 MHz dan MLO membutuhkan Android 13+ dengan Wi-Fi 7.
3. Buka aplikasi dan izinkan lokasi **akurat** serta, di Android 13+, “Perangkat Wi-Fi di sekitar”. Tanpa izin ini Android tidak memberikan hasil pindaian.
4. Biarkan layanan lokasi tetap aktif. Aplikasi memindai setiap 30 detik saat terlihat; ketuk muat ulang untuk memindai sekarang.
5. Tidak punya ponsel yang cocok? Pilih mode demo untuk menjelajahi lingkungan Wi-Fi 7 sintetis.

**Ingin rilis di Google Play?** Ikuti [docs/PUBLISHING.zh-TW.md](docs/PUBLISHING.zh-TW.md) ([ringkasan bahasa Inggris](docs/PUBLISHING.md)): ganti ID aplikasi, buat kunci unggah di luar proyek, build App Bundle yang ditandatangani, dan selesaikan pengujian tertutup Play Console.

<a id="workflow"></a>
## Alur kerja

```text
Aplikasi terlihat, setiap 30 detik (Android mengizinkan 4 pindaian latar depan per 2 menit)
  → WifiManager.startScan(); permintaan yang dibatasi memakai hasil cache
  → rapikan setiap ScanResult: SSID, BSSID, pita, kanal utama
  → blok yang ditempati dari lebar dan frekuensi tengah (20–320 MHz, 80+80)
  → kelompokkan link Wi-Fi 7 menurut alamat MLD; cari vendor secara offline
  → perbarui daftar, spektrum, tren 60 pindaian, penilaian kanal, dan MLO
```

Pemindaian berhenti saat aplikasi meninggalkan layar; tidak ada yang berjalan di latar belakang. Bila driver tidak memberi frekuensi tengah atau menaruh pusat 160 MHz di `centerFreq1`, blok diturunkan dari rencana kanal standar. Kolom MLO membutuhkan Android 13; sinyal per link dari router yang terhubung membutuhkan Android 14.

<a id="features"></a>
## Fitur

| Fitur | Perilaku |
| --- | --- |
| Pita | 2,4, 5 (termasuk UNII-4), dan 6 GHz; pemberitahuan pembatasan, Wi-Fi mati, dan lokasi mati |
| Lebar kanal | 20, 40, 80, 160, 80+80, dan 320 MHz; rentang yang ditempati, kanal tengah, dan kanal yang tercakup |
| Wi-Fi 7 MLO | Alamat MLD, ID link, dan link terkait; link aktif dan RSSI per link dari koneksi |
| Spektrum | Satu parabola per AP; ringkasan seluruh pita dengan zoom; link router yang terhubung ditebalkan |
| Tren | RSSI 10 AP terkuat selama 60 pindaian; garis terputus saat AP tidak terlihat |
| Penilaian kanal | Dibobot menurut sinyal dan tumpang tindih; hanya 1/6/11 di 2,4 GHz; utamakan non-DFS; PSC di 6 GHz |
| Detail | Vendor dari registri IEEE, deteksi MAC acak, keamanan termasuk WPA3 SAE-EXT dan OWE, perkiraan jarak |
| Privasi | Tanpa akses jaringan, iklan, atau analitik; hasil pindaian hanya di memori |

Jarak adalah perkiraan ruang bebas; dinding membuatnya lebih pendek. Isi pindaian bergantung pada chip Wi-Fi dan driver ponsel; periksa nilai 6 GHz, 320 MHz, dan MLO dengan perangkat Anda sendiri sebelum mengandalkannya.

<a id="demo"></a>
## Demo

Demo tanpa suara berdurasi 40 detik ini memakai **antarmuka asli aplikasi dengan lingkungan sintetis bawaan**: apartemen di Taipei dengan router Wi-Fi 7 (tiga link MLO) dan tetangga yang padat, disertai teks dua bahasa. Ini bukan pindaian nyata dan tidak mencerminkan radio ponsel mana pun.

![Demo aplikasi dengan data sintetis](docs/media/wifi-spectrum-demo.gif)

[Buka atau unduh demo MP4](docs/media/wifi-spectrum-demo.mp4)

| Spektrum 2,4 GHz | 6 GHz dengan 320 MHz | Link MLO Wi-Fi 7 |
| --- | --- | --- |
| ![Spektrum 2,4 GHz](docs/media/spectrum-24ghz.png) | ![6 GHz dengan 320 MHz](docs/media/spectrum-6ghz.png) | ![Link MLO Wi-Fi 7](docs/media/mlo-links.png) |

## Perubahan terbaru

Rilis pertama: pemindaian tiga pita, lebar 20–320 MHz, Wi-Fi 7 MLO, spektrum, tren, penilaian kanal, aset Google Play, dan panduan rilis, diverifikasi dengan 214 pengujian offline pada Android 8–16. Lihat [catatan perubahan](CHANGELOG.md).

<a id="controls"></a>
## Kontrol

| Kontrol | Tindakan |
| --- | --- |
| Muat ulang | Pindai sekarang |
| Jeda / Putar | Hentikan atau lanjutkan pemindaian otomatis |
| ⋮ → interval | 3, 5, 10, 30, atau 60 detik; di bawah 30 detik Android bisa membatasi |
| ⋮ → mode demo | Beralih antara pemindaian nyata dan data sintetis |
| Chip pita / lebar / Wi-Fi 7 | Saring daftar; chip lebar bergantian ≥40/80/160/320 MHz |
| Zoom (5/6 GHz) | Beralih antara ringkasan seluruh pita dan tampilan dekat yang dapat digulir |
| Ketuk AP | Lihat rentang yang ditempati, kanal yang tercakup, keamanan, dan link MLO |

<a id="faq"></a>
## Tanya jawab

**Mengapa butuh lokasi?** Android memperlakukan hasil pindaian Wi-Fi sebagai data lokasi. Tanpa lokasi akurat (dan “Perangkat Wi-Fi di sekitar” di Android 13+) sistem tidak mengembalikan hasil. Aplikasi tidak membaca koordinat GPS.

**Mengapa 6 GHz kosong?** Ponsel harus mendukung Wi-Fi 6E dan router 6 GHz harus berada dalam jangkauan.

**Mengapa tidak ada MLO atau 320 MHz?** Dibutuhkan ponsel Wi-Fi 7 dengan Android 13 atau lebih baru dan router Wi-Fi 7 dengan MLO aktif.

**Mengapa pemindaian disebut dibatasi?** Android mengizinkan empat pindaian latar depan setiap dua menit. Pertahankan interval 30 detik, atau saat pengujian matikan pembatasan pemindaian Wi-Fi di Opsi developer.

**Apakah ada versi iOS?** API Apple tidak membuka jaringan di sekitar, sehingga iOS tidak bisa melakukan pindaian ini. Lihat [IOS_wifi-spectrum](https://github.com/tonykfr1234/ios_wifi-spectrum) untuk fitur yang didukung iOS.

<a id="docs"></a>
<a id="documentation"></a>
## Dokumentasi

| Topik | Dokumen |
| --- | --- |
| Penggunaan harian | [繁中使用指南](docs/USER_GUIDE.zh-TW.md) · [English User Guide](docs/USER_GUIDE.md) |
| Cara kerja internal | [Arsitektur](docs/ARCHITECTURE.md) |
| Rilis | [Panduan Google Play (繁中)](docs/PUBLISHING.zh-TW.md) · [Ringkasan bahasa Inggris](docs/PUBLISHING.md) · [Kebijakan privasi](docs/PRIVACY.md) |
| Versi dan keamanan | [Catatan perubahan](CHANGELOG.md) · [Keamanan](SECURITY.md) · [Berkontribusi](CONTRIBUTING.md) |

Kode ada di `app/src/main/java`, pengujian di `app/src/test`, alat di `scripts/`, dan gambar Google Play di `store/`. 214 pengujian berjalan di JVM tanpa ponsel; pastikan nilai 6 GHz, 320 MHz, dan MLO yang nyata dengan perangkat Anda.

## Keamanan

Jangan pernah meng-commit keystore, `keystore.properties`, `local.properties`, kunci API, atau hasil pindaian jaringan milik orang lain. Sebelum push, jalankan pemindai yang sama dengan GitHub Actions:

```bash
python scripts/security_scan.py .
```

## Lisensi

[MIT](LICENSE).

### 2026-10-20 · 1.0.0

首次發布：2.4／5／6 GHz 掃描、20–320 MHz 頻寬、Wi-Fi 7 MLO、頻譜圖、趨勢與頻道評分；214 個離線測試涵蓋 Android 8–16。

First release: 2.4/5/6 GHz scanning, 20–320 MHz widths, Wi-Fi 7 MLO, spectrum, trend and channel rating; 214 offline tests cover Android 8–16.

[Panduan penggunaan publik (bahasa Tionghoa tradisional, teks biasa)](docs/使用說明.txt)
