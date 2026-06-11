package com.SE114.food_tracker.feature.diary.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.core.designsystem.theme.FoodTrackerTheme
import com.SE114.food_tracker.feature.diary.DiaryCategory
import com.SE114.food_tracker.core.designsystem.theme.StatRed
import com.SE114.food_tracker.core.designsystem.theme.LightPeach

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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LOẠI",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            // GAP: "QUẢN LÝ" now calls onManageClick
            Text(
                text = "QUẢN LÝ",
                fontSize = 11.sp,
                color = StatRed,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { onManageClick() }
            )
        }

        Spacer(Modifier.height(12.dp))

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
                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = categories,
                    key = { it.categoryId }
                ) { category ->
                    val isSelected = category.categoryId == selectedCategoryId

                    CategoryCard(
                        name = category.name,
                        icon = category.iconUrl ?: "🍽️",
                        isSelected = isSelected,
                        onClick = { onCategorySelected(category.categoryId) }
                    )
                }

                item {
                    CategoryCard(
                        name = "Thêm",
                        icon = "+",
                        isSelected = false,
                        isAddButton = true,
                        onClick = onAddClick
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryCard(
    name: String,
    icon: String,
    isSelected: Boolean,
    isAddButton: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = if (isSelected) LightPeach else Color.White,
        shadowElevation = 3.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(4.dp)
        ) {
            if (isAddButton) {
                Text(
                    text = icon,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.DarkGray
                )
            } else {
                Text(text = icon, fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = name,
                fontSize = 10.sp,
                color = Color.DarkGray,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF5E4)
@Composable
fun CategorySelectorPreview() {
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