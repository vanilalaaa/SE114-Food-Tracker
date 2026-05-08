package com.SE114.food_tracker.ui.screens.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.data.local.entities.Category
import com.SE114.food_tracker.data.local.entities.Item
import com.SE114.food_tracker.ui.theme.*

@Composable
fun FoodEntrySheet(
    item: Item? = null, // Nếu truyền item vào là Sửa, không truyền là Thêm mới
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (Item) -> Unit
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var price by remember { mutableStateOf(item?.price?.toInt()?.toString() ?: "") }
    var selectedCategoryId by remember {
        mutableIntStateOf(
            item?.categoryId ?: categories.firstOrNull()?.categoryId ?: 1
        )
    }
    var note by remember { mutableStateOf(item?.note ?: "") }
    var rating by remember { mutableIntStateOf(item?.rating ?: 0) }
    var timeType by remember { mutableIntStateOf(item?.timeType ?: 0) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = null)
                }
                Text(
                    "Sửa món ăn",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(150.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 16.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFDE3D6)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Restaurant,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = Orange
                )
            }

            Text("TÊN MÓN", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFFF2F2F2),
                    focusedContainerColor = Color(0xFFF2F2F2)
                )
            )

            Text("GIÁ", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFFF2F2F2),
                    focusedContainerColor = Color(0xFFF2F2F2)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("LOẠI", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                Text(
                    "QUẢN LÝ",
                    fontSize = 10.sp,
                    color = Color(0xFFC98989),
                    modifier = Modifier.clickable { })
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier
                    .height(140.dp)
                    .padding(vertical = 8.dp)
            ) {
                items(categories) { category ->
                    CategoryItemSelector(
                        category = category,
                        isSelected = category.categoryId == selectedCategoryId,
                        onClick = { selectedCategoryId = category.categoryId }
                    )
                }
            }

            Text("THỜI GIAN", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimeChip("15-20", timeType == 0) { timeType = 0 }
                TimeChip("Chiều", timeType == 1) { timeType = 1 }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Đánh giá", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(16.dp))
                RatingBar(rating = rating, onRatingChange = { rating = it })
            }

            Text(
                "Ghi chú",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text("VD: Món này ngon quá!") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { /* onSave logic */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MintGreen),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    "Lưu thay đổi",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CategoryItemSelector(category: Category, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(4.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (isSelected) Orange.copy(alpha = 0.2f) else Color(0xFFF2F2F2),
                    CircleShape
                )
                .border(
                    if (isSelected) 1.dp else 0.dp,
                    if (isSelected) Orange else Color.Transparent,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Icon mẫu
            Icon(
                Icons.Default.Fastfood,
                null,
                modifier = Modifier.size(20.dp),
                tint = if (isSelected) Orange else Color.Gray
            )
        }
        Text(category.name, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun TimeChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        color = if (isSelected) Color.LightGray else Color(0xFFF1F1F1),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            label,
            color = if (isSelected) Color.Black else Color.Black,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun RatingBar(rating: Int, onRatingChange: (Int) -> Unit) {
    Row {
        for (i in 1..5) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onRatingChange(i) },
                tint = if (i <= rating) Color(0xFFFFD700) else Color.LightGray
            )
        }
    }
}
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun FoodEntrySheetPreview() {
    val mockCategories = listOf(
        Category(categoryId = 1, name = "Cơm", iconUrl = ""),
        Category(categoryId = 2, name = "Mì & Phở", iconUrl = ""),
        Category(categoryId = 3, name = "Đồ uống", iconUrl = ""),
        Category(categoryId = 4, name = "Tráng miệng", iconUrl = ""),
        Category(categoryId = 5, name = "Ăn vặt", iconUrl = "")
    )
    FoodEntrySheet(
        item = null,
        categories = mockCategories,
        onDismiss = {},
        onSave = {}
    )
}