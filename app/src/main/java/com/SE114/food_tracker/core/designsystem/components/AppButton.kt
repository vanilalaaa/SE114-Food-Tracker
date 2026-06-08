package com.SE114.food_tracker.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.SE114.food_tracker.core.designsystem.theme.FoodTrackerTheme

enum class AppButtonVariant { Primary, Secondary, Text, Destructive }

/**
 * Standard app button. While [loading] is true an inline spinner replaces the
 * leading icon and the click is suppressed (the button also reads as disabled).
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AppButtonVariant = AppButtonVariant.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null
) {
    val clickable = enabled && !loading
    val content: @Composable () -> Unit = {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = LocalContentColor.current
                )
            } else if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Text(text = text, style = MaterialTheme.typography.titleSmall)
        }
    }

    when (variant) {
        AppButtonVariant.Primary -> Button(
            onClick = onClick,
            enabled = clickable,
            modifier = modifier.height(52.dp),
            colors = ButtonDefaults.buttonColors(),
            content = { content() }
        )

        AppButtonVariant.Destructive -> Button(
            onClick = onClick,
            enabled = clickable,
            modifier = modifier.height(52.dp),
            colors = destructiveColors(),
            content = { content() }
        )

        AppButtonVariant.Secondary -> FilledTonalButton(
            onClick = onClick,
            enabled = clickable,
            modifier = modifier.height(52.dp),
            content = { content() }
        )

        AppButtonVariant.Text -> TextButton(
            onClick = onClick,
            enabled = clickable,
            modifier = modifier,
            content = { content() }
        )
    }
}

@Composable
private fun destructiveColors(): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.error,
    contentColor = MaterialTheme.colorScheme.onError
)

@Preview(showBackground = true)
@Composable
private fun AppButtonPreview() {
    FoodTrackerTheme {
        Row(Modifier.height(220.dp)) {
            AppButton(text = "Primary", onClick = {})
            AppButton(text = "Loading", onClick = {}, loading = true)
            AppButton(text = "Secondary", onClick = {}, variant = AppButtonVariant.Secondary)
            AppButton(text = "Delete", onClick = {}, variant = AppButtonVariant.Destructive)
            AppButton(text = "Text", onClick = {}, variant = AppButtonVariant.Text)
        }
    }
}
