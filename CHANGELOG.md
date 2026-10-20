# Changelog

## 1.0.0 - 2026-10-20

- Scan 2.4, 5 and 6 GHz access points with Android's Wi-Fi scanner, honouring the four-scans-per-two-minutes foreground limit (30-second default interval) and reporting throttled, Wi-Fi-off and location-off states.
- Identify 20, 40, 80, 160, 80+80 and 320 MHz channel widths. Compute each AP's occupied frequency block and covered channels, tolerating missing or VHT-style centre frequencies reported by drivers.
- Read Wi-Fi 7 multi-link (MLO) data on Android 13+: MLD address, link ID and affiliated links from scan results; active associated links with per-link RSSI on Android 14+. Group links of one MLD across bands.
- Spectrum view with one parabola per AP, bold traces for every link of the connected MLD, a fit-to-width overview for 5/6 GHz and a scrollable zoom.
- Signal trend over the last 60 scans with bounded per-AP history.
- Channel rating per band: only 1/6/11 on 2.4 GHz; non-DFS and non-UNII-4 channels preferred on ties at 5 GHz; PSC channels preferred at 6 GHz.
- Offline vendor names from the bundled IEEE MA-L registry (40,354 entries); randomised MACs are marked instead of misattributed.
- Search and filters for band, minimum width, Wi-Fi 7 and hidden networks; three sort orders.
- Permission screen with prominent disclosure, a precise-location hint and a system-settings fallback for permanently denied permissions. Demo mode with a synthetic Taipei apartment for exploring without hardware.
- 214 offline tests: core logic, mapping of real `ScanResult` objects on Android 8/10/11/13/14/15/16, ViewModel timing, Compose interaction, rendered-screen checks, 1,000-AP stress, 20,000-scan memory bound and 200,000-input fuzzing. Android Lint clean.
- Google Play listing assets, App Bundle signing setup and a step-by-step publishing guide.
- 12 localized READMEs, a 40-second GIF/MP4 walkthrough rendered from the real UI with synthetic data, secret scanner and GitHub Actions CI.

### 繁體中文

- 使用 Android Wi-Fi 掃描 2.4、5、6 GHz 基地台，遵守前景每 2 分鐘 4 次的掃描限制（預設每 30 秒），並顯示節流、Wi-Fi 關閉、定位關閉狀態。
- 辨識 20、40、80、160、80+80、320 MHz 頻寬，計算每台基地台實際佔用的頻率範圍與涵蓋頻道；驅動程式回報的中心頻率缺漏或格式不同時，改用標準頻道規劃推算。
- Android 13 以上讀取 Wi-Fi 7 MLO：MLD 位址、Link ID、關聯連結；Android 14 以上讀取連線中各條連結與訊號。同一台 MLD 的各頻段連結合併顯示。
- 頻譜圖：每台基地台一條拋物線，連線中 MLD 的所有連結加粗；5／6 GHz 預設整段總覽，可放大滑動。
- 訊號趨勢記錄最近 60 次掃描，記憶體有上限。
- 頻道評分：2.4 GHz 只推薦 1／6／11；5 GHz 干擾相同時優先非 DFS、非 UNII-4；6 GHz 優先 PSC。
- 內建 IEEE MA-L 廠商表（40,354 筆），離線顯示廠商；隨機 MAC 會標示而不誤判廠商。
- 權限畫面明確說明用途，提示選「精確位置」，並提供永久拒絕後到系統設定開啟的入口。示範模式可在沒有硬體時體驗。
- 214 個離線測試：核心邏輯、7 個 Android 版本的真實 `ScanResult` 轉換、ViewModel 計時、介面互動、渲染畫面檢查、1,000 台基地台壓力、2 萬次掃描記憶體上限、20 萬筆亂碼輸入。Lint 零問題。
- Google Play 商店素材、App Bundle 簽章設定與上架指南。
- 12 種語言 README、以真實介面加合成資料渲染的 40 秒 GIF／MP4 示範、機密掃描與 GitHub Actions CI。
