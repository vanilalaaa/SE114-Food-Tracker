package com.SE114.food_tracker.feature.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.SE114.food_tracker.core.designsystem.theme.*
import com.SE114.food_tracker.feature.chat.components.MessageBubble
import com.SE114.food_tracker.feature.chat.components.MessageUiModel

@Composable
fun ChatScreen(
    conversationId: String,
    conversationName: String,
    viewModel: ChatViewModel = viewModel(),
    onBackClick: () -> Unit = {}
) {
    val myId = viewModel.currentUserId

    // Đọc luồng dữ liệu thật realtime từ Room DB thông qua Flow
    val messageList by viewModel.getMessagesState(conversationId)
        .collectAsState(initial = emptyList())

    // Chuyển tiếp dữ liệu và sự kiện xuống hàm xử lý giao diện thuần
    ChatScreenContent(
        conversationName = conversationName,
        messageList = messageList,
        myId = myId,
        onBackClick = onBackClick,
        onSendMessage = { text -> viewModel.sendTextMessage(conversationId, text) },
        onSendImage = { uri -> viewModel.sendImageMessage(conversationId, uri) },
        onRetryMessage = { rawMsg -> viewModel.retryFailedMessage(rawMsg) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreenContent(
    conversationName: String,
    messageList: List<MessageUiModel>,
    myId: String,
    onBackClick: () -> Unit,
    onSendMessage: (String) -> Unit,
    onSendImage: (String) -> Unit,
    onRetryMessage: (com.SE114.food_tracker.data.local.entities.Message) -> Unit,
    modifier: Modifier = Modifier
) {
    var textInput by remember { mutableStateOf("") }

    // Bộ phóng mở Gallery thật của hệ điều hành lấy Uri ảnh thật
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onSendImage(it.toString()) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = conversationName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
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
        containerColor = MainBackground,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Vùng hiển thị nội dung danh sách tin nhắn
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                reverseLayout = true, // Giữ nguyên để đẩy tin nhắn từ dưới đáy mượt mà
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp, top = 16.dp)
            ) {
                val reversedList = messageList.reversed()

                items(reversedList.size, key = { reversedList[it].localId }) { index ->
                    val message = reversedList[index]

                    MessageBubble(
                        message = message,
                        isMine = message.senderId == myId,
                        onRetryClick = {
                            message.rawEntity?.let { onRetryMessage(it) }
                        }
                    )

                    val hasOlderMessage = index + 1 < reversedList.size
                    val olderMessage = if (hasOlderMessage) reversedList[index + 1] else null

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
                        imagePickerLauncher.launch("image/*")
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
                                onSendMessage(textInput)
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
        ChatScreenContent(
            conversationName = "Azun (Data)",
            myId = "vy_id",
            messageList = listOf(
                MessageUiModel(
                    senderId = "azun_id",
                    body = "Ăn bún bò Huế đi",
                    imageUrl = null,
                    dateLabel = "Hôm nay"
                ),
                MessageUiModel(
                    senderId = "vy_id",
                    body = "Oki chốt kèo luôn nha!",
                    imageUrl = null,
                    dateLabel = "Hôm nay"
                )
            ),
            onBackClick = {},
            onSendMessage = {},
            onSendImage = {},
            onRetryMessage = {}
        )
    }
}