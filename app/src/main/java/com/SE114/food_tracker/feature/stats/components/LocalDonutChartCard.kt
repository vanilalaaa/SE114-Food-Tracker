package com.SE114.food_tracker.feature.stats.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.core.designsystem.theme.*

data class DonutSegment(val label: String, val value: Float, val color: Color)

@Composable
fun LocalDonutChartCard(
    title: String,
    categories: List<DonutSegment>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = title, style = StatSectionTitleStyle, color = TextPrimaryStat, fontSize = 14.sp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val total = categories.sumOf { it.value.toDouble() }.toFloat()

                Canvas(modifier = Modifier.size(100.dp)) {
                    var startAngle = -90f
                    categories.forEach { segment ->
                        val sweepAngle = (segment.value / total) * 360f
                        drawArc(
                            color = segment.color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = 24f, cap = StrokeCap.Round)
                        )
                        startAngle += sweepAngle
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { segment ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(12.dp).background(segment.color, RoundedCornerShape(3.dp)))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "${segment.label} (${(segment.value/total*100).toInt()}%)", fontSize = 12.sp, color = TextLabelGray)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LocalDonutChartCardPreview() {
    FoodTrackerTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MainBackground)
                .padding(16.dp)
        ) {
            LocalDonutChartCard(
                title = "Tỷ trọng chi tiêu theo danh mục",
                categories = listOf(
                    DonutSegment("Mì & Phở", 70000f, Color(0xFFE8AEB4)),
                    DonutSegment("Cơm", 30000f, Color(0xFFFBE3B5)),
                    DonutSegment("Nước uống", 20000f, Color(0xFFAED9E0))
                )
            )
        }
    }
}