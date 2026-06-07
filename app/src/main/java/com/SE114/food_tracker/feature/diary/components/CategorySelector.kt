package com.SE114.food_tracker.feature.diary.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.core.designsystem.theme.FoodTrackerTheme
import com.SE114.food_tracker.feature.diary.DiaryCategory

@Composable
fun CategorySelector(
    categories: List<DiaryCategory>,
    selectedCategoryId: String,
    onCategorySelected: (String) -> Unit,
    onManageClick: () -> Unit = {},
    onAddClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
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
            // GAP: "QUẢN LÝ" now calls onManageClick, which DiaryScreen routes to a
            // category management bottom sheet. The sheet UI is TODO Sprint 2 —
            // CategoryRowItem in CategoryItem.kt already has the row design ready.
            Text(
                text = "QUẢN LÝ",
                fontSize = 11.sp,
                color = Color(0xFFC98989),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { onManageClick() }
            )
        }

        Spacer(Modifier.height(8.dp))

        if (categories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = Color(0xFFC98989),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = "Đang tải danh mục...",
                        fontSize = 12.sp,
                        color = Color(0xFF999999)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.heightIn(max = 250.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    //items = categories,
                    items = categories,
                    key = { it.categoryId }
                ) { category ->
                    CategorySquareItem(
                        category = category,
                        isSelected = category.categoryId == selectedCategoryId,
                        onClick = { onCategorySelected(category.categoryId) }
                    )
                }

                // GAP: onAddClick routes to onManageCategories in FoodEntryScreen,
                // which will open the add-category form. TODO Sprint 2.
                item {
                    CategorySquareItem(
                        category = DiaryCategory(
                            categoryId = "__add__",
                            name = "Thêm",
                            iconUrl = "➕",
                            isHidden = false,
                            isSystem = true
                        ),
                        isSelected = false,
                        onClick = onAddClick
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF5E4)
@Composable
fun CategorySelectorPreview() {
    // Hardcoded preview categories use "preview-N" IDs to clearly signal they are
    // NOT real UUIDs. This block is compile-time only and has zero effect at runtime.
    val previewCategories = listOf(
        DiaryCategory("sys-cat-1", "Cơm",        "🍚", isSystem = true),
        DiaryCategory("sys-cat-2", "Mì & Phở",   "🍜", isSystem = true),
        DiaryCategory("sys-cat-3", "Bánh mì",    "🥖", isSystem = true),
        DiaryCategory("sys-cat-4", "Đồ uống",    "🥤", isSystem = true),
        DiaryCategory("sys-cat-5", "Tráng miệng","🍰", isSystem = true),
        DiaryCategory("sys-cat-6", "Ăn vặt",     "🍡", isSystem = true)
    )
    FoodTrackerTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            CategorySelector(
                categories = previewCategories,
                selectedCategoryId = "sys-cat-2",
                onCategorySelected = {}
            )
        }
    }
}