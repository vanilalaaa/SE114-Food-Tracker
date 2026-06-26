package com.SE114.food_tracker.feature.admin.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Small pill showing a status label in [content] over [container]. */
@Composable
fun AdminStatusBadge(
    text: String,
    container: Color,
    content: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = container
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = content,
            modifier = androidx.compose.ui.Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}
