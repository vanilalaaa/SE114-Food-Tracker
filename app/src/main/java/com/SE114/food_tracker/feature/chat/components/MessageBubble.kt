package com.SE114.food_tracker.feature.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.SE114.food_tracker.core.designsystem.theme.*
import com.SE114.food_tracker.data.local.entities.MessageSyncStatus
import java.util.UUID

// Data class đại diện cho trạng thái hiển thị của một tin nhắn trên UI
data class MessageUiModel(
    val localId: String = UUID.randomUUID().toString(),
    val senderId: String,
    val body: String?,
    val imageUrl: String?,
    val isSystem: Boolean = false,
    val syncStatus: MessageSyncStatus = MessageSyncStatus.SENT,
    val timeLabel: String = "10:15 PM",
    val dateLabel: String = "Hôm nay",
    val rawEntity: com.SE114.food_tracker.data.local.entities.Message? = null
)

@Composable
fun MessageBubble(
    message: MessageUiModel,
    isMine: Boolean,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 1. Trường hợp đặc biệt: Tin nhắn hệ thống (System Message)
    if (message.isSystem) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = Color(0xFFECEFF1),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = message.body ?: "",
                    color = Color(0xFF546E7A),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    // 2. Trường hợp thông thường: Tin nhắn Chat giữa các thành viên
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMine) {
            // Avatar đối phương
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFAED9E0)),
                contentAlignment = Alignment.Center
            ) {
                Text("A", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimaryStat)
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        // Khối nội dung bong bóng chat chính
        Column(
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
        ) {
            Surface(
                color = if (isMine) LightPeach else LightGreenStat,
                contentColor = if (isMine) Color.White else TextPrimaryStat,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isMine) 16.dp else 2.dp,
                    bottomEnd = if (isMine) 2.dp else 16.dp
                )
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    if (message.imageUrl != null) {
                        Column {
                            // Dùng AsyncImage của thư viện Coil để hiển thị ảnh từ Supabase Storage
                            AsyncImage(
                                model = message.imageUrl,
                                contentDescription = "Hình ảnh gửi kèm",
                                modifier = Modifier
                                    .size(150.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            // Nếu có text caption kèm theo ảnh thì vẽ thêm ở dưới cách 4dp
                            if (!message.body.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = message.body, fontSize = 14.sp)
                            }
                        }
                    } else {
                        // Tin nhắn chỉ chứa ký tự chữ thuần túy
                        if (message.body != null) {
                            Text(text = message.body, fontSize = 14.sp)
                        }
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
            ) {
                Text(text = message.timeLabel, fontSize = 10.sp, color = TextLabelGray)

                // Cờ trạng thái đồng bộ hàng đợi offline (Chỉ hiển thị cho chính mình gửi)
                if (isMine) {
                    Spacer(modifier = Modifier.width(4.dp))
                    when (message.syncStatus) {
                        MessageSyncStatus.PENDING -> {
                            Text(text = "🕒", fontSize = 10.sp) // Đồng hồ chờ gửi
                        }

                        MessageSyncStatus.SENT -> {
                            Text(
                                text = "✓",
                                color = Color(0xFF4CAF50),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        MessageSyncStatus.FAILED -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { onRetryClick() }
                            ) {
                                Text(
                                    text = "🚨 Thất bại",
                                    color = Color.Red,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Retry",
                                    tint = Color.Red,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Tin nhắn đối phương gửi", showBackground = true)
@Composable
fun MessageBubblePeerPreview() {
    FoodTrackerTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MainBackground)
                .padding(16.dp)
        ) {
            MessageBubble(
                message = MessageUiModel(
                    senderId = "azun_id",
                    body = "Ăn bún bò Huế đi zz",
                    imageUrl = null
                ),
                isMine = false,
                onRetryClick = {}
            )
        }
    }
}

@Preview(name = "Tin nhắn của mình - Đã gửi", showBackground = true)
@Composable
fun MessageBubbleMineSentPreview() {
    FoodTrackerTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MainBackground)
                .padding(16.dp)
        ) {
            MessageBubble(
                message = MessageUiModel(
                    senderId = "vy_id",
                    body = "Oki lên kèo. Ăn quán này đi",
                    imageUrl = null,
                    syncStatus = MessageSyncStatus.SENT
                ),
                isMine = true,
                onRetryClick = {}
            )
        }
    }
}

@Preview(name = "Tin nhắn của mình - Đang chờ (Pending)", showBackground = true)
@Composable
fun MessageBubbleMinePendingPreview() {
    FoodTrackerTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MainBackground)
                .padding(16.dp)
        ) {
            MessageBubble(
                message = MessageUiModel(
                    senderId = "vy_id",
                    body = "Ủa cái ảnh này gửi lên chưa ta?",
                    imageUrl = null,
                    syncStatus = MessageSyncStatus.PENDING
                ),
                isMine = true,
                onRetryClick = {}
            )
        }
    }
}

@Preview(name = "Tin nhắn của mình - Lỗi (Failed)", showBackground = true)
@Composable
fun MessageBubbleMineFailedPreview() {
    FoodTrackerTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MainBackground)
                .padding(16.dp)
        ) {
            MessageBubble(
                message = MessageUiModel(
                    senderId = "vy_id",
                    body = "Mạng yếu quá tin nhắn này bị lỗi rồi",
                    imageUrl = null,
                    syncStatus = MessageSyncStatus.FAILED
                ),
                isMine = true,
                onRetryClick = {}
            )
        }
    }
}

@Preview(name = "Tin nhắn hệ thống (System)", showBackground = true)
@Composable
fun MessageBubbleSystemPreview() {
    FoodTrackerTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MainBackground)
                .padding(16.dp)
        ) {
            MessageBubble(
                message = MessageUiModel(
                    senderId = "system",
                    body = "Hệ thống: Vy đã nộp 100k vào quỹ nhóm",
                    imageUrl = null,
                    isSystem = true
                ),
                isMine = false,
                onRetryClick = {}
            )
        }
    }
}