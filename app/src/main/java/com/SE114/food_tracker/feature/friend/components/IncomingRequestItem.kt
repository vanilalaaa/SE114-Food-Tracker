package com.SE114.food_tracker.feature.friend.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
fun IncomingRequestItem(request: FriendItemDto, onAccept: (String) -> Unit, onDecline: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfileAvatar(avatarUrl = request.avatarUrl)
        Spacer(modifier = Modifier.width(12.dp))

        Text(request.displayName, color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))

        Row {
            Button(
                onClick = { onAccept(request.friendshipId) },
                colors = ButtonDefaults.buttonColors(containerColor = OrangeMain),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Nhận", color = CardWhite, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { onDecline(request.friendshipId) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Clear, contentDescription = "Xóa", tint = HintGray)
            }
        }
    }
}
