package com.SE114.food_tracker.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.core.designsystem.theme.CardWhite
import com.SE114.food_tracker.core.designsystem.theme.HintGray
import com.SE114.food_tracker.core.designsystem.theme.MainBackground
import com.SE114.food_tracker.core.designsystem.theme.TextLabelGray
import com.SE114.food_tracker.core.designsystem.theme.TextPrimary

@Composable
fun ProfileScreen(
    profileId: String,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MainBackground)
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Text(
                text = "Profile",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(36.dp))

        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .background(HintGray, CircleShape)
                .padding(34.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = CardWhite
            )
        }

        Spacer(Modifier.height(18.dp))

        Text(
            text = profileId.ifBlank { "Unknown profile" },
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Text(
            text = "Trang profile sẽ hiển thị chi tiết người dùng ở đây.",
            color = TextLabelGray,
            fontSize = 13.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}
