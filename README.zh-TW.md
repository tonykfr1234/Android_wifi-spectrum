> **新電腦安裝與日常操作：[安裝／使用說明](INSTALL.zh-TW.md)**

<p align="center">
  <a href="README.md">English</a> · <a href="README.zh-CN.md">简体中文</a> · <strong>繁體中文</strong> · <a href="README.ja.md">日本語</a> · <a href="README.ko.md">한국어</a><br>
  <a href="README.de.md">Deutsch</a> · <a href="README.es.md">Español</a> · <a href="README.fr.md">Français</a> · <a href="README.pt-BR.md">Português (Brasil)</a> · <a href="README.vi.md">Tiếng Việt</a> · <a href="README.id.md">Bahasa Indonesia</a> · <a href="README.it.md">Italiano</a>
</p>

<p align="center"><img src="assets/wifi-spectrum.png" width="112" alt="Wi-Fi 頻譜分析儀圖示"></p>
<h1 align="center">專業 Wi-Fi 頻譜分析儀（Android）</h1>
<p align="center">Wi-Fi 頻譜分析儀 · 2.4／5／6 GHz · 20–320 MHz · Wi-Fi 7 MLO</p>
<p align="center">
  <a href="https://github.com/tonykfr1234/android_wifi-spectrum/actions/workflows/security.yml"><img src="https://github.com/tonykfr1234/android_wifi-spectrum/actions/workflows/security.yml/badge.svg" alt="CI"></a>
  <img src="https://img.shields.io/badge/platform-Android%208%2B-3DDC84" alt="Android 8+">
  <img src="https://img.shields.io/badge/Kotlin-2.2-7F52FF" alt="Kotlin 2.2">
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4" alt="Jetpack Compose">
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-green" alt="MIT 授權"></a>
</p>
<p align="center">
  <a href="#quick-start">快速開始</a> · <a href="#workflow">運作流程</a> · <a href="#features">功能</a> · <a href="#demo">示範</a> · <a href="#faq">常見問題</a> · <a href="#documentation">文件</a>
</p>

```bash
./gradlew testDebugUnitTest assembleDebug
```

---

1.0 版：三個頻段、最高 320 MHz 的所有頻寬，以及依 MLD 位址合併的 Wi-Fi 7 多連結（MLO）。

Wi-Fi 頻譜分析儀掃描附近 2.4、5、6 GHz 的基地台，標出每一台實際發射的位置：主頻道、完整的 20／40／80／160／80+80／320 MHz 區塊，以及涵蓋的所有頻道。頻譜圖、訊號趨勢與頻道評分幫你挑出安靜的頻道；MLO 頁面把每台 Wi-Fi 7 路由器的各條連結合併顯示。

Apple 不允許 iOS App 掃描附近的 Wi-Fi，所以這些功能只有 Android 版有。iOS 版 [IOS_wifi-spectrum](https://github.com/tonykfr1234/ios_wifi-spectrum) 提供 iOS 允許範圍內的功能。

12 種 README 語言是文件翻譯，App 介面目前使用繁體中文。

<a id="quick-start"></a>
## 快速開始

1. 從 [GitHub Actions](https://github.com/tonykfr1234/android_wifi-spectrum/actions) 最新一次成功的執行下載 `wifi-spectrum-debug-apk`，或用 `./gradlew assembleDebug` 自行建置（需要 JDK 17 以上與 Android SDK）。
2. 把 `app-debug.apk` 安裝到 Android 8 以上的手機。6 GHz 需要支援 Wi-Fi 6E；320 MHz 與 MLO 需要 Android 13 以上、支援 Wi-Fi 7 的手機。
3. 開啟 App，允許**精確**位置；Android 13 以上也要允許「附近的 Wi-Fi 裝置」。沒有這些權限，Android 不會提供掃描結果。
4. 保持定位服務開啟。App 在畫面上時每 30 秒掃描一次，按重新整理可立即掃描。
5. 手邊沒有合適的手機？選示範模式，看模擬的 Wi-Fi 7 環境。

**要上架 Google Play？** 依照 [docs/PUBLISHING.zh-TW.md](docs/PUBLISHING.zh-TW.md)：改套件名稱、在專案外產生上傳金鑰、建置簽章的 App Bundle，並完成 Play Console 的封閉測試。

<a id="workflow"></a>
## 運作流程

```text
App 在畫面上，每 30 秒一次（Android 前景每 2 分鐘最多 4 次）
  → WifiManager.startScan()；被節流時沿用系統快取結果
  → 整理每筆 ScanResult：SSID、BSSID、頻段、主頻道
  → 依頻寬與中心頻率算出佔用區塊（20–320 MHz、80+80）
  → 依 MLD 位址合併 Wi-Fi 7 連結，離線查詢廠商
  → 更新清單、頻譜圖、60 次趨勢、頻道評分與 MLO 頁面
```

App 離開畫面就停止掃描，不在背景執行。驅動程式沒有回報中心頻率，或把 160 MHz 中心放在 `centerFreq1` 時，改用標準頻道規劃推算。MLO 欄位需要 Android 13；連線中路由器的各條連結訊號需要 Android 14。

<a id="features"></a>
## 功能

| 功能 | 行為 |
| --- | --- |
| 頻段 | 2.4、5（含 UNII-4）、6 GHz；顯示節流、Wi-Fi 關閉、定位關閉提示 |
| 頻寬 | 20、40、80、160、80+80、320 MHz；佔用範圍、中心頻道與涵蓋頻道 |
| Wi-Fi 7 MLO | MLD 位址、Link ID 與關聯連結；連線中的使用中連結與各連結訊號 |
| 頻譜圖 | 每台基地台一條拋物線；整段總覽可放大；連線路由器的各條連結加粗 |
| 趨勢 | 最強 10 台基地台在 60 次掃描中的訊號；沒掃到時線條中斷 |
| 頻道評分 | 依訊號與重疊加權；2.4 GHz 只推薦 1／6／11；優先非 DFS；6 GHz 優先 PSC |
| 詳細資料 | IEEE 登錄表廠商、隨機 MAC 判斷、加密方式（含 WPA3 SAE-EXT、OWE）、距離估算 |
| 隱私 | 沒有網路功能、廣告與分析工具；掃描結果只存在記憶體 |

距離是無遮蔽環境的估算，穿牆會更近。掃描內容取決於手機的 Wi-Fi 晶片與驅動程式；要依賴 6 GHz、320 MHz 或 MLO 數值前，請用自己的設備確認。

<a id="demo"></a>
## 示範

以下 40 秒無聲示範使用**程式實際介面，搭配內建的合成環境**：一間有 Wi-Fi 7 路由器（三條 MLO 連結）與擁擠鄰居的台北公寓，並以中英字幕解說每個畫面。這不是實際掃描，也不代表任何手機的無線效能。

![以合成資料操作 App 的示範](docs/media/wifi-spectrum-demo.gif)

[播放或下載 MP4 示範影片](docs/media/wifi-spectrum-demo.mp4)

| 2.4 GHz 頻譜圖 | 6 GHz 與 320 MHz | Wi-Fi 7 MLO 連結 |
| --- | --- | --- |
| ![2.4 GHz 頻譜圖](docs/media/spectrum-24ghz.png) | ![6 GHz 與 320 MHz](docs/media/spectrum-6ghz.png) | ![Wi-Fi 7 MLO 連結](docs/media/mlo-links.png) |

## 最新變更

首次發布：三頻段掃描、20–320 MHz 頻寬、Wi-Fi 7 MLO、頻譜圖、趨勢、頻道評分、Google Play 素材與上架指南，並通過 214 個涵蓋 Android 8–16 的離線測試。詳見[更新記錄](CHANGELOG.md)。

<a id="controls"></a>
## 操作

| 操作 | 功能 |
| --- | --- |
| 重新整理 | 立即掃描 |
| 暫停／播放 | 停止或恢復自動掃描 |
| ⋮ → 掃描間隔 | 3、5、10、30、60 秒；低於 30 秒可能被 Android 節流 |
| ⋮ → 示範模式 | 在真實掃描與合成資料之間切換 |
| 頻段／頻寬／Wi-Fi 7 篩選 | 篩選清單；點頻寬可依序切換 ≥40／80／160／320 MHz |
| 放大（5／6 GHz） | 切換整段總覽與可滑動的放大檢視 |
| 點基地台 | 查看佔用範圍、涵蓋頻道、加密方式與 MLO 連結 |

<a id="faq"></a>
## 常見問題

**為什麼需要位置權限？** Android 把 Wi-Fi 掃描結果視為位置資訊。沒有精確位置（Android 13 以上還要「附近的 Wi-Fi 裝置」），系統不會回傳任何結果。App 不讀取 GPS 座標。

**為什麼 6 GHz 是空的？** 手機需要支援 Wi-Fi 6E，附近也要有 6 GHz 路由器。

**為什麼沒有 MLO 或 320 MHz？** 需要 Android 13 以上、支援 Wi-Fi 7 的手機，以及開啟 MLO 的 Wi-Fi 7 路由器。

**為什麼顯示掃描被節流？** Android 前景每 2 分鐘只允許掃描 4 次。保持 30 秒間隔，或測試時到開發人員選項關閉「Wi-Fi 掃描節流」。

**有 iOS 版嗎？** Apple 的 API 不提供附近網路資訊，iOS 做不到這些掃描。iOS 能做的功能請見 [IOS_wifi-spectrum](https://github.com/tonykfr1234/ios_wifi-spectrum)。

<a id="docs"></a>
<a id="documentation"></a>
## 文件

| 主題 | 文件 |
| --- | --- |
| 日常操作 | [繁中使用指南](docs/USER_GUIDE.zh-TW.md) · [English User Guide](docs/USER_GUIDE.md) |
| 程式設計 | [架構](docs/ARCHITECTURE.md) |
| 上架 | [Google Play 上架指南](docs/PUBLISHING.zh-TW.md) · [English summary](docs/PUBLISHING.md) · [隱私權政策](docs/PRIVACY.md) |
| 版本與安全 | [更新記錄](CHANGELOG.md) · [資安說明](SECURITY.md) · [貢獻指南](CONTRIBUTING.md) |

程式在 `app/src/main/java`，測試在 `app/src/test`，工具在 `scripts/`，Google Play 圖片在 `store/`。214 個測試在電腦上執行，不需要手機；實際的 6 GHz、320 MHz 與 MLO 數值仍需用自己的設備確認。

## 資安

不要提交金鑰庫、`keystore.properties`、`local.properties`、API Key，或不屬於你的網路掃描資料。Push 前執行與 GitHub Actions 相同的掃描：

```bash
python scripts/security_scan.py .
```

## 授權

[MIT](LICENSE)。

### 2026-10-20 · 1.0.0

首次發布：2.4／5／6 GHz 掃描、20–320 MHz 頻寬、Wi-Fi 7 MLO、頻譜圖、趨勢與頻道評分；214 個離線測試涵蓋 Android 8–16。

First release: 2.4/5/6 GHz scanning, 20–320 MHz widths, Wi-Fi 7 MLO, spectrum, trend and channel rating; 214 offline tests cover Android 8–16.

[公開版使用說明（繁體中文純文字）](docs/使用說明.txt)
