# WebAITransTTS - Trình Duyệt Đọc Truyện & Dịch Thuật AI Tích Hợp TTS

**WebAITransTTS** là một ứng dụng Android hiện đại được phát triển bằng Kotlin và Jetpack Compose. Ứng dụng được thiết kế chuyên biệt để duyệt web, đọc truyện chữ, dịch thuật chất lượng cao bằng AI (Gemini) và nghe đọc bằng giọng nói (Text-to-Speech) hoạt động bền bỉ, mượt mà kể cả khi tắt màn hình hoặc chạy dưới nền.

---

## 🌟 Các Tính Năng Nổi Bật

### 1. Trình Duyệt Web Chrome-like Hiện Đại
- **Giao diện tối giản:** Thanh công cụ phía trên được thiết kế tối ưu, tự động co giãn thanh địa chỉ khi nhập liệu, tích hợp nút Đánh dấu trang (Bookmark) độc lập có màu vàng Gold trực quan.
- **Duyệt web mượt mà:** Ẩn các nút điều hướng thừa (Back/Forward), người dùng vuốt màn hình để quay lại trang trước hoặc đi tiếp, tối ưu diện tích hiển thị nội dung.

### 2. Dịch Toàn Trang Web Tại Chỗ (In-place Translation)
- **Không dùng Proxy:** Sử dụng trực tiếp Google Translate Element Widget nhúng trực tiếp vào WebView để dịch tại chỗ sang Tiếng Việt. URL gốc và Bookmark được giữ nguyên sạch sẽ.
- **Dịch tự động chuyển chương:** Tự động áp dụng dịch thuật khi người dùng nhấn chuyển chương mới nhờ cơ chế lưu cookie `googtrans=/auto/vi`.
- **Bảo toàn nguyên tác gốc cho AI:** Tự động sao lưu mã nguồn chữ gốc (tiếng Trung/tiếng Anh) vào cache `window.originalWebContent` trước khi Google dịch sửa đổi cấu trúc trang. Nhờ đó, tính năng Dịch AI & đọc TTS bằng Gemini vẫn hoạt động chính xác 100% dựa trên nguyên tác gốc.

### 3. Dịch Thuật AI Nâng Cao Với Gemini & Dịch Streaming
- **Dịch truyền trực tuyến (Streaming):** Bản dịch từ Gemini API được cập nhật liên tục theo thời gian thực (real-time stream). Người dùng có thể bắt đầu đọc và nghe TTS ngay khi đoạn đầu tiên được dịch xong mà không cần chờ toàn bộ trang web dịch xong.
- **Trích xuất thông minh (Hybrid DOM Extractor):** Tự động loại bỏ quảng cáo, menu điều hướng, script, style rác... Chỉ giữ lại phần chương truyện/bài viết chính để dịch thuật, giúp tiết kiệm số lượng Token.
- **Văn phong Literary chuẩn:** Prompt dịch thuật được tối ưu hóa cho truyện chữ, giữ lại các thuật ngữ Hán-Việt truyền thống (nữ nhân, nam nhân, thẩm, bài phường...) để bảo toàn màu sắc nguyên bản của tác phẩm.
- **Tự động dịch tiêu đề:** Tiêu đề chương truyện được dịch sang tiếng Việt tự động trước khi lưu vào thư viện.

### 4. Đọc Giọng Nói (TTS) Chạy Nền Bền Bỉ (Background Playback)
- **Khắc phục lỗi đóng băng nền:** Giải quyết triệt để cơ chế tối ưu pin khắc nghiệt trên các dòng điện thoại Android Trung Quốc (OriginOS, HyperOS, ColorOS...) bằng sự kết hợp của:
  - Khởi chạy coroutines chuyển đoạn trên thread pool nền (`Dispatchers.Default`).
  - Đăng ký `PowerManager.WakeLock` (loại `PARTIAL_WAKE_LOCK`).
  - Đăng ký `MediaSession` hệ thống với trạng thái `PLAYING` thông báo cho hệ điều hành.
  - **Phát âm thanh tĩnh (Silence Audio):** Sử dụng `AudioTrack` phát dữ liệu PCM tĩnh lặp vô hạn dưới nền để hệ thống Android nhận diện ứng dụng là một trình phát Media hoạt động thực thụ, ngăn ngừa việc đóng băng tiến trình.
- **Buộc kích hoạt Google TTS:** Hỗ trợ tùy chọn ép buộc khởi chạy động cơ Google Speech Services (`com.google.android.tts`) trên các máy nội địa Trung Quốc bị ẩn cài đặt, đồng thời tự động khôi phục (fallback) về TTS mặc định nếu thiết bị không có Google TTS.
- **Playlist Playback:** Tự động đọc tiếp chương tiếp theo trong hàng chờ khi chương cũ kết thúc.

### 5. Quản Lý Thư Mục & Hàng Chờ Dịch Thuật
- **Hàng chờ "Đã dịch":** Danh sách bài viết được chia làm 3 mục trực quan: *Đang dịch* (hiển thị tiến trình chi tiết từng bước), *Dịch lỗi* (hiển thị nguyên nhân lỗi kèm nút thử lại), và *Đã dịch xong*.
- **Tổ chức theo Thư mục (Folders):** Hỗ trợ tạo mới, đổi tên, xóa thư mục (lựa chọn giữ lại bài viết hoặc xóa tất cả) và di chuyển bài viết giữa các thư mục.
- **Tự động kế thừa:** Bài viết chương tiếp theo tự động kế thừa thư mục của chương trước khi người dùng nhấn dịch tiếp.
- **Kéo thả sắp xếp:** Cho phép chạm giữ và kéo thả (Drag and Drop) để sắp xếp lại thứ tự phát nhạc/đọc truyện trong danh sách đã dịch xong.

### 6. Xoay Vòng API Key & Nhật Ký Chi Tiết (Key Rotation & Logs)
- **Xoay vòng khóa API:** Cho phép nhập nhiều API Key Gemini (phân tách bằng dấu phẩy hoặc xuống dòng). Khi một khóa bị lỗi hoặc hết hạn mức, hệ thống tự động chuyển sang khóa tiếp theo.
- **Phân tích lỗi đệ quy:** Tự động bóc tách các Exception lồng nhau của Ktor và Google API để đưa ra nguyên nhân lỗi chi tiết nhất.
- **Nhật ký thời gian thực (Live Logs):** Ghi nhận chi tiết từng bước hoạt động dịch dưới nền, hiển thị trạng thái từng bước trên giao diện tải, và lưu trữ 50 nhật ký dịch thuật gần nhất trong phần Cài đặt kèm nút sao chép log nhanh để báo cáo lỗi.

---

## 📂 Cấu Trúc Mã Nguồn (Architecture)

Mã nguồn được tổ chức sạch sẽ theo mô hình MVVM (Model-View-ViewModel):

```
app/src/main/java/com/example/webreader/
│
├── data/
│   ├── BookmarkItem.kt             # Data class biểu diễn trang đánh dấu
│   ├── BookmarkRepository.kt       # Quản lý lưu trữ Bookmark (JSON)
│   ├── QueueFolder.kt              # Data class thư mục lưu trữ hàng chờ
│   ├── QueueItem.kt                # Data class bài viết trong hàng chờ dịch
│   ├── QueueRepository.kt          # Quản lý lưu trữ Hàng chờ & Thư mục (JSON)
│   ├── SettingsRepository.kt       # Quản lý cài đặt ứng dụng (SharedPreferences)
│   ├── TransactionLog.kt           # Data class nhật ký dịch thuật
│   ├── TransactionLogRepository.kt # Quản lý lưu trữ Nhật ký dịch thuật (JSON)
│   ├── GeminiManager.kt            # Xử lý kết nối Gemini API (Dịch Streaming, Phân đoạn 8000 từ, Xoay vòng Key)
│   ├── TtsManager.kt               # Quản lý khởi tạo Engine Text-to-Speech
│   └── TtsService.kt               # Service chạy nền quản lý phát âm thanh, WakeLock, MediaSession, Silence Audio
│
├── ui/
│   ├── BrowserScreen.kt            # Giao diện trình duyệt web WebView, tích hợp Google Translate Widget
│   ├── BrowserViewModel.kt         # Điều phối trạng thái duyệt web, dịch thuật, hàng chờ phát và quản lý thư mục
│   ├── ReaderSheet.kt              # Trình đọc nội dung dịch, quản lý tab Đã dịch (Folders, Kéo thả), Bookmark
│   ├── SettingsScreen.kt           # Giao diện Cài đặt (nhập API Key, chọn giọng đọc, xem Nhật ký dịch thuật)
│   └── theme/                      # Định nghĩa hệ thống màu sắc, kiểu chữ của ứng dụng
│
├── MainActivity.kt                 # Component khởi chạy ứng dụng chính
├── Navigation.kt                   # Thiết lập điều hướng Jetpack Compose Navigation
└── NavigationKeys.kt               # Định nghĩa các Route điều hướng
```

---

## 🛠️ Yêu Cầu Hệ Thống & Build

### 1. Yêu cầu build
- **Android Studio** Ladybug hoặc mới hơn.
- **JDK 17** trở lên.
- **Gradle wrapper** tích hợp sẵn trong dự án.

### 2. Biên dịch từ dòng lệnh (Gradle)
Bạn có thể tự build phiên bản Debug APK bằng cách chạy lệnh sau trong thư mục gốc của dự án:

```powershell
# Trên Windows
./gradlew.bat assembleDebug

# Trên macOS/Linux
./gradlew assembleDebug
```

Tệp cài đặt APK sau khi biên dịch thành công sẽ nằm ở đường dẫn:
`app/build/outputs/apk/debug/WebAITransTTS_v1.0.0.apk`

---

## ⚙️ Hướng Dẫn Cấu Hình Khi Sử Dụng

1. **Cài đặt Gemini API Keys:**
   * **Cách lấy API Key miễn phí từ Google AI Studio:**
     1. Truy cập vào trang web [Google AI Studio](https://aistudio.google.com/).
     2. Đăng nhập bằng tài khoản Google (Gmail) của bạn.
     3. Nhấp vào nút **"Get API key"** ở danh mục menu bên trái.
     4. Nhấn chọn **"Create API key"** (Tạo khóa API), sau đó chọn tạo khóa trong một dự án mới hoặc dự án sẵn có.
     5. Sao chép chuỗi mã API được tạo ra (mã khóa thường bắt đầu bằng tiền tố `AIzaSy...`).
   * **Nhập khóa vào ứng dụng:**
     * Truy cập vào **Cài đặt** (Settings) trong ứng dụng thông qua menu 3 chấm.
     * Dán danh sách các API Key Gemini đã sao chép vào ô **Gemini API Keys**. Bạn có thể nhập một hoặc nhiều khóa khác nhau để xoay vòng tự động (mỗi khóa cách nhau bởi dấu phẩy `,` hoặc xuống dòng `\n`).
     * Lựa chọn Model dịch (mặc định khuyến nghị là `gemini-3.5-flash`).

2. **Cấu hình Giọng Đọc (TTS):**
   * Nếu bạn đang sử dụng điện thoại nội địa Trung Quốc hoặc các dòng máy không hiển thị Google TTS, hãy chọn **Google TTS (Buộc kích hoạt)** trong mục chọn Động cơ TTS tại cài đặt để kích hoạt giọng đọc Tiếng Việt chuẩn.
   * Cài đặt tốc độ đọc và âm điệu phù hợp với sở thích của bạn.

---

## 🛡️ Giấy Phép & Bảo Mật
- Các khóa API được lưu trữ cục bộ trên thiết bị của bạn thông qua `SharedPreferences`.
- Nhật ký dịch thuật lưu trữ trong thiết bị tự động làm mờ (obfuscate) các ký tự giữa của khóa API (`AIza...xxxx`) để đảm bảo không rò rỉ thông tin cá nhân khi sao chép gửi báo cáo lỗi.
