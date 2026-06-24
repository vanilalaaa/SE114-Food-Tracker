package com.SE114.food_tracker.feature.friend.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.data.local.dao.FriendItemDto
import com.SE114.food_tracker.core.designsystem.theme.*

@Composable
fun FriendListItem(
    friend: FriendItemDto,
    onOpenProfile: () -> Unit,
    isBusy: Boolean,
    onUnfriend: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfileAvatar(
            avatarUrl = friend.avatarUrl,
            modifier = Modifier.clickable(onClick = onOpenProfile)
        )
        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = friend.displayName,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = "@${friend.searchUserId}",
                color = HintGray,
                fontSize = 12.sp
            )
        }

        IconButton(
            onClick = { onUnfriend(friend.friendshipId) },
            enabled = !isBusy
        ) {
            Icon(Icons.Default.Clear, contentDescription = "Xóa", tint = HintGray)
        }
    }
}
