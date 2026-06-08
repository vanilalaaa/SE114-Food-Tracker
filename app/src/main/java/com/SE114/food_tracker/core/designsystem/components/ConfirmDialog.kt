package com.SE114.food_tracker.core.designsystem.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.SE114.food_tracker.core.designsystem.theme.FoodTrackerTheme

@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    cancelLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false
) {
    val confirmColor =
        if (destructive) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleSmall) },
        text = { Text(body, style = MaterialTheme.typography.bodyLarge) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = confirmColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(cancelLabel)
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun ConfirmDialogPreview() {
    FoodTrackerTheme {
        ConfirmDialog(
            title = "Đăng xuất?",
            body = "Bạn sẽ cần đăng nhập lại để tiếp tục.",
            confirmLabel = "Đăng xuất",
            cancelLabel = "Huỷ",
            onConfirm = {},
            onDismiss = {},
            destructive = true
        )
    }
}
