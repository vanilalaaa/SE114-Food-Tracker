package com.SE114.food_tracker.feature.chat.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.SE114.food_tracker.core.designsystem.theme.StatPinkDark

// Thanh cuộn dành cho Column (Màn Quản trị nhóm)
fun Modifier.columnVerticalScrollbar(
    scrollState: ScrollState,
    width: Dp = 4.dp,
    color: Color = Color(0xFFE91E63).copy(alpha = 0.7f)
): Modifier = drawWithContent {
    drawContent()
    val viewportHeight = size.height
    if (scrollState.maxValue > 0) {
        val totalHeight = scrollState.maxValue + viewportHeight
        val thumbHeight = (viewportHeight / totalHeight) * viewportHeight
        val thumbOffset = (scrollState.value.toFloat() / totalHeight) * viewportHeight

        // Vẽ đường rãnh mờ phía sau để định vị khu vực cuộn
        drawRoundRect(
            color = color.copy(alpha = 0.1f),
            topLeft = Offset(size.width - width.toPx(), 0f),
            size = Size(width.toPx(), viewportHeight),
            cornerRadius = CornerRadius(width.toPx() / 2)
        )

        // Vẽ cục cuộn chính
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width - width.toPx(), thumbOffset),
            size = Size(width.toPx(), thumbHeight),
            cornerRadius = CornerRadius(width.toPx() / 2)
        )
    }
}

// Thanh cuộn dành cho LazyColumn (Màn Tạo nhóm mới)

fun Modifier.lazyVerticalScrollbar(
    lazyListState: LazyListState,
    width: Dp = 4.dp,
    color: Color = StatPinkDark
): Modifier = drawWithContent {
    drawContent()

    val layoutInfo = lazyListState.layoutInfo
    val totalItemsCount = layoutInfo.totalItemsCount
    val viewportHeight = size.height

    if (totalItemsCount > 0 && layoutInfo.visibleItemsInfo.isNotEmpty()) {

        drawRoundRect(
            color = color.copy(alpha = 0.1f),
            topLeft = Offset(size.width - width.toPx(), 0f),
            size = Size(width.toPx(), viewportHeight),
            cornerRadius = CornerRadius(width.toPx() / 2)
        )

        val firstVisibleItem = layoutInfo.visibleItemsInfo.first()
        val visibleItemsCount = layoutInfo.visibleItemsInfo.size

        if (visibleItemsCount < totalItemsCount) {
            val estimatedTotalHeight = (viewportHeight / visibleItemsCount) * totalItemsCount
            val thumbHeight = (viewportHeight / estimatedTotalHeight) * viewportHeight

            val itemHeight = firstVisibleItem.size.toFloat()
            val scrolledFraction =
                if (itemHeight > 0) firstVisibleItem.offset.toFloat() / itemHeight else 0f
            val smoothIndex = firstVisibleItem.index.toFloat() - scrolledFraction

            val thumbOffset = (smoothIndex / totalItemsCount) * viewportHeight

            drawRoundRect(
                color = color,
                topLeft = Offset(size.width - width.toPx(), thumbOffset),
                size = Size(width.toPx(), thumbHeight),
                cornerRadius = CornerRadius(width.toPx() / 2)
            )
        }
    }
}