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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.ui.theme.*

data class MealRecord(
    val time: String,
    val name: String,
    val category: String,
    val price: String
)

data class DayGroup(
    val dateLabel: String? = null,
    val meals: List<MealRecord>
)

@Composable
fun DetailItemRow(
    meal: MealRecord,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(32.dp)
                .background(color = Color(0xFFD2EBD9), shape = RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = meal.time,
                style = StatLabelStyle,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryStat
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(Color(0xFFE8D3C7))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = meal.name,
                style = StatSectionTitleStyle,
                fontSize = 16.sp,
                color = TextPrimaryStat
            )
            Text(
                text = meal.category,
                style = StatLabelStyle,
                color = TextLabelGray,
                fontSize = 13.sp
            )
        }

        Text(
            text = meal.price,
            style = StatValueStyle,
            color = StatPinkDark,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SingleDayCard(
    group: DayGroup,
    isWeeklyMode: Boolean,
    modifier: Modifier = Modifier
) {
    val strokeColor = Color(0xFFD39292)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        if (isWeeklyMode && group.dateLabel != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(25.dp)
                    .background(
                        color = strokeColor,
                        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
                    )
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = group.dateLabel,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        val bodyShape = if (isWeeklyMode) {
            RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp)
        } else {
            RoundedCornerShape(30.dp)
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = bodyShape,
            color = CardWhite,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                group.meals.forEach { meal ->
                    DetailItemRow(meal = meal)
                }
            }
        }
    }
}

@Composable
fun DetailCardSection(
    dataGroups: List<DayGroup>,
    isWeeklyMode: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "CHI TIẾT",
            style = StatSectionTitleStyle,
            color = TextPrimaryStat,
            fontSize = 20.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        dataGroups.forEach { group ->
            SingleDayCard(group = group, isWeeklyMode = isWeeklyMode)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DetailCardDailyPreview() {
    val sampleData = listOf(
        DayGroup(meals = listOf(MealRecord("15:20", "Phở Hà Nội", "Mì & Phở", "30k đ")))
    )
    FoodTrackerTheme {
        Box(modifier = Modifier.background(MainBackground).padding(16.dp)) {
            DetailCardSection(dataGroups = sampleData, isWeeklyMode = false)
        }
    }
}