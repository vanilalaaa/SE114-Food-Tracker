package com.SE114.food_tracker.feature.feed // Sửa lại package nếu cần

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.core.designsystem.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onNavigateToFriend: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Newsfeed", color = TextPrimary, fontWeight = FontWeight.Bold)
                },
                actions = {
                    // ---> NÚT BẠN BÈ Ở GÓC TRÊN BÊN PHẢI NÈ <---
                    IconButton(onClick = onNavigateToFriend) {
                        Icon(
                            imageVector = Icons.Outlined.People,
                            contentDescription = "Bạn bè",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MainBackground)
            )
        },
        containerColor = MainBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Text("Nội dung feed ở đây nha...")
        }
    }
}