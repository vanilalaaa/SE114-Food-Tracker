package com.SE114.food_tracker.feature.friend.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.SE114.food_tracker.core.designsystem.theme.CardWhite
import com.SE114.food_tracker.core.designsystem.theme.HintGrayStat
import com.SE114.food_tracker.core.designsystem.theme.OrangeMain

@Composable
fun ProfileAvatar(
    avatarUrl: String?,
    modifier: Modifier = Modifier,
    hasStory: Boolean = false,
    size: Dp = 50.dp
) {
    val imageSize = Modifier
        .size(size)
        .clip(CircleShape)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.then(
            if (hasStory) {
                Modifier
                    .border(2.dp, OrangeMain, CircleShape)
                    .padding(4.dp)
            } else {
                Modifier.padding(6.dp)
            }
        )
    ) {
        if (avatarUrl.isNullOrEmpty()) {
            Box(
                modifier = imageSize.background(HintGrayStat),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = CardWhite)
            }
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Avatar",
                contentScale = ContentScale.Crop,
                modifier = imageSize.background(CardWhite)
            )
        }
    }
}
