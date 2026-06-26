package com.SE114.food_tracker.feature.diary.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.SE114.food_tracker.core.designsystem.theme.FoodTrackerTheme
import com.SE114.food_tracker.core.designsystem.theme.MainBackground
import com.SE114.food_tracker.core.designsystem.theme.MintGreen
import com.SE114.food_tracker.core.designsystem.theme.StatTabActiveStyle
import com.commandiron.wheel_picker_compose.WheelTimePicker
import com.commandiron.wheel_picker_compose.core.TimeFormat
import java.time.LocalTime

@Composable
fun WheelTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onDone: (hour: Int, minute: Int) -> Unit
) {
    // Khởi tạo thời gian mặc định ban đầu
    var selectedTime by remember {
        mutableStateOf(LocalTime.of(initialHour, initialMinute))
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Chọn thời gian",
                    style = StatTabActiveStyle,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Component Con lăn từ thư viện mới
                WheelTimePicker(
                    startTime = LocalTime.of(initialHour, initialMinute),
                    timeFormat = TimeFormat.HOUR_24,
                    onSnappedTime = { snapTime ->
                        selectedTime = snapTime
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Hủy", color = Color.Gray)
                    }
                    TextButton(onClick = {
                        onDone(selectedTime.hour, selectedTime.minute)
                    }) {
                        Text("Xong", color = MintGreen)
                    }
                }
            }
        }
    }
}
@Preview(showSystemUi = true, device = "spec:width=411dp,height=891dp")
@Composable
fun WheelTimePickerDialogPreview() {
    var showDialog by remember { mutableStateOf(true) }
    var selectedTimeText by remember { mutableStateOf("Chưa chọn") }

    FoodTrackerTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MainBackground
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Thời gian đã chọn: $selectedTimeText", style = StatTabActiveStyle)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { showDialog = true }) {
                        Text("Mở bộ chọn thời gian")
                    }
                }

                if (showDialog) {
                    WheelTimePickerDialog(
                        initialHour = 12,
                        initialMinute = 30,
                        onDismiss = { showDialog = false },
                        onDone = { hour, minute ->
                            selectedTimeText = String.format("%02d:%02d", hour, minute)
                            showDialog = false
                        }
                    )
                }
            }
        }
    }
}