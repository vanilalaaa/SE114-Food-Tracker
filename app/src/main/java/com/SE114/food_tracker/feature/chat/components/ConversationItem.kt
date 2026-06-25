package com.SE114.food_tracker.feature.chat.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.SE114.food_tracker.data.local.dao.ConversationWithUnread

@Composable
fun ConversationItem(
    conversation: ConversationWithUnread,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUnread = conversation.isUnread

    // Animate the dot color so the transition feels smooth
    val dotAlpha by animateColorAsState(
        targetValue = if (isUnread) StatPinkDark else Color.Transparent,
        animationSpec = tween(durationMillis = 300),
        label = "unread_dot"
    )

    val snippetColor = if (isUnread) TextPrimary else TextLabelGray
    val snippetWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal
    val nameWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold

    val snippet = when {
        conversation.lastMessageSnippet.isNullOrBlank() -> "Nhấp để xem tin nhắn..."
        else -> conversation.lastMessageSnippet
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
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

        // Name + snippet
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = conversation.name ?: "Không có tên",
                fontSize = 15.sp,
                fontWeight = nameWeight,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = snippet,
                fontSize = 13.sp,
                color = snippetColor,
                fontWeight = snippetWeight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Unread dot — always sized, color animates to transparent when read
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(dotAlpha)
        )
    }

    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = Color(0xFFE0E0E0)
    )
}

// ── Previews ─────────────────────────────────────────────────────────────────

private fun fakeConversation(
    id: String,
    name: String,
    isGroup: Boolean,
    isUnread: Boolean,
    snippet: String? = null
) = ConversationWithUnread(
    id = id,
    isGroup = isGroup,
    name = name,
    walletId = null,
    lastMessageAt = if (isUnread) System.currentTimeMillis() else 0L,
    lastMessageSnippet = snippet,
    createdAt = System.currentTimeMillis(),
    isUnread = isUnread
)

@Preview(showBackground = true)
@Composable
fun ConversationItemUnreadPreview() {
    FoodTrackerTheme {
        ConversationItem(
            conversation = fakeConversation(
                id = "1",
                name = "Azun (Data)",
                isGroup = false,
                isUnread = true,
                snippet = "Ăn bún bò Huế đi!"
            ),
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ConversationItemReadPreview() {
    FoodTrackerTheme {
        ConversationItem(
            conversation = fakeConversation(
                id = "2",
                name = "Quỹ Nhóm Food Tracker",
                isGroup = true,
                isUnread = false,
                snippet = "Hệ thống: Azun đã nộp 100,000 VND"
            ),
            onClick = {}
        )
    }
}