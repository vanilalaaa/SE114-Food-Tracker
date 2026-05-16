package com.SE114.food_tracker.ui.screens.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.data.local.entities.Category
import com.SE114.food_tracker.ui.components.*
import com.SE114.food_tracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodEntryScreen(
    onDismiss: () -> Unit,
    onSave: (String, String, Int, Int, String) -> Unit
) {
    var foodName by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableIntStateOf(2) }
    var rating by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MainBackground)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Text(
                text = "Thêm món ăn",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center),
                color = Color(0xFF333333)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = Color.Black)
            }
        }

        Spacer(Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.size(140.dp),
                shape = RoundedCornerShape(70.dp),
                color = Color(0xFFFFE9DD)
            ) {
                Text(
                    text = "🍲",
                    fontSize = 60.sp,
                    modifier = Modifier.wrapContentSize()
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        FoodInputField(label = "TÊN MÓN", value = foodName, onValueChange = { foodName = it }, placeholder = "VD: Phở Bò")
        FoodInputField(label = "GIÁ", value = price, onValueChange = { price = it }, placeholder = "0", trailingText = "K đ")

        CategorySelector(
            categories = emptyList(),
            selectedCategoryId = selectedCategoryId,
            onCategorySelected = { selectedCategoryId = it }
        )

        Spacer(Modifier.height(16.dp))

        TimeSelector(time = "15:20", session = "Chiều", onTimeClick = { })

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Đánh giá", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(4.dp))
                Text("⭐", fontSize = 12.sp)
            }
            StarRatingBar(
                rating = rating,
                onRatingChange = { rating = it },
                modifier = Modifier.width(180.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        FoodInputField(
            label = "Ghi chú",
            labelIcon = "📅",
            value = note,
            onValueChange = { note = it },
            placeholder = "VD: Món này ngon quá!",
            isSingleLine = false
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { onSave(foodName, price, selectedCategoryId, rating, note) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA8D5BA))
        ) {
            Text(text = "Thêm món", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Preview(showSystemUi = true, device = "spec:width=411dp,height=891dp")
@Composable
fun FoodEntryScreenPreview() {
    FoodTrackerTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MainBackground) {
            FoodEntryScreen(onDismiss = {}, onSave = { _, _, _, _, _ -> })
        }
    }
}