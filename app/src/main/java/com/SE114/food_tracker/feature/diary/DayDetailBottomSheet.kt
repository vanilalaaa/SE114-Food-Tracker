package com.SE114.food_tracker.feature.diary

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.core.designsystem.components.ConfirmDialog
import com.SE114.food_tracker.core.designsystem.theme.AppTypography
import com.SE114.food_tracker.core.designsystem.theme.FoodTrackerTheme
import com.SE114.food_tracker.feature.diary.components.DayItem
import com.SE114.food_tracker.feature.diary.components.PrimaryButton
import com.SE114.food_tracker.feature.diary.components.StatBox
import com.SE114.food_tracker.feature.diary.components.getEmojiByName
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import com.SE114.food_tracker.core.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailBottomSheet(
    onDismiss: () -> Unit,
    selectedDate: LocalDate,
    items: List<DiaryItem>,
    categories: List<DiaryCategory>,
    onEditItem: (DiaryItem) -> Unit,
    onAddItem: () -> Unit,
    onDeleteItem: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = Color(0xFFCCCCCC),
                width = 40.dp,
                height = 4.dp
            )
        }
    ) {
        DayDetailBottomSheetContent(
            selectedDate = selectedDate,
            items = items,
            categories = categories,
            onEditItemClick = onEditItem,
            onAddItem = onAddItem,
            onDeleteItem = onDeleteItem
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailBottomSheetContent(
    selectedDate: LocalDate,
    items: List<DiaryItem>,
    categories: List<DiaryCategory>,
    onEditItemClick: (DiaryItem) -> Unit,
    onAddItem: () -> Unit,
    onDeleteItem: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    val totalEntry = items.sumOf { it.price }
    val displayTotal = LocalCurrencyDisplay.current.formatShort(totalEntry)

    val dayOfWeekLabel = when (selectedDate.dayOfWeek.ordinal) {
        0 -> "Thứ Hai"
        1 -> "Thứ Ba"
        2 -> "Thứ Tư"
        3 -> "Thứ Năm"
        4 -> "Thứ Sáu"
        5 -> "Thứ Bảy"
        else -> "Chủ Nhật"
    }
    val dateLabel = "$dayOfWeekLabel, ${selectedDate.dayOfMonth} tháng ${selectedDate.monthNumber}, ${selectedDate.year}"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(scrollState)
    ) {
        Text(dateLabel, style = AppTypography.titleLarge.copy(fontSize = 22.sp))
        Text("${items.size} entries", style = AppTypography.labelMedium)

        Spacer(Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatBox(label = "Món", value = "${items.size}", modifier = Modifier.weight(1f))
            StatBox(label = "Chi", value = displayTotal, modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(24.dp))

        PrimaryButton(
            text = "Thêm món",
            onClick = onAddItem
        )

        Spacer(Modifier.height(28.dp))
        Text("Danh sách", style = AppTypography.titleSmall)
        Text(
            "Swipe left to reveal delete. Tap to edit.",
            style = AppTypography.labelMedium,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 8.dp)
        )

        Spacer(Modifier.height(12.dp))

        // Single dialog anchor — one dialog for all rows
        var itemToDelete by remember { mutableStateOf<DiaryItem?>(null) }

        itemToDelete?.let { target ->
            ConfirmDialog(
                title        = "Xóa món ăn?",
                body         = "Bạn có chắc chắn muốn xóa món ăn này không? Hành động này không thể hoàn tác.",
                confirmLabel = "Xóa",
                cancelLabel  = "Huỷ",
                destructive  = true,
                onConfirm    = {
                    onDeleteItem(target.itemId)
                    itemToDelete = null
                },
                onDismiss    = { itemToDelete = null }
            )
        }

        items.forEach { item ->
            key(item.itemId) {
                SwipeToRevealDeleteRow(
                    isDialogOpen = itemToDelete != null,
                    onDeleteRequest = { itemToDelete = item }
                ) {
                    val matchedCategory = categories.find { it.categoryId == item.categoryId }
                    val catName = matchedCategory?.name ?: item.categoryName
                    val catIcon = matchedCategory?.iconUrl ?: getEmojiByName(catName)

                    DayItem(
                        name         = item.name,
                        category     = catName,
                        categoryIcon = catIcon,
                        imageUrl     = item.imageUrl,
                        price        = item.price,
                        createdAt    = item.createdAt,
                        timeLabel    = item.timeLabel,
                        onClick      = { onEditItemClick(item) }
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun SwipeToRevealDeleteRow(
    isDialogOpen: Boolean,
    onDeleteRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    val density       = LocalDensity.current
    val revealWidthDp = 80.dp
    val revealWidthPx = with(density) { revealWidthDp.toPx() }

    // Animated horizontal offset of the foreground card (always <= 0)
    val offsetX = remember { Animatable(0f) }
    val scope   = rememberCoroutineScope()

    LaunchedEffect(isDialogOpen) {
        if (!isDialogOpen && offsetX.value != 0f) {
            offsetX.animateTo(
                targetValue = 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            )
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        // ── Background: fixed-width red panel anchored to the right ──────────
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(width = revealWidthDp, height = 72.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFE57373), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onDeleteRequest) {
                    Icon(
                        imageVector        = Icons.Default.Delete,
                        contentDescription = "Xóa",
                        tint               = Color.White,
                        modifier           = Modifier.size(28.dp)
                    )
                }
            }
        }

        // ── Foreground: the actual item card, draggable ───────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(isDialogOpen) {
                    if (isDialogOpen) return@pointerInput

                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                val target = if (offsetX.value < -(revealWidthPx / 2f)) {
                                    -revealWidthPx
                                } else {
                                    0f
                                }
                                offsetX.animateTo(
                                    targetValue   = target,
                                    animationSpec = spring(stiffness = Spring.StiffnessMedium)
                                )
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                offsetX.animateTo(
                                    targetValue   = 0f,
                                    animationSpec = spring(stiffness = Spring.StiffnessMedium)
                                )
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            val newValue = (offsetX.value + dragAmount).coerceIn(-revealWidthPx, 0f)
                            scope.launch { offsetX.snapTo(newValue) }

                            change.consume()
                        }
                    )
                }
        ) {
            content()
        }
    }
}

private fun Int.toTimeLabel(): String = when (this) {
    0    -> "Sáng"
    1    -> "Trưa/Chiều"
    2    -> "Tối"
    else -> "Khác"
}

@Preview(showBackground = true)
@Composable
fun DayDetailBottomSheetPreview() {
    FoodTrackerTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
        ) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = Color.White
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .size(40.dp, 4.dp)
                            .background(Color(0xFFCCCCCC), RoundedCornerShape(2.dp))
                    )

                    DayDetailBottomSheetContent(
                        selectedDate = LocalDate(2026, 6, 6),
                        items = emptyList(),
                        categories = emptyList(),
                        onEditItemClick = {},
                        onAddItem = {},
                        onDeleteItem = {}
                    )
                }
            }
        }
    }
}