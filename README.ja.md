<p align="center">
  <a href="README.md">English</a> · <a href="README.zh-CN.md">简体中文</a> · <a href="README.zh-TW.md">繁體中文</a> · <strong>日本語</strong> · <a href="README.ko.md">한국어</a><br>
  <a href="README.de.md">Deutsch</a> · <a href="README.es.md">Español</a> · <a href="README.fr.md">Français</a> · <a href="README.pt-BR.md">Português (Brasil)</a> · <a href="README.vi.md">Tiếng Việt</a> · <a href="README.id.md">Bahasa Indonesia</a> · <a href="README.it.md">Italiano</a>
</p>

<p align="center"><img src="assets/wifi-spectrum.png" width="112" alt="Wi-Fi Spectrum アイコン"></p>
<h1 align="center">Android 向けプロ仕様 Wi-Fi アナライザー</h1>
<p align="center">Wi-Fi 頻譜分析儀 · 2.4 / 5 / 6 GHz · 20–320 MHz · Wi-Fi 7 MLO</p>
<p align="center">
  <a href="https://github.com/tonykfr1234/android_wifi-spectrum/actions/workflows/security.yml"><img src="https://github.com/tonykfr1234/android_wifi-spectrum/actions/workflows/security.yml/badge.svg" alt="CI"></a>
  <img src="https://img.shields.io/badge/platform-Android%208%2B-3DDC84" alt="Android 8+">
  <img src="https://img.shields.io/badge/Kotlin-2.2-7F52FF" alt="Kotlin 2.2">
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4" alt="Jetpack Compose">
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-green" alt="MIT ライセンス"></a>
</p>
<p align="center">
  <a href="#quick-start">はじめに</a> · <a href="#workflow">動作の流れ</a> · <a href="#features">機能</a> · <a href="#demo">デモ</a> · <a href="#faq">よくある質問</a> · <a href="#documentation">ドキュメント</a>
</p>

```bash
./gradlew testDebugUnitTest assembleDebug
```

---

バージョン 1.0：3 つの周波数帯、最大 320 MHz までのすべてのチャネル幅、MLD アドレスでまとめた Wi-Fi 7 マルチリンク（MLO）。

Wi-Fi Spectrum は周囲の 2.4・5・6 GHz アクセスポイントをスキャンし、それぞれが実際に送信している範囲を表示します。プライマリチャネル、20/40/80/160/80+80/320 MHz のブロック全体、そしてカバーするすべてのチャネルです。スペクトラム表示、信号の推移、チャネル評価で空いているチャネルを選べます。MLO 画面では Wi-Fi 7 ルーターごとのリンクをまとめて表示します。

Apple は iOS アプリに周囲の Wi-Fi のスキャンを許可していないため、これらの機能は Android 版のみです。iOS 版 [IOS_wifi-spectrum](https://github.com/tonykfr1234/ios_wifi-spectrum) は iOS で可能な範囲の機能を提供します。

12 言語の README はドキュメントの翻訳です。アプリの画面は現在、繁体字中国語です。

<a id="quick-start"></a>
## はじめに

1. [GitHub Actions](https://github.com/tonykfr1234/android_wifi-spectrum/actions) の最新の成功した実行から `wifi-spectrum-debug-apk` をダウンロードするか、`./gradlew assembleDebug` でビルドします（JDK 17 以上と Android SDK が必要）。
2. `app-debug.apk` を Android 8 以上のスマートフォンにインストールします。6 GHz には Wi-Fi 6E、320 MHz と MLO には Wi-Fi 7 対応の Android 13 以上が必要です。
3. アプリを開き、**正確な**位置情報と、Android 13 以上では「付近の Wi-Fi デバイス」を許可します。許可がないと Android はスキャン結果を返しません。
4. 位置情報サービスをオンにしておきます。表示中は 30 秒ごとにスキャンし、更新ボタンですぐにスキャンできます。
5. 対応端末が手元にない場合は、デモモードで合成された Wi-Fi 7 環境を確認できます。

**Google Play に公開する場合**は [docs/PUBLISHING.zh-TW.md](docs/PUBLISHING.zh-TW.md)（[英語の概要](docs/PUBLISHING.md)）に従い、アプリ ID の変更、プロジェクト外でのアップロード鍵の作成、署名済み App Bundle のビルド、Play Console のクローズドテストを行ってください。

<a id="workflow"></a>
## 動作の流れ

```text
アプリ表示中、30 秒ごと（Android はフォアグラウンドで 2 分間に 4 回まで）
  → WifiManager.startScan()。制限時はキャッシュ済みの結果を使用
  → 各 ScanResult を整理：SSID、BSSID、周波数帯、プライマリチャネル
  → チャネル幅と中心周波数から占有ブロックを算出（20–320 MHz、80+80）
  → MLD アドレスで Wi-Fi 7 リンクをまとめ、ベンダーをオフライン検索
  → 一覧、スペクトラム、60 回分の推移、チャネル評価、MLO を更新
```

アプリが画面から離れるとスキャンを停止し、バックグラウンドでは動作しません。ドライバーが中心周波数を返さない場合や、160 MHz の中心を `centerFreq1` に入れる場合は、標準のチャネル配置から算出します。MLO の項目には Android 13、接続中ルーターのリンク別信号には Android 14 が必要です。

<a id="features"></a>
## 機能

| 機能 | 動作 |
| --- | --- |
| 周波数帯 | 2.4、5（UNII-4 を含む）、6 GHz。スキャン制限、Wi-Fi オフ、位置情報オフを通知 |
| チャネル幅 | 20、40、80、160、80+80、320 MHz。占有範囲、中心チャネル、カバーするチャネル |
| Wi-Fi 7 MLO | MLD アドレス、リンク ID、関連リンク。接続中のアクティブリンクとリンク別 RSSI |
| スペクトラム | AP ごとに放物線を描画。全帯域の概要と拡大表示。接続中ルーターのリンクは太線 |
| 推移 | 上位 10 台の AP の RSSI を 60 回分表示。検出されなかった回は線が途切れる |
| チャネル評価 | 信号強度と重なりで重み付け。2.4 GHz は 1/6/11 のみ。非 DFS を優先。6 GHz は PSC を優先 |
| 詳細 | IEEE 登録簿のベンダー名、ランダム MAC の判定、WPA3 SAE-EXT や OWE を含む暗号化方式、距離の推定 |
| プライバシー | ネットワーク機能、広告、解析ツールなし。スキャン結果はメモリ上のみ |

距離は障害物のない環境での推定値で、壁があると短くなります。スキャン内容は端末の Wi-Fi チップとドライバーに依存するため、6 GHz、320 MHz、MLO の値は自分の機器で確認してください。

<a id="demo"></a>
## デモ

この 40 秒の無音デモは、**実際のアプリ画面を内蔵の合成環境で動かしたもの**です。Wi-Fi 7 ルーター（MLO リンク 3 本）と混み合った近隣がある台北のアパートを想定し、各画面に中英の字幕を付けています。実際のスキャンではなく、特定の端末の無線性能を示すものでもありません。

![合成データによるアプリのデモ](docs/media/wifi-spectrum-demo.gif)

[MP4 デモを再生またはダウンロード](docs/media/wifi-spectrum-demo.mp4)

| 2.4 GHz スペクトラム | 6 GHz と 320 MHz | Wi-Fi 7 MLO リンク |
| --- | --- | --- |
| ![2.4 GHz スペクトラム](docs/media/spectrum-24ghz.png) | ![6 GHz と 320 MHz](docs/media/spectrum-6ghz.png) | ![Wi-Fi 7 MLO リンク](docs/media/mlo-links.png) |

## 最新の変更

初回リリース：3 帯域スキャン、20–320 MHz のチャネル幅、Wi-Fi 7 MLO、スペクトラム、推移、チャネル評価、Google Play 用素材と公開ガイド。Android 8–16 を対象とした 214 件のオフラインテストで検証済みです。[変更履歴](CHANGELOG.md)を参照してください。

<a id="controls"></a>
## 操作

| 操作 | 動作 |
| --- | --- |
| 更新 | すぐにスキャン |
| 一時停止／再生 | 自動スキャンの停止と再開 |
| ⋮ → 間隔 | 3、5、10、30、60 秒。30 秒未満は Android に制限される場合あり |
| ⋮ → デモモード | 実際のスキャンと合成データを切り替え |
| 周波数帯／幅／Wi-Fi 7 チップ | 一覧を絞り込み。幅チップで ≥40/80/160/320 MHz を順に切り替え |
| 拡大（5/6 GHz） | 全帯域の概要とスクロールできる拡大表示を切り替え |
| AP をタップ | 占有範囲、カバーするチャネル、暗号化方式、MLO リンクを表示 |

<a id="faq"></a>
## よくある質問

**なぜ位置情報が必要ですか？** Android は Wi-Fi のスキャン結果を位置情報として扱います。正確な位置情報（Android 13 以上では付近の Wi-Fi デバイスも）がないと結果は返りません。アプリは GPS 座標を読み取りません。

**6 GHz に何も表示されないのはなぜですか？** 端末が Wi-Fi 6E に対応し、近くに 6 GHz のルーターがある必要があります。

**MLO や 320 MHz が表示されないのはなぜですか？** Wi-Fi 7 対応の Android 13 以上の端末と、MLO を有効にした Wi-Fi 7 ルーターが必要です。

**スキャンが制限されていると表示されるのはなぜですか？** Android はフォアグラウンドで 2 分間に 4 回までスキャンを許可します。30 秒間隔のままにするか、テスト時は開発者向けオプションで Wi-Fi スキャンの制限を無効にしてください。

**iOS 版はありますか？** Apple の API は周囲のネットワークを公開していないため、iOS ではこのスキャンはできません。iOS で可能な機能は [IOS_wifi-spectrum](https://github.com/tonykfr1234/ios_wifi-spectrum) をご覧ください。

<a id="docs"></a>
<a id="documentation"></a>
## ドキュメント

| 項目 | ドキュメント |
| --- | --- |
| 日常の操作 | [繁中使用指南](docs/USER_GUIDE.zh-TW.md) · [English User Guide](docs/USER_GUIDE.md) |
| 内部構造 | [アーキテクチャ](docs/ARCHITECTURE.md) |
| 公開 | [Google Play 公開ガイド（繁体字）](docs/PUBLISHING.zh-TW.md) · [英語の概要](docs/PUBLISHING.md) · [プライバシーポリシー](docs/PRIVACY.md) |
| バージョンと安全性 | [変更履歴](CHANGELOG.md) · [セキュリティ](SECURITY.md) · [コントリビューション](CONTRIBUTING.md) |

コードは `app/src/main/java`、テストは `app/src/test`、ツールは `scripts/`、Google Play 用画像は `store/` にあります。214 件のテストは端末なしで JVM 上で動きます。実際の 6 GHz、320 MHz、MLO の値は自分の機器で確認してください。

## セキュリティ

キーストア、`keystore.properties`、`local.properties`、API キー、自分のものではないネットワークのスキャン結果をコミットしないでください。プッシュ前に GitHub Actions と同じスキャナーを実行します：

```bash
python scripts/security_scan.py .
```

## ライセンス

[MIT](LICENSE)。

### 2026-10-20 · 1.0.0

首次發布：2.4／5／6 GHz 掃描、20–320 MHz 頻寬、Wi-Fi 7 MLO、頻譜圖、趨勢與頻道評分；214 個離線測試涵蓋 Android 8–16。

First release: 2.4/5/6 GHz scanning, 20–320 MHz widths, Wi-Fi 7 MLO, spectrum, trend and channel rating; 214 offline tests cover Android 8–16.

[公開版の使い方（繁体字中国語、テキスト形式）](docs/使用說明.txt)
