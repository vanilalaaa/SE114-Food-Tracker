package com.SE114.food_tracker.ui.screens.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.data.local.entities.Category
import com.SE114.food_tracker.data.local.entities.Item
import com.SE114.food_tracker.ui.theme.MintGreen
import com.SE114.food_tracker.ui.theme.Orange
import androidx.compose.foundation.clickable

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
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.LightGray) }
    ) {
        DayDetailBottomSheetContent(
            items = items,
            categories = categories,
            onEditItemClick = onEditItem
        )
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier) {
    Surface(
        modifier = modifier,
        color = Color(0xFFFDE3D6).copy(alpha = 0.7f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC98989))
            Text(label, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun FoodItemCard(item: Item, categoryName: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFFFDE3D6), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Sau này load ảnh từ item.imagePath ở đây
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(categoryName, fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.width(6.dp))
                    val session = if (item.timeType == 1) "Chiều" else "Sáng"
                    Surface(
                        color = Color.LightGray.copy(alpha = 0.3f), shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            session,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
            Text(
                "${(item.price / 1000).toInt()}k đ",
                color = Color(0xFFBE744D),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DayDetailBottomSheetContent(items: List<Item>, categories: List<Category>, onEditItemClick: (Item) -> Unit) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(scrollState)
    ) {
        Text("03/04/2026", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Thứ Sáu", fontSize = 14.sp, color = Color.Gray)
        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(label = "Món", value = "${items.size}", modifier = Modifier.weight(1f))
            val totalEntry = items.sumOf { it.price }.toInt()
            StatCard(
                label = "Chi", value = "${totalEntry / 1000}k đ", modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA9CFB8)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Thêm món", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(24.dp))
        Text("Danh sách", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("← Vuốt để xóa . Nhấn để sửa", fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(12.dp))

        items.forEach { item ->
            val categoryName = categories.find { it.categoryId == item.categoryId }?.name ?: "Khác"
            FoodItemCard(
                item = item,
                categoryName = categoryName,
                onClick = { onEditItemClick(item) }
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun DayDetailPreview() {

    ModalBottomSheet(onDismissRequest = {}, dragHandle = { BottomSheetDefaults.DragHandle() }) {
        DayDetailBottomSheetContent(
            items = DiaryMockData.items, categories = DiaryMockData.categories, onEditItemClick = {}
        )
    }
}