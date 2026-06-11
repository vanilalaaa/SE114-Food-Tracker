package com.SE114.food_tracker.feature.diary

import android.app.DatePickerDialog
import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.SE114.food_tracker.core.designsystem.components.DiaryTopBar
import com.SE114.food_tracker.core.designsystem.components.DraggableFoodItem
import com.SE114.food_tracker.core.designsystem.components.NutritionCard
import com.SE114.food_tracker.core.designsystem.theme.FoodTrackerTheme
import com.SE114.food_tracker.core.designsystem.theme.MainBackground
import com.SE114.food_tracker.feature.diary.components.CalendarCard
import com.SE114.food_tracker.feature.diary.components.CategoryRowItem
import kotlinx.datetime.LocalDate
import timber.log.Timber
import java.io.File

@Composable
fun DiaryScreen(
    triggerAdd: Boolean = false,
    onAddTriggered: () -> Unit = {}
) {
    val diaryViewModel    = hiltViewModel<DiaryViewModel>()
    val categoryViewModel = hiltViewModel<CategoryViewModel>()
    val uiState           by diaryViewModel.uiState.collectAsStateWithLifecycle()
    val categoryState     by categoryViewModel.visibleCategories.collectAsStateWithLifecycle()
    val pendingImageUri   by diaryViewModel.pendingImageUri.collectAsStateWithLifecycle()

    val categories = categoryState.ifEmpty { uiState.categories }

    DiaryScreenContent(
        uiState         = uiState,
        pendingImageUri = pendingImageUri,
        categories      = categories,
        triggerAdd      = triggerAdd,
        onAddTriggered  = onAddTriggered,
        onLoadDate      = { diaryViewModel.loadDate(it) },
        onImageSelected = { diaryViewModel.onImageSelected(it) },
        onSaveItem      = { n, p, c, r, no, t -> diaryViewModel.saveItem(n, p, c, r, no, t) },
        onUpdateItem    = { id, n, p, c, r, no, t -> diaryViewModel.updateItem(id, n, p, c, r, no, t) },
        onDeleteItem    = { diaryViewModel.deleteItem(it) },
        onDeleteCategory           = { categoryViewModel.deleteCategory(it) },
        onToggleCategoryVisibility = { categoryViewModel.toggleVisibility(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreenContent(
    uiState: DiaryUiState,
    pendingImageUri: Uri?,
    categories: List<DiaryCategory>,
    triggerAdd: Boolean,
    onAddTriggered: () -> Unit,
    onLoadDate: (LocalDate) -> Unit,
    onImageSelected: (Uri) -> Unit,
    onSaveItem: (String, Double, String, Int, String, Int) -> Unit,
    onUpdateItem: (String, String, Double, String, Int, String, Int) -> Unit,
    onDeleteItem: (String) -> Unit,
    onDeleteCategory: (DiaryCategory) -> Unit,
    onToggleCategoryVisibility: (DiaryCategory) -> Unit
) {
    var showDetailSheet     by remember { mutableStateOf(false) }
    var showEntryScreen     by remember { mutableStateOf(false) }
    var showSourceScreen    by remember { mutableStateOf(false) }
    var selectedItemForEdit by remember { mutableStateOf<DiaryItem?>(null) }
    var showManageCategories by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val context = LocalContext.current

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // Bộ chọn Gallery hiện đại
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onImageSelected(uri)
            showEntryScreen = true // Cài đặt ảnh xong chuyển hướng thẳng sang màn nhập liệu
        }
    }

    // Bộ kích hoạt Camera chụp hình
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { isSuccess: Boolean ->
        if (isSuccess) {
            tempCameraUri?.let { uri ->
                onImageSelected(uri)
                showEntryScreen = true // Chụp ảnh thành công chuyển sang màn nhập liệu
            }
        }
    }

    LaunchedEffect(triggerAdd) {
        if (triggerAdd) {
            selectedItemForEdit = null
            showSourceScreen = true
            onAddTriggered()
        }
    }

    val datePickerDialog = remember(uiState.selectedYear, uiState.selectedMonth, uiState.selectedDayOfMonth) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                runCatching { LocalDate(year, month + 1, dayOfMonth) }
                    .getOrNull()
                    ?.let { onLoadDate(it) }
            },
            uiState.selectedYear,
            uiState.selectedMonth - 1,
            uiState.selectedDayOfMonth
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MainBackground)
            .verticalScroll(scrollState)
    ) {
        DiaryTopBar(
            streakCount  = uiState.streak.toString(),
            currentMonth = "Tháng ${uiState.selectedMonth} ${uiState.selectedYear}",
            onMonthClick = { datePickerDialog.show() }
        )
        NutritionCard(
            onMenuClick = { },
            items = uiState.items.map { item ->
                DraggableFoodItem(
                    id       = item.itemId,
                    imageUrl = item.imageUrl,
                    emoji    = item.categoryIconUrl.ifBlank { "🍱" }
                )
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        CalendarCard(
            selectedYear  = uiState.selectedYear,
            selectedMonth = uiState.selectedMonth,
            onDateClick = { day ->
                runCatching {
                    LocalDate(uiState.selectedYear, uiState.selectedMonth, day)
                }.getOrNull()?.let { selectedDate ->
                    onLoadDate(selectedDate)
                    showDetailSheet = true
                }
            },
            hasDataDates = uiState.datesWithData.toList()
        )
        Spacer(modifier = Modifier.height(32.dp))
    }

    // ── Toàn bộ các hệ thống Popups / Overlays ──────────────────────────────

    if (showDetailSheet) {
        DayDetailBottomSheet(
            onDismiss    = { showDetailSheet = false },
            selectedDate = uiState.selectedDate,
            items        = uiState.filteredItems,
            categories   = categories,
            onEditItem   = { item ->
                selectedItemForEdit = item
                showDetailSheet     = false
                showEntryScreen     = true
            },
            onAddItem    = {
                selectedItemForEdit = null
                showDetailSheet     = false
                showSourceScreen    = true
            },
            onDeleteItem = onDeleteItem
        )
    }

    if (showSourceScreen) {
        AddFoodSourceScreen(
            onBack = { showSourceScreen = false },
            onCameraSelected = {
                showSourceScreen = false
                runCatching { createTempImageUri(context) }
                    .onSuccess { uri ->
                        tempCameraUri = uri
                        cameraLauncher.launch(uri) // Gọi app Camera hệ thống
                    }
                    .onFailure { e ->
                        Timber.e(e, "Không tạo được file tạm để chụp ảnh")
                    }
            },
            onGallerySelected = {
                showSourceScreen = false
                // Gọi bộ chọn ảnh hệ thống (Chỉ hiển thị tệp Định dạng hình ảnh)
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onManualSelected = {
                showSourceScreen = false
                showEntryScreen  = true
            }
        )
    }

    if (showEntryScreen) {
        val editingItem = selectedItemForEdit
        FoodEntryScreen(
            existingItem    = editingItem,
            categories      = categories,
            pendingImageUri = pendingImageUri,
            onDismiss       = {
                showEntryScreen     = false
                selectedItemForEdit = null
            },
            onSave = { name, price, categoryId, rating, note, timeType ->
                if (editingItem == null) {
                    onSaveItem(name, price, categoryId, rating, note, timeType)
                } else {
                    onUpdateItem(editingItem.itemId, name, price, categoryId, rating, note, timeType)
                }
                showEntryScreen     = false
                selectedItemForEdit = null
                showDetailSheet     = true
            },
            onDelete = { itemId ->
                onDeleteItem(itemId)
                showEntryScreen     = false
                selectedItemForEdit = null
            },
            onManageCategories = { showManageCategories = true }
        )
    }

    if (showManageCategories) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showManageCategories = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Quản lý danh mục",
                    modifier = Modifier.padding(vertical = 16.dp),
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge
                )
                LazyColumn {
                    items(
                        items = categories,
                        key   = { it.categoryId }
                    ) { category ->
                        CategoryRowItem(
                            category = category,
                            onEdit   = { },
                            onDelete = { onDeleteCategory(category) },
                            onVisibilityToggle = { onToggleCategoryVisibility(category) }
                        )
                    }
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "＋ Thêm danh mục mới (Sprint 2)",
                            modifier = Modifier.padding(12.dp),
                            color = androidx.compose.ui.graphics.Color(0xFFC98989)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DiaryScreenPreview() {
    FoodTrackerTheme {
        DiaryScreenContent(
            uiState = DiaryUiState(
                streak = 5,
                datesWithData = setOf(4, 5, 6),
                totalSpend = 45000.0,
                itemCount = 2
            ),
            pendingImageUri = null,
            categories = listOf(
                DiaryCategory("1", "Cơm", "🍚", isSystem = true),
                DiaryCategory("2", "Mì & Phở", "🍜", isSystem = true)
            ),
            triggerAdd = false,
            onAddTriggered = {},
            onLoadDate = {},
            onImageSelected = {},
            onSaveItem = { _, _, _, _, _, _ -> },
            onUpdateItem = { _, _, _, _, _, _, _ -> },
            onDeleteItem = {},
            onDeleteCategory = {},
            onToggleCategoryVisibility = {}
        )
    }
}
private fun createTempImageUri(context: Context): Uri {
    val tempFile = File.createTempFile(
        "JPEG_${System.currentTimeMillis()}_",
        ".jpg",
        context.cacheDir // Lưu trong thư mục cache của app để tự động dọn dẹp
    ).apply {
        createNewFile()
        deleteOnExit()
    }
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        tempFile
    )
}