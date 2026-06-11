package com.SE114.food_tracker.feature.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.core.designsystem.theme.*
import com.SE114.food_tracker.data.local.entities.Conversation

@Composable
fun ConversationItem(
    conversation: Conversation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Avatar tròn tone màu pastel xinh xắn
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (conversation.isGroup) LightPeach else LightGreenStat),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = (conversation.name ?: "U").take(1).uppercase(),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = if (conversation.isGroup) Color.White else TextPrimaryStat
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 2. Phần nội dung Tên và Snippet tin nhắn cuối cùng
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = conversation.name ?: "Không có tên",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (conversation.isGroup) "Nhóm: Nhấp để xem tin nhắn..." else "Nhấp để vào phòng chat 1-1",
                fontSize = 13.sp,
                color = TextLabelGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 3. Dấu chấm tròn nhỏ màu hồng pastel báo trạng thái tin nhắn
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(StatPinkDark)
        )
    }
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = Color(0xFFE0E0E0)
    )
}

@Preview(showBackground = true)
@Composable
fun ConversationItem1On1Preview() {
    FoodTrackerTheme {
        ConversationItem(
            conversation = Conversation(
                id = "1",
                isGroup = false,
                name = "Azun (Data)",
                walletId = "w1"
            ),
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ConversationItemGroupPreview() {
    FoodTrackerTheme {
        ConversationItem(
            conversation = Conversation(
                id = "2",
                isGroup = true,
                name = "Quỹ Nhóm 4 Food Tracker",
                walletId = "w2"
            ),
            onClick = {}
        )
    }
}