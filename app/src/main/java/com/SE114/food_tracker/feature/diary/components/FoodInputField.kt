package com.SE114.food_tracker.feature.diary.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.core.designsystem.theme.*

@Composable
fun FoodInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    isSingleLine: Boolean = true,
    trailingText: String? = null,
    labelIcon: Any? = null,
    maxChars: Int? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label.uppercase(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            if (labelIcon != null) {
                Spacer(Modifier.width(6.dp))
                when (labelIcon) {
                    is ImageVector -> Icon(
                        imageVector = labelIcon,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.Gray
                    )
                    is String -> Text(labelIcon, fontSize = 14.sp)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Ô nhập dữ liệu (TextField) ────────────────────────────────────────
        TextField(
            value = value,
            onValueChange = { newValue ->
                // Chỉ nhận giá trị mới nếu chưa vượt quá giới hạn ký tự (hoặc không cài giới hạn)
                if (maxChars == null || newValue.length <= maxChars) {
                    onValueChange(newValue)
                }
            },
            placeholder = { Text(placeholder, fontSize = 14.sp, color = Color.LightGray) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = isSingleLine,
            maxLines = if (isSingleLine) 1 else 3,
            shape = RoundedCornerShape(12.dp),
            suffix = {
                if (trailingText != null) {
                    Text(text = trailingText, color = Color.LightGray, fontSize = 14.sp)
                }
            },
            supportingText = {
                // Hiển thị bộ đếm ký tự nhỏ xinh dưới góc phải khi có cài đặt maxChars
                if (maxChars != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text(
                            text = "${value.length}/$maxChars",
                            fontSize = 11.sp,
                            color = if (value.length >= maxChars) Color(0xFFE57373) else Color.Gray
                        )
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = OrangeMain
            )
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF5E4)
@Composable
fun FoodInputFieldPreview() {
    FoodTrackerTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            // Tên món: Tối đa 50 ký tự
            FoodInputField(
                label = "TÊN MÓN",
                value = "Bún chả Obam",
                onValueChange = {},
                placeholder = "VD: Phở Bò",
                maxChars = 50
            )

            // Không giới hạn ký tự
            FoodInputField(
                label = "GIÁ",
                value = "35",
                onValueChange = {},
                trailingText = "K đ"
            )

            // Ghi chú: Tối đa 200 ký tự công thức/lưu ý mua đồ
            FoodInputField(
                label = "Ghi chú",
                labelIcon = "📝",
                value = "Ít bún, nhiều chả băm, không lấy đu đủ",
                onValueChange = {},
                isSingleLine = false,
                maxChars = 200
            )
        }
    }
}