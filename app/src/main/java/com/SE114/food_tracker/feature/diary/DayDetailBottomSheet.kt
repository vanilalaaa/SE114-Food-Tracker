package com.SE114.food_tracker.feature.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.ui.theme.*
import com.SE114.food_tracker.ui.components.*
import com.SE114.food_tracker.data.local.entities.Item
import com.SE114.food_tracker.data.local.entities.Category

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailBottomSheet(
    onDismiss: () -> Unit,
    items: List<Item>,
    categories: List<Category>,
    onEditItem: (Item) -> Unit
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
        DayDetailBottomSheetContent(items, categories, onEditItem)
    }
}

@Composable
fun DayDetailBottomSheetContent(
    items: List<Item>,
    categories: List<Category>,
    onEditItemClick: (Item) -> Unit
) {
    val scrollState = rememberScrollState()
    val totalEntry = items.sumOf { it.price }.toInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(scrollState)
    ) {
        Text("03/04/2026", style = AppTypography.titleLarge.copy(fontSize = 22.sp))
        Text("Thứ Sáu", style = AppTypography.labelMedium)

        Spacer(Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatBox(label = "Món", value = "${items.size}", modifier = Modifier.weight(1f))
            StatBox(label = "Chi", value = "${totalEntry / 1000}k đ", modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(24.dp))

        PrimaryButton(
            text = "Thêm món",
            onClick = { /* Mở màn hình FoodEntryScreen */ }
        )

        Spacer(Modifier.height(28.dp))
        Text("Danh sách", style = AppTypography.titleSmall)
        Text(
            "← Vuốt để xóa . Nhấn để sửa",
            style = AppTypography.labelMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 8.dp)
        )

        Spacer(Modifier.height(12.dp))

        items.forEach { item ->
            val catName = categories.find { it.categoryId == item.categoryId }?.name ?: "Khác"
            DayItem(
                name = item.name,
                category = catName,
                price = item.price,
                time = if (item.timeType == 1) "Chiều" else "Sáng",
                onClick = { onEditItemClick(item) }
            )
            Spacer(Modifier.height(12.dp))
        }
    }
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
                        items = DiaryMockData.items,
                        categories = DiaryMockData.categories,
                        onEditItemClick = {}
                    )
                }
            }
        }
    }
}