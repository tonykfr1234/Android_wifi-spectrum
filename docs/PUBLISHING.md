# Publishing to Google Play

A full walkthrough in Traditional Chinese, including listing text, is in [PUBLISHING.zh-TW.md](PUBLISHING.zh-TW.md). Summary:

1. **Change `applicationId`** in `app/build.gradle.kts` to your own package name. It cannot change after the first upload.
2. **Create an upload key outside the project folder**:
   ```bash
   mkdir -p ~/android-keys
   keytool -genkeypair -v -keystore ~/android-keys/wifi-spectrum-upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
   cp keystore.properties.example keystore.properties   # fill in the absolute path and passwords
   ```
3. **Build**:
   ```bash
   python scripts/security_scan.py .
   ./gradlew testDebugUnitTest lintDebug bundleRelease
   jarsigner -verify app/build/outputs/bundle/release/app-release.aab
   ```
4. **Play Console**: create the app, upload the bundle to internal testing and accept Play App Signing.
5. **Closed testing**: personal developer accounts created after 13 November 2023 need at least 12 testers opted in for 14 days before applying for production.
6. **Store listing**: icon `store/icon_512.png`, feature graphic `store/feature_1024x500.png`, phone screenshots `store/phone_*.png` (1078×2152, within the 2:1 limit).
7. **App content**: privacy policy URL (publish `docs/PRIVACY.md`, e.g. with GitHub Pages from `/docs`), no ads, data safety "no data collected or shared", content rating category Utility. No background-location declaration is needed.
8. **Release**: create a production release, submit for review. For updates, increase `versionCode`.
