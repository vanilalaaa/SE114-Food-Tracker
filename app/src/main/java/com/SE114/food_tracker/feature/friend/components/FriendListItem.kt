package com.SE114.food_tracker.feature.friend.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.core.designsystem.theme.HintGray
import com.SE114.food_tracker.core.designsystem.theme.TextPrimary
import com.SE114.food_tracker.data.local.dao.FriendItemDto

@Composable
fun FriendListItem(
    friend: FriendItemDto,
    onOpenProfile: () -> Unit,
    isBusy: Boolean,
    onReport: () -> Unit,
    onUnfriend: () -> Unit
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenProfile)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfileAvatar(avatarUrl = friend.avatarUrl)
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

        Box {
            IconButton(
                onClick = { isMenuExpanded = true },
                enabled = !isBusy
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Mở menu",
                    tint = HintGray
                )
            }
            DropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { isMenuExpanded = false },
                containerColor = Color.White
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Báo cáo",
                            color = Color.Black,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    colors = MenuDefaults.itemColors(textColor = Color.Black),
                    onClick = {
                        isMenuExpanded = false
                        onReport()
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Hủy kết bạn",
                            color = Color.Black,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    colors = MenuDefaults.itemColors(textColor = Color.Black),
                    onClick = {
                        isMenuExpanded = false
                        onUnfriend()
                    }
                )
            }
        }
    }
}
