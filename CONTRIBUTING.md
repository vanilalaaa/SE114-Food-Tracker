# Contributing Guide — Food Tracker (SE114)

## Prerequisites

- Android Studio Iguana hoặc mới hơn
- JDK 17
- Android SDK (Min API: 24, Target API: 35)
- Node.js 18+ (chỉ cần nếu làm Admin web)
- Git
- Tài khoản Supabase (xin invite từ Lead)

## Tech Stack

| Hạng mục | Lựa chọn |
|----------|----------|
| Ngôn ngữ | Kotlin 2.0+ |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Repository |
| DI | Hilt |
| Local DB | Room |
| Async | Coroutines + Flow |
| Navigation | Navigation Compose |
| Backend | Supabase (Auth + Postgres + Storage + Realtime) |
| SDK Supabase | supabase-kt (jan-tennert/supabase-kt) |
| Image loading | Coil |
| Background sync | WorkManager + Hilt Worker |
| Biểu đồ | Vico |
| Date/time | kotlinx-datetime |
| Logging | Timber |
| Admin web | Next.js 14 + Tailwind + supabase-js |

Phiên bản cụ thể trong `gradle/libs.versions.toml`. **Không nâng version giữa sprint.**

## Git Workflow

### Branch Strategy

```text
main              ← Production (build nộp bài, protected)
  │
develop           ← Integration branch (protected, merge qua PR)
  │
feature/tv{N}-{tên}    ← Tính năng mới (TV1/TV2/TV3/TV4)
bugfix/tv{N}-{tên}     ← Sửa lỗi
hotfix/{tên}           ← Sửa lỗi gấp trên main
```

**Quy ước branch theo thành viên:**
- **TV1 (Lead):** `feature/tv1-auth`, `feature/tv1-sync-framework`, `feature/tv1-admin-web`, ...
- **TV2 (DB):** `feature/tv2-diary-logic`, `feature/tv2-stats`, ...
- **TV3 (UI):** `feature/tv3-friendship`, `feature/tv3-newsfeed`, `feature/ui-diary-base` (UI Nhật ký kế thừa), ...
- **TV4:** `feature/tv4-chat`, `feature/tv4-wallet`, `feature/ui-stats-base` (UI Thống kê kế thừa), ...

### Quy trình làm việc

```bash
# 1. Clone repo (lần đầu)
git clone <repo-url>
cd SE114-Food-Tracker

# 2. Luôn bắt đầu từ develop mới nhất
git checkout develop
git pull origin develop

# 3. Tạo branch mới (đúng quy ước)
git checkout -b feature/tv3-friendship

# 4. Code và commit thường xuyên
git add .
git commit -m "feat(friend): add user search by user_id"

# 5. Push branch lên remote
git push origin feature/tv3-friendship

# 6. Tạo Pull Request trên GitHub
#    - Base: develop
#    - Compare: feature/tv3-friendship
#    - Tag ít nhất 1 reviewer (không phải tác giả)

# 7. Sau khi PR được merge, xóa branch local
git checkout develop
git pull origin develop
git branch -d feature/tv3-friendship
```

### Commit Message Convention

Format: `type(scope): message`

| Type | Mô tả |
|------|-------|
| `feat` | Tính năng mới |
| `fix` | Sửa lỗi |
| `refactor` | Tối ưu code (không đổi logic) |
| `docs` | Thêm/sửa tài liệu |
| `style` | Format code, không đổi logic |
| `chore` | Cập nhật Gradle, lib, setup |

Scope là tên feature: `auth`, `diary`, `stats`, `friend`, `feed`, `chat`, `wallet`, `report`, `settings`, `core`, `sync`.

Ví dụ:
```text
feat(diary): add filter by category in calendar
fix(chat): retry failed messages when network back
refactor(stats): extract forecast logic to UseCase
chore: bump compose bom to 2024.10.00
```

### Code Review Checklist

Trước khi tạo PR, tự kiểm tra:
- [ ] App build thành công, không lint error nghiêm trọng
- [ ] Đã chạy thử trên thiết bị thật (không chỉ emulator)
- [ ] Không hardcode string (dùng `strings.xml`)
- [ ] Không hardcode màu (dùng `MaterialTheme.colorScheme`)
- [ ] Không gọi Supabase trực tiếp từ ViewModel (phải qua Repository)
- [ ] ViewModel không reference Context (trừ Application)
- [ ] Composable công khai có `@Preview`
- [ ] Test happy path + 2 edge case (mất mạng, data rỗng)

Mỗi PR cần **ít nhất 1 reviewer (không phải tác giả) approve** trước khi merge.

## Getting Started

### 1. Mở project

```text
Mở Android Studio → Open Project → chọn thư mục SE114-Food-Tracker
Đợi Gradle Sync xong (lần đầu mất 5-10 phút)
```

### 2. Cấu hình Supabase (BẮT BUỘC)

Xin Lead 2 giá trị: **Supabase URL** và **Supabase Anon Key**.

Tạo file `local.properties` ở root project (nếu chưa có) và thêm:

```properties
SUPABASE_URL=https://xxxxx.supabase.co
SUPABASE_ANON_KEY=eyJhbGc...
```

**KHÔNG commit `local.properties` lên Git** — đã chặn sẵn trong `.gitignore`.

### 3. Chạy app

```bash
# Bấm nút Run (▶) trên Android Studio
# Hoặc:
./gradlew assembleDebug
```

## Project Structure

Dự án theo kiến trúc **MVVM + Repository**, chia theo feature:

```text
app/src/main/java/com/SE114/Food_tracker/
├── core/                       # Code dùng chung toàn app
│   ├── common/                 # Result, Constants, extensions
│   ├── designsystem/           # Theme, Color, Typography, components shared
│   ├── network/                # SupabaseClient, NetworkMonitor
│   ├── database/               # AppDatabase, Converters
│   └── sync/                   # SyncWorker, SyncManager (Lead xây dựng)
├── feature/                    # Mỗi feature 1 thư mục, không phụ thuộc chéo
│   ├── auth/                   # TV1
│   ├── diary/                  # TV2 (logic) + TV3 (UI)
│   ├── stats/                  # TV2 (logic) + TV4 (UI)
│   ├── settings/               # TV1
│   ├── profile/                # TV1 (mình) + TV3 (bạn bè)
│   ├── friend/                 # TV3
│   ├── feed/                   # TV3
│   ├── chat/                   # TV4
│   ├── wallet/                 # TV4
│   └── report/                 # TV4
├── data/                       # Tầng data
│   ├── local/                  # Entity, DAO
│   ├── remote/                 # DTO, Supabase calls
│   ├── repository/             # Impl Repository (nối Room + Supabase)
│   └── model/                  # Domain model
├── di/                         # Hilt modules
└── MainActivity.kt
```

**Quy tắc:**
- Feature CHỈ phụ thuộc `core/`. Không import chéo giữa các `feature/*`.
- Nếu feature A cần data của feature B → gọi qua Repository chung trong `data/repository/`.

## Development Workflow

### 1. Thêm Entity Room

Mọi entity sync được phải có 3 cờ: `updatedAt`, `syncStatus`, `isDeleted`.

```kotlin
// data/local/ItemEntity.kt
@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val id: String,           // UUID sinh ở client
    val ownerId: String,
    val categoryId: String,
    val name: String,
    val price: Double,
    val timeType: Int,                    // 0=Sáng, 1=Trưa/Chiều, 2=Tối
    val entryDate: Long,                  // epoch millis
    val rating: Int?,
    val note: String?,
    val imageUrl: String?,
    val isShared: Boolean,
    val walletId: String?,
    val currencyCode: String,
    val updatedAt: Long,                  // BẮT BUỘC cho sync
    val syncStatus: SyncStatus,           // PENDING / SYNCED / FAILED
    val isDeleted: Boolean = false        // Soft delete
)
```

### 2. Thêm DAO

```kotlin
// data/local/ItemDao.kt
@Dao
interface ItemDao {
    @Query("SELECT * FROM items WHERE ownerId = :ownerId AND isDeleted = 0 ORDER BY entryDate DESC")
    fun observeItems(ownerId: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSync(): List<ItemEntity>

    @Upsert
    suspend fun upsert(item: ItemEntity)
}
```

Đăng ký DAO vào `AppDatabase.kt`.

### 3. Thêm Repository

Repository LUÔN ghi xuống Room trước, đẩy lên Supabase sau qua SyncWorker:

```kotlin
// data/repository/ItemRepositoryImpl.kt
class ItemRepositoryImpl @Inject constructor(
    private val itemDao: ItemDao,
    private val syncManager: SyncManager
) : ItemRepository {

    override fun observeItems(ownerId: String) = itemDao.observeItems(ownerId)

    override suspend fun addItem(item: Item) {
        val entity = item.toEntity().copy(
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )
        itemDao.upsert(entity)
        syncManager.enqueueItemSync()   // WorkManager đẩy lên Supabase
    }
}
```

### 4. Thêm màn hình Compose

```kotlin
// feature/diary/DiaryViewModel.kt
@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val repository: ItemRepository
) : ViewModel() {
    val uiState: StateFlow<DiaryUiState> = repository.observeItems(currentUserId)
        .map { DiaryUiState(items = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DiaryUiState())

    fun addItem(item: Item) = viewModelScope.launch {
        repository.addItem(item)
    }
}
```

```kotlin
// feature/diary/DiaryScreen.kt
@Composable
fun DiaryScreen(
    viewModel: DiaryViewModel = hiltViewModel(),
    onAddClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(...) { /* UI */ }
}

@Preview
@Composable
private fun DiaryScreenPreview() {
    AppTheme { /* preview */ }
}
```

Khai báo route vào navigation graph của feature.

## Quy tắc đặc biệt — Mảng Diary và Stats

Plan dự án có một ngoại lệ: **UI và logic của Diary / Stats nằm ở 2 người khác nhau**:

- **UI Nhật ký**: TV3 dựng → branch `feature/ui-diary-base`
- **UI Thống kê**: TV4 dựng → branch `feature/ui-stats-base`
- **Logic (ViewModel + Repository)**: TV2 phụ trách

### State contract (BẮT BUỘC chốt ngày 1 Sprint 1)

Trước khi viết code, TV2 + TV3 + TV4 ngồi cùng chốt:

1. **TV2 expose state gì**: `DiaryUiState` chứa field nào (items, selectedDate, filter, isLoading...).
2. **TV2 expose action gì**: hàm trong ViewModel mà UI sẽ gọi (`onDateSelect`, `onAddItem`, `onFilterChange`...).
3. **TV3 / TV4 commit UI base lên branch trước** để TV2 import và ráp ViewModel.

File chốt: `docs/state-contracts/diary.md` và `docs/state-contracts/stats.md`. Mọi thay đổi state contract sau ngày 1 phải báo cả hai bên.

## Sync Framework (do Lead xây dựng)

Mọi feature có data sync với Supabase phải dùng `SyncManager`:

```kotlin
// Khi tạo/sửa data
suspend fun addItem(item: Item) {
    itemDao.upsert(item.toEntity(syncStatus = PENDING))
    syncManager.enqueueItemSync()       // Đẩy lên Supabase nền
}

// Khi mở app
syncManager.startInitialSync()          // Pull thay đổi từ Supabase
```

**Cờ trạng thái trên entity:**

| Field | Type | Ý nghĩa |
|-------|------|---------|
| `updatedAt` | Long (epoch UTC) | Mọi insert/update set = `now()` |
| `syncStatus` | PENDING / SYNCED / FAILED | PENDING = chưa push |
| `isDeleted` | Boolean | Soft delete (tránh conflict khi xóa offline) |

**Conflict resolution:** Last-Write-Wins theo `updatedAt`.

**Lưu ý:** Quỹ nhóm (`wallet_*`) là ngoại lệ — **bắt buộc online**, gọi RPC function của Supabase trong transaction để tránh race condition.

## Code Style

- Tuân thủ [Kotlin Style Guide](https://developer.android.com/kotlin/style-guide).
- Format code bằng `Ctrl + Alt + L` (Windows) / `Cmd + Option + L` (Mac) trước commit.
- **100% UI bằng Jetpack Compose**, không dùng XML layout.
- Mọi I/O (Supabase, Room, file) **bắt buộc** chạy trên `Dispatchers.IO` qua suspend function.
- **Không gọi Supabase trực tiếp từ ViewModel** — phải qua Repository.
- **Không hardcode** string (dùng `strings.xml`) và màu (dùng `MaterialTheme.colorScheme`).
- Composable đặt tên **danh từ** (`DiaryScreen`, `ItemCard`), không phải động từ (`ShowDiary`).
- Function đặt tên **động từ** camelCase: `addItem()`, `observeItems()`.
- Hằng số đặt trong `companion object`: `const val MAX_NAME_LENGTH = 50`.
- Resource string: snake_case có prefix feature: `diary_add_item`, `auth_login_title`.

## Testing

Dự án **không yêu cầu unit test bắt buộc** — ưu tiên app hoạt động mượt và demo tốt. Tuy nhiên khuyến khích viết test cho:

- Hàm util (format tiền, parse ngày)
- Logic dự báo / so sánh kỳ trong Stats
- RPC quỹ nhóm (kiểm tra concurrency)

```bash
# Chạy unit test (optional)
./gradlew testDebugUnitTest
```

## Definition of Done

Một feature chỉ được coi là **Done** khi:

- [ ] Code merged vào `develop` qua PR đã review
- [ ] Build pass, không warning lint nghiêm trọng
- [ ] Chạy được trên thiết bị thật (Android 9+ và Android 13+)
- [ ] Có `@Preview` cho Composable công khai
- [ ] Test happy path + 2 edge case (mất mạng, dữ liệu rỗng)
- [ ] Test sync: tạo offline → bật mạng → kiểm tra data lên Supabase đúng
- [ ] Không crash khi xoay màn hình
- [ ] String dùng `strings.xml`

## Commands Reference

| Command | Mô tả |
|---------|-------|
| `./gradlew assembleDebug` | Build APK debug |
| `./gradlew assembleRelease` | Build APK release (cần signing config) |
| `./gradlew clean` | Xóa thư mục build (khi Gradle lỗi) |
| `./gradlew lint` | Kiểm tra lint |
| `./gradlew testDebugUnitTest` | Chạy unit test (optional) |

## Môi trường & Lưu ý

- App hoạt động **offline-first** qua Room.
- Ảnh upload lên **Supabase Storage** (bucket: `avatars`, `items`, `posts`, `messages`).
- File `local.properties` chứa Supabase keys — **không commit lên Git** (đã trong `.gitignore`).
- **Quỹ nhóm bắt buộc online** — đừng cho phép thao tác khi mất mạng (race condition).
- Tỷ giá multi-currency cache trong DataStore, refresh mỗi 24h.
