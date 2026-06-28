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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Delete // THÊM IMPORT ICON XÓA
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
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
import com.SE114.food_tracker.core.designsystem.components.AppTopBar
import com.SE114.food_tracker.core.designsystem.theme.*
import com.SE114.food_tracker.feature.chat.components.GroupSettingsDialog
import com.SE114.food_tracker.feature.chat.components.ChatImageViewer
import com.SE114.food_tracker.feature.chat.components.MessageBubble
import com.SE114.food_tracker.feature.chat.components.MessageUiModel
import kotlinx.coroutines.launch
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material.icons.filled.Image
import androidx.hilt.navigation.compose.hiltViewModel
import com.SE114.food_tracker.feature.friend.FriendViewModel
import kotlinx.coroutines.flow.firstOrNull

private const val MAX_MESSAGE_LENGTH = 2000

@Composable
fun ChatScreen(
    conversationId: String,
    conversationName: String,
    targetFriendId: String? = null,
    viewModel: ChatViewModel,
    friendViewModel: FriendViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onWalletClick: () -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            if (event == "DISBANDED" || event == "LEFT") {
                onBackClick() // Tự động thoát ra ngay lập tức
            }
        }
    }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val conversationState by viewModel.getConversationState(conversationId)
        .collectAsState(initial = null)
    val messages by viewModel.getMessagesState(conversationId).collectAsState(initial = emptyList())
    val currentUserId = viewModel.currentUserId

    val isGroup = conversationState?.isGroup ?: false
    val acceptedFriends by friendViewModel.acceptedFriends.collectAsState()

    val friendList = remember(acceptedFriends) {
        acceptedFriends.map {
            Pair(it.userId, it.displayName)
        }
    }
    LaunchedEffect(conversationId) {
        viewModel.connectToConversation(conversationId)
        viewModel.loadGroupMembers(conversationId)
    }

    val latestMessageId = messages.lastOrNull()?.localId
    LaunchedEffect(conversationId, latestMessageId) {
        viewModel.markAsRead(conversationId)
    }

    val memberList by viewModel.groupMembers.collectAsState()
    val isAdmin by viewModel.isCurrentAdmin.collectAsState()
    val friendId = memberList.firstOrNull { it.first != currentUserId }?.first

    ChatScreenContent(
        conversationId = conversationId,
        conversationName = if (isGroup) conversationState?.name
            ?: conversationName else conversationName,
        groupAvatarUrl = conversationState?.avatarUrl,
        messageList = messages,
        myId = currentUserId,
        isGroup = isGroup,
        isAdmin = isAdmin,
        onDisbandGroup = {
            viewModel.disbandGroup(conversationId)
        },
        onDeleteDirectChat = {
            viewModel.deleteDirectChat(conversationId) // Gọi hàm xóa 1-1 từ ViewModel
        },
        memberList = memberList.map { Pair(it.first, it.second) },
        friendList = friendList,
        onAddMembers = { userIds ->
            viewModel.addMembers(conversationId, userIds)
        },
        onBackClick = onBackClick,
        onWalletClick = onWalletClick,
        onCreateWalletClick = {},

        onSendMessage = { text ->
            viewModel.sendTextMessage(
                conversationId = conversationId,
                text = text,
                isOneToOne = false
            )
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
        onKickMemberWithResult = { convId, userId, name ->
            viewModel.kickGroupMember(convId, userId, name)
        },
        onRetryMessage = { messageEntity ->
            viewModel.retryFailedMessage(messageEntity)
        },
        viewModel = viewModel
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreenContent(
    conversationId: String,
    conversationName: String,
    groupAvatarUrl: String? = null,
    messageList: List<MessageUiModel>,
    myId: String,
    isGroup: Boolean,
    isAdmin: Boolean,
    onDisbandGroup: () -> Unit,
    onDeleteDirectChat: () -> Unit, // Callback xóa 1-1
    memberList: List<Pair<String, String>>,
    onBackClick: () -> Unit,
    onWalletClick: () -> Unit,
    onCreateWalletClick: () -> Unit,
    onSendMessage: (String) -> Unit,
    onSendImage: (String) -> Unit,
    onRetryMessage: (com.SE114.food_tracker.data.local.entities.Message) -> Unit,
    onRenameGroup: (String, String) -> Unit,
    friendList: List<Pair<String, String>>,
    onAddMembers: (List<String>) -> Unit,
    onKickMember: (String, String, String) -> Unit,
    onKickMemberWithResult: ((String, String, String) -> Unit)? = null,
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var textInput by remember { mutableStateOf("") }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showDeleteDirectDialog by remember { mutableStateOf(false) } // State Dialog xóa 1-1
    var viewerImageUrl by remember { mutableStateOf<String?>(null) } // Ảnh đang xem toàn màn hình

    var memberToKick by remember { mutableStateOf<Pair<String, String>?>(null) }

    viewerImageUrl?.let { url ->
        ChatImageViewer(imageUrl = url, onDismiss = { viewerImageUrl = null })
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onSendImage(it.toString()) }
    }

    // Dialog xác nhận xóa cuộc trò chuyện 1-1
    if (showDeleteDirectDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDirectDialog = false },
            title = {
                Text("Xóa cuộc trò chuyện", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Bạn có chắc chắn muốn xóa toàn bộ lịch sử cuộc trò chuyện này không? Hành động này không thể hoàn tác.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDirectDialog = false
                        onDeleteDirectChat()
                        Toast.makeText(context, "Đã xóa cuộc trò chuyện thành công!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatRed)
                ) {
                    Text("Xóa", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDirectDialog = false }) {
                    Text("Hủy", color = TextLabelGray)
                }
            },
            containerColor = CardWhite,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (memberToKick != null) {
        AlertDialog(
            onDismissRequest = { memberToKick = null },
            title = {
                Text("Xác nhận mời ra khỏi nhóm", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Bạn có chắc chắn muốn mời ${memberToKick?.second} rời khỏi nhóm này không?",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        memberToKick?.let { (userId, name) ->
                            if (onKickMemberWithResult != null) {
                                onKickMemberWithResult(conversationId, userId, name)
                            } else {
                                onKickMember(conversationId, userId, name)
                            }
                        }
                        memberToKick = null
                        showSettingsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatPinkDark)
                ) {
                    Text("Đồng ý", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToKick = null }) {
                    Text("Hủy", color = TextLabelGray)
                }
            },
            containerColor = CardWhite,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showSettingsDialog) {
        GroupSettingsDialog(
            conversationName = conversationName,
            currentAvatarUrl = groupAvatarUrl,
            memberList = memberList,
            onDismissRequest = { showSettingsDialog = false },
            onRenameGroup = { newName ->
                onRenameGroup(conversationId, newName)
                showSettingsDialog = false
            },
            onChangeAvatar = { uri ->
                viewModel.updateGroupAvatar(conversationId, uri) { success ->
                    Toast.makeText(
                        context,
                        if (success) "Đã cập nhật ảnh đại diện nhóm! 🖼️" else "Lỗi cập nhật ảnh, thử lại nhé!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onRemoveAvatar = {
                viewModel.removeGroupAvatar(conversationId) { success ->
                    Toast.makeText(
                        context,
                        if (success) "Đã gỡ ảnh đại diện nhóm!" else "Lỗi gỡ ảnh, thử lại nhé!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onKickMember = { userId, name ->
                memberToKick = Pair(userId, name)
            },
            isAdmin = isAdmin,
            onDisbandGroup = onDisbandGroup,
            friendList = friendList,
            onAddMembers = { userIds -> viewModel.addMembers(conversationId, userIds) }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = conversationName,
                subtitle = "Đang trực tuyến",
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isGroup) {
                        IconButton(onClick = { showSettingsDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = StatPinkDark
                            )
                        }
                    } else {
                        // NÚT XÓA TRÒ CHUYỆN HIỂN THỊ KHI LÀ CHAT 1-1
                        IconButton(onClick = { showDeleteDirectDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Chat",
                                tint = StatRed
                            )
                        }
                    }
                }
            )
        },
        containerColor = MainBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .imePadding()
        ) {
            var isRefreshing by remember { mutableStateOf(false) }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    viewModel.refreshChatData(conversationId) {
                        isRefreshing = false
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
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
                        val isSystemMessage = message.isSystem || message.senderId == "system" || message.senderId == "SYSTEM"
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (!isMine && isGroup && !isSystemMessage) {
                                Text(
                                    text = message.senderName,
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(start = 56.dp, bottom = 2.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = when {
                                    isSystemMessage -> Arrangement.Center
                                    isMine -> Arrangement.End
                                    else -> Arrangement.Start
                                },
                                verticalAlignment = Alignment.Bottom
                            ) {

                                MessageBubble(
                                    message = message,
                                    isMine = isMine,
                                    senderName = message.senderName,
                                    onRetryClick = { message.rawEntity?.let { onRetryMessage(it) } },
                                    onImageClick = { viewerImageUrl = it }
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
            }

            Surface(
                color = MainBackground,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MainBackground)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Send Image",
                            tint = StatPinkDark
                        )
                    }

                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it.take(MAX_MESSAGE_LENGTH) },
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
                        maxLines = 4,
                        supportingText = if (textInput.length > MAX_MESSAGE_LENGTH - 200) {
                            {
                                Text(
                                    text = "${textInput.length}/$MAX_MESSAGE_LENGTH",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.End,
                                    fontSize = 11.sp,
                                    color = if (textInput.length >= MAX_MESSAGE_LENGTH) StatRed else TextLabelGray
                                )
                            }
                        } else null
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
            onKickMember = { _, _, _ -> },
            onDisbandGroup = {},
            onDeleteDirectChat = {},
            friendList = emptyList(),
            onAddMembers = {},
            viewModel = viewModel()
        )
    }
}