package com.SE114.food_tracker.feature.diary.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.emoji2.emojipicker.EmojiPickerView
import com.SE114.food_tracker.feature.diary.DiaryCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoryBottomSheet(
    categories: List<DiaryCategory>,
    onDismiss: () -> Unit,
    onToggleVisibility: (DiaryCategory) -> Unit,
    onDeleteCategory: (DiaryCategory) -> Unit,
    onEditCategory: (category: DiaryCategory, newName: String, newIconUrl: String) -> Unit,
    onCreateNew: (name: String, emoji: String) -> Unit,
    deleteError: String? = null,
    onClearDeleteError: () -> Unit = {}
) {
    val systemCategories = categories.filter { it.isSystem }
    val customCategories = categories.filter { !it.isSystem }

    var showCreateSheet by remember { mutableStateOf(false) }

    // Category staged for the edit sheet
    var pendingEditCategory by remember { mutableStateOf<DiaryCategory?>(null) }

    // Category staged for the delete confirmation dialog
    var pendingDeleteCategory by remember { mutableStateOf<DiaryCategory?>(null) }

    // ── Edit bottom sheet ──────────────────────────────────────────────────
    pendingEditCategory?.let { target ->
        CategoryFormBottomSheet(
            initialCategory = target,
            onDismiss = { pendingEditCategory = null },
            onSave = { newName, newIconUrl ->
                onEditCategory(target, newName, newIconUrl)
                pendingEditCategory = null
            }
        )
    }

    // ── Delete confirmation dialog ─────────────────────────────────────────
    pendingDeleteCategory?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDeleteCategory = null },
            shape            = RoundedCornerShape(20.dp),
            containerColor   = Color.White,
            title = {
                Text("Xóa danh mục?", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Text(
                    "Danh mục \"${target.name}\" sẽ bị xóa vĩnh viễn. " +
                            "Thao tác này không thể hoàn tác.",
                    fontSize = 14.sp,
                    color = Color(0xFF555555)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteCategory(target)
                    pendingDeleteCategory = null
                }) {
                    Text("Xóa", color = Color(0xFFE57373), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteCategory = null }) {
                    Text("Hủy", color = Color.Gray)
                }
            }
        )
    }

    // ── Delete / RESTRICT error dialog ────────────────────────────────────
    deleteError?.let { msg ->
        AlertDialog(
            onDismissRequest = onClearDeleteError,
            shape            = RoundedCornerShape(20.dp),
            containerColor   = Color.White,
            title = {
                Text("Không thể thực hiện", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Text(msg, fontSize = 14.sp, color = Color(0xFF555555))
            },
            confirmButton = {
                TextButton(onClick = onClearDeleteError) {
                    Text("Đã hiểu", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = Color.White,
        dragHandle       = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "Quản lý loại",
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = Color.Black
                )
                IconButton(
                    onClick  = onDismiss,
                    modifier = Modifier
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                        .size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Đóng",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Category list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                if (systemCategories.isNotEmpty()) {
                    item {
                        Text(
                            "MẶC ĐỊNH",
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.Gray,
                            modifier   = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(systemCategories) { category ->
                        CategoryItemRow(
                            category          = category,
                            showEditActions   = false,
                            onToggleVisibility = { onToggleVisibility(category) }
                        )
                    }
                }

                if (customCategories.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "TỰ TẠO",
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.Gray,
                            modifier   = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(customCategories) { category ->
                        CategoryItemRow(
                            category          = category,
                            showEditActions   = true,
                            onEdit            = { pendingEditCategory = category },
                            // Confirmation first; ViewModel runs the RESTRICT check on confirm.
                            onDelete          = { pendingDeleteCategory = category },
                            onToggleVisibility = { onToggleVisibility(category) }
                        )
                    }
                }
            }

            // Create button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Button(
                    onClick  = { showCreateSheet = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape  = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE89A7A))
                ) {
                    Text(
                        "+ TẠO LOẠI MỚI",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                }
            }
        }
    }

    if (showCreateSheet) {
        CategoryFormBottomSheet(
            onDismiss = { showCreateSheet = false },
            onSave = { name, emoji ->
                onCreateNew(name, emoji)
                showCreateSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFormBottomSheet(
    initialCategory: DiaryCategory? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, emoji: String) -> Unit
) {
    val isEditing = initialCategory != null
    var categoryName by remember(initialCategory?.categoryId) {
        mutableStateOf(initialCategory?.name ?: "")
    }
    var selectedEmoji by remember(initialCategory?.categoryId) {
        mutableStateOf(initialCategory?.iconUrl ?: "🍽️")
    }
    var showEmojiPicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = Color.White,
        dragHandle       = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    if (isEditing) "Chỉnh sửa loại" else "Tạo loại mới",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick  = onDismiss,
                    modifier = Modifier.background(Color(0xFFF5F5F5), CircleShape).size(32.dp)
                ) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(16.dp))
                            .clickable { showEmojiPicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(selectedEmoji, fontSize = 28.sp)
                    }
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(Color.White, CircleShape)
                            .padding(2.dp)
                            .background(
                                if (isEditing) Color(0xFFE89A7A) else Color(0xFFE57373),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(10.dp))
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                BasicTextField(
                    value         = categoryName,
                    onValueChange = { categoryName = it },
                    textStyle     = TextStyle(fontSize = 16.sp, color = Color.Black),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .background(Color(0xFFF5F5F5), RoundedCornerShape(16.dp))
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (categoryName.isEmpty()) {
                                Text("Tên loại", color = Color.LightGray, fontSize = 16.sp)
                            }
                            innerTextField()
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick  = { onSave(categoryName.trim(), selectedEmoji) },
                enabled  = categoryName.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape  = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor         = Color(0xFFE89A7A),
                    disabledContainerColor = Color(0xFFE0E0E0)
                )
            ) {
                Text(
                    if (isEditing) "Lưu thay đổi" else "Tạo loại",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color      = if (categoryName.isNotBlank()) Color.White else Color.Gray
                )
            }
        }
    }

    if (showEmojiPicker) {
        EmojiPickerBottomSheet(
            onDismiss       = { showEmojiPicker = false },
            onEmojiSelected = { selectedEmoji = it; showEmojiPicker = false }
        )
    }
}

@Composable
fun EmojiPickerBottomSheet(
    onDismiss:       () -> Unit,
    onEmojiSelected: (String) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(24.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text("Chọn emoji", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton(
                        onClick  = onDismiss,
                        modifier = Modifier
                            .background(Color(0xFFF5F5F5), CircleShape)
                            .size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                    }
                }
                AndroidView(
                    factory  = { context: android.content.Context ->
                        EmojiPickerView(context).apply {
                            setOnEmojiPickedListener { emojiItem ->
                                onEmojiSelected(emojiItem.emoji)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

// ── Category row ────────────────────────────────────────────────────────────
@Composable
private fun CategoryItemRow(
    category:          DiaryCategory,
    showEditActions:   Boolean = false,
    onEdit:            () -> Unit = {},
    onDelete:          () -> Unit = {},
    onToggleVisibility: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF9F9F9))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = category.iconUrl, fontSize = 24.sp)
        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text     = category.name,
            fontSize = 16.sp,
            color    = if (category.isHidden) Color.Gray else Color.Black,
            modifier = Modifier.weight(1f)
        )

        if (showEditActions) {
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = "Sửa",
                    tint     = Color(0xFFE6C229),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Xóa",
                    tint     = Color(0xFFE57373),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        IconButton(onClick = onToggleVisibility, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector        = if (category.isHidden) Icons.Outlined.VisibilityOff
                else Icons.Outlined.Visibility,
                contentDescription = "Ẩn/Hiện",
                tint               = if (category.isHidden) Color.LightGray else Color.Gray,
                modifier           = Modifier.size(20.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Preview_ManageCategoryBottomSheet() {
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize().background(Color.Gray)) {
            ManageCategoryBottomSheet(
                categories = listOf(
                    DiaryCategory("1", "Bánh mì",     "🥖", isSystem = true,  isHidden = false),
                    DiaryCategory("2", "Cơm",         "🍚", isSystem = true,  isHidden = false),
                    DiaryCategory("4", "Tráng miệng", "🍰", isSystem = true,  isHidden = true),
                    DiaryCategory("5", "Ăn vặt",      "🍡", isSystem = false, isHidden = false)
                ),
                onDismiss          = {},
                onToggleVisibility = {},
                onDeleteCategory   = {},
                onEditCategory     = { _, _, _ -> },
                onCreateNew        = { _, _ -> }
            )
        }
    }
}