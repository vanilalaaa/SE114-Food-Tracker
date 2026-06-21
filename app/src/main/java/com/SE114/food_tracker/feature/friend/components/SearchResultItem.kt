package com.SE114.food_tracker.feature.friend.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.data.remote.dto.ProfileDTO
import com.SE114.food_tracker.core.designsystem.theme.*

@Composable
fun SearchResultItem(profile: ProfileDTO, onSendRequest: (String) -> Unit) {
    val displayName = profile.displayName?.takeIf { it.isNotBlank() }
        ?: profile.userId?.takeIf { it.isNotBlank() }
        ?: "Người dùng"
    val userTag = profile.userId?.takeIf { it.isNotBlank() } ?: profile.id

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).background(CardWhite, RoundedCornerShape(12.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfileAvatar(avatarUrl = profile.avatarUrl, hasStory = false)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(displayName, color = TextPrimary, fontWeight = FontWeight.Bold)
            Text("@$userTag", color = TextLabelGray, fontSize = 12.sp)
        }
        Button(
            onClick = { onSendRequest(profile.id) },
            colors = ButtonDefaults.buttonColors(containerColor = OrangeMain),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Thêm", color = CardWhite)
        }
    }
}
