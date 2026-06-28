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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.SE114.food_tracker.core.designsystem.theme.*
import com.SE114.food_tracker.data.local.dao.ConversationWithUnread

@Composable
fun ConversationItem(
    conversation: ConversationWithUnread,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUnread = conversation.isUnread
    val title = conversation.displayName ?: "Không có tên"

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
        // Avatar — group photo or 1-1 peer photo when available, otherwise a colored initial.
        val avatarUrl = if (conversation.isGroup) conversation.avatarUrl else conversation.peerAvatar
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (conversation.isGroup) LightPeach else LightGreenStat),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (conversation.isGroup) Color.White else TextPrimaryStat
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Name + snippet
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
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

        // Unread indicator — always the exact "new messages" count (no bare dot); gone once read.
        if (conversation.unreadCount > 0) {
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                    .clip(CircleShape)
                    .background(StatPinkDark)
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
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
    unreadCount: Int,
    snippet: String? = null
) = ConversationWithUnread(
    id = id,
    isGroup = isGroup,
    name = name,
    walletId = null,
    lastMessageAt = if (unreadCount > 0) System.currentTimeMillis() else 0L,
    lastMessageSnippet = snippet,
    createdAt = System.currentTimeMillis(),
    isUnread = unreadCount > 0,
    unreadCount = unreadCount
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
                unreadCount = 3,
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
                unreadCount = 0,
                snippet = "Hệ thống: Azun đã nộp 100,000 VND"
            ),
            onClick = {}
        )
    }
}
