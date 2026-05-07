# Contributing Guide (Android)

## Prerequisites

- Android Studio (Phiên bản Iguana hoặc mới hơn)
- JDK 17
- Android SDK (Min API: 24, Target API: 34)
- Git

## Git Workflow

### Branch Strategy

```text
main              ← Production (Bản build nộp bài - protected)
  │
develop           ← Integration branch (Gộp code để test - protected)
  │
feature/* ← New features (Tính năng mới)
bugfix/* ← Bug fixes (Sửa lỗi)
hotfix/* ← Urgent fixes (Sửa lỗi gấp trên main)
```

### Quy trình làm việc

```bash
# 1. Clone repo (lần đầu)
git clone <repo-url>
cd SE114-Food-Tracker
# (Mở project bằng Android Studio và đợi Gradle Sync)

# 2. Luôn bắt đầu từ develop mới nhất
git checkout develop
git pull origin develop

# 3. Tạo branch mới
git checkout -b feature/them-man-hinh-map

# 4. Code và commit thường xuyên
git add .
git commit -m "feat: add map screen with google maps sdk"

# 5. Push branch lên remote
git push origin feature/them-man-hinh-map

# 6. Tạo Pull Request trên GitHub
#    - Base: develop
#    - Compare: feature/them-man-hinh-map

# 7. Sau khi PR được merge, xóa branch local
git checkout develop
git pull origin develop
git branch -d feature/them-man-hinh-map
```

### Commit Message Convention

Format: `type: message`

| Type | Mô tả |
|------|-------|
| `feat` | Tính năng mới (UI mới, logic mới) |
| `fix` | Sửa lỗi (Crash, UI lệch, sai logic) |
| `refactor` | Tối ưu code (Không đổi logic/chức năng) |
| `docs` | Thêm tài liệu (README, comment code) |
| `test` | Thêm/sửa Unit Test, UI Test |
| `chore` | Cập nhật Gradle, thêm thư viện, setup |

Ví dụ:
```text
feat: add firebase storage for image upload
fix: resolve database sync issue on offline mode
chore: update compose bom version
```

### Code Review

- Mỗi PR cần ít nhất 1 thành viên review và approve.
- Đảm bảo app build thành công (không có lỗi Gradle).
- Resolve tất cả comments trước khi merge.

## Getting Started

```bash
# 1. Mở Android Studio -> Open Project -> Chọn thư mục SE114-Food-Tracker.
# 2. Chờ Android Studio tự động chạy Gradle Sync.

# Cấu hình Firebase (Bắt buộc):
# - Lấy file google-services.json từ trưởng nhóm.
# - Copy file này thả vào thư mục app/ của dự án.

# Chạy App trên máy ảo (Emulator) hoặc thiết bị thật:
# Bấm nút [Run] (Tam giác xanh) trên thanh công cụ của Android Studio.
# Hoặc chạy lệnh:
./gradlew assembleDebug
```

## Project Structure

Dự án áp dụng kiến trúc **MVVM (Model-View-ViewModel)**.

```text
app/src/main/java/com/SE114/Food_tracker/
├── data/               # Tầng xử lý Data
│   ├── local/          # Room DB (Entities, DAOs, Database class)
│   └── repository/     # Logic lấy/lưu data (nối Room & Firebase)
├── domain/             # Tầng nghiệp vụ cốt lõi
│   └── model/          # Các Data classes dùng chung
├── ui/                 # Tầng Giao diện (Jetpack Compose)
│   ├── components/     # UI dùng chung (Buttons, TopBar, Dialogs)
│   ├── navigation/     # Cấu hình chuyển trang (NavHost)
│   ├── screens/        # Chia theo luồng tính năng (Home, Map, Add)
│   └── theme/          # Màu sắc, Typography, Shape
└── util/               # Các hàm hỗ trợ
    ├── Constants.kt    # Hằng số (Tên bảng, Firebase keys...)
    └── Extension.kt    # Hàm mở rộng (Format tiền, thời gian)
```

## Development Workflow

### 1. Thêm một Bảng mới vào Database (Room)

1. Tạo Entity trong `data/local/`:
```kotlin
// data/local/CategoryEntity.kt
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val iconUrl: String
)
```

2. Tạo DAO trong `data/local/`:
```kotlin
// data/local/CategoryDao.kt
@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<CategoryEntity>>
    
    @Insert
    suspend fun insertCategory(category: CategoryEntity)
}
```

3. Đăng ký DAO vào file `AppDatabase.kt`.

### 2. Thêm một Màn hình mới (Jetpack Compose)

1. Tạo ViewModel trong `ui/screens/`:
```kotlin
// ui/screens/home/HomeViewModel.kt
class HomeViewModel(private val repository: DiaryRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    fun loadData() { ... }
}
```

2. Tạo Compose Screen trong `ui/screens/`:
```kotlin
// ui/screens/home/HomeScreen.kt
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToAdd: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    
    Scaffold(...) { padding ->
        // Vẽ UI tại đây
    }
}
```

3. Khai báo màn hình vào `ui/navigation/AppNavigation.kt`.

## Testing

```bash
# Chạy toàn bộ Unit Test (Logic, ViewModel, Utils)
./gradlew testDebugUnitTest

# Chạy Instrumented Test (UI Test, Room DB Test - Yêu cầu bật máy ảo)
./gradlew connectedAndroidTest
```

### Writing Tests

Unit Test (viết trong thư mục `test/`):
```kotlin
// util/ExtensionTest.kt
@Test
fun `formatCurrency returns correctly formatted VND`() {
    val amount = 150000.0
    val result = formatCurrency(amount)
    assertEquals("150,000 ₫", result)
}
```

UI Test (viết trong thư mục `androidTest/`):
```kotlin
// ui/components/PrimaryButtonTest.kt
@get:Rule
val composeTestRule = createComposeRule()

@Test
fun buttonDisplaysCorrectText() {
    composeTestRule.setContent {
        PrimaryButton(text = "Lưu bữa ăn", onClick = {})
    }
    composeTestRule.onNodeWithText("Lưu bữa ăn").assertIsDisplayed()
}
```

## Code Style

- Tuân thủ chuẩn [Kotlin Style Guide](https://developer.android.com/kotlin/style-guide).
- Format code bằng `Ctrl + Alt + L` (Windows) hoặc `Cmd + Option + L` (Mac) trước khi commit.
- Sử dụng **Jetpack Compose** cho 100% UI (Không dùng XML layout).
- Các tác vụ gọi mạng (Firebase) hoặc Database (Room) **bắt buộc** phải dùng `Coroutines` (suspend functions) chạy trên `Dispatchers.IO`.

## Commands Reference

| Command | Mô tả |
|---------|-------------|
| `./gradlew assembleDebug` | Build file APK (chế độ Dev) |
| `./gradlew build` | Build toàn bộ project & chạy Test |
| `./gradlew clean` | Xóa thư mục build (Dùng khi Gradle bị lỗi) |
| `./gradlew lint` | Kiểm tra các lỗi Code Style & Cảnh báo |

## Môi trường & Test Data

App hoạt động theo cơ chế **Offline-first (Room Database)**.
- Ảnh được lưu lên `Firebase Storage` thông qua cơ chế Anonymous Auth (Không cần tạo tài khoản mật khẩu).
- **Lưu ý:** Không đẩy file `google-services.json` lên Git để bảo mật Firebase. (File này đã được chặn mặc định trong `.gitignore` của Android).
