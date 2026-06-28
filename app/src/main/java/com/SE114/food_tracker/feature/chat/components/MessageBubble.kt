package com.SE114.food_tracker.feature.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
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
import coil.compose.SubcomposeAsyncImage // 🔥 ĐÃ ĐỔI: Dùng Subcompose để bắt trạng thái Error/Loading gài avatar mặc định
import com.SE114.food_tracker.core.designsystem.theme.*
import com.SE114.food_tracker.data.local.entities.MessageSyncStatus
import java.util.UUID

data class MessageUiModel(
    val localId: String = UUID.randomUUID().toString(),
    val senderId: String,
    val body: String?,
    val imageUrl: String?,
    val isSystem: Boolean = false,
    val syncStatus: MessageSyncStatus = MessageSyncStatus.SENT,
    val timeLabel: String = "10:15 PM",
    val dateLabel: String = "Hôm nay",
    val rawEntity: com.SE114.food_tracker.data.local.entities.Message? = null,
    val senderName: String = "Thành viên",
    val senderAvatarUrl: String = ""
)

@Composable
fun MessageBubble(
    message: MessageUiModel,
    isMine: Boolean,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
    senderName: String = "Thành viên",
    onImageClick: (String) -> Unit = {}
) {

    if (message.isSystem || message.senderId == "system" || message.senderId == "SYSTEM") {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (message.imageUrl != null) {
                // Purchase system message: food photo card + caption
                Surface(
                    color = Color(0xFFECEFF1),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(0.72f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(10.dp)
                    ) {
                        AsyncImage(
                            model = message.imageUrl,
                            contentDescription = "Ảnh món ăn",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = message.body ?: "",
                            color = Color(0xFF546E7A),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Plain system message (deposit, withdrawal, group events)
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
        }
        return
    }

    Row(
        modifier = modifier,
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMine) {
            // Khối vẽ Avatar mặc định (Chữ cái đầu tiên trên nền pastel)
            val displayAvatarName = if (message.senderName.isNotBlank()) message.senderName else senderName
            val avatarChar = displayAvatarName.trim().take(1).uppercase()
            val defaultAvatarModifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(LightGreenStat)

            val defaultAvatar = @Composable {
                Box(
                    modifier = defaultAvatarModifier,
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = avatarChar,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = TextPrimaryStat
                    )
                }
            }

            if (message.senderAvatarUrl.isNotBlank()) {
                SubcomposeAsyncImage(
                    model = message.senderAvatarUrl,
                    contentDescription = "User Avatar",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    loading = { defaultAvatar() },
                    error = { defaultAvatar() }
                )
            } else {
                defaultAvatar()
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

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
                            SubcomposeAsyncImage(
                                model = message.imageUrl,
                                contentDescription = "Hình ảnh gửi kèm",
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier
                                    .size(150.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onImageClick(message.imageUrl) },
                                loading = {
                                    Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                },
                                error = {
                                    Box(
                                        modifier = Modifier
                                            .size(150.dp)
                                            .background(Color(0xFFECECEC)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "⚠️\nKhông tải được ảnh",
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center,
                                            color = TextLabelGray
                                        )
                                    }
                                }
                            )
                            if (!message.body.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = message.body, fontSize = 14.sp)
                            }
                        }
                    } else {
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

                if (isMine) {
                    Spacer(modifier = Modifier.width(4.dp))
                    when (message.syncStatus) {
                        MessageSyncStatus.PENDING -> {
                            Text(text = "🕒 đang chờ", fontSize = 10.sp, color = TextLabelGray)
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
                    imageUrl = null,
                    senderName = "AnhZun"
                ),
                isMine = false,
                senderName = "AnhZun",
                onRetryClick = {}
            )
        }
    }
}