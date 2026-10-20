<p align="center">
  <a href="README.md">English</a> · <a href="README.zh-CN.md">简体中文</a> · <a href="README.zh-TW.md">繁體中文</a> · <a href="README.ja.md">日本語</a> · <strong>한국어</strong><br>
  <a href="README.de.md">Deutsch</a> · <a href="README.es.md">Español</a> · <a href="README.fr.md">Français</a> · <a href="README.pt-BR.md">Português (Brasil)</a> · <a href="README.vi.md">Tiếng Việt</a> · <a href="README.id.md">Bahasa Indonesia</a> · <a href="README.it.md">Italiano</a>
</p>

<p align="center"><img src="assets/wifi-spectrum.png" width="112" alt="Wi-Fi Spectrum 아이콘"></p>
<h1 align="center">Android용 전문 Wi-Fi 분석기</h1>
<p align="center">Wi-Fi 頻譜分析儀 · 2.4 / 5 / 6 GHz · 20–320 MHz · Wi-Fi 7 MLO</p>
<p align="center">
  <a href="https://github.com/tonykfr1234/android_wifi-spectrum/actions/workflows/security.yml"><img src="https://github.com/tonykfr1234/android_wifi-spectrum/actions/workflows/security.yml/badge.svg" alt="CI"></a>
  <img src="https://img.shields.io/badge/platform-Android%208%2B-3DDC84" alt="Android 8+">
  <img src="https://img.shields.io/badge/Kotlin-2.2-7F52FF" alt="Kotlin 2.2">
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4" alt="Jetpack Compose">
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-green" alt="MIT 라이선스"></a>
</p>
<p align="center">
  <a href="#quick-start">빠른 시작</a> · <a href="#workflow">동작 흐름</a> · <a href="#features">기능</a> · <a href="#demo">데모</a> · <a href="#faq">자주 묻는 질문</a> · <a href="#documentation">문서</a>
</p>

```bash
./gradlew testDebugUnitTest assembleDebug
```

---

버전 1.0: 세 개의 주파수 대역, 최대 320 MHz까지 모든 채널 폭, MLD 주소로 묶은 Wi-Fi 7 멀티링크(MLO).

Wi-Fi Spectrum은 주변의 2.4·5·6 GHz 액세스 포인트를 스캔하고 각 AP가 실제로 송신하는 범위를 보여줍니다. 기본 채널, 20/40/80/160/80+80/320 MHz 블록 전체, 그리고 포함되는 모든 채널입니다. 스펙트럼 보기, 신호 추이, 채널 평가로 한산한 채널을 고를 수 있고, MLO 화면은 Wi-Fi 7 라우터별 링크를 하나로 묶어 보여줍니다.

Apple은 iOS 앱이 주변 Wi-Fi를 스캔하는 것을 허용하지 않으므로 이 기능은 Android 전용입니다. iOS 버전 [IOS_wifi-spectrum](https://github.com/tonykfr1234/ios_wifi-spectrum)은 iOS가 허용하는 범위의 기능을 제공합니다.

12개 언어 README는 문서 번역입니다. 앱 화면은 현재 번체 중국어를 사용합니다.

<a id="quick-start"></a>
## 빠른 시작

1. [GitHub Actions](https://github.com/tonykfr1234/android_wifi-spectrum/actions)의 최근 성공한 실행에서 `wifi-spectrum-debug-apk`를 내려받거나 `./gradlew assembleDebug`로 빌드합니다(JDK 17 이상과 Android SDK 필요).
2. `app-debug.apk`를 Android 8 이상 휴대폰에 설치합니다. 6 GHz는 Wi-Fi 6E, 320 MHz와 MLO는 Wi-Fi 7을 지원하는 Android 13 이상이 필요합니다.
3. 앱을 열고 **정확한** 위치와, Android 13 이상에서는 '주변 Wi-Fi 기기'를 허용합니다. 권한이 없으면 Android가 스캔 결과를 주지 않습니다.
4. 위치 서비스를 켜 두세요. 앱이 화면에 있을 때 30초마다 스캔하며, 새로고침을 누르면 바로 스캔합니다.
5. 알맞은 휴대폰이 없다면 데모 모드에서 합성된 Wi-Fi 7 환경을 살펴볼 수 있습니다.

**Google Play에 출시하려면** [docs/PUBLISHING.zh-TW.md](docs/PUBLISHING.zh-TW.md)([영어 요약](docs/PUBLISHING.md))를 따라 애플리케이션 ID를 바꾸고, 프로젝트 밖에 업로드 키를 만들고, 서명된 App Bundle을 빌드한 뒤 Play Console 비공개 테스트를 완료하세요.

<a id="workflow"></a>
## 동작 흐름

```text
앱이 화면에 있을 때 30초마다 (Android는 포그라운드에서 2분에 4회까지 허용)
  → WifiManager.startScan(), 제한되면 캐시된 결과 사용
  → 각 ScanResult 정리: SSID, BSSID, 대역, 기본 채널
  → 채널 폭과 중심 주파수로 점유 블록 계산 (20–320 MHz, 80+80)
  → MLD 주소로 Wi-Fi 7 링크 묶기, 제조사 오프라인 조회
  → 목록, 스펙트럼, 60회 추이, 채널 평가, MLO 화면 갱신
```

앱이 화면에서 벗어나면 스캔을 멈추며 백그라운드에서 동작하지 않습니다. 드라이버가 중심 주파수를 주지 않거나 160 MHz 중심을 `centerFreq1`에 넣는 경우 표준 채널 배치로 계산합니다. MLO 항목은 Android 13, 연결된 라우터의 링크별 신호는 Android 14가 필요합니다.

<a id="features"></a>
## 기능

| 기능 | 동작 |
| --- | --- |
| 대역 | 2.4, 5(UNII-4 포함), 6 GHz. 스캔 제한, Wi-Fi 꺼짐, 위치 꺼짐 알림 |
| 채널 폭 | 20, 40, 80, 160, 80+80, 320 MHz. 점유 범위, 중심 채널, 포함 채널 |
| Wi-Fi 7 MLO | MLD 주소, 링크 ID, 연관 링크. 연결의 활성 링크와 링크별 RSSI |
| 스펙트럼 | AP마다 포물선 하나. 전체 대역 개요와 확대. 연결된 라우터의 링크는 굵게 표시 |
| 추이 | 가장 강한 AP 10개의 RSSI를 60회 표시. 검출되지 않은 회차는 선이 끊김 |
| 채널 평가 | 신호와 겹침으로 가중. 2.4 GHz는 1/6/11만. 비 DFS 우선. 6 GHz는 PSC 우선 |
| 상세 정보 | IEEE 등록부 제조사, 랜덤 MAC 판별, WPA3 SAE-EXT와 OWE를 포함한 보안 방식, 거리 추정 |
| 개인정보 | 네트워크 기능, 광고, 분석 도구 없음. 스캔 결과는 메모리에만 보관 |

거리는 장애물이 없는 환경의 추정치이며 벽이 있으면 더 짧아집니다. 스캔 내용은 휴대폰의 Wi-Fi 칩과 드라이버에 따라 다르므로 6 GHz, 320 MHz, MLO 값은 직접 확인하세요.

<a id="demo"></a>
## 데모

이 40초 무음 데모는 **실제 앱 화면을 내장 합성 환경으로 구동한 것**입니다. Wi-Fi 7 라우터(MLO 링크 3개)와 혼잡한 이웃이 있는 타이베이 아파트를 가정했고, 화면마다 중국어·영어 자막을 붙였습니다. 실제 스캔이 아니며 특정 휴대폰의 무선 성능을 나타내지 않습니다.

![합성 데이터로 앱을 조작하는 데모](docs/media/wifi-spectrum-demo.gif)

[MP4 데모 재생 또는 다운로드](docs/media/wifi-spectrum-demo.mp4)

| 2.4 GHz 스펙트럼 | 6 GHz와 320 MHz | Wi-Fi 7 MLO 링크 |
| --- | --- | --- |
| ![2.4 GHz 스펙트럼](docs/media/spectrum-24ghz.png) | ![6 GHz와 320 MHz](docs/media/spectrum-6ghz.png) | ![Wi-Fi 7 MLO 링크](docs/media/mlo-links.png) |

## 최근 변경 사항

첫 출시: 3개 대역 스캔, 20–320 MHz 채널 폭, Wi-Fi 7 MLO, 스펙트럼, 추이, 채널 평가, Google Play 자료와 출시 가이드. Android 8–16을 대상으로 한 214개의 오프라인 테스트로 검증했습니다. [변경 기록](CHANGELOG.md)을 참고하세요.

<a id="controls"></a>
## 조작

| 조작 | 동작 |
| --- | --- |
| 새로고침 | 즉시 스캔 |
| 일시정지 / 재생 | 자동 스캔 중지 또는 재개 |
| ⋮ → 간격 | 3, 5, 10, 30, 60초. 30초 미만은 Android가 제한할 수 있음 |
| ⋮ → 데모 모드 | 실제 스캔과 합성 데이터 전환 |
| 대역 / 폭 / Wi-Fi 7 칩 | 목록 필터. 폭 칩을 누르면 ≥40/80/160/320 MHz 순으로 전환 |
| 확대 (5/6 GHz) | 전체 대역 개요와 스크롤 가능한 확대 보기 전환 |
| AP 누르기 | 점유 범위, 포함 채널, 보안 방식, MLO 링크 표시 |

<a id="faq"></a>
## 자주 묻는 질문

**왜 위치 권한이 필요한가요?** Android는 Wi-Fi 스캔 결과를 위치 정보로 취급합니다. 정확한 위치(Android 13 이상에서는 주변 Wi-Fi 기기도)가 없으면 결과를 주지 않습니다. 앱은 GPS 좌표를 읽지 않습니다.

**6 GHz가 비어 있는 이유는?** 휴대폰이 Wi-Fi 6E를 지원하고 주변에 6 GHz 라우터가 있어야 합니다.

**MLO나 320 MHz가 보이지 않는 이유는?** Wi-Fi 7을 지원하는 Android 13 이상 휴대폰과 MLO를 켠 Wi-Fi 7 라우터가 필요합니다.

**스캔이 제한되었다고 나오는 이유는?** Android는 포그라운드에서 2분에 4번만 스캔을 허용합니다. 30초 간격을 유지하거나 테스트할 때 개발자 옵션에서 Wi-Fi 스캔 제한을 끄세요.

**iOS 버전이 있나요?** Apple API는 주변 네트워크를 제공하지 않아 iOS에서는 이 스캔을 할 수 없습니다. iOS에서 가능한 기능은 [IOS_wifi-spectrum](https://github.com/tonykfr1234/ios_wifi-spectrum)을 참고하세요.

<a id="docs"></a>
<a id="documentation"></a>
## 문서

| 주제 | 문서 |
| --- | --- |
| 일상 사용 | [繁中使用指南](docs/USER_GUIDE.zh-TW.md) · [English User Guide](docs/USER_GUIDE.md) |
| 내부 구조 | [아키텍처](docs/ARCHITECTURE.md) |
| 출시 | [Google Play 출시 가이드(번체)](docs/PUBLISHING.zh-TW.md) · [영어 요약](docs/PUBLISHING.md) · [개인정보처리방침](docs/PRIVACY.md) |
| 버전과 보안 | [변경 기록](CHANGELOG.md) · [보안](SECURITY.md) · [기여 가이드](CONTRIBUTING.md) |

코드는 `app/src/main/java`, 테스트는 `app/src/test`, 도구는 `scripts/`, Google Play 이미지는 `store/`에 있습니다. 214개의 테스트는 휴대폰 없이 JVM에서 실행됩니다. 실제 6 GHz, 320 MHz, MLO 값은 직접 확인하세요.

## 보안

키스토어, `keystore.properties`, `local.properties`, API 키, 본인 소유가 아닌 네트워크의 스캔 결과를 커밋하지 마세요. 푸시 전에 GitHub Actions와 같은 스캐너를 실행하세요:

```bash
python scripts/security_scan.py .
```

## 라이선스

[MIT](LICENSE).

### 2026-10-20 · 1.0.0

首次發布：2.4／5／6 GHz 掃描、20–320 MHz 頻寬、Wi-Fi 7 MLO、頻譜圖、趨勢與頻道評分；214 個離線測試涵蓋 Android 8–16。

First release: 2.4/5/6 GHz scanning, 20–320 MHz widths, Wi-Fi 7 MLO, spectrum, trend and channel rating; 214 offline tests cover Android 8–16.

[공개 사용 설명서(번체 중국어, 텍스트)](docs/使用說明.txt)
