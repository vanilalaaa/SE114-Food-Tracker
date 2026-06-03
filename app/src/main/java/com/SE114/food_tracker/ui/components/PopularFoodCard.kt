package com.SE114.food_tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.SE114.food_tracker.core.designsystem.theme.*

@Composable
fun PopularFoodItem(
    foodName: String,
    recordCount: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(Color(0xFFE8D3C7))
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = foodName,
                style = StatSectionTitleStyle,
                color = TextPrimaryStat
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Ghi nhận: $recordCount",
                style = StatLabelStyle,
                color = TextLabelGray
            )
        }
    }
}

@Composable
fun PopularFoodCard(
    foodList: List<PopularFoodData>,
    modifier: Modifier = Modifier
) {
    val lightPeachStat = Color(0xFFFFEADF)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = lightPeachStat,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "PHỔ BIẾN NHẤT",
                style = StatSectionTitleStyle,
                color = TextPrimaryStat
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                foodList.forEach { food ->
                    PopularFoodItem(
                        foodName = food.name,
                        recordCount = food.recordCount
                    )
                }
            }
        }
    }
}

data class PopularFoodData(
    val name: String,
    val recordCount: String
)

@Preview(showBackground = true)
@Composable
fun PopularFoodCardPreview() {
    val sampleFoods = listOf(
        PopularFoodData("Phở Hà Nội", "1"),
        PopularFoodData("Phở Vĩ", "1")
    )
    FoodTrackerTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MainBackground)
                .padding(16.dp)
        ) {
            PopularFoodCard(foodList = sampleFoods)
        }
    }
}