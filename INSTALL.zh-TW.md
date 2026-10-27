# Android Wi-Fi 頻譜分析儀：安裝與使用

## 最快使用：下載 APK
1. 開啟 [GitHub Actions](https://github.com/tonykfr1234/Android_wifi-spectrum/actions)，選最新成功的建置，下載 `wifi-spectrum-debug-apk`。下載 Actions 成品通常需登入 GitHub；過期的成品請改用自行建置。
2. 解壓縮後把 `app-debug.apk` 傳到 Android 8 以上手機，點選安裝；依手機提示允許該來源安裝。
3. 開啟 App，允許精確位置及系統要求的附近 Wi-Fi 裝置權限，並開啟系統定位服務。

## Windows 一鍵建置並安裝
先安裝 JDK 17、Android Studio 的 Android SDK 36 與 Platform-Tools。Android Studio 開啟本專案完成 SDK 設定與授權，或設定 `ANDROID_HOME` 指向 SDK。
下載整個儲存庫 ZIP 並完整解壓縮，或使用 Git clone。不要只下載批次檔。
手機開啟開發人員選項、USB 偵錯，以 USB 連接並同意授權；只保留一台已授權裝置，關閉其他模擬器。

雙擊根目錄 **`INSTALL.bat`**。它會建置 debug APK，再透過 ADB 安裝到唯一裝置；不會自動解除安裝舊版本或清除資料。
首次建置需要網路下載 Gradle 與相依套件。無裝置、多裝置或未授權時會停止安裝，APK 仍在 `app/build/outputs/apk/debug/app-debug.apk`。

macOS／Linux 可使用 `./gradlew assembleDebug`，再手動安裝 APK。此腳本不會替你安裝 JDK 或接受 SDK 授權。

## 日常操作
開啟 App，查看頻譜、訊號趨勢、頻道評分及 MLO 頁面；掃描受 Android 權限與節流限制。
6 GHz、320 MHz 與 MLO 是否可用取決於手機硬體及系統版本。沒有資料時先確認定位服務與權限，也可使用示範模式。
更多操作見 [完整使用說明](docs/USER_GUIDE.zh-TW.md)。

## 更新與排錯
重新取得最新版完整專案後執行 `INSTALL.bat`，或安裝新 APK。同一 applicationId 的更新必須使用相同簽章；不同電腦的 debug 金鑰可能不同。遇到簽章不符，先保留資料並評估備份，不要直接解除安裝。
找不到 SDK：先在 Android Studio 同步專案。ADB 找不到：安裝 Platform-Tools，設定 `ANDROID_HOME`。未授權：解鎖手機確認 USB 提示。
本次安裝入口已檢查腳本與錯誤處理；不代表每款手機皆已實機驗證。
