package com.SE114.food_tracker.feature.diary.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.feature.diary.DiaryCategory

@Composable
fun CategorySquareItem(
    category: DiaryCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(4.dp)
            .clickable { onClick() }
    ) {
        Surface(
            modifier = Modifier.size(60.dp),
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) Color(0xFFFFE9DD) else Color.White,
            shadowElevation = 2.dp,
            border = if (isSelected) BorderStroke(1.dp, Color(0xFFC98989)) else null
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = category.iconUrl.ifEmpty { getEmojiByName(category.name) },
                    fontSize = 26.sp
                )
            }
        }
        Text(
            text = category.name,
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 4.dp),
            color = Color.Black
        )
    }
}

@Composable
fun CategoryRowItem(
    category: DiaryCategory,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onVisibilityToggle: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = category.iconUrl.ifEmpty { getEmojiByName(category.name) },
                fontSize = 22.sp
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = category.name,
                modifier = Modifier.weight(1f),
                fontSize = 14.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!category.isSystem) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, null, tint = Color(0xFFFFD54F), modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, null, tint = Color(0xFFE57373), modifier = Modifier.size(20.dp))
                    }
                }
                IconButton(onClick = onVisibilityToggle) {
                    Icon(
                        imageVector = if (category.isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

fun getEmojiByName(name: String): String =
    when (name) {
        "Cơm" -> "🍚"
        "Mì & Phở" -> "🍜"
        "Bánh mì" -> "🥖"
        "Đồ uống" -> "🥤"
        "Tráng miệng" -> "🍰"
        "Ăn vặt" -> "🍡"
        "Hải sản" -> "🦪"
        "Thịt" -> "🥩"
        "Đồ dùng" -> "🍴"
        else -> "🍱"
    }
