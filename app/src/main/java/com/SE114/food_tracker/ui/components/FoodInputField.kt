package com.SE114.food_tracker.ui.components

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
import com.SE114.food_tracker.ui.theme.*

@Composable
fun FoodInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    isSingleLine: Boolean = true,
    trailingText: String? = null,
    labelIcon: Any? = null,
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

        TextField(
            value = value,
            onValueChange = onValueChange,
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
            FoodInputField(label = "TÊN MÓN", value = "", onValueChange = {}, placeholder = "VD: Phở Bò")
            FoodInputField(label = "GIÁ", value = "30", onValueChange = {}, trailingText = "K đ")
            FoodInputField(label = "Ghi chú", labelIcon = "📝", value = "", onValueChange = {}, isSingleLine = false)
        }
    }
}