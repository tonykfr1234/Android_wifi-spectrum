# 上架 Google Play 步驟

這份指南從產生金鑰一路到送出審核。指令都在專案根目錄（`wifi-spectrum/`）執行。

## 0. 事前準備

| 項目 | 說明 |
|---|---|
| 電腦 | Windows、macOS 或 Linux 都可以，需要 JDK 17 以上 |
| Android Studio | 建議安裝，會一併裝好 Android SDK |
| Google Play 開發人員帳號 | 一次性費用 25 美元，需要身分驗證 |
| 隱私權政策網址 | 公開可存取的網頁，內容可用 `docs/PRIVACY.md` |

## 1. 改成你自己的套件名稱（只能在第一次上架前改）

套件名稱一旦上架就永遠不能改。打開 `app/build.gradle.kts`，把兩處 `com.wifispectrum.analyzer` 改成你自己的，例如 `com.你的名字.wifispectrum`：

```kotlin
namespace = "com.wifispectrum.analyzer"
applicationId = "com.wifispectrum.analyzer"
```

`applicationId` 一定要改；`namespace` 可以不改（改了的話，Kotlin 原始碼的 package 也要一起搬）。

同時填好 `docs/PRIVACY.md` 最後的聯絡信箱。

## 2. 產生上傳金鑰

金鑰要放在**專案資料夾外面**。`scripts/security_scan.py` 只要在專案裡看到 `.jks` 檔就會判定失敗，避免不小心提交。

macOS／Linux：

```bash
mkdir -p ~/android-keys
keytool -genkeypair -v -keystore ~/android-keys/wifi-spectrum-upload.jks -alias upload \
  -keyalg RSA -keysize 2048 -validity 10000
```

Windows（PowerShell）：

```powershell
mkdir $HOME\android-keys
keytool -genkeypair -v -keystore $HOME\android-keys\wifi-spectrum-upload.jks -alias upload `
  -keyalg RSA -keysize 2048 -validity 10000
```

接著複製設定檔，填入金鑰的**完整路徑**和剛才設定的密碼：

```bash
cp keystore.properties.example keystore.properties
```

`keystore.properties` 已經被 `.gitignore` 排除，掃描器也會在它被 Git 追蹤時判定失敗。金鑰檔請另外備份，例如存到密碼管理器。Google Play 會另外保管正式的簽署金鑰（Play 應用程式簽署），所以上傳金鑰遺失時可以申請重設，但會耽誤好幾天。

## 3. 建置要上傳的 App Bundle

```bash
python3 scripts/security_scan.py .
./gradlew testDebugUnitTest lintDebug bundleRelease
```

這兩條指令會先做機密掃描，再跑完全部測試和 Lint，都通過才會產生：

```
app/build/outputs/bundle/release/app-release.aab
```

確認有簽章：

```bash
jarsigner -verify app/build/outputs/bundle/release/app-release.aab
```

看到 `jar verified.` 就可以上傳。

## 4. 在 Play Console 建立應用程式

1. 到 [Google Play Console](https://play.google.com/console) 按「建立應用程式」。
2. 應用程式名稱：`Wi-Fi 頻譜分析儀`；預設語言：中文（台灣）；類型：應用程式；免費。
3. 勾選同意開發人員計畫政策。

## 5. 先跑測試軌道

**2023 年 11 月 13 日之後註冊的個人開發人員帳號**，必須先完成「封閉測試」：至少 12 位測試人員、連續加入 14 天以上，才能申請上正式版。公司帳號沒有這個限制。

1. 「測試 → 內部測試」：建立版本，上傳 `app-release.aab`。第一次上傳時接受「Play 應用程式簽署」。
2. 用自己的手機從內部測試連結安裝，實際掃描一次：
   - 2.4、5、6 GHz 是否都有掃到（6 GHz 需要手機和路由器都支援 Wi-Fi 6E 以上）。
   - 有 Wi-Fi 7 路由器的話，到「MLO」分頁確認多條連結有合併顯示。
   - 權限畫面選「大概位置」時，App 會提示要選「精確位置」。
3. 「測試 → 封閉測試」：加入 12 位以上測試人員的 Gmail，等滿 14 天。

## 6. 商店資訊

「拓展 → 主要商店資訊」：

| 欄位 | 填入 |
|---|---|
| 應用程式名稱（30 字內） | Wi-Fi 頻譜分析儀 |
| 簡短說明（80 字內） | 掃描 2.4/5/6 GHz Wi-Fi，分析頻道、頻寬（含 320 MHz）與 Wi-Fi 7 MLO |
| 完整說明 | 見下方範本 |
| 應用程式圖示 | `store/icon_512.png` |
| 主題圖片 | `store/feature_1024x500.png` |
| 手機螢幕截圖 | `store/phone_1_list.png` 到 `phone_6_detail.png` |

完整說明範本：

```
Wi-Fi 頻譜分析儀是給網管人員與進階使用者的專業 Wi-Fi 分析工具。

【全頻段掃描】
同時掃描 2.4 GHz、5 GHz、6 GHz 附近所有基地台，顯示訊號強度、頻道、Wi-Fi 標準與加密方式。

【頻寬辨識】
辨識 20、40、80、160、80+80、320 MHz 頻寬，標出每台基地台實際佔用的頻率範圍與涵蓋的頻道。

【Wi-Fi 7 多連結（MLO）】
讀取 Wi-Fi 7 基地台的 MLD 位址與各條連結，把同一台路由器在不同頻段的連結合併顯示，並顯示你目前連線使用中的連結。

【頻道圖與訊號趨勢】
用頻譜曲線一眼看出頻道重疊情形；時間圖記錄最近 60 次掃描的訊號變化。

【頻道評分】
依附近基地台的訊號強度與重疊程度幫每個頻道打分數，推薦最適合的頻道。2.4 GHz 只推薦 1、6、11；5 GHz 優先推薦非 DFS 頻道；6 GHz 優先推薦 PSC 頻道。

【隱私】
不需要網路、沒有廣告、不收集任何資料。

※ 6 GHz 需要支援 Wi-Fi 6E 的手機；320 MHz 與 MLO 需要 Android 13 以上且支援 Wi-Fi 7 的手機。
```

## 7. 應用程式內容（政策問卷）

「政策 → 應用程式內容」逐項完成：

| 項目 | 怎麼填 |
|---|---|
| 隱私權政策 | 貼上隱私權政策網址（見第 8 步） |
| 廣告 | 否，沒有廣告 |
| 應用程式存取權 | 所有功能都不需要登入 |
| 內容分級 | 填問卷，類別選「工具」，所有暴力、色情等問題都選否 |
| 目標對象 | 18 歲以上（或 13 歲以上） |
| 資料安全性 | 「應用程式會收集或分享使用者資料嗎？」選**否**。掃描結果只在手機上處理、不傳出裝置，依 Google 定義不算「收集」 |
| 政府應用程式、金融功能、健康 | 都選否 |

本 App 只在前景使用位置，不需要填「背景位置存取權」聲明。

## 8. 公開隱私權政策網址

最簡單的方式是用 GitHub Pages：

1. 把專案推到你的 GitHub repo。
2. 「Settings → Pages」，Source 選 `main` 分支、資料夾選 `/docs`。
3. 網址會是 `https://你的帳號.github.io/repo名稱/PRIVACY`。

不用 GitHub 的話，Google 協作平台（Google Sites）也可以免費建立公開頁面。

## 9. 送審

1. 封閉測試滿 14 天後，在「資訊主頁」申請正式版存取權。
2. 「正式版 → 建立新版本」，選擇已上傳的 AAB，填寫版本資訊（例如「首次發布」）。
3. 送審。第一次審核通常要幾天，最長可能一到兩週。

## 10. 之後要更新

1. `app/build.gradle.kts` 的 `versionCode` 加 1（例如 1 → 2），`versionName` 改成新版本號（例如 `1.0.1`）。
2. 重跑第 3 步。
3. 在 Play Console 建立新版本並上傳。

## 常見退件原因

- **隱私權政策網址打不開**或內容沒提到位置權限：本專案的 `PRIVACY.md` 已經寫明。
- **商店說明誇大**：不要寫「所有手機都能看 6 GHz」，說明範本已經註明硬體需求。
- **截圖比例錯誤**：長邊不能超過短邊 2 倍；`store/` 裡的截圖是 1078×2152，符合規定。
