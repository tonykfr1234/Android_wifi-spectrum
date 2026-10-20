# Contributing

Contributions are welcome. Keep changes focused and testable.

Before opening a pull request:

1. Do not commit signing keys, `keystore.properties`, `local.properties`, API keys or real scan exports.
2. Run `python scripts/security_scan.py .`.
3. Run `./gradlew testDebugUnitTest lintDebug`. All tests must pass and Lint must report no issues.
4. Run `python scripts/update_checksums.py` after staging your changes, then commit `SHA256SUMS.txt`.
5. Describe the phones and routers used for testing. For scanning changes, state the Android version, whether the phone supports Wi-Fi 6E/7, and what the router reported (band, width, MLO).

The tests need no phone, emulator or Wi-Fi hardware. Robolectric renders the Compose UI on the JVM and downloads its Android runtime jars on first use.

For channel-width changes, cover every band and width in `ChannelsTest`, including drivers that omit `centerFreq0` or report the 160 MHz centre in `centerFreq1`. For MLO changes, test missing MLD addresses, `00:00:00:00:00:00` and links whose RSSI is unknown. New Android API calls must be guarded by the API level Lint reports; add the version to `AndroidScanSourceTest` if a new code path depends on it.

Keep the English and Traditional Chinese guides and all 12 README introductions aligned when behavior changes. The application interface currently uses Traditional Chinese.

## Generated media

Run `python scripts/generate_demo.py` with the Android SDK, JDK 17+ and ffmpeg installed. It renders the real UI with the built-in synthetic environment and regenerates `docs/media/` (GIF, MP4 and three PNGs), `assets/wifi-spectrum.png` and the Google Play images in `store/`. Captions say the data is synthetic; keep that statement in every localized description and do not substitute private scans.

`SHA256SUMS.txt` records SHA-256 hashes of the committed file bytes, excluding itself. Git may convert text line endings in a Windows checkout; the committed/archive bytes are the reference.

`app/src/main/assets/oui.tsv.gz` is generated from the IEEE registry with `python scripts/build_oui.py oui.csv app/src/main/assets/oui.tsv.gz`.
