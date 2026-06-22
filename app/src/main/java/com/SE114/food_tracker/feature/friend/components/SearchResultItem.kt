package com.SE114.food_tracker.feature.friend.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.core.designsystem.theme.CardWhite
import com.SE114.food_tracker.core.designsystem.theme.HintGray
import com.SE114.food_tracker.core.designsystem.theme.OrangeMain
import com.SE114.food_tracker.core.designsystem.theme.TextLabelGray
import com.SE114.food_tracker.core.designsystem.theme.TextPrimary
import com.SE114.food_tracker.data.remote.dto.ProfileDTO
import com.SE114.food_tracker.feature.friend.FriendRelationship

@Composable
fun SearchResultItem(
    profile: ProfileDTO,
    relationship: FriendRelationship,
    onSendRequest: (String) -> Unit
) {
    val displayName = profile.displayName?.takeIf { it.isNotBlank() }
        ?: profile.userId?.takeIf { it.isNotBlank() }
        ?: "Người dùng"
    val userTag = profile.userId?.takeIf { it.isNotBlank() } ?: profile.id

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(CardWhite, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfileAvatar(avatarUrl = profile.avatarUrl, hasStory = false)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(displayName, color = TextPrimary, fontWeight = FontWeight.Bold)
            Text("@$userTag", color = TextLabelGray, fontSize = 12.sp)
        }

        if (relationship.canSendRequest) {
            Button(
                onClick = { onSendRequest(profile.id) },
                colors = ButtonDefaults.buttonColors(containerColor = OrangeMain),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Thêm", color = CardWhite)
            }
        } else {
            Surface(
                color = CardWhite,
                contentColor = HintGray,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = relationship.label.orEmpty(),
                    color = HintGray,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}
