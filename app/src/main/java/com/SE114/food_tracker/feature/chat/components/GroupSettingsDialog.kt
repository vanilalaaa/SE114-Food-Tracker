package com.SE114.food_tracker.feature.chat.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.core.designsystem.theme.*

@Composable
fun GroupSettingsDialog(
    conversationName: String,
    memberList: List<Pair<String, String>>,
    onDismissRequest: () -> Unit,
    onRenameGroup: (String) -> Unit,
    onKickMember: (userId: String, name: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var groupNameInput by remember { mutableStateOf(conversationName) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = LightPinkBG,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Quản trị nhóm Chat",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = StatPinkDark
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {

                OutlinedTextField(
                    value = groupNameInput,
                    onValueChange = { groupNameInput = it },
                    label = { Text("Tên nhóm mới", color = TextLabelGray) },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardWhite,
                        unfocusedContainerColor = CardWhite,
                        focusedBorderColor = StatPinkDark,
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedLabelColor = StatPinkDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (groupNameInput.isNotBlank()) {
                            onRenameGroup(groupNameInput)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatPinkDark),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        "Cập nhật tên",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                HorizontalDivider(thickness = 1.dp, color = CalendarHighlight)

                Text(
                    text = "Danh sách thành viên",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextSecondary
                )

                // Kiểm tra nếu danh sách trống từ server
                if (memberList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Chưa tải được danh sách thành viên", color = HintGray, fontSize = 13.sp)
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        memberList.forEach { member ->
                            Surface(
                                color = CardWhite,
                                shape = RoundedCornerShape(16.dp),
                                shadowElevation = 1.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = member.second,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )

                                    // Chỉ cho phép click gọi hàm Kick Member với đúng thông tin thực tế
                                    Surface(
                                        color = StatPinkLight,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .clickable { onKickMember(member.first, member.second) }
                                    ) {
                                        Text(
                                            text = "Mời ra ❌",
                                            color = StatRed,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(
                                                horizontal = 12.dp,
                                                vertical = 6.dp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(
                    text = "Đóng",
                    color = TextLabelGray,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        },
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GroupSettingsDialogPreview() {
    val previewMembers = listOf(
        Pair("azun_id", "Azun (Data) 🥑"),
        Pair("vy_id", "Vy Nguyễn (BA) 🥰")
    )
    FoodTrackerTheme {
        GroupSettingsDialog(
            conversationName = "Team SE114 - Food Tracker 🥑",
            memberList = previewMembers,
            onDismissRequest = {},
            onRenameGroup = {},
            onKickMember = { _, _ -> }
        )
    }
}