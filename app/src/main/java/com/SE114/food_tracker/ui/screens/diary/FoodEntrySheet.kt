package com.SE114.food_tracker.ui.screens.diary

import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.data.local.entities.Category
import com.SE114.food_tracker.data.local.entities.Item
import com.SE114.food_tracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodEntrySheet(
    item: Item? = null, categories: List<Category>, onDismiss: () -> Unit, onSave: (Item) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.LightGray) }) {
        FoodEntryContent(item, categories, onDismiss, onSave)
    }
}

@Composable
fun FoodEntryContent(
    item: Item? = null, categories: List<Category>, onDismiss: () -> Unit, onSave: (Item) -> Unit
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var price by remember { mutableStateOf(item?.price?.toInt()?.toString() ?: "") }
    var selectedCategoryId by remember { mutableIntStateOf(item?.categoryId ?: 1) }
    var note by remember { mutableStateOf(item?.note ?: "") }
    var rating by remember { mutableIntStateOf(item?.rating ?: 0) }
    var timeType by remember { mutableIntStateOf(item?.timeType ?: 0) } // 0: Sáng, 1: Chiều

    val scrollState = rememberScrollState()

    // Map Icon cho Category
    val categoryIcons = mapOf(
        "Cơm" to Icons.Default.RiceBowl,
        "Mì & Phở" to Icons.Default.SoupKitchen,
        "Bánh mì" to Icons.Default.BakeryDining,
        "Đồ uống" to Icons.Default.LocalDrink,
        "Tráng miệng" to Icons.Default.Cake
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 20.dp)
            .verticalScroll(scrollState)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, bottom = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(32.dp)
                    .background(Color(0xFFF2F2F2), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.Black
                )
            }

            Text(
                text = if (item == null) "Thêm món ăn" else "Sửa món ăn",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        // Ảnh món ăn
        Box(
            modifier = Modifier
                .size(140.dp)
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 8.dp)
                .clip(CircleShape)
                .background(Color(0xFFFDE3D6)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Restaurant, null, modifier = Modifier.size(50.dp), tint = Orange)
        }

        // TÊN MÓN
        Text("TÊN MÓN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        CustomTextField(value = name, onValueChange = { name = it })

        // GIÁ
        Text(
            "GIÁ",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )
        CustomTextField(value = price, onValueChange = { price = it })

        // LOẠI
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("LOẠI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(
                "QUẢN LÝ",
                fontSize = 10.sp,
                color = Color(0xFFC98989),
                modifier = Modifier.clickable {})
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier
                .height(180.dp)
                .padding(vertical = 8.dp)
        ) {
            items(categories) { category ->
                CategorySquareSelector(
                    name = category.name,
                    icon = categoryIcons[category.name] ?: Icons.Default.Fastfood,
                    isSelected = category.categoryId == selectedCategoryId,
                    onClick = { selectedCategoryId = category.categoryId })
            }
            item {
                CategorySquareSelector(
                    name = "Thêm", icon = Icons.Default.Add, isSelected = false, onClick = {})
            }
        }

        // THỜI GIAN
        Text(
            "THỜI GIAN",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TimeChip(label = "15:20", isAction = true) {
                /* TODO: Mở TimePickerDialog ở đây */
            }
            TimeChip(label = "Chiều", isAction = false, hasIcon = true) {
                /* Logic đổi thủ công hoặc để trống */
            }
        }

        // ĐÁNH GIÁ
        Row(
            verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)
        ) {
            Text("Đánh giá", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Icon(
                Icons.Default.Star,
                null,
                tint = Orange,
                modifier = Modifier
                    .size(16.dp)
                    .padding(start = 4.dp)
            )
            Spacer(Modifier.weight(1f))
            RatingBarWithSpacing(rating = rating, onRatingChange = { rating = it })
        }

        // GHI CHÚ
        Row(
            verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)
        ) {
            Text("Ghi chú", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Icon(
                Icons.Default.CalendarToday,
                null,
                tint = Color.Gray,
                modifier = Modifier
                    .size(14.dp)
                    .padding(start = 6.dp)
            )
        }

        TextField(
            value = note,
            onValueChange = { note = it },
            placeholder = { Text("VD: Món này ngon quá!", fontSize = 12.sp, color = Color.Gray) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            minLines = 1,
            maxLines = 3,
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        // NÚT LƯU
        // NÚT LƯU / THÊM MÓN
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = { /* onSave */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA9CFB8).copy(alpha = 0.8f)),
            shape = RoundedCornerShape(25.dp)
        ) {
            Text(
                // Nếu item null (chưa có món) thì hiện "Thêm món", ngược lại hiện "Lưu thay đổi"
                text = if (item == null) "Thêm món" else "Lưu thay đổi",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    modifier: Modifier = Modifier
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            if (placeholder.isNotEmpty()) Text(
                placeholder, fontSize = 12.sp, color = Color.Gray
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}

@Composable
fun TimeChip(
    label: String, isAction: Boolean = false, hasIcon: Boolean = false, onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        color = if (isAction) Color.White else Color(0xFFFFE9DD),
        shape = RoundedCornerShape(12.dp),
        border = if (!isAction) BorderStroke(0.5.dp, Color(0xFFFDE3D6)) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasIcon) {
                Icon(
                    Icons.Default.CloudQueue,
                    null,
                    modifier = Modifier
                        .size(16.dp)
                        .padding(end = 4.dp),
                    tint = Orange
                )
            }
            Text(
                text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black
            )
        }
    }
}

@Composable
fun CategorySquareSelector(
    name: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(4.dp)
            .clickable { onClick() }) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isSelected) Color(0xFFFFE9DD) else Color.White)
                .border(
                    width = if (isSelected) 1.dp else 0.dp,
                    color = if (isSelected) Color(0xFFC98989) else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                ), contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                null,
                modifier = Modifier.size(26.dp),
                tint = if (isSelected) Orange else Color.Gray
            )
        }
        Text(name, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun RatingBarWithSpacing(rating: Int, onRatingChange: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (i in 1..5) {
            Icon(
                if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                null,
                modifier = Modifier
                    .size(28.dp)
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
        Category(1, "Cơm", ""),
        Category(2, "Mì & Phở", ""),
        Category(3, "Bánh mì", ""),
        Category(4, "Đồ uống", ""),
        Category(5, "Tráng miệng", "")
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFCDFCF).copy(alpha = 0.4f))
    ) {
        FoodEntryContent(item = null, categories = mockCategories, onDismiss = {}, onSave = {})
    }
}