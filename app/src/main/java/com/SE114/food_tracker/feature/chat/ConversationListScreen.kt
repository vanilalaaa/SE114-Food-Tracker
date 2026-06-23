package com.SE114.food_tracker.feature.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

                friend::class.members.find { it.name == "profile" || it.name == "friendProfile" || it.name == "user" }?.call(friend)
            } catch(e: Exception) { null }

            val finalId = when {
                actualProfile != null -> {

                    val pId = try { actualProfile::class.members.find { it.name == "userId" }?.call(actualProfile) } catch(e: Exception) { null }
                    pId ?: try { actualProfile::class.members.find { it.name == "id" }?.call(actualProfile) } catch(e: Exception) { null }
                }
                else -> {

                    try { friend::class.members.find { it.name == "friendId" || it.name == "userId" || it.name == "id" }?.call(friend) } catch(e: Exception) { null }
                }
            }?.toString() ?: UUID.randomUUID().toString()

            val finalName = when {
                actualProfile != null -> {
                    try { actualProfile::class.members.find { it.name == "displayName" || it.name == "name" }?.call(actualProfile) } catch(e: Exception) { null }
                }
                else -> {
                    try { friend::class.members.find { it.name == "friendName" || it.name == "displayName" }?.call(friend) } catch(e: Exception) { null }
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
            title = { Text("Nhóm Chat mới", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = StatPinkDark) },
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
                                    Text("Không tìm thấy bạn bè nào để thêm", color = HintGray, fontSize = 13.sp)
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
                                    Text(text = friend.second, fontSize = 14.sp, color = TextPrimary)
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
                            viewModel.createGroup(name = newGroupName, members = selectedMembers.toList())
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
        onConversationClick = onConversationClick,
        onBackClick = onBackClick,
        onCreateGroupClick = {
            showCreateGroupDialog = true
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreenContent(
    conversationList: List<Conversation>,
    onConversationClick: (id: String, name: String) -> Unit,
    onBackClick: (() -> Unit)?,
    onCreateGroupClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
        if (conversationList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Chưa có cuộc hội thoại nào\n(Bấm nút ➕ góc trên để tạo nhóm)",
                    color = TextLabelGray,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(conversationList, key = { it.id }) { conversation ->
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
            onConversationClick = { _, _ -> },
            onBackClick = {},
            onCreateGroupClick = {}
        )
    }
}