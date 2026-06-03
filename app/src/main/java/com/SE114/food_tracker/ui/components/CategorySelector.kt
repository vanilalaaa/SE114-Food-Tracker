package com.SE114.food_tracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.data.local.entities.Category
import com.SE114.food_tracker.core.designsystem.theme.FoodTrackerTheme

@Composable
fun CategorySelector(
    categories: List<Category>,
    selectedCategoryId: String,
    onCategorySelected: (String) -> Unit,
    onManageClick: () -> Unit = {},
    onAddClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Dữ liệu mặc định khớp với Model: categoryId, name, iconUrl, isHidden, isSystem
    val defaultCategories = listOf(
        Category("1", "Cơm", "🍚", isHidden = false, isSystem = true),
        Category("2", "Mì & Phở", "🍜", isHidden = false, isSystem = true),
        Category("3", "Bánh mì", "🥖", isHidden = false, isSystem = true),
        Category("4", "Đồ uống", "🥤", isHidden = false, isSystem = true),
        Category("5", "Tráng miệng", "🍰", isHidden = false, isSystem = true),
        Category("6", "Ăn vặt", "🍡", isHidden = false, isSystem = true),
        Category("7", "Hải sản", "🦪", isHidden = false, isSystem = true),
        Category("8", "Thịt", "🥩", isHidden = false, isSystem = true),
        Category("9", "Đồ dùng", "🍴", isHidden = false, isSystem = true)
    )

    // Gộp danh sách mặc định và danh sách người dùng tự thêm
    val displayList = (defaultCategories + categories).distinctBy { it.name }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "LOẠI",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = "QUẢN LÝ",
                fontSize = 11.sp,
                color = Color(0xFFC98989),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { onManageClick() }
            )
        }

        Spacer(Modifier.height(8.dp))

        // Hiển thị lưới 5 cột chuẩn Figma
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier.heightIn(max = 250.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(displayList) { category ->
                CategorySquareItem(
                    category = category,
                    isSelected = category.categoryId == selectedCategoryId,
                    onClick = { onCategorySelected(category.categoryId) }
                )
            }

            // Nút "Thêm" thủ công luôn nằm cuối lưới
            item {
                CategorySquareItem(
                    category = Category("0", "Thêm", "➕", isHidden = false, isSystem = true),
                    isSelected = false,
                    onClick = onAddClick
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF5E4)
@Composable
fun CategorySelectorPreview() {
    FoodTrackerTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            CategorySelector(
                categories = emptyList(),
                selectedCategoryId = "2",
                onCategorySelected = {}
            )
        }
    }
}