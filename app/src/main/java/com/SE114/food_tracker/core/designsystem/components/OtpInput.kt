package com.SE114.food_tracker.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.SE114.food_tracker.core.designsystem.theme.FoodTrackerTheme

/**
 * Boxed numeric OTP field: [length] single-digit cells backed by one hidden text field.
 * Reused by both OTP screens (email signup confirmation and password recovery).
 */
@Composable
fun OtpInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 6,
    isError: Boolean = false,
    enabled: Boolean = true
) {
    BasicTextField(
        value = value,
        onValueChange = { input -> onValueChange(input.filter { it.isDigit() }.take(length)) },
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = modifier,
        decorationBox = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(length) { index ->
                    val char = value.getOrNull(index)?.toString().orEmpty()
                    val isCursor = index == value.length
                    val borderColor = when {
                        isError -> MaterialTheme.colorScheme.error
                        isCursor || index < value.length -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outline
                    }
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(52.dp)
                            .border(
                                BorderStroke(if (isCursor) 2.dp else 1.dp, borderColor),
                                MaterialTheme.shapes.medium
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun OtpInputPreview() {
    FoodTrackerTheme {
        OtpInput(value = "123", onValueChange = {})
    }
}
