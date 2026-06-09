package com.SE114.food_tracker.core.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.SE114.food_tracker.core.designsystem.theme.FoodTrackerTheme

/**
 * Animated shimmer placeholder. Caller controls the footprint via [modifier]
 * (e.g. `Modifier.size(...)`) and the silhouette via [shape].
 */
@Composable
fun LoadingShimmer(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer-translate"
    )

    val brush = Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(translate - 400f, 0f),
        end = Offset(translate, 0f)
    )

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .clip(shape)
            .background(brush)
    )
}

@Preview(showBackground = true)
@Composable
private fun LoadingShimmerPreview() {
    FoodTrackerTheme {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LoadingShimmer(modifier = Modifier.size(56.dp), shape = CircleShape)
            LoadingShimmer(modifier = Modifier.width(200.dp).height(16.dp))
            LoadingShimmer(modifier = Modifier.width(120.dp).height(16.dp))
        }
    }
}
