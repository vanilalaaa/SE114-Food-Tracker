package com.SE114.food_tracker.feature.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
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
import com.SE114.food_tracker.feature.chat.components.GroupSettingsDialog
import com.SE114.food_tracker.feature.chat.components.MessageBubble
import com.SE114.food_tracker.feature.chat.components.MessageUiModel

@Composable
fun ChatScreen(
    conversationId: String,
    conversationName: String,
    viewModel: ChatViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onWalletClick: (id: String) -> Unit = {}
) {
    val myId = viewModel.currentUserId

    // KÍCH HOẠT LẮNG NGHE REALTIME ĐỘNG THEO ID THẬT ĐƯỢC CHỌN TỪ CONVERSATION LIST
    LaunchedEffect(conversationId) {
        viewModel.connectToConversation(conversationId)
    }

    // Đọc luồng dữ liệu thật realtime từ Room DB thông qua Flow
    val messageList by viewModel.getMessagesState(conversationId)
        .collectAsState(initial = emptyList())

    ChatScreenContent(
        conversationId = conversationId,
        conversationName = conversationName,
        messageList = messageList,
        myId = myId,
        onBackClick = onBackClick,
        onWalletClick = { onWalletClick(conversationId) }, // 🌟 Bắn dữ liệu Id thật ra ngoài khi nhấn nút túi tiền
        onSendMessage = { text -> viewModel.sendTextMessage(conversationId, text) },
        onSendImage = { uri -> viewModel.sendImageMessage(conversationId, uri) },
        onRetryMessage = { rawMsg -> viewModel.retryFailedMessage(rawMsg) },
        onRenameGroup = { id, name -> viewModel.renameGroup(id, name) },
        onKickMember = { id, uid, name -> viewModel.kickGroupMember(id, uid, name) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreenContent(
    conversationId: String,
    conversationName: String,
    messageList: List<MessageUiModel>,
    myId: String,
    onBackClick: () -> Unit,
    onWalletClick: () -> Unit,
    onSendMessage: (String) -> Unit,
    onSendImage: (String) -> Unit,
    onRetryMessage: (com.SE114.food_tracker.data.local.entities.Message) -> Unit,
    onRenameGroup: (String, String) -> Unit,
    onKickMember: (String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var textInput by remember { mutableStateOf("") }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onSendImage(it.toString()) }
    }

    if (showSettingsDialog) {
        GroupSettingsDialog(
            conversationName = conversationName,
            onDismissRequest = { showSettingsDialog = false },
            onRenameGroup = { newName ->
                onRenameGroup(conversationId, newName)
                showSettingsDialog = false
            },
            onKickMember = { userId, memberName ->
                onKickMember(conversationId, userId, memberName)
                showSettingsDialog = false
            }
        )
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
                actions = {
                    IconButton(onClick = onWalletClick) {
                        Text("💰", fontSize = 22.sp)
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = StatPinkDark
                        )
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
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                reverseLayout = true,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp, top = 16.dp)
            ) {
                val reversedList = messageList.reversed()

                items(reversedList.size, key = { reversedList[it].localId }) { index ->
                    val message = reversedList[index]

                    MessageBubble(
                        message = message,
                        isMine = message.senderId == myId,
                        onRetryClick = { message.rawEntity?.let { onRetryMessage(it) } }
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
                    IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
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
            conversationId = "2",
            conversationName = "Team SE114 - Food Tracker 🥑",
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
            onWalletClick = {},
            onSendMessage = {},
            onSendImage = {},
            onRetryMessage = {},
            onRenameGroup = { _, _ -> },
            onKickMember = { _, _, _ -> }
        )
    }
}