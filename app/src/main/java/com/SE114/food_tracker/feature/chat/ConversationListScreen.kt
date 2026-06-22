package com.SE114.food_tracker.feature.chat

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.SE114.food_tracker.core.designsystem.theme.*
import com.SE114.food_tracker.data.local.entities.Conversation
import com.SE114.food_tracker.feature.chat.components.ConversationItem
import com.SE114.food_tracker.feature.friend.FriendViewModel
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import java.util.UUID

@Composable
fun ConversationListScreen(
    viewModel: ChatViewModel = hiltViewModel(),
    friendViewModel: FriendViewModel = hiltViewModel(),
    onConversationClick: (id: String, name: String) -> Unit = { _, _ -> },
    onBackClick: (() -> Unit)? = null
) {
    val conversationList by viewModel.getConversationsFlow().collectAsState(initial = emptyList())

    // Lắng nghe danh sách bạn bè thật (dạng List<ProfileDTO>) từ StateFlow
    val acceptedFriendsList by friendViewModel.acceptedFriends.collectAsState()
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    val selectedMembers = remember { mutableStateListOf<String>() }

    val friendListCheck = remember(acceptedFriendsList) {
        acceptedFriendsList.map { friend ->
            val actualProfile = try {
                friend::class.members.find { it.name == "profile" || it.name == "friendProfile" || it.name == "user" }
                    ?.call(friend)
            } catch (e: Exception) {
                null
            }

            val finalId = when {
                actualProfile != null -> {
                    val pId = try {
                        actualProfile::class.members.find { it.name == "userId" }
                            ?.call(actualProfile)
                    } catch (e: Exception) {
                        null
                    }
                    pId ?: try {
                        actualProfile::class.members.find { it.name == "id" }?.call(actualProfile)
                    } catch (e: Exception) {
                        null
                    }
                }

                else -> {
                    try {
                        friend::class.members.find { it.name == "friendId" || it.name == "userId" || it.name == "id" }
                            ?.call(friend)
                    } catch (e: Exception) {
                        null
                    }
                }
            }?.toString() ?: UUID.randomUUID().toString()

            val finalName = when {
                actualProfile != null -> {
                    try {
                        actualProfile::class.members.find { it.name == "displayName" || it.name == "name" }
                            ?.call(actualProfile)
                    } catch (e: Exception) {
                        null
                    }
                }

                else -> {
                    try {
                        friend::class.members.find { it.name == "friendName" || it.name == "displayName" }
                            ?.call(friend)
                    } catch (e: Exception) {
                        null
                    }
                }
            }?.toString() ?: "Thành viên nhóm"

            Pair(finalId, finalName)
        }
    }

    if (showCreateGroupDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateGroupDialog = false
                selectedMembers.clear()
                newGroupName = ""
            },
            title = {
                Text(
                    "Nhóm Chat mới",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = StatPinkDark
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        placeholder = { Text("Nhập tên nhóm ăn uống...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = StatPinkDark)
                    )

                    Text(
                        text = "Chọn thành viên nhóm:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (friendListCheck.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillParentMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Không tìm thấy bạn bè nào để thêm",
                                        color = HintGray,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        } else {
                            items(friendListCheck) { friend ->
                                val isChecked = selectedMembers.contains(friend.first)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isChecked) selectedMembers.remove(friend.first)
                                            else selectedMembers.add(friend.first)
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { status ->
                                            if (status == true) selectedMembers.add(friend.first)
                                            else selectedMembers.remove(friend.first)
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = StatPinkDark)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = friend.second,
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                val isValid = newGroupName.isNotBlank() && selectedMembers.size >= 2
                Button(
                    onClick = {
                        if (isValid) {
                            viewModel.createGroup(
                                name = newGroupName,
                                members = selectedMembers.toList()
                            )
                            newGroupName = ""
                            selectedMembers.clear()
                            showCreateGroupDialog = false
                        }
                    },
                    enabled = isValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatPinkDark,
                        disabledContainerColor = HintGray
                    )
                ) {
                    Text("Tạo Nhóm", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreateGroupDialog = false
                        selectedMembers.clear()
                        newGroupName = ""
                    }
                ) {
                    Text("Hủy", color = TextLabelGray)
                }
            },
            containerColor = CardWhite,
            shape = RoundedCornerShape(24.dp)
        )
    }

    ConversationListScreenContent(
        conversationList = conversationList,
        friendList = friendListCheck,
        onConversationClick = onConversationClick,
        onBackClick = onBackClick,
        onCreateGroupClick = {
            showCreateGroupDialog = true
        },
        onRefreshData = { onComplete ->
            try {
                val refreshMethod = viewModel::class.members.find {
                    it.name == "refreshConversations" || it.name == "refreshChatData" || it.name == "loadConversations"
                }
                if (refreshMethod != null) {
                    if (refreshMethod.parameters.size > 1) {
                        refreshMethod.call(viewModel, onComplete)
                    } else {
                        refreshMethod.call(viewModel)
                        onComplete()
                    }
                } else {
                    onComplete()
                }
            } catch (e: Exception) {
                onComplete()
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreenContent(
    conversationList: List<Conversation>,
    friendList: List<Pair<String, String>> = emptyList(),
    onConversationClick: (id: String, name: String) -> Unit,
    onBackClick: (() -> Unit)?,
    onCreateGroupClick: () -> Unit,
    onRefreshData: (onComplete: () -> Unit) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var isRefreshing by remember { mutableStateOf(false) }

    // 1. Lọc phòng chat cũ đã từng nhắn tin
    val filteredConversations = remember(searchQuery, conversationList) {
        if (searchQuery.isBlank()) {
            conversationList
        } else {
            conversationList.filter {
                (it.name ?: "Người dùng").contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // 2. Lọc danh sách bạn bè mới CHƯA CÓ phòng chat để làm gợi ý Chat 1-1
    val filteredNewFriends = remember(searchQuery, friendList, conversationList) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            // Lấy danh sách ID của những cuộc hội thoại 1-1 đã có sẵn
            val existingIds = conversationList.map { it.id }
            friendList.filter { friend ->
                friend.second.contains(searchQuery, ignoreCase = true) && !existingIds.contains(
                    friend.first
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Tin nhắn cuộc trò chuyện",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onCreateGroupClick) {
                        Text("➕", fontSize = 20.sp)
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
            // Thanh Tìm kiếm thông minh pastel
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Tìm kiếm phòng chat hoặc bạn bè...", fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Icon",
                        tint = Color.Gray
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardWhite,
                    unfocusedContainerColor = CardWhite,
                    focusedBorderColor = StatPinkDark,
                    unfocusedBorderColor = Color(0xFFE0E0E0)
                )
            )

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    onRefreshData {
                        isRefreshing = false
                        Toast.makeText(
                            context,
                            "Đã cập nhật hội thoại mới nhất! 💬",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (filteredConversations.isEmpty() && filteredNewFriends.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) {
                                "Chưa có cuộc hội thoại nào\n(Bấm nút ➕ góc trên để tạo nhóm)"
                            } else {
                                "Không tìm thấy phòng chat hay bạn bè nào khớp với \"$searchQuery\""
                            },
                            color = TextLabelGray,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        if (filteredConversations.isNotEmpty()) {
                            if (searchQuery.isNotBlank()) {
                                item {
                                    Text(
                                        text = "Cuộc trò chuyện hiện tại",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StatPinkDark,
                                        modifier = Modifier.padding(
                                            horizontal = 16.dp,
                                            vertical = 6.dp
                                        )
                                    )
                                }
                            }

                            items(filteredConversations, key = { it.id }) { conversation ->
                                ConversationItem(
                                    conversation = conversation,
                                    onClick = {
                                        onConversationClick(
                                            conversation.id,
                                            conversation.name ?: "Người dùng"
                                        )
                                    }
                                )
                            }
                        }

                        if (filteredNewFriends.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Gợi ý bạn bè mới",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                            }

                            items(filteredNewFriends) { friend ->
                                val mockConversation = remember(friend) {
                                    Conversation(
                                        id = friend.first,
                                        name = friend.second,
                                        isGroup = false,
                                        walletId = null
                                    )
                                }
                                ConversationItem(
                                    conversation = mockConversation,
                                    onClick = {
                                        onConversationClick(friend.first, friend.second)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun ConversationListScreenPreview() {
    FoodTrackerTheme {
        ConversationListScreenContent(
            conversationList = listOf(
                Conversation(id = "1", isGroup = false, name = "Azun (Data)", walletId = "w1"),
                Conversation(
                    id = "2",
                    isGroup = true,
                    name = "Quỹ Nhóm 4 Food Tracker",
                    walletId = "w2"
                )
            ),
            friendList = listOf(Pair("3", "Thúy Vy"), Pair("4", "Hải Đăng")),
            onConversationClick = { _, _ -> },
            onBackClick = {},
            onCreateGroupClick = {}
        )
    }
}