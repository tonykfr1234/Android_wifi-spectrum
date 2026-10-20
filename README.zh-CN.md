<p align="center">
  <a href="README.md">English</a> · <strong>简体中文</strong> · <a href="README.zh-TW.md">繁體中文</a> · <a href="README.ja.md">日本語</a> · <a href="README.ko.md">한국어</a><br>
  <a href="README.de.md">Deutsch</a> · <a href="README.es.md">Español</a> · <a href="README.fr.md">Français</a> · <a href="README.pt-BR.md">Português (Brasil)</a> · <a href="README.vi.md">Tiếng Việt</a> · <a href="README.id.md">Bahasa Indonesia</a> · <a href="README.it.md">Italiano</a>
</p>

<p align="center"><img src="assets/wifi-spectrum.png" width="112" alt="Wi-Fi 频谱分析仪图标"></p>
<h1 align="center">专业 Wi-Fi 频谱分析仪（Android）</h1>
<p align="center">Wi-Fi 頻譜分析儀 · 2.4／5／6 GHz · 20–320 MHz · Wi-Fi 7 MLO</p>
<p align="center">
  <a href="https://github.com/tonykfr1234/android_wifi-spectrum/actions/workflows/security.yml"><img src="https://github.com/tonykfr1234/android_wifi-spectrum/actions/workflows/security.yml/badge.svg" alt="CI"></a>
  <img src="https://img.shields.io/badge/platform-Android%208%2B-3DDC84" alt="Android 8+">
  <img src="https://img.shields.io/badge/Kotlin-2.2-7F52FF" alt="Kotlin 2.2">
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4" alt="Jetpack Compose">
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-green" alt="MIT 许可证"></a>
</p>
<p align="center">
  <a href="#quick-start">快速开始</a> · <a href="#workflow">工作流程</a> · <a href="#features">功能</a> · <a href="#demo">演示</a> · <a href="#faq">常见问题</a> · <a href="#documentation">文档</a>
</p>

```bash
./gradlew testDebugUnitTest assembleDebug
```

---

1.0 版：三个频段、最高 320 MHz 的全部信道宽度，以及按 MLD 地址合并的 Wi-Fi 7 多链路（MLO）。

Wi-Fi 频谱分析仪扫描附近 2.4、5、6 GHz 的接入点，标出每一台实际发射的位置：主信道、完整的 20／40／80／160／80+80／320 MHz 区块以及覆盖的全部信道。频谱图、信号趋势和信道评分帮你挑出空闲信道；MLO 页面把每台 Wi-Fi 7 路由器的各条链路合并显示。

Apple 不允许 iOS 应用扫描附近的 Wi-Fi，因此这些功能只有 Android 版提供。iOS 版 [IOS_wifi-spectrum](https://github.com/tonykfr1234/ios_wifi-spectrum) 提供 iOS 允许范围内的功能。

12 种 README 语言是文档翻译，应用界面目前使用繁体中文。

<a id="quick-start"></a>
## 快速开始

1. 从 [GitHub Actions](https://github.com/tonykfr1234/android_wifi-spectrum/actions) 最近一次成功的运行下载 `wifi-spectrum-debug-apk`，或用 `./gradlew assembleDebug` 自行构建（需要 JDK 17 以上和 Android SDK）。
2. 把 `app-debug.apk` 安装到 Android 8 以上的手机。6 GHz 需要支持 Wi-Fi 6E；320 MHz 和 MLO 需要 Android 13 以上且支持 Wi-Fi 7 的手机。
3. 打开应用，允许**精确**位置；Android 13 以上还要允许“附近的 Wi-Fi 设备”。没有这些权限，Android 不会提供扫描结果。
4. 保持定位服务开启。应用在前台时每 30 秒扫描一次，点刷新可立即扫描。
5. 手边没有合适的手机？选择演示模式，查看模拟的 Wi-Fi 7 环境。

**要上架 Google Play？** 按照 [docs/PUBLISHING.zh-TW.md](docs/PUBLISHING.zh-TW.md)（[英文摘要](docs/PUBLISHING.md)）：修改应用 ID，在项目外生成上传密钥，构建签名的 App Bundle，并完成 Play 管理中心的封闭测试。

<a id="workflow"></a>
## 工作流程

```text
应用在前台，每 30 秒一次（Android 前台每 2 分钟最多 4 次）
  → WifiManager.startScan()；被限流时沿用系统缓存结果
  → 整理每条 ScanResult：SSID、BSSID、频段、主信道
  → 根据信道宽度和中心频率计算占用区块（20–320 MHz、80+80）
  → 按 MLD 地址合并 Wi-Fi 7 链路，离线查询厂商
  → 更新列表、频谱图、60 次趋势、信道评分和 MLO 页面
```

应用离开屏幕即停止扫描，不在后台运行。驱动没有报告中心频率，或把 160 MHz 中心放在 `centerFreq1` 时，改用标准信道规划推算。MLO 字段需要 Android 13；当前连接路由器各链路的信号需要 Android 14。

<a id="features"></a>
## 功能

| 功能 | 行为 |
| --- | --- |
| 频段 | 2.4、5（含 UNII-4）、6 GHz；显示限流、Wi-Fi 关闭、定位关闭提示 |
| 信道宽度 | 20、40、80、160、80+80、320 MHz；占用范围、中心信道与覆盖信道 |
| Wi-Fi 7 MLO | MLD 地址、链路 ID 与关联链路；当前连接的活动链路及各链路信号 |
| 频谱图 | 每个接入点一条抛物线；整段总览可放大；所连路由器的各条链路加粗 |
| 趋势 | 最强 10 个接入点在 60 次扫描中的信号；未扫到时线条中断 |
| 信道评分 | 按信号与重叠加权；2.4 GHz 只推荐 1／6／11；优先非 DFS；6 GHz 优先 PSC |
| 详细信息 | IEEE 注册表厂商、随机 MAC 识别、加密方式（含 WPA3 SAE-EXT、OWE）、距离估算 |
| 隐私 | 无网络功能、广告和统计工具；扫描结果只保存在内存 |

距离是无遮挡环境下的估算，穿墙后会更近。扫描内容取决于手机的 Wi-Fi 芯片和驱动；依赖 6 GHz、320 MHz 或 MLO 数值前，请用自己的设备确认。

<a id="demo"></a>
## 演示

以下 40 秒无声演示使用**应用的真实界面，配合内置的合成环境**：一间有 Wi-Fi 7 路由器（三条 MLO 链路）和拥挤邻居的台北公寓，并以中英字幕说明每个画面。这不是真实扫描，也不代表任何手机的无线性能。

![使用合成数据操作应用的演示](docs/media/wifi-spectrum-demo.gif)

[播放或下载 MP4 演示视频](docs/media/wifi-spectrum-demo.mp4)

| 2.4 GHz 频谱图 | 6 GHz 与 320 MHz | Wi-Fi 7 MLO 链路 |
| --- | --- | --- |
| ![2.4 GHz 频谱图](docs/media/spectrum-24ghz.png) | ![6 GHz 与 320 MHz](docs/media/spectrum-6ghz.png) | ![Wi-Fi 7 MLO 链路](docs/media/mlo-links.png) |

## 最新变更

首次发布：三频段扫描、20–320 MHz 信道宽度、Wi-Fi 7 MLO、频谱图、趋势、信道评分、Google Play 素材和上架指南，通过了 214 个覆盖 Android 8–16 的离线测试。详见[更新记录](CHANGELOG.md)。

<a id="controls"></a>
## 操作

| 操作 | 功能 |
| --- | --- |
| 刷新 | 立即扫描 |
| 暂停／播放 | 停止或恢复自动扫描 |
| ⋮ → 扫描间隔 | 3、5、10、30、60 秒；低于 30 秒可能被 Android 限流 |
| ⋮ → 演示模式 | 在真实扫描与合成数据之间切换 |
| 频段／宽度／Wi-Fi 7 筛选 | 筛选列表；点宽度可依次切换 ≥40／80／160／320 MHz |
| 放大（5／6 GHz） | 切换整段总览与可滑动的放大视图 |
| 点接入点 | 查看占用范围、覆盖信道、加密方式与 MLO 链路 |

<a id="faq"></a>
## 常见问题

**为什么需要位置权限？** Android 把 Wi-Fi 扫描结果视为位置信息。没有精确位置（Android 13 以上还需“附近的 Wi-Fi 设备”），系统不会返回任何结果。应用不读取 GPS 坐标。

**为什么 6 GHz 是空的？** 手机需要支持 Wi-Fi 6E，附近也要有 6 GHz 路由器。

**为什么没有 MLO 或 320 MHz？** 需要 Android 13 以上、支持 Wi-Fi 7 的手机，以及开启 MLO 的 Wi-Fi 7 路由器。

**为什么提示扫描被限流？** Android 前台每 2 分钟只允许扫描 4 次。保持 30 秒间隔，或测试时在开发者选项中关闭“WLAN 扫描调节”。

**有 iOS 版吗？** Apple 的 API 不提供附近网络信息，iOS 无法进行这些扫描。iOS 支持的功能见 [IOS_wifi-spectrum](https://github.com/tonykfr1234/ios_wifi-spectrum)。

<a id="docs"></a>
<a id="documentation"></a>
## 文档

| 主题 | 文档 |
| --- | --- |
| 日常使用 | [繁中使用指南](docs/USER_GUIDE.zh-TW.md) · [English User Guide](docs/USER_GUIDE.md) |
| 程序设计 | [架构](docs/ARCHITECTURE.md) |
| 上架 | [Google Play 上架指南（繁中）](docs/PUBLISHING.zh-TW.md) · [英文摘要](docs/PUBLISHING.md) · [隐私政策](docs/PRIVACY.md) |
| 版本与安全 | [更新记录](CHANGELOG.md) · [安全说明](SECURITY.md) · [贡献指南](CONTRIBUTING.md) |

代码在 `app/src/main/java`，测试在 `app/src/test`，工具在 `scripts/`，Google Play 图片在 `store/`。214 个测试在电脑上运行，不需要手机；真实的 6 GHz、320 MHz 和 MLO 数值仍需用自己的设备确认。

## 安全

不要提交密钥库、`keystore.properties`、`local.properties`、API Key，或不属于你的网络的扫描数据。推送前运行与 GitHub Actions 相同的扫描：

```bash
python scripts/security_scan.py .
```

## 许可证

[MIT](LICENSE)。

### 2026-10-20 · 1.0.0

首次發布：2.4／5／6 GHz 掃描、20–320 MHz 頻寬、Wi-Fi 7 MLO、頻譜圖、趨勢與頻道評分；214 個離線測試涵蓋 Android 8–16。

First release: 2.4/5/6 GHz scanning, 20–320 MHz widths, Wi-Fi 7 MLO, spectrum, trend and channel rating; 214 offline tests cover Android 8–16.

[公开版使用说明（繁体中文纯文本）](docs/使用說明.txt)
