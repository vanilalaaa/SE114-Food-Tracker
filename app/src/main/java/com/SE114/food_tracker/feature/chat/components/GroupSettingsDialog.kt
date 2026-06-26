package com.SE114.food_tracker.feature.chat.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.SE114.food_tracker.core.designsystem.theme.*
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Edit
@Composable
fun GroupSettingsDialog(
    conversationName: String,
    memberList: List<Pair<String, String>>,
    onDismissRequest: () -> Unit,
    onRenameGroup: (String) -> Unit,
    onKickMember: (userId: String, name: String) -> Unit,
    isAdmin: Boolean,
    onDisbandGroup: () -> Unit,
    currentAvatarUrl: String? = null,
    onChangeAvatar: (imageUri: String) -> Unit = {},
    onRemoveAvatar: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var groupNameInput by remember { mutableStateOf(conversationName) }
    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { onChangeAvatar(it.toString()) } }

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

                // Group avatar with an edit badge → opens the image picker.
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    if (!currentAvatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = currentAvatarUrl,
                            contentDescription = "Ảnh đại diện nhóm",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(76.dp).clip(CircleShape).background(LightPeach)
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(76.dp).clip(CircleShape).background(LightPeach),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = conversationName.trim().take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 30.sp
                            )
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = StatPinkDark,
                        modifier = Modifier.clickable { avatarPicker.launch("image/*") }
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Edit,
                            contentDescription = "Chỉnh sửa ảnh đại diện",
                            tint = Color.White,
                            modifier = Modifier
                                .padding(6.dp)
                                .size(16.dp)
                        )
                    }
                }

                if (!currentAvatarUrl.isNullOrBlank()) {
                    TextButton(
                        onClick = onRemoveAvatar,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = "Gỡ ảnh đại diện",
                            color = StatRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

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
                                        Icon(
                                            imageVector = androidx.compose.material.icons.Icons.Default.PersonRemove,
                                            contentDescription = "Mời ra khỏi nhóm",
                                            tint = StatRed,
                                            modifier = Modifier
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                                .size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (isAdmin) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(thickness = 1.dp, color = StatRed.copy(alpha = 0.3f))

                        Button(
                            onClick = onDisbandGroup,
                            colors = ButtonDefaults.buttonColors(containerColor = StatRed),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Text(
                                "Giải tán nhóm",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
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
            onKickMember = { _, _ -> },
            isAdmin = true,
            onDisbandGroup = {}
        )
    }
}