package com.SE114.food_tracker.feature.diary

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.SE114.food_tracker.core.designsystem.theme.FoodTrackerTheme
import com.SE114.food_tracker.core.designsystem.theme.MainBackground
import com.SE114.food_tracker.feature.diary.components.CategorySelector
import com.SE114.food_tracker.feature.diary.components.FoodInputField
import com.SE114.food_tracker.feature.diary.components.ManageCategoryBottomSheet
import com.SE114.food_tracker.feature.diary.components.StarRatingBar
import com.SE114.food_tracker.feature.diary.components.TimeSelector
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodEntryScreen(
    existingItem: DiaryItem? = null,
    preSelectedCategory: DiaryCategory? = null,
    categories: List<DiaryCategory> = emptyList(),
    pendingImageUri: Uri? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, price: Double, categoryId: String, rating: Int, note: String, timeType: Int) -> Unit,
    onDelete: ((String) -> Unit)? = null,
    onToggleCategoryVisibility: (DiaryCategory) -> Unit = {},
    onDeleteCategory: (DiaryCategory) -> Unit = {},
    onCreateCategory: (String, String) -> Unit = { _, _ -> }
) {
    var foodName by remember(existingItem?.itemId, preSelectedCategory) {
        mutableStateOf(existingItem?.name ?: preSelectedCategory?.name.orEmpty())
    }

    var price by remember(existingItem?.itemId) {
        mutableStateOf(existingItem?.price?.takeIf { it > 0.0 }?.toLong()?.toString().orEmpty())
    }

    var note by remember(existingItem?.itemId) { mutableStateOf(existingItem?.note.orEmpty()) }

    var selectedCategoryId by remember(existingItem?.itemId, categories) {
        val fallbackCategories = categories.ifEmpty {
            listOf(DiaryCategory("mock-1", "Cơm", "🍚", isSystem = true))
        }
        mutableStateOf(
            existingItem?.categoryId
                ?: preSelectedCategory?.categoryId
                ?: fallbackCategories.firstOrNull()?.categoryId
                ?: ""
        )
    }

    var rating        by remember(existingItem?.itemId) { mutableIntStateOf(existingItem?.rating ?: 0) }
    var nameError     by remember { mutableStateOf<String?>(null) }
    var priceError    by remember { mutableStateOf<String?>(null) }
    var categoryError by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog    by remember { mutableStateOf(false) }
    var showManageCategoriesSheet by remember { mutableStateOf(false) }

    var selectedHour by remember(existingItem?.itemId) {
        val initialHour = when (existingItem?.timeType) {
            0 -> 8   // Nếu là món Sáng cũ -> đưa về 8 giờ mặc định
            1 -> 12  // Nếu là món Trưa cũ -> đưa về 12 giờ mặc định
            2 -> 19  // Nếu là món Tối cũ -> đưa về 19 giờ mặc định
            else -> Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        }
        mutableIntStateOf(initialHour)
    }
    var selectedMinute by remember(existingItem?.itemId) {
        mutableIntStateOf(if (existingItem != null) 0 else Calendar.getInstance().get(Calendar.MINUTE))
    }
    var showTimePicker by remember { mutableStateOf(false) }

    // Tự động phân loại timeType khớp với constraint của Database (0: Sáng, 1: Trưa/Chiều, 2: Tối)
    val autoTimeType = when (selectedHour) {
        in 5..10  -> 0
        in 11..16 -> 1
        else      -> 2
    }

    // Nhãn hiển thị chi tiết cho UI người dùng
    val sessionLabel = when (selectedHour) {
        in 5..10  -> "Sáng"
        in 11..12 -> "Trưa"
        in 13..16 -> "Chiều"
        else      -> "Tối"
    }

    val sessionIcon = when (selectedHour) {
        in 5..10  -> "🌅"
        in 11..12 -> "☀️"
        in 13..16 -> "⛅"
        else      -> "🌙"
    }

    val displayTime = String.format("%02d:%02d", selectedHour, selectedMinute)

    val selectedCategoryObj = categories.find { it.categoryId == selectedCategoryId }
    val displayIcon         = selectedCategoryObj?.iconUrl ?: "🍱"

    fun submit() {
        val trimmedName = foodName.trim()
        val parsedPrice = price.trim().toDoubleOrNull()

        nameError     = if (trimmedName.isBlank()) "Tên món không được để trống" else null
        priceError    = when {
            parsedPrice == null -> "Vui lòng nhập giá hợp lệ"
            parsedPrice <= 0.0  -> "Giá phải lớn hơn 0"
            else                -> null
        }
        categoryError = if (selectedCategoryId.isBlank()) "Vui lòng chọn loại món" else null

        if (nameError == null && priceError == null && categoryError == null && parsedPrice != null) {
            onSave(trimmedName, parsedPrice, selectedCategoryId, rating, note.trim(), autoTimeType)
        }
    }

    // ── Delete confirmation dialog ─────────────────────────────────────────
    if (showDeleteDialog && existingItem != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title   = { Text("Xoá món này?") },
            text    = { Text("Món ăn sẽ bị xoá khỏi nhật ký của bạn.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete(existingItem.itemId)
                }) {
                    Text("Xoá", color = Color(0xFFE57373))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Huỷ") }
            }
        )
    }

    // ── Time picker dialog (TV3) ───────────────────────────────────────────
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour   = selectedHour,
            initialMinute = selectedMinute,
            is24Hour      = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedHour   = timePickerState.hour
                    selectedMinute = timePickerState.minute
                    showTimePicker = false
                }) { Text("Xong") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Hủy") }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TimePicker(state = timePickerState)
                }
            }
        )
    }

    // ── Manage categories bottom sheet (TV3) ──────────────────────────────
    if (showManageCategoriesSheet) {
        ManageCategoryBottomSheet(
            categories          = categories,
            onDismiss           = { showManageCategoriesSheet = false },
            onToggleVisibility  = onToggleCategoryVisibility,
            onDeleteCategory    = onDeleteCategory,
            onEditCategory      = { /* TODO Sprint 2 */ },
            onCreateNew         = { name, emoji ->
                onCreateCategory(name, emoji)
                showManageCategoriesSheet = false
            }
        )
    }

    // ── Main content ──────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MainBackground)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text(
                text     = if (existingItem == null) "Thêm món" else "Sửa món",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.align(Alignment.Center),
                color      = Color(0xFF333333)
            )
            IconButton(
                onClick  = onDismiss,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Đóng", tint = Color.Black)
            }
            if (existingItem != null && onDelete != null) {
                IconButton(
                    onClick  = { showDeleteDialog = true },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Xoá", tint = Color(0xFFE57373))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier
                    .size(140.dp)
                    .clickable { /* TODO Sprint 2: swap/remove image */ },
                shape          = RoundedCornerShape(70.dp),
                color          = Color(0xFFFFE9DD),
                shadowElevation = 4.dp
            ) {
                val imageSource = pendingImageUri ?: existingItem?.imageUrl

                if (imageSource != null) {
                    AsyncImage(
                        model              = imageSource,
                        contentDescription = "Ảnh món ăn",
                        modifier           = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(70.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text     = displayIcon,
                            fontSize = 64.sp,
                            modifier = Modifier.wrapContentSize()
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        FoodInputField(
            label         = "TÊN MÓN",
            value         = foodName,
            onValueChange = {
                foodName  = it
                nameError = null
            },
            placeholder = "VD: Phở Bò"
        )
        FieldError(nameError)

        FoodInputField(
            label         = "GIÁ",
            value         = price,
            onValueChange = { input ->
                val digitsOnly = input.filter { it.isDigit() }
                price      = digitsOnly
                priceError = null
            },
            placeholder  = "0",
            trailingText = "đ"
        )
        FieldError(priceError)

        CategorySelector(
            categories         = categories,
            selectedCategoryId = selectedCategoryId,
            onCategorySelected = {
                selectedCategoryId = it
                categoryError      = null
            },
            onManageClick = { showManageCategoriesSheet = true },
            onAddClick    = { showManageCategoriesSheet = true }
        )
        FieldError(categoryError)

        Spacer(Modifier.height(16.dp))

        TimeSelector(
            time        = displayTime,
            session     = sessionLabel,
            icon        = sessionIcon,
            onTimeClick = { showTimePicker = true }
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Đánh giá", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(4.dp))
                Text("⭐", fontSize = 12.sp)
            }
            StarRatingBar(
                rating         = rating,
                onRatingChange = { rating = it },
                modifier       = Modifier.width(180.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        FoodInputField(
            label         = "Ghi chú",
            labelIcon     = "📝",
            value         = note,
            onValueChange = { note = it },
            placeholder   = "VD: Món này ngon quá!",
            isSingleLine  = false
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick  = ::submit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape  = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA8D5BA))
        ) {
            Text(
                text       = if (existingItem == null) "Thêm món" else "Lưu thay đổi",
                fontSize   = 16.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun FieldError(error: String?) {
    if (error != null) {
        Text(
            text     = error,
            color    = Color(0xFFE57373),
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
    }
}

@Preview(showSystemUi = true, device = "spec:width=411dp,height=891dp")
@Composable
fun FoodEntryScreenPreview() {
    val previewCategories = listOf(
        DiaryCategory("preview-1", "Cơm",        "🍚", isSystem = true),
        DiaryCategory("preview-2", "Mì & Phở",   "🍜", isSystem = true),
        DiaryCategory("preview-3", "Bánh mì",    "🥖", isSystem = true),
        DiaryCategory("preview-4", "Đồ uống",    "🥤", isSystem = true),
        DiaryCategory("preview-5", "Tráng miệng","🍰", isSystem = true),
    )
    FoodTrackerTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MainBackground) {
            FoodEntryScreen(
                categories = previewCategories,
                onDismiss  = {},
                onSave     = { _, _, _, _, _, _ -> }
            )
        }
    }
}