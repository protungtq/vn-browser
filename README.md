# VN Browser

Trình duyệt Android gọn nhẹ: nhiều tab, chặn quảng cáo, lịch sử, dấu trang,
sao lưu và khôi phục, tải file và tải video. Dùng engine WebView (Chromium)
sẵn trong máy.

- Mã gói (applicationId): `vn.tunglv.browser`
- Tên hiển thị: VN Browser

## Tải về

Tải APK mới nhất ở mục [Releases](https://github.com/protungtq/vn-browser/releases)
(bản `arm64` cho hầu hết điện thoại Android hiện nay). Yêu cầu Android 8.0+.

APK là bản dựng debug từ GitHub Actions. Khi cài đặt, cần cho phép cài đặt
từ nguồn không xác định. Nếu cài đè lên bản cũ mà máy báo lỗi chữ ký, gỡ
bản cũ rồi cài lại (nên xuất sao lưu JSON trong app trước khi gỡ để giữ
tab, lịch sử và dấu trang).

## Tính năng (bản 0.1.6)

- Nhiều tab: mở, đóng, chuyển tab; mở lại app vẫn còn tab cũ.
- Chặn quảng cáo theo 4 bộ lọc: FMSF 2.0, ABPVN, AdGuard Mobile Ads,
  EasyList. Bộ lọc tải về máy, tự kiểm tra bản mới mỗi 24 giờ.
- Lịch sử duyệt web: xem, tìm, xóa từng mục hoặc xóa hết.
- Dấu trang: chọn **Lưu trang này** trong menu, mở lại từ mục **Dấu trang**.
- Sao lưu và khôi phục: xuất dấu trang, lịch sử, cài đặt và tab đã lưu ra
  file JSON, khôi phục lại trên máy khác.
- Trang chủ với 12 trang phổ thông gợi ý sẵn.
- Tải file bằng DownloadManager của máy, có kèm cookie và User-Agent.
- Tải video YouTube: bắt link phát trực tiếp từ request mạng của trang,
  gộp hình và tiếng bằng MediaMuxer, ưu tiên tiếng AAC (mở video cho phát
  vài giây rồi bấm nút tải).
- Chọn công cụ tìm kiếm cho trang chủ và thanh địa chỉ. Giao diện sáng/tối,
  thanh điều hướng phía dưới và lưới tab thích ứng với chiều rộng màn hình.
- Cuộn trang xuống thì thanh địa chỉ và thanh điều hướng tự ẩn cho toàn
  màn hình, cuộn ngược lên thì hiện lại.
- Báo cáo sự cố tích hợp (CrashReporter).

## Bộ máy

- Kotlin + Jetpack Compose (Material 3), minSdk 26 (Android 8.0+).
- Bộ máy tải media (youtubedl-android 0.18.1: yt-dlp + FFmpeg),
  dùng chung với app VDM.

## Build

```sh
gradle assembleDebug
```

APK ra ở `app/build/outputs/apk/debug/` (chia theo kiến trúc CPU + 1 bản
universal). GitHub Actions tự build mỗi lần có thay đổi trên nhánh `main`
và đính kèm APK vào mục Artifacts của lần chạy đó.

Yêu cầu: JDK 17, Android SDK với platform API 35.

## Lịch sử

Repo này tách ra từ thư mục `browser/` của
[protungtq/vdm](https://github.com/protungtq/vdm) ngày 17/11/2026. Bản phát
hành 0.1.6 trước đó vẫn nằm ở repo cũ; từ bản 0.1.6 trở đi, bản phát hành
mới nằm ở repo này.

## Ghi công

- Bộ lọc quảng cáo: [FMSF 2.0](https://github.com/nmtrung/FMSF-2.0),
  [ABPVN](https://github.com/abpvn/abpvn),
  [AdGuard](https://adguard.com),
  [EasyList](https://easylist.to).
- VN Browser được làm bởi TungTQ.

## Giấy phép

MIT — xem file `LICENSE`.
