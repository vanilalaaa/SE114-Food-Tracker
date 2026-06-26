package com.SE114.food_tracker.feature.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.core.designsystem.theme.LightPinkBG
import com.SE114.food_tracker.core.designsystem.theme.StatPinkDark

@Composable
fun AddMemberDialog(
    friendList: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    val selectedMembers = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LightPinkBG,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Thêm thành viên",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = StatPinkDark
            )
        },
        text = {
            if (friendList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Hiện không có bạn bè nào để thêm",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    items(friendList) { friend ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (selectedMembers.contains(friend.first)) selectedMembers.remove(
                                        friend.first
                                    )
                                    else selectedMembers.add(friend.first)
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedMembers.contains(friend.first),
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(checkedColor = StatPinkDark)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(friend.second)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedMembers.toList()) },
                colors = ButtonDefaults.buttonColors(containerColor = StatPinkDark)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Thêm")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun AddMemberDialogPreview() {
    val previewFriends = listOf(
        Pair("1", "Azun"),
        Pair("2", "Tzan"),
        Pair("3", "Uyen")
    )
    androidx.compose.material3.MaterialTheme {
        AddMemberDialog(
            friendList = previewFriends,
            onDismiss = {},
            onConfirm = {}
        )
    }
}