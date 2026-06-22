package com.SE114.food_tracker.feature.chat

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.* import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.SE114.food_tracker.core.designsystem.theme.*
import com.SE114.food_tracker.feature.chat.components.GroupSettingsDialog
import com.SE114.food_tracker.feature.chat.components.MessageBubble
import com.SE114.food_tracker.feature.chat.components.MessageUiModel
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    conversationId: String,
    conversationName: String,
    viewModel: ChatViewModel,
    onBackClick: () -> Unit,
    onWalletClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Lấy dữ liệu Realtime sống từ Room DB cục bộ
    val conversationState by viewModel.getConversationState(conversationId).collectAsState(initial = null)
    val messages by viewModel.getMessagesState(conversationId).collectAsState(initial = emptyList())
    val currentUserId = viewModel.currentUserId

    val isGroup = conversationState?.isGroup ?: false
    val hasWallet = conversationState?.walletId != null &&
            conversationState?.walletId != "wallet_default" &&
            conversationState?.walletId?.isNotBlank() == true

    LaunchedEffect(conversationId) {
        viewModel.loadGroupMembers(conversationId)
    }

    // Hứng dữ liệu an toàn từ StateFlow tập trung của ViewModel
    val memberList by viewModel.groupMembers.collectAsState()
    val isAdmin by viewModel.isCurrentAdmin.collectAsState()

    // State điều khiển Dialog nhập tên tạo Quỹ nhóm mới
    var showCreateWalletDialog by remember { mutableStateOf(false) }
    var walletNameInput by remember { mutableStateOf("") }

    if (showCreateWalletDialog) {
        AlertDialog(
            onDismissRequest = { showCreateWalletDialog = false },
            title = {
                Text(
                    "Khởi tạo Quỹ Nhóm Mới",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        "Nhập tên cho ví gắn liền với phòng chat này:",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = walletNameInput,
                        onValueChange = { walletNameInput = it },
                        placeholder = { Text("Ví dụ: Quỹ Ăn Trưa $conversationName") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (walletNameInput.isNotBlank()) {
                            viewModel.createGroupWallet(
                                conversationId,
                                walletNameInput
                            ) { success ->
                                if (success) {
                                    Toast.makeText(
                                        context,
                                        "Khởi tạo Quỹ Nhóm thành công! 💰",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Lỗi tạo ví, vui lòng thử lại!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                showCreateWalletDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatPinkDark)
                ) {
                    Text("Tạo Quỹ", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateWalletDialog = false }) {
                    Text("Hủy", color = TextLabelGray)
                }
            },
            containerColor = CardWhite,
            shape = RoundedCornerShape(24.dp)
        )
    }

    ChatScreenContent(
        conversationId = conversationId,
        conversationName = conversationState?.name ?: conversationName,
        messageList = messages,
        myId = currentUserId,
        isGroup = isGroup,
        hasWallet = hasWallet,
        isAdmin = isAdmin,
        memberList = memberList,
        onBackClick = onBackClick,
        onWalletClick = onWalletClick,
        onCreateWalletClick = {
            walletNameInput = "Quỹ của ${conversationState?.name ?: conversationName}"
            showCreateWalletDialog = true
        },
        onSendMessage = { text ->
            viewModel.sendTextMessage(conversationId, text)
        },
        onSendImage = { imageUri ->
            viewModel.sendImageMessage(conversationId, imageUri)
        },
        onRenameGroup = { convId, newName ->
            viewModel.renameGroup(convId, newName)
            Toast.makeText(context, "Cập nhật tên nhóm mới thành công!", Toast.LENGTH_SHORT).show()
        },
        onKickMember = { convId, userId, name ->
            viewModel.kickGroupMember(convId, userId, name)
            Toast.makeText(context, "Đã mời $name rời khỏi nhóm!", Toast.LENGTH_SHORT).show()
        },
        onRetryMessage = { messageEntity ->
            viewModel.retryFailedMessage(messageEntity)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreenContent(
    conversationId: String,
    conversationName: String,
    messageList: List<MessageUiModel>,
    myId: String,
    isGroup: Boolean,
    hasWallet: Boolean,
    isAdmin: Boolean,
    memberList: List<Pair<String, String>>,
    onBackClick: () -> Unit,
    onWalletClick: () -> Unit,
    onCreateWalletClick: () -> Unit,
    onSendMessage: (String) -> Unit,
    onSendImage: (String) -> Unit,
    onRetryMessage: (com.SE114.food_tracker.data.local.entities.Message) -> Unit,
    onRenameGroup: (String, String) -> Unit,
    onKickMember: (String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var textInput by remember { mutableStateOf("") }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Ánh xạ danh sách mảng sang Map để tra cứu tên hiển thị động theo ID thần tốc
    val memberMap = remember(memberList) { memberList.toMap() }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onSendImage(it.toString()) }
    }

    if (showSettingsDialog) {
        GroupSettingsDialog(
            conversationName = conversationName,
            memberList = memberList,
            onDismissRequest = { showSettingsDialog = false },
            onRenameGroup = { newName ->
                onRenameGroup(conversationId, newName)
                showSettingsDialog = false
            },
            onKickMember = { userId, name ->
                onKickMember(conversationId, userId, name)
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
                    if (isGroup) {
                        if (hasWallet) {
                            IconButton(onClick = onWalletClick) {
                                Text("💰", fontSize = 22.sp)
                            }
                        } else if (isAdmin) {
                            TextButton(onClick = onCreateWalletClick) {
                                Text(
                                    "Tạo Quỹ",
                                    color = StatPinkDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
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

                    val isMine = message.senderId == myId ||
                            message.syncStatus == com.SE114.food_tracker.data.local.entities.MessageSyncStatus.PENDING ||
                            message.syncStatus == com.SE114.food_tracker.data.local.entities.MessageSyncStatus.FAILED

                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Nếu là đối phương gửi và là phòng Chat Nhóm, hiển thị Tên hiển thị phía trên bong bóng
                        if (!isMine && isGroup && message.senderId != "system") {
                            val senderName = memberMap[message.senderId] ?: "Thành viên nhóm"
                            Text(
                                text = senderName,
                                fontSize = 11.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(start = 56.dp, bottom = 2.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            if (!isMine && isGroup && message.senderId == "system") {
                                Spacer(modifier = Modifier.width(44.dp))
                            }

                            // 🔥 ĐÃ FIX: Truyền thêm thuộc tính senderName lấy từ Map ra để MessageBubble tự sinh Avatar xịn
                            val senderName = memberMap[message.senderId] ?: "Thành viên nhóm"

                            MessageBubble(
                                message = message,
                                isMine = isMine,
                                senderName = senderName, // 🔥 TRUYỀN THÊM VÀO ĐÂY
                                onRetryClick = { message.rawEntity?.let { onRetryMessage(it) } }
                            )
                        }
                    }

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
            isGroup = true,
            hasWallet = true,
            isAdmin = true,
            memberList = listOf(Pair("azun_id", "Azun"), Pair("vy_id", "Vy")),
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
            onCreateWalletClick = {},
            onSendMessage = {},
            onSendImage = {},
            onRetryMessage = {},
            onRenameGroup = { _, _ -> },
            onKickMember = { _, _, _ -> }
        )
    }
}