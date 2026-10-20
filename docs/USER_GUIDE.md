# User Guide

The app interface is in Traditional Chinese. Labels below are given as 中文 (English).

## First launch

1. The app explains why it needs permissions. Tap 允許權限並開始掃描 (allow and start scanning).
2. When Android asks for location, choose **Precise** and **While using the app**. With approximate location only, Android withholds Wi-Fi scan results.
3. On Android 13+, also allow Nearby Wi-Fi devices.
4. If no prompt appears (you previously chose "Don't ask again"), tap 到系統設定開啟權限 to open system settings.
5. To explore without scanning, tap 先看示範模式 (demo mode).

## Tabs

| Tab | Purpose |
| --- | --- |
| 網路 (Networks) | Current connection (speeds, IP, active MLO links) and every access point |
| 頻譜 (Spectrum) | One curve per AP: height is signal strength, width is channel width |
| 趨勢 (Trend) | Signal over the last 60 scans |
| 評分 (Rating) | Channel congestion and recommended channels |
| MLO | Links of each Wi-Fi 7 router grouped together |

## Top bar

| Button | Action |
| --- | --- |
| Refresh | Scan now |
| Pause / Play | Stop or resume automatic scanning |
| ⋮ | Scan interval, demo mode, privacy policy, about |

The default interval is 30 seconds. Android lets a foreground app scan four times per two minutes; shorter intervals show a throttling notice and the previous results. Developers can disable Wi-Fi scan throttling in Developer options.

## Network list

- The number on the left is RSSI in dBm: -50 or higher is excellent, -60 good, -70 fair, below -80 very weak.
- Tags: band and primary channel, width, Wi-Fi generation, security, MLO link ID.
- A red security tag marks open, WEP or WPA-only networks.
- Distance is a free-space estimate; walls make the real distance shorter.
- Search by SSID or BSSID. Filters: band, minimum width (≥40/80/160/320), Wi-Fi 7 only, hidden networks, sort order.
- Tap an AP for its occupied range, covered channels, PSC/DFS status, raw capabilities and MLO links.

## Spectrum

- 2.4 GHz has three non-overlapping channels: 1, 6 and 11. Overlapping curve bases interfere.
- 5 and 6 GHz open as a full-band overview. Tap 放大 (zoom) to scroll.
- Bold curves are the connected AP and, for a Wi-Fi 7 router, its links on other bands.
- Purple channel numbers on 6 GHz are Preferred Scanning Channels.

## Channel rating

- Scores weigh nearby APs by signal strength and overlapping bandwidth.
- 2.4 GHz recommends only 1, 6 and 11.
- On 5 GHz, ties prefer 36–48 and 149–165. Channels 52–144 are DFS (marked D): the router must vacate them when it detects radar, so they are recommended only when clearly quieter.
- On 6 GHz, PSC channels (marked P) are preferred so phones discover the network quickly.

## MLO

- Requires a Wi-Fi 7 router and an Android 13+ phone with Wi-Fi 7 support.
- The connected MLD section shows the links in use; per-link signal needs Android 14+.
- 已宣告，這次沒掃到 (advertised, not seen) means the router announced the link but this scan did not hear it, common for 6 GHz through walls.

## Troubleshooting

| Symptom | Cause |
| --- | --- |
| No 6 GHz APs | Phone lacks Wi-Fi 6E, or no 6 GHz router nearby |
| No 320 MHz or MLO | Phone lacks Wi-Fi 7, Android below 13, or feature off on the router |
| Wi-Fi standard 未知 (unknown) | Android 10 and earlier do not report it |
| 定位服務已關閉 (location off) | Turn on location; Android requires it for scan results |
| Connection name needs permission | Precise location not granted |
