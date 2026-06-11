package com.SE114.food_tracker.feature.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.core.designsystem.theme.*

@Composable
fun DeleteAllConfirmDialog(
    onDismissRequest: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = CardWhite,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(top = 24.dp, bottom = 16.dp, start = 20.dp, end = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Xóa tất cả dữ liệu?",
                    style = AppTypography.titleMedium,
                    color = StatRed,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "\nHành động này không thể hoàn tác.\nTất cả dữ liệu của bạn sẽ bị xóa vĩnh viễn.",
                    style = AppTypography.bodyLarge,
                    color = TextSecondary, //
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(HintGray.copy(alpha = 0.2f))
                            .clickable { onDismissRequest() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Hủy",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 16.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(HintGray.copy(alpha = 0.2f))
                            .clickable { onConfirmDelete() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Xóa",
                            fontWeight = FontWeight.Bold,
                            color = StatRed,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun DeleteAllConfirmDialogPreview() {
    FoodTrackerTheme {
        DeleteAllConfirmDialog(
            onDismissRequest = {},
            onConfirmDelete = {}
        )
    }
}