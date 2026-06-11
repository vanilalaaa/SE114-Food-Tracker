package com.SE114.food_tracker.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.core.designsystem.theme.*
import com.SE114.food_tracker.data.local.entities.MessageSyncStatus
import com.SE114.food_tracker.feature.chat.components.MessageBubble
import com.SE114.food_tracker.feature.chat.components.MessageUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    conversationName: String = "Azun (Data)",
    onBackClick: () -> Unit = {}
) {
    val myId = "vy_id"
    var messageList by remember {
        mutableStateOf(
            listOf(
                MessageUiModel(senderId = "azun_id", body = "Ăn bún bò Huế đi", imageUrl = null, timeLabel = "10:12 PM", dateLabel = "07/06/2026"),
                MessageUiModel(senderId = "system", body = "Vy đã nộp 100k vào quỹ nhóm", imageUrl = null, isSystem = true, timeLabel = "10:13 PM", dateLabel = "07/06/2026"),

                MessageUiModel(senderId = "vy_id", body = "Oki lên kèo. Ăn chỗ này đi", imageUrl = null, timeLabel = "10:15 PM", dateLabel = "Hôm nay"),
                MessageUiModel(senderId = "vy_id", body = null, imageUrl = "MOCK_URL_IMAGE", syncStatus = MessageSyncStatus.SENT, timeLabel = "10:15 PM", dateLabel = "Hôm nay"),
                MessageUiModel(senderId = "vy_id", body = "Ủa cái ảnh này gửi lên chưa ta?", imageUrl = null, syncStatus = MessageSyncStatus.PENDING, timeLabel = "10:16 PM", dateLabel = "Hôm nay"),
                MessageUiModel(senderId = "vy_id", body = "Tin nhắn này bị lỗi mạng rồi nè", imageUrl = null, syncStatus = MessageSyncStatus.FAILED, timeLabel = "10:17 PM", dateLabel = "Hôm nay")
            ).reversed() // Đảo ngược mảng để tin mới nhất nằm dưới đáy
        )
    }

    var textInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = conversationName, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Đang trực tuyến", fontSize = 11.sp, color = Color(0xFF4CAF50))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MainBackground)
            )
        },
        containerColor = MainBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Vùng hiển thị nội dung tin nhắn kèm dòng phân cách ngày tháng động
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                reverseLayout = true, // Đảo ngược layout
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp, top = 16.dp)
            ) {
                // Duyệt danh sách theo index để xử lý logic so sánh ngày giữa các phần tử liền kề
                items(messageList.size, key = { messageList[it].localId }) { index ->
                    val message = messageList[index]

                    // Vẽ bong bóng chat bình thường
                    MessageBubble(
                        message = message,
                        isMine = message.senderId == myId,
                        onRetryClick = {
                            messageList = messageList.map {
                                if (it.localId == message.localId) it.copy(syncStatus = MessageSyncStatus.PENDING) else it
                            }
                        }
                    )

                    // LOGIC TỰ ĐỘNG CHÈN DÒNG PHÂN CÁCH NGÀY THÁNG:
                    // Vì danh sách reverse nên phần tử phía sau trong mảng (index + 1) thực chất lại là tin nhắn cũ hơn
                    val hasOlderMessage = index + 1 < messageList.size
                    val olderMessage = if (hasOlderMessage) messageList[index + 1] else null

                    // Điều kiện hiện dòng ngày:
                    // Nếu nó là tin nhắn đầu tiên của cuộc trò chuyện (olderMessage == null)
                    // HOẶC ngày của tin nhắn hiện tại khác hoàn toàn với ngày của tin nhắn cũ hơn trước đó
                    if (olderMessage == null || message.dateLabel != olderMessage.dateLabel) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "— ${message.dateLabel} —",
                                color = TextLabelGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // 2. Ô soạn thảo tin nhắn (Bottom Input Bar)
            Surface(
                color = MainBackground,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .navigationBarsPadding()
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        val newImageMessage = MessageUiModel(
                            senderId = myId,
                            body = null,
                            imageUrl = "LOCAL_GALLERY_URI",
                            syncStatus = MessageSyncStatus.PENDING,
                            timeLabel = "Vừa xong",
                            dateLabel = "Hôm nay"
                        )
                        messageList = listOf(newImageMessage) + messageList
                    }) {
                        Text("🖼️", fontSize = 22.sp)
                    }

                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("Nhập tin nhắn...", fontSize = 14.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CardWhite,
                            unfocusedContainerColor = CardWhite,
                            focusedBorderColor = StatPinkDark,
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        ),
                        maxLines = 4
                    )

                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                val newMsg = MessageUiModel(
                                    senderId = myId,
                                    body = textInput,
                                    imageUrl = null,
                                    syncStatus = MessageSyncStatus.PENDING,
                                    timeLabel = "Vừa xong",
                                    dateLabel = "Hôm nay"
                                )
                                messageList = listOf(newMsg) + messageList
                                textInput = ""
                            }
                        },
                        enabled = textInput.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = if (textInput.isNotBlank()) StatPinkDark else Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun ChatScreenPreview() {
    FoodTrackerTheme {
        ChatScreen(conversationId = "1")
    }
}