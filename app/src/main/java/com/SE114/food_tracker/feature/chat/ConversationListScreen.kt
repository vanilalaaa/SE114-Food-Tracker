package com.SE114.food_tracker.feature.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.SE114.food_tracker.core.designsystem.theme.*
import com.SE114.food_tracker.data.local.entities.Conversation
import com.SE114.food_tracker.feature.chat.components.ConversationItem

@Composable
fun ConversationListScreen(
    viewModel: ChatViewModel = hiltViewModel(),
    onConversationClick: (id: String, name: String) -> Unit = { _, _ -> },
    onBackClick: (() -> Unit)? = null
) {
    val conversationList by viewModel.getConversationsFlow().collectAsState(initial = emptyList())
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }

    if (showCreateGroupDialog) {
        AlertDialog(
            onDismissRequest = { showCreateGroupDialog = false },
            title = { Text("Tạo Nhóm Chat Mới 🥑", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    placeholder = { Text("Nhập tên nhóm ăn uống...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newGroupName.isNotBlank()) {
                            // Đúc nhóm lên server thật (Mặc định thêm các thành viên từ luồng chọn hoặc mồi sau)
                            viewModel.createGroup(name = newGroupName, members = emptyList())
                            newGroupName = ""
                            showCreateGroupDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatPinkDark)
                ) {
                    Text("Tạo", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateGroupDialog = false }) {
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
                    text = "Chưa có cuộc hội thoại nào\n(Bấm nút ➕ góc trên để tạo nhóm test thật nhe!)",
                    color = TextLabelGray,
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