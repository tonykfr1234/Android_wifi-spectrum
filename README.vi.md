<p align="center">
  <a href="README.md">English</a> · <a href="README.zh-CN.md">简体中文</a> · <a href="README.zh-TW.md">繁體中文</a> · <a href="README.ja.md">日本語</a> · <a href="README.ko.md">한국어</a><br>
  <a href="README.de.md">Deutsch</a> · <a href="README.es.md">Español</a> · <a href="README.fr.md">Français</a> · <a href="README.pt-BR.md">Português (Brasil)</a> · <strong>Tiếng Việt</strong> · <a href="README.id.md">Bahasa Indonesia</a> · <a href="README.it.md">Italiano</a>
</p>

<p align="center"><img src="assets/wifi-spectrum.png" width="112" alt="Biểu tượng Wi-Fi Spectrum"></p>
<h1 align="center">Trình phân tích Wi-Fi chuyên nghiệp cho Android</h1>
<p align="center">Wi-Fi Spectrum (Wi-Fi 頻譜分析儀) · 2,4 / 5 / 6 GHz · 20–320 MHz · Wi-Fi 7 MLO</p>
<p align="center">
  <a href="https://github.com/tonykfr1234/android_wifi-spectrum/actions/workflows/security.yml"><img src="https://github.com/tonykfr1234/android_wifi-spectrum/actions/workflows/security.yml/badge.svg" alt="CI"></a>
  <img src="https://img.shields.io/badge/platform-Android%208%2B-3DDC84" alt="Android 8+">
  <img src="https://img.shields.io/badge/Kotlin-2.2-7F52FF" alt="Kotlin 2.2">
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4" alt="Jetpack Compose">
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-green" alt="Giấy phép MIT"></a>
</p>
<p align="center">
  <a href="#quick-start">Bắt đầu nhanh</a> · <a href="#workflow">Quy trình</a> · <a href="#features">Tính năng</a> · <a href="#demo">Minh họa</a> · <a href="#faq">Câu hỏi thường gặp</a> · <a href="#documentation">Tài liệu</a>
</p>

```bash
./gradlew testDebugUnitTest assembleDebug
```

---

Phiên bản 1.0: cả ba băng tần, mọi độ rộng kênh đến 320 MHz và đa liên kết Wi-Fi 7 (MLO) được gom theo địa chỉ MLD.

Wi-Fi Spectrum quét các điểm truy cập gần đó trên 2,4, 5 và 6 GHz và cho thấy mỗi điểm thực sự phát ở đâu: kênh chính, toàn bộ khối 20/40/80/160/80+80/320 MHz và mọi kênh bị chiếm. Biểu đồ phổ, xu hướng tín hiệu và chấm điểm kênh giúp chọn kênh ít nhiễu; màn hình MLO gom các liên kết của từng router Wi-Fi 7.

Apple không cho ứng dụng iOS quét mạng Wi-Fi xung quanh, nên các tính năng này chỉ có trên Android. Bản iOS, [IOS_wifi-spectrum](https://github.com/tonykfr1234/ios_wifi-spectrum), cung cấp những gì iOS cho phép.

README có 12 ngôn ngữ là bản dịch tài liệu. Giao diện ứng dụng hiện dùng tiếng Trung phồn thể.

<a id="quick-start"></a>
## Bắt đầu nhanh

1. Tải `wifi-spectrum-debug-apk` từ [lần chạy GitHub Actions](https://github.com/tonykfr1234/android_wifi-spectrum/actions) thành công gần nhất, hoặc tự build bằng `./gradlew assembleDebug` (JDK 17+ và Android SDK).
2. Cài `app-debug.apk` trên điện thoại Android 8+. 6 GHz cần Wi-Fi 6E; 320 MHz và MLO cần Android 13+ có Wi-Fi 7.
3. Mở ứng dụng và cho phép vị trí **chính xác**, và trên Android 13+ cho phép “Thiết bị Wi-Fi ở gần”. Thiếu các quyền này Android sẽ không trả kết quả quét.
4. Giữ dịch vụ vị trí luôn bật. Ứng dụng quét mỗi 30 giây khi đang hiển thị; nhấn làm mới để quét ngay.
5. Không có điện thoại phù hợp? Chọn chế độ minh họa để xem môi trường Wi-Fi 7 mô phỏng.

**Phát hành trên Google Play?** Làm theo [docs/PUBLISHING.zh-TW.md](docs/PUBLISHING.zh-TW.md) ([tóm tắt tiếng Anh](docs/PUBLISHING.md)): đổi ID ứng dụng, tạo khóa tải lên bên ngoài dự án, build App Bundle đã ký và hoàn tất thử nghiệm kín trên Play Console.

<a id="workflow"></a>
## Quy trình

```text
Ứng dụng hiển thị, mỗi 30 giây (Android cho phép 4 lần quét ở nền trước mỗi 2 phút)
  → WifiManager.startScan(); khi bị giới hạn thì dùng lại kết quả đã lưu
  → làm sạch từng ScanResult: SSID, BSSID, băng tần, kênh chính
  → tính khối chiếm dụng từ độ rộng và tần số trung tâm (20–320 MHz, 80+80)
  → gom liên kết Wi-Fi 7 theo địa chỉ MLD; tra nhà sản xuất ngoại tuyến
  → cập nhật danh sách, phổ, xu hướng 60 lần quét, chấm điểm kênh và MLO
```

Việc quét dừng khi ứng dụng rời màn hình; không có gì chạy nền. Khi driver bỏ trống tần số trung tâm hoặc đặt tâm 160 MHz vào `centerFreq1`, khối được suy ra từ kế hoạch kênh chuẩn. Trường MLO cần Android 13; tín hiệu từng liên kết của router đang kết nối cần Android 14.

<a id="features"></a>
## Tính năng

| Tính năng | Hoạt động |
| --- | --- |
| Băng tần | 2,4, 5 (gồm UNII-4) và 6 GHz; thông báo khi bị giới hạn, tắt Wi-Fi hoặc tắt vị trí |
| Độ rộng kênh | 20, 40, 80, 160, 80+80 và 320 MHz; dải chiếm dụng, kênh trung tâm và các kênh bị phủ |
| Wi-Fi 7 MLO | Địa chỉ MLD, ID liên kết và liên kết liên quan; liên kết đang hoạt động và RSSI từng liên kết |
| Phổ | Mỗi AP một đường parabol; xem toàn băng có phóng to; liên kết của router đang kết nối được tô đậm |
| Xu hướng | RSSI của 10 AP mạnh nhất qua 60 lần quét; đứt đoạn khi không thấy AP |
| Chấm điểm kênh | Theo cường độ và mức chồng lấn; chỉ 1/6/11 ở 2,4 GHz; ưu tiên kênh không DFS; ưu tiên PSC ở 6 GHz |
| Chi tiết | Nhà sản xuất từ sổ đăng ký IEEE, nhận diện MAC ngẫu nhiên, bảo mật gồm WPA3 SAE-EXT và OWE, ước lượng khoảng cách |
| Quyền riêng tư | Không truy cập mạng, không quảng cáo, không phân tích; kết quả chỉ nằm trong bộ nhớ |

Khoảng cách là ước lượng trong không gian trống; tường sẽ làm nó ngắn lại. Nội dung quét phụ thuộc chip Wi-Fi và driver của máy; hãy kiểm tra giá trị 6 GHz, 320 MHz và MLO bằng thiết bị của bạn trước khi dựa vào chúng.

<a id="demo"></a>
## Minh họa

Đoạn minh họa không tiếng dài 40 giây này dùng **giao diện thật của ứng dụng với môi trường mô phỏng tích hợp**: một căn hộ ở Đài Bắc có router Wi-Fi 7 (ba liên kết MLO) và hàng xóm đông đúc, kèm phụ đề song ngữ. Đây không phải lần quét thật và không phản ánh sóng của bất kỳ điện thoại nào.

![Minh họa ứng dụng với dữ liệu mô phỏng](docs/media/wifi-spectrum-demo.gif)

[Mở hoặc tải video minh họa MP4](docs/media/wifi-spectrum-demo.mp4)

| Phổ 2,4 GHz | 6 GHz với 320 MHz | Liên kết MLO Wi-Fi 7 |
| --- | --- | --- |
| ![Phổ 2,4 GHz](docs/media/spectrum-24ghz.png) | ![6 GHz với 320 MHz](docs/media/spectrum-6ghz.png) | ![Liên kết MLO Wi-Fi 7](docs/media/mlo-links.png) |

## Thay đổi mới nhất

Bản phát hành đầu tiên: quét ba băng tần, độ rộng 20–320 MHz, Wi-Fi 7 MLO, phổ, xu hướng, chấm điểm kênh, tài nguyên Google Play và hướng dẫn phát hành, đã kiểm tra bằng 214 bài kiểm thử ngoại tuyến trên Android 8–16. Xem [nhật ký thay đổi](CHANGELOG.md).

<a id="controls"></a>
## Điều khiển

| Điều khiển | Tác dụng |
| --- | --- |
| Làm mới | Quét ngay |
| Tạm dừng / Phát | Dừng hoặc tiếp tục quét tự động |
| ⋮ → khoảng thời gian | 3, 5, 10, 30 hoặc 60 giây; dưới 30 giây Android có thể giới hạn |
| ⋮ → chế độ minh họa | Chuyển giữa quét thật và dữ liệu mô phỏng |
| Chip băng tần / độ rộng / Wi-Fi 7 | Lọc danh sách; chip độ rộng lần lượt chuyển ≥40/80/160/320 MHz |
| Phóng to (5/6 GHz) | Chuyển giữa toàn băng và chế độ phóng to có thể cuộn |
| Chạm vào AP | Xem dải chiếm dụng, kênh bị phủ, bảo mật và liên kết MLO |

<a id="faq"></a>
## Câu hỏi thường gặp

**Vì sao cần quyền vị trí?** Android coi kết quả quét Wi-Fi là dữ liệu vị trí. Không có vị trí chính xác (và “Thiết bị Wi-Fi ở gần” trên Android 13+) thì hệ thống không trả kết quả. Ứng dụng không đọc tọa độ GPS.

**Vì sao 6 GHz trống?** Điện thoại cần hỗ trợ Wi-Fi 6E và phải có router 6 GHz trong tầm.

**Vì sao không có MLO hay 320 MHz?** Cần điện thoại Wi-Fi 7 chạy Android 13 trở lên và router Wi-Fi 7 đã bật MLO.

**Vì sao báo việc quét bị giới hạn?** Android cho phép bốn lần quét ở nền trước mỗi hai phút. Giữ khoảng 30 giây, hoặc khi thử nghiệm hãy tắt giới hạn quét Wi-Fi trong Tùy chọn cho nhà phát triển.

**Có bản iOS không?** API của Apple không cung cấp mạng xung quanh, nên iOS không thể quét như vậy. Xem [IOS_wifi-spectrum](https://github.com/tonykfr1234/ios_wifi-spectrum) để biết iOS hỗ trợ gì.

<a id="docs"></a>
<a id="documentation"></a>
## Tài liệu

| Chủ đề | Tài liệu |
| --- | --- |
| Sử dụng hằng ngày | [繁中使用指南](docs/USER_GUIDE.zh-TW.md) · [English User Guide](docs/USER_GUIDE.md) |
| Cấu trúc bên trong | [Kiến trúc](docs/ARCHITECTURE.md) |
| Phát hành | [Hướng dẫn Google Play (繁中)](docs/PUBLISHING.zh-TW.md) · [Tóm tắt tiếng Anh](docs/PUBLISHING.md) · [Chính sách quyền riêng tư](docs/PRIVACY.md) |
| Phiên bản và bảo mật | [Nhật ký thay đổi](CHANGELOG.md) · [Bảo mật](SECURITY.md) · [Đóng góp](CONTRIBUTING.md) |

Mã nguồn ở `app/src/main/java`, kiểm thử ở `app/src/test`, công cụ ở `scripts/` và ảnh Google Play ở `store/`. 214 bài kiểm thử chạy trên JVM, không cần điện thoại; hãy xác nhận giá trị 6 GHz, 320 MHz và MLO thật bằng thiết bị của bạn.

## Bảo mật

Không bao giờ commit keystore, `keystore.properties`, `local.properties`, khóa API hay dữ liệu quét của mạng không thuộc về bạn. Trước khi push, chạy cùng trình quét mà GitHub Actions dùng:

```bash
python scripts/security_scan.py .
```

## Giấy phép

[MIT](LICENSE).

### 2026-10-20 · 1.0.0

首次發布：2.4／5／6 GHz 掃描、20–320 MHz 頻寬、Wi-Fi 7 MLO、頻譜圖、趨勢與頻道評分；214 個離線測試涵蓋 Android 8–16。

First release: 2.4/5/6 GHz scanning, 20–320 MHz widths, Wi-Fi 7 MLO, spectrum, trend and channel rating; 214 offline tests cover Android 8–16.

[Hướng dẫn sử dụng công khai (tiếng Trung phồn thể, văn bản thuần)](docs/使用說明.txt)
