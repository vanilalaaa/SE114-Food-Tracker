package com.SE114.food_tracker.feature.friend.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.core.designsystem.theme.*
import com.SE114.food_tracker.data.local.dao.FriendItemDto

@Composable
fun OutgoingRequestItem(
    request: FriendItemDto,
    isBusy: Boolean,
    onCancel: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfileAvatar(avatarUrl = request.avatarUrl)
        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = request.displayName,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = "Đang chờ phản hồi",
                color = TextLabelGray,
                fontSize = 12.sp
            )
        }

        OutlinedButton(
            onClick = { onCancel(request.friendshipId) },
            enabled = !isBusy,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = HintGray),
            contentPadding = PaddingValues(horizontal = 10.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Hủy lời mời",
                modifier = Modifier.size(16.dp),
                tint = HintGray
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Hủy", fontSize = 13.sp)
        }
    }
}
