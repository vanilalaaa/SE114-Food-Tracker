package com.SE114.food_tracker.feature.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
                MessageUiModel(senderId = "azun_id", body = "Tớ bật cờ Realtime cho bảng message rồi á Vy!", imageUrl = null),
                MessageUiModel(senderId = "system", body = "Vy đã nộp 100k vào quỹ nhóm", imageUrl = null, isSystem = true),
                MessageUiModel(senderId = "vy_id", body = "Oki để tớ up cái giao diện khung chat lên luôn nè", imageUrl = null),
                MessageUiModel(senderId = "vy_id", body = null, imageUrl = "MOCK_URL_IMAGE", syncStatus = MessageSyncStatus.SENT),
                MessageUiModel(senderId = "vy_id", body = "Ủa cái ảnh này gửi lên chưa ta?", imageUrl = null, syncStatus = MessageSyncStatus.PENDING),
                MessageUiModel(senderId = "vy_id", body = "Tin nhắn này bị lỗi mạng rồi nè", imageUrl = null, syncStatus = MessageSyncStatus.FAILED)
            ).reversed()
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
            // 1. Vùng hiển thị nội dung tin nhắn
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                reverseLayout = true,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp, top = 16.dp)
            ) {
                items(messageList, key = { it.localId }) { message ->
                    MessageBubble(
                        message = message,
                        isMine = message.senderId == myId,
                        onRetryClick = {
                            messageList = messageList.map {
                                if (it.localId == message.localId) it.copy(syncStatus = MessageSyncStatus.PENDING) else it
                            }
                        }
                    )
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
                            timeLabel = "Vừa xong"
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
                                    timeLabel = "Vừa xong"
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