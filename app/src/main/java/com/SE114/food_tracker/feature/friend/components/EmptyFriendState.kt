package com.SE114.food_tracker.feature.friend.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.core.designsystem.theme.*

@Composable
fun EmptyFriendState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("👋", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Chưa có bạn bè", color = TextLabelGray, fontSize = 16.sp)
    }
}