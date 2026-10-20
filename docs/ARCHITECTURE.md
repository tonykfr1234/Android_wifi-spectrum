# Architecture

```text
WifiManager.startScan()               every 30 s while the app is visible
  → SCAN_RESULTS_AVAILABLE broadcast   (or cached results when throttled)
  → AndroidScanSource                  ScanResult → RawScan, API-level guarded
  → ScanMapper                         clean SSID/BSSID, drop invalid rows, dedupe
  → AccessPoint                        band, channel, occupied spans, security
  → ScanViewModel                      history, MLO groups, filters, scan loop
  → Compose screens                    list, spectrum, trend, rating, MLO
```

## Layers

| Package | Responsibility | Android dependency |
| --- | --- | --- |
| `core` | Channel/frequency maths, occupied spans, security parsing, MLO grouping, channel rating, signal history, filters, IEEE vendor lookup | None |
| `data` | `AndroidScanSource` (WifiManager, broadcast receiver, permissions, connection info), `ScanMapper`, `DemoScanSource` | Only `AndroidScanSource` |
| root | `ScanViewModel`, `MainActivity` | Lifecycle, activity |
| `ui` | Theme, components, charts, screens, `WifiApp` | Compose |

`core` is plain Kotlin so every rule can be tested on the JVM without Robolectric.

## Occupied spans

`Channels.occupiedMhz` returns the frequency blocks an AP transmits on:

- 20 MHz: primary ±10.
- 40/80/160/320 MHz: `centerFreq0` if the primary 20 MHz fits inside the block, otherwise the standard channel plan (`inferCenter`).
- 160 MHz reported VHT-style (centre of the primary 80 in `centerFreq0`, full centre in `centerFreq1`): use `centerFreq1`.
- 80+80 MHz: two 80 MHz blocks.

2.4 GHz 40 MHz without a centre assumes the secondary channel above for channels 1–7 and below for 8–13. 6 GHz 320 MHz without a centre uses the 320 MHz-1 plan (centres 31, 95, 159).

## MLO

Scan results expose `getApMldMacAddress`, `getApMloLinkId` and `getAffiliatedMloLinks` from API 33. `MloLink.getRssi` and `WifiInfo.getAssociatedMloLinks` need API 34; Lint enforces both guards and `AndroidScanSourceTest` runs on API 26–36. `Mlo.group` merges scan entries by case-insensitive MLD address and lists advertised links that the scan did not see.

## Scan loop and throttling

`ScanViewModel` scans immediately on start, then every `intervalSec`. Android permits four foreground scans per two minutes; when `startScan` returns false the source republishes cached results with `fresh = false` and the UI shows the throttling banner. The loop stops in `onStop`, so nothing scans in the background.

## Memory bounds

`SignalHistory` keeps at most 120 samples per BSSID and evicts APs unseen for 30 scans. The stress tests confirm the tracked set stays below `aps-per-scan × 31` over 20,000 scans with constantly changing BSSIDs.

## Channel rating

For each 20 MHz candidate channel, every overlapping AP adds `weight(rssi) × overlap / 20 MHz`, where `weight` maps -100…-30 dBm to 0…1 squared. Stars come from the sum. `recommend` sorts by interference, then by preferred channel (1/6/11; 36–48 and 149–165; PSC), then by number.
