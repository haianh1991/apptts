package com.example.webreader.ui

import androidx.compose.runtime.staticCompositionLocalOf

interface AppStrings {
    val appName: String
    val appDesc: String
    
    // Shared / Common Buttons
    val btnSave: String
    val btnCancel: String
    val btnOk: String
    val btnDelete: String
    val btnEdit: String
    val btnAdd: String
    val statusSuccess: String
    val statusRunning: String
    val statusFailed: String
    val info: String
    val loading: String

    // LoginScreen Strings
    val loginFeature1Title: String
    val loginFeature1Desc: String
    val loginFeature2Title: String
    val loginFeature2Desc: String
    val loginFeature3Title: String
    val loginFeature3Desc: String
    val loginGoogleButton: String
    val loginRequiredSubtitle: String
    val toastLoginSuccess: String
    val toastLoginFailed: String
    val toastNoGoogleToken: String
    val toastLoginFailedCode: String

    // SettingsScreen Strings
    val settingsTitle: String
    val accountCardTitle: String
    val accountLoggedAs: String
    val accountNoEmail: String
    val accountLogoutButton: String
    val accountLoginSyncPrompt: String
    val geminiCardTitle: String
    val geminiApiKeyLabel: String
    val geminiApiKeyPlaceholder: String
    val geminiApiKeyInfo1: String
    val geminiApiKeyInfo2: String
    val geminiModelLabel: String
    val transCardTitle: String
    val transSourceLabel: String
    val transTargetLabel: String
    val transInstructionsLabel: String
    val transInstructionsPlaceholder: String
    val transInstructionsHelp: String
    val btnRestoreDefault: String
    val ttsCardTitle: String
    val ttsEngineLabel: String
    val ttsDefaultEngine: String
    val ttsGoogleEngineForce: String
    val ttsSpeedLabel: String
    val ttsPitchLabel: String
    val btnTtsTest: String
    val ttsTestText: String
    val logsCardTitle: String
    val logsEmpty: String
    val toastLogsCopied: String
    val btnSaveSettings: String
    val settingsDisplayLanguage: String
    val settingsDisplayLanguageLabel: String
    val settingsDisplayLanguageVi: String
    val settingsDisplayLanguageEn: String
    val settingsDisplayLanguageZh: String
    
    // BrowserScreen Strings
    val browserMenuReload: String
    val browserMenuViewOriginal: String
    val browserMenuReloadOriginal: String
    val browserMenuTranslate: String
    val toastReloadingOriginal: String
    val toastTranslatingToVietnamese: String
    val toastTranslatingToTarget: String
    val browserMenuSettings: String
    val dialogReaderTitle: String
    val dialogReaderPrompt: String
    val saveToFolderLabel: String
    val btnReadNow: String
    val btnAddToQueue: String
    val dialogNewFolderTitle: String
    val folderNameLabel: String
    val btnCreate: String
    val optionRootFolder: String
    val optionCreateNewFolder: String
    val updateDialogTitle: String
    val updateDialogForceTitle: String
    val updateDialogMessage: String
    val updateDialogMessageTemplate: String
    val updateDialogReleaseNotes: String
    val btnUpdateNow: String
    val btnUpdateLater: String
    val contentDescriptionBack: String
    val contentDescriptionClear: String
    
    // ReaderSheet Strings
    val readerTabTranslate: String
    val readerTabQueue: String
    val readerTabBookmarks: String
    val readerTabHistory: String
    val btnClearAllHistory: String
    val historyEmpty: String
    val btnGoToSettings: String
    val btnIgnoreError: String
    val folderTitle: String
    val dialogDeleteFolderTitle: String
    val dialogDeleteFolderPrompt: String
    val btnKeepArticles: String
    val btnDeleteAll: String
    val dialogMoveArticleTitle: String
    val logDetailTitle: String
    val logTime: String
    val logType: String
    val logUrl: String
    val logStatus: String
    val logUsedKeys: String
    val logDetailedProgress: String
    val logResponseSuccess: String
    val logResponseRunning: String
    val logResponseFailed: String
    val toastLogCopied: String
    val toastBookmarkAdded: String
    val toastBookmarkRemoved: String
    val ttsPlay: String
    val ttsPause: String
    val ttsForward: String
    val ttsRewind: String

    val readerPanelTitleOffline: String
    val readerPanelTitlePlaying: String
    val readerTranslatingPrompt: String
    val readerTranslatingSubPrompt: String
    val readerErrorTitle: String
    val readerNoContent: String
    val readerNoContentSub: String
    val readerTabContent: String
    val readerTabQueueFormat: String
    val readerTabBookmarksFormat: String
    val readerTranslatingCount: String
    val readerPreparingTranslation: String
    val readerTranslatingFailedCount: String
    val readerFolderCount: String
    val readerRootItemsCount: String
    val readerFinishedCount: String
    val readerArticleListTitle: String
    val readerEmptyQueue: String
    val readerEmptyQueueSub: String
    val readerNoBookmarks: String
    val readerNoBookmarksSub: String
    val readerSavedPagesTitle: String
    val dialogRenameFolderTitle: String
    val folderRenameLabel: String
    val dialogMoveArticlePrompt: String
    val folderItemCountTemplate: String
    val folderRenameMenu: String
    val folderDeleteMenu: String
    val queueItemMoveMenu: String
    val queueItemMoveDown: String
    val queueItemMoveToBottom: String
    val paragraphCountTemplate: String
    val readerTranslatingMore: String
    val readerTitle: String
    val btnClose: String
    val btnRetry: String
    val contentDescriptionCollapse: String
    val contentDescriptionExpand: String
    val contentDescriptionClose: String
}

class ViAppStrings : AppStrings {
    override val appName = "WebAITransTTS"
    override val appDesc = "Trình duyệt dịch thuật AI & Đọc sách TTS"
    
    override val btnSave = "Lưu"
    override val btnCancel = "Hủy"
    override val btnOk = "OK"
    override val btnDelete = "Xóa"
    override val btnEdit = "Sửa"
    override val btnAdd = "Thêm"
    override val statusSuccess = "Thành công"
    override val statusRunning = "Đang chạy"
    override val statusFailed = "Thất bại"
    override val info = "Thông tin"
    override val loading = "Đang tải..."

    override val loginFeature1Title = "Dịch thuật AI Hán-Việt tối ưu"
    override val loginFeature1Desc = "Sử dụng mô hình Gemini dịch truyện song ngữ giữ nguyên văn phong văn học."
    override val loginFeature2Title = "Đồng bộ hóa hàng chờ & TTS nền"
    override val loginFeature2Desc = "Nghe đọc sách bằng giọng đọc Google TTS mượt mà kể cả khi tắt màn hình."
    override val loginFeature3Title = "Quản lý và gom nhóm thư mục"
    override val loginFeature3Desc = "Tổ chức các chương truyện dịch thông minh theo từng thư mục phân loại."
    override val loginGoogleButton = "Đăng nhập bằng tài khoản Google"
    override val loginRequiredSubtitle = "Vui lòng đăng nhập để tiếp tục sử dụng dịch vụ."
    override val toastLoginSuccess = "Đăng nhập thành công!"
    override val toastLoginFailed = "Đăng nhập thất bại: "
    override val toastNoGoogleToken = "Không nhận được ID Token từ Google"
    override val toastLoginFailedCode = "Đăng nhập thất bại (Mã lỗi: %s)"

    override val settingsTitle = "Cài đặt ứng dụng"
    override val accountCardTitle = "Tài khoản người dùng"
    override val accountLoggedAs = "Người dùng Web Reader"
    override val accountNoEmail = "Không có email"
    override val accountLogoutButton = "Đăng xuất tài khoản"
    override val accountLoginSyncPrompt = "Đăng nhập bằng Google để đồng bộ cài đặt và theo dõi quyền lợi người dùng."
    override val geminiCardTitle = "Cấu hình Gemini API"
    override val geminiApiKeyLabel = "Gemini API Key"
    override val geminiApiKeyPlaceholder = "Dán các API key tại đây, cách nhau bởi dấu phẩy hoặc dòng mới"
    override val geminiApiKeyInfo1 = "Hỗ trợ nhiều khóa API để tự động xoay vòng khi có lỗi."
    override val geminiApiKeyInfo2 = "Lấy khóa API miễn phí từ Google AI Studio. Có thể dán danh sách khóa cách nhau bởi dấu phẩy hoặc ngắt dòng."
    override val geminiModelLabel = "Mô hình AI"
    override val transCardTitle = "Cấu hình Dịch thuật"
    override val transSourceLabel = "Ngôn ngữ nguồn"
    override val transTargetLabel = "Ngôn ngữ đích"
    override val transInstructionsLabel = "Chỉ dẫn dịch thuật cá nhân hóa"
    override val transInstructionsPlaceholder = "Ví dụ: Giữ nguyên cách xưng hô Hán-Việt..."
    override val transInstructionsHelp = "Thêm các yêu cầu riêng cho AI khi dịch."
    override val btnRestoreDefault = "Khôi phục mặc định"
    override val ttsCardTitle = "Cài đặt giọng đọc (TTS)"
    override val ttsEngineLabel = "Công cụ giọng đọc (TTS Engine)"
    override val ttsDefaultEngine = "Mặc định hệ thống"
    override val ttsGoogleEngineForce = "Google TTS (Buộc kích hoạt)"
    override val ttsSpeedLabel = "Tốc độ đọc"
    override val ttsPitchLabel = "Cao độ giọng"
    override val btnTtsTest = "Nghe thử giọng đọc Tiếng Việt"
    override val ttsTestText = "Xin chào, đây là giọng đọc thử tiếng Việt trên ứng dụng của bạn."
    override val logsCardTitle = "Nhật ký dịch thuật"
    override val logsEmpty = "Chưa có nhật ký dịch thuật nào."
    override val toastLogsCopied = "Đã sao chép nhật ký vào Clipboard"
    override val btnSaveSettings = "Lưu thiết lập"
    override val settingsDisplayLanguage = "Ngôn ngữ hiển thị"
    override val settingsDisplayLanguageLabel = "Ngôn ngữ hiển thị giao diện (UI)"
    override val settingsDisplayLanguageVi = "Tiếng Việt"
    override val settingsDisplayLanguageEn = "English"
    override val settingsDisplayLanguageZh = "简体中文"
    
    override val browserMenuReload = "Tải lại trang"
    override val browserMenuViewOriginal = "Xem trang gốc"
    override val browserMenuReloadOriginal = "Tải lại trang gốc"
    override val browserMenuTranslate = "Dịch trang web"
    override val toastReloadingOriginal = "Đang tải lại trang gốc..."
    override val toastTranslatingToVietnamese = "Đang dịch trang web sang Tiếng Việt..."
    override val toastTranslatingToTarget = "Đang dịch trang web sang ngôn ngữ đích..."
    override val browserMenuSettings = "Cài đặt"
    override val dialogReaderTitle = "Lựa chọn chế độ đọc"
    override val dialogReaderPrompt = "Bạn muốn dịch và nghe trang này ngay lập tức, hay dịch dưới nền và thêm vào hàng chờ để nghe liên tục?"
    override val saveToFolderLabel = "Lưu vào thư mục:"
    override val btnReadNow = "Đọc ngay"
    override val btnAddToQueue = "Thêm vào hàng chờ"
    override val dialogNewFolderTitle = "Tạo thư mục mới"
    override val folderNameLabel = "Tên thư mục"
    override val btnCreate = "Tạo"
    override val optionRootFolder = "Thư mục gốc"
    override val optionCreateNewFolder = "Tạo thư mục mới..."
    override val updateDialogTitle = "Cập nhật ứng dụng"
    override val updateDialogForceTitle = "Yêu cầu cập nhật bắt buộc"
    override val updateDialogMessage = "Đã có phiên bản mới của WebAITransTTS. Vui lòng cập nhật để sử dụng đầy đủ các tính năng."
    override val updateDialogMessageTemplate = "Ứng dụng %s đã sẵn sàng để tải xuống."
    override val updateDialogReleaseNotes = "Nhật ký thay đổi:"
    override val btnUpdateNow = "Cập nhật ngay"
    override val btnUpdateLater = "Để sau"
    override val contentDescriptionBack = "Hủy nhập"
    override val contentDescriptionClear = "Xóa"
    
    override val readerTabTranslate = "Nội dung dịch"
    override val readerTabQueue = "Hàng chờ đọc"
    override val readerTabBookmarks = "Đã lưu"
    override val readerTabHistory = "Nhật ký"
    override val btnClearAllHistory = "Xóa tất cả nhật ký"
    override val historyEmpty = "Không có lịch sử đọc truyện nào."
    override val btnGoToSettings = "Đi đến Cài đặt"
    override val btnIgnoreError = "Bỏ qua lỗi"
    override val folderTitle = "Thư mục"
    override val dialogDeleteFolderTitle = "Xóa thư mục"
    override val dialogDeleteFolderPrompt = "Bạn có muốn giữ lại các bài viết trong thư mục này (chuyển ra ngoài thư mục gốc) hay xóa tất cả?"
    override val btnKeepArticles = "Giữ lại bài viết"
    override val btnDeleteAll = "Xóa tất cả"
    override val dialogMoveArticleTitle = "Di chuyển bài viết"
    override val logDetailTitle = "--- NHẬT KÝ DỊCH THUẬT ---"
    override val logTime = "Thời gian"
    override val logType = "Loại dịch"
    override val logUrl = "URL"
    override val logStatus = "Trạng thái"
    override val logUsedKeys = "Khóa API đã dùng"
    override val logDetailedProgress = "Tiến trình chi tiết:"
    override val logResponseSuccess = "Phản hồi dịch từ Gemini:"
    override val logResponseRunning = "Chi tiết trạng thái:"
    override val logResponseFailed = "Chi tiết lỗi:"
    override val toastLogCopied = "Đã sao chép nhật ký vào Clipboard"
    override val toastBookmarkAdded = "Đã thêm vào mục Đã lưu"
    override val toastBookmarkRemoved = "Đã xóa khỏi mục Đã lưu"
    override val ttsPlay = "Phát"
    override val ttsPause = "Tạm dừng"
    override val ttsForward = "Tua tới"
    override val ttsRewind = "Tua lui"

    override val readerPanelTitleOffline = "Ngoại tuyến"
    override val readerPanelTitlePlaying = "Đang phát đoạn %d/%d"
    override val readerTranslatingPrompt = "Đang dịch trang web bằng Gemini AI..."
    override val readerTranslatingSubPrompt = "Quá trình này có thể mất vài giây tùy thuộc vào độ dài trang."
    override val readerErrorTitle = "Đã xảy ra lỗi"
    override val readerNoContent = "Không có nội dung dịch."
    override val readerNoContentSub = "Hãy tải một trang web và nhấn nút Dịch ở góc màn hình."
    override val readerTabContent = "Nội dung"
    override val readerTabQueueFormat = "Đã dịch (%d)"
    override val readerTabBookmarksFormat = "Đánh dấu (%d)"
    override val readerTranslatingCount = "Đang dịch (%d)"
    override val readerPreparingTranslation = "Đang chuẩn bị dịch..."
    override val readerTranslatingFailedCount = "Dịch lỗi (%d)"
    override val readerFolderCount = "Thư mục (%d)"
    override val readerRootItemsCount = "Mục riêng lẻ (%d)"
    override val readerFinishedCount = "Đã dịch xong (%d)"
    override val readerArticleListTitle = "Danh sách bài viết"
    override val readerEmptyQueue = "Danh sách trống"
    override val readerEmptyQueueSub = "Bạn có thể dịch nhiều trang để thêm vào đây và nghe liên tục."
    override val readerNoBookmarks = "Không có trang đánh dấu"
    override val readerNoBookmarksSub = "Bấm biểu tượng Bookmark trên ô địa chỉ để lưu các trang yêu thích của bạn."
    override val readerSavedPagesTitle = "Trang đã lưu"
    override val dialogRenameFolderTitle = "Đổi tên thư mục"
    override val folderRenameLabel = "Tên thư mục mới"
    override val dialogMoveArticlePrompt = "Chọn thư mục đích cho bài viết:\n\"%s\""
    override val folderItemCountTemplate = "%d bài viết"
    override val folderRenameMenu = "Đổi tên"
    override val folderDeleteMenu = "Xóa thư mục"
    override val queueItemMoveMenu = "Di chuyển vào thư mục"
    override val queueItemMoveDown = "Chuyển xuống (1 hàng)"
    override val queueItemMoveToBottom = "Chuyển xuống cuối"
    override val paragraphCountTemplate = "%d đoạn văn"
    override val readerTranslatingMore = "Đang dịch tiếp..."
    override val readerTitle = "Trình Đọc Báo Tiếng Việt"
    override val btnClose = "Đóng"
    override val btnRetry = "Thử lại"
    override val contentDescriptionCollapse = "Thu gọn"
    override val contentDescriptionExpand = "Mở rộng"
    override val contentDescriptionClose = "Đóng"
}

class EnAppStrings : AppStrings {
    override val appName = "WebAITransTTS"
    override val appDesc = "AI Translation Browser & TTS Reader"
    
    override val btnSave = "Save"
    override val btnCancel = "Cancel"
    override val btnOk = "OK"
    override val btnDelete = "Delete"
    override val btnEdit = "Edit"
    override val btnAdd = "Add"
    override val statusSuccess = "Success"
    override val statusRunning = "Running"
    override val statusFailed = "Failed"
    override val info = "Information"
    override val loading = "Loading..."

    override val loginFeature1Title = "Optimized AI CJK Translation"
    override val loginFeature1Desc = "Translate bilingual novels using Gemini, retaining literary tone and traditional terms."
    override val loginFeature2Title = "Queue Sync & Background TTS"
    override val loginFeature2Desc = "Listen to chapters smoothly via Google TTS, even with your screen off."
    override val loginFeature3Title = "Folder Grouping & Management"
    override val loginFeature3Desc = "Organize your translated novel chapters into custom categorized folders."
    override val loginGoogleButton = "Sign in with Google Account"
    override val loginRequiredSubtitle = "Please sign in to continue using the service."
    override val toastLoginSuccess = "Login successful!"
    override val toastLoginFailed = "Login failed: "
    override val toastNoGoogleToken = "Failed to receive ID Token from Google"
    override val toastLoginFailedCode = "Login failed (Error Code: %s)"

    override val settingsTitle = "App Settings"
    override val accountCardTitle = "User Account"
    override val accountLoggedAs = "Web Reader User"
    override val accountNoEmail = "No email provided"
    override val accountLogoutButton = "Sign Out"
    override val accountLoginSyncPrompt = "Sign in with Google to sync settings and manage user benefits."
    override val geminiCardTitle = "Gemini API Configuration"
    override val geminiApiKeyLabel = "Gemini API Key"
    override val geminiApiKeyPlaceholder = "Paste your API keys here, separated by commas or newlines"
    override val geminiApiKeyInfo1 = "Supports multiple API keys for automatic failover rotation."
    override val geminiApiKeyInfo2 = "Get a free API key from Google AI Studio. You can paste a list separated by commas or line breaks."
    override val geminiModelLabel = "AI Model"
    override val transCardTitle = "Translation Settings"
    override val transSourceLabel = "Source Language"
    override val transTargetLabel = "Target Language"
    override val transInstructionsLabel = "Personalized Translation Instructions"
    override val transInstructionsPlaceholder = "e.g., Keep CJK honorifics and xing-huo style..."
    override val transInstructionsHelp = "Add specific instructions for the AI when translating."
    override val btnRestoreDefault = "Restore Default"
    override val ttsCardTitle = "Text-to-Speech (TTS) Settings"
    override val ttsEngineLabel = "TTS Engine"
    override val ttsDefaultEngine = "System Default"
    override val ttsGoogleEngineForce = "Google TTS (Force Enable)"
    override val ttsSpeedLabel = "Speech Speed"
    override val ttsPitchLabel = "Voice Pitch"
    override val btnTtsTest = "Listen to Vietnamese TTS Sample"
    override val ttsTestText = "Hello, this is a test speech on your reader app."
    override val logsCardTitle = "Translation Logs"
    override val logsEmpty = "No translation logs available."
    override val toastLogsCopied = "Copied logs to clipboard"
    override val btnSaveSettings = "Save Settings"
    override val settingsDisplayLanguage = "Display Language"
    override val settingsDisplayLanguageLabel = "UI Display Language"
    override val settingsDisplayLanguageVi = "Tiếng Việt"
    override val settingsDisplayLanguageEn = "English"
    override val settingsDisplayLanguageZh = "简体中文"
    
    override val browserMenuReload = "Reload Page"
    override val browserMenuViewOriginal = "View Original"
    override val browserMenuReloadOriginal = "Reload Original Page"
    override val browserMenuTranslate = "Translate Page"
    override val toastReloadingOriginal = "Reloading original page..."
    override val toastTranslatingToVietnamese = "Translating webpage to Vietnamese..."
    override val toastTranslatingToTarget = "Translating webpage to target language..."
    override val browserMenuSettings = "Settings"
    override val dialogReaderTitle = "Select Reading Mode"
    override val dialogReaderPrompt = "Do you want to translate and read this page immediately, or translate in the background and add to queue for continuous playback?"
    override val saveToFolderLabel = "Save to folder:"
    override val btnReadNow = "Read Now"
    override val btnAddToQueue = "Add to Queue"
    override val dialogNewFolderTitle = "Create New Folder"
    override val folderNameLabel = "Folder Name"
    override val btnCreate = "Create"
    override val optionRootFolder = "Root Folder"
    override val optionCreateNewFolder = "Create New Folder..."
    override val updateDialogTitle = "App Update"
    override val updateDialogForceTitle = "Required Update"
    override val updateDialogMessage = "A new version of WebAITransTTS is available. Please update to access all new features."
    override val updateDialogMessageTemplate = "App %s is ready to download."
    override val updateDialogReleaseNotes = "Release notes:"
    override val btnUpdateNow = "Update Now"
    override val btnUpdateLater = "Later"
    override val contentDescriptionBack = "Cancel input"
    override val contentDescriptionClear = "Clear"
    
    override val readerTabTranslate = "Translation"
    override val readerTabQueue = "Reading Queue"
    override val readerTabBookmarks = "Bookmarks"
    override val readerTabHistory = "Logs"
    override val btnClearAllHistory = "Clear All Logs"
    override val historyEmpty = "No reading history available."
    override val btnGoToSettings = "Go to Settings"
    override val btnIgnoreError = "Ignore Error"
    override val folderTitle = "Folder"
    override val dialogDeleteFolderTitle = "Delete Folder"
    override val dialogDeleteFolderPrompt = "Do you want to keep the articles in this folder (move to root folder) or delete everything?"
    override val btnKeepArticles = "Keep articles"
    override val btnDeleteAll = "Delete all"
    override val dialogMoveArticleTitle = "Move Article"
    override val logDetailTitle = "--- TRANSLATION LOG ---"
    override val logTime = "Time"
    override val logType = "Translation Type"
    override val logUrl = "URL"
    override val logStatus = "Status"
    override val logUsedKeys = "Used API Keys"
    override val logDetailedProgress = "Detailed Steps:"
    override val logResponseSuccess = "Gemini Response:"
    override val logResponseRunning = "Status Details:"
    override val logResponseFailed = "Error Details:"
    override val toastLogCopied = "Logs copied to clipboard"
    override val toastBookmarkAdded = "Added to Bookmarks"
    override val toastBookmarkRemoved = "Removed from Bookmarks"
    override val ttsPlay = "Play"
    override val ttsPause = "Pause"
    override val ttsForward = "Forward"
    override val ttsRewind = "Rewind"

    override val readerPanelTitleOffline = "Offline"
    override val readerPanelTitlePlaying = "Playing segment %d/%d"
    override val readerTranslatingPrompt = "Translating webpage using Gemini AI..."
    override val readerTranslatingSubPrompt = "This process may take a few seconds depending on page length."
    override val readerErrorTitle = "An error occurred"
    override val readerNoContent = "No translated content."
    override val readerNoContentSub = "Load a webpage and click the Translate button at the corner."
    override val readerTabContent = "Content"
    override val readerTabQueueFormat = "Translated (%d)"
    override val readerTabBookmarksFormat = "Bookmarks (%d)"
    override val readerTranslatingCount = "Translating (%d)"
    override val readerPreparingTranslation = "Preparing translation..."
    override val readerTranslatingFailedCount = "Failed (%d)"
    override val readerFolderCount = "Folders (%d)"
    override val readerRootItemsCount = "Uncategorized (%d)"
    override val readerFinishedCount = "Finished (%d)"
    override val readerArticleListTitle = "Article List"
    override val readerEmptyQueue = "Queue empty"
    override val readerEmptyQueueSub = "You can translate multiple pages to add here and listen continuously."
    override val readerNoBookmarks = "No bookmarks"
    override val readerNoBookmarksSub = "Click the Bookmark icon on the address bar to save your favorite pages."
    override val readerSavedPagesTitle = "Saved Pages"
    override val dialogRenameFolderTitle = "Rename Folder"
    override val folderRenameLabel = "New Folder Name"
    override val dialogMoveArticlePrompt = "Choose destination folder for article:\n\"%s\""
    override val folderItemCountTemplate = "%d articles"
    override val folderRenameMenu = "Rename"
    override val folderDeleteMenu = "Delete Folder"
    override val queueItemMoveMenu = "Move to folder"
    override val queueItemMoveDown = "Move Down (1 row)"
    override val queueItemMoveToBottom = "Move to Bottom"
    override val paragraphCountTemplate = "%d paragraphs"
    override val readerTranslatingMore = "Translating next..."
    override val readerTitle = "Article Reader"
    override val btnClose = "Close"
    override val btnRetry = "Retry"
    override val contentDescriptionCollapse = "Collapse"
    override val contentDescriptionExpand = "Expand"
    override val contentDescriptionClose = "Close"
}

class ZhAppStrings : AppStrings {
    override val appName = "WebAITransTTS"
    override val appDesc = "AI 翻译浏览器 & TTS 阅读器"
    
    override val btnSave = "保存"
    override val btnCancel = "取消"
    override val btnOk = "确定"
    override val btnDelete = "删除"
    override val btnEdit = "编辑"
    override val btnAdd = "添加"
    override val statusSuccess = "成功"
    override val statusRunning = "运行中"
    override val statusFailed = "失败"
    override val info = "信息"
    override val loading = "正在加载..."

    override val loginFeature1Title = "优化的 AI 中越翻译"
    override val loginFeature1Desc = "使用 Gemini 翻译双语小说，保留文学基调和中越传统术语。"
    override val loginFeature2Title = "队列同步 & 后台 TTS"
    override val loginFeature2Desc = "使用 Google TTS 语音流畅听书，即使屏幕关闭也可以播放。"
    override val loginFeature3Title = "文件夹管理 & 分类"
    override val loginFeature3Desc = "将翻译的小说章节整理到自定义分类文件夹中。"
    override val loginGoogleButton = "使用 Google 账号登录"
    override val loginRequiredSubtitle = "请登录以继续使用服务。"
    override val toastLoginSuccess = "登录成功！"
    override val toastLoginFailed = "登录失败："
    override val toastNoGoogleToken = "无法从 Google 获取 ID Token"
    override val toastLoginFailedCode = "登录失败（错误码：%s）"

    override val settingsTitle = "应用设置"
    override val accountCardTitle = "用户账户"
    override val accountLoggedAs = "网络阅读器用户"
    override val accountNoEmail = "未提供电子邮箱"
    override val accountLogoutButton = "退出登录"
    override val accountLoginSyncPrompt = "使用 Google 登录以同步设置并管理用户权益。"
    override val geminiCardTitle = "Gemini API 配置"
    override val geminiApiKeyLabel = "Gemini API Key"
    override val geminiApiKeyPlaceholder = "在此处粘贴您的 API Key，以逗号或换行符分隔"
    override val geminiApiKeyInfo1 = "支持多个 API Key，以在出错时自动轮换。"
    override val geminiApiKeyInfo2 = "从 Google AI Studio 获取免费的 API Key。您可以粘贴以逗号或换行符分隔的密钥列表。"
    override val geminiModelLabel = "AI 模型"
    override val transCardTitle = "翻译配置"
    override val transSourceLabel = "源语言"
    override val transTargetLabel = "目标语言"
    override val transInstructionsLabel = "个性化翻译规则"
    override val transInstructionsPlaceholder = "例如：保留中越语境下的代词及历史称呼..."
    override val transInstructionsHelp = "在翻译时为 AI 添加特定指令。"
    override val btnRestoreDefault = "恢复默认"
    override val ttsCardTitle = "语音合成 (TTS) 设置"
    override val ttsEngineLabel = "语音合成引擎"
    override val ttsDefaultEngine = "系统默认"
    override val ttsGoogleEngineForce = "Google TTS (强制启用)"
    override val ttsSpeedLabel = "语速"
    override val ttsPitchLabel = "音调"
    override val btnTtsTest = "试听越南语 TTS 语音"
    override val ttsTestText = "您好，这是您阅读器应用上的测试语音。"
    override val logsCardTitle = "翻译日志"
    override val logsEmpty = "暂无翻译日志。"
    override val toastLogsCopied = "已复制日志到剪贴板"
    override val btnSaveSettings = "保存设置"
    override val settingsDisplayLanguage = "显示语言"
    override val settingsDisplayLanguageLabel = "界面显示语言"
    override val settingsDisplayLanguageVi = "Tiếng Việt"
    override val settingsDisplayLanguageEn = "English"
    override val settingsDisplayLanguageZh = "简体中文"
    
    override val browserMenuReload = "重新加载"
    override val browserMenuViewOriginal = "查看原网页"
    override val browserMenuReloadOriginal = "重新加载原网页"
    override val browserMenuTranslate = "翻译网页"
    override val toastReloadingOriginal = "正在重新加载原网页..."
    override val toastTranslatingToVietnamese = "正在将网页翻译为越南语..."
    override val toastTranslatingToTarget = "正在将网页翻译为目标语言..."
    override val browserMenuSettings = "设置"
    override val dialogReaderTitle = "选择阅读模式"
    override val dialogReaderPrompt = "您想立即翻译并阅读此页面，还是后台翻译并添加到队列中以便连续播放？"
    override val saveToFolderLabel = "保存到文件夹："
    override val btnReadNow = "立即阅读"
    override val btnAddToQueue = "添加到队列"
    override val dialogNewFolderTitle = "创建新文件夹"
    override val folderNameLabel = "文件夹名称"
    override val btnCreate = "创建"
    override val optionRootFolder = "根文件夹"
    override val optionCreateNewFolder = "创建新文件夹..."
    override val updateDialogTitle = "应用更新"
    override val updateDialogForceTitle = "强制更新要求"
    override val updateDialogMessage = "已有新版本 WebAITransTTS。请更新以访问所有新功能。"
    override val updateDialogMessageTemplate = "应用 %s 已准备好下载。"
    override val updateDialogReleaseNotes = "更新日志："
    override val btnUpdateNow = "立即更新"
    override val btnUpdateLater = "稍后"
    override val contentDescriptionBack = "取消输入"
    override val contentDescriptionClear = "清除"
    
    override val readerTabTranslate = "翻译内容"
    override val readerTabQueue = "阅读队列"
    override val readerTabBookmarks = "书签"
    override val readerTabHistory = "日志"
    override val btnClearAllHistory = "清空所有日志"
    override val historyEmpty = "暂无阅读历史。"
    override val btnGoToSettings = "前往设置"
    override val btnIgnoreError = "忽略错误"
    override val folderTitle = "文件夹"
    override val dialogDeleteFolderTitle = "删除文件夹"
    override val dialogDeleteFolderPrompt = "您想保留此文件夹中的文章（移动到根文件夹）还是删除所有内容？"
    override val btnKeepArticles = "保留文章"
    override val btnDeleteAll = "全部删除"
    override val dialogMoveArticleTitle = "移动文章"
    override val logDetailTitle = "--- 翻译日志 ---"
    override val logTime = "时间"
    override val logType = "翻译类型"
    override val logUrl = "URL"
    override val logStatus = "状态"
    override val logUsedKeys = "已使用的 API Key"
    override val logDetailedProgress = "详细步骤："
    override val logResponseSuccess = "Gemini 响应："
    override val logResponseRunning = "状态详情："
    override val logResponseFailed = "错误详情："
    override val toastLogCopied = "已复制日志到剪贴板"
    override val toastBookmarkAdded = "已添加到书签"
    override val toastBookmarkRemoved = "已从书签中移除"
    override val ttsPlay = "播放"
    override val ttsPause = "暂停"
    override val ttsForward = "快进"
    override val ttsRewind = "快退"

    override val readerPanelTitleOffline = "离线"
    override val readerPanelTitlePlaying = "正在播放第 %d/%d 段"
    override val readerTranslatingPrompt = "正在使用 Gemini AI 翻译网页..."
    override val readerTranslatingSubPrompt = "此过程可能需要几秒钟，具体取决于页面长度。"
    override val readerErrorTitle = "发生错误"
    override val readerNoContent = "暂无翻译内容。"
    override val readerNoContentSub = "请加载一个网页，然后点击角落的翻译按钮。"
    override val readerTabContent = "内容"
    override val readerTabQueueFormat = "已翻译 (%d)"
    override val readerTabBookmarksFormat = "书签 (%d)"
    override val readerTranslatingCount = "正在翻译 (%d)"
    override val readerPreparingTranslation = "正在准备翻译..."
    override val readerTranslatingFailedCount = "翻译失败 (%d)"
    override val readerFolderCount = "文件夹 (%d)"
    override val readerRootItemsCount = "未分类 (%d)"
    override val readerFinishedCount = "已完成 (%d)"
    override val readerArticleListTitle = "文章列表"
    override val readerEmptyQueue = "列表为空"
    override val readerEmptyQueueSub = "您可以翻译多个页面添加到此处进行连续播放。"
    override val readerNoBookmarks = "无书签"
    override val readerNoBookmarksSub = "点击地址栏上的书签图标以保存您喜爱的页面。"
    override val readerSavedPagesTitle = "已保存的页面"
    override val dialogRenameFolderTitle = "重命名文件夹"
    override val folderRenameLabel = "新文件夹名称"
    override val dialogMoveArticlePrompt = "选择文章的目的文件夹：\n\"%s\""
    override val folderItemCountTemplate = "%d 篇文章"
    override val folderRenameMenu = "重命名"
    override val folderDeleteMenu = "删除文件夹"
    override val queueItemMoveMenu = "移动到文件夹"
    override val queueItemMoveDown = "下移一格"
    override val queueItemMoveToBottom = "移至底部"
    override val paragraphCountTemplate = "%d 段"
    override val readerTranslatingMore = "正在翻译下一段..."
    override val readerTitle = "网页阅读器"
    override val btnClose = "关闭"
    override val btnRetry = "重试"
    override val contentDescriptionCollapse = "收起"
    override val contentDescriptionExpand = "展开"
    override val contentDescriptionClose = "关闭"
}

val LocalAppStrings = staticCompositionLocalOf<AppStrings> {
    ViAppStrings()
}
