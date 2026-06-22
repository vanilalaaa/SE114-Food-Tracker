package com.SE114.food_tracker.feature.stats.components

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.SE114.food_tracker.core.designsystem.theme.*
import com.SE114.food_tracker.core.util.LocalCurrencyDisplay
import com.SE114.food_tracker.feature.stats.CategoryStat

@Composable
fun CategoryStatItem(
    name: String,
    iconUrl: String,
    imageUrl: String? = null,
    amount: Double,
    fraction: Float,
    barColor: Color,
    trackColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Icon / image avatar ──────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(Color(0xFFFFE9DD)),
            contentAlignment = Alignment.Center
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(50.dp))
                )
            } else {
                Text(text = iconUrl, fontSize = 20.sp)
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // ── Category name ────────────────────────────────────────────────────
        Text(
            text = name,
            style = StatLabelStyle,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimaryStat,
            modifier = Modifier.width(64.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        // ── Progress bar (track + fill) ──────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .background(trackColor, RoundedCornerShape(50)),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f).coerceAtLeast(0.06f))
                    .height(10.dp)
                    .background(barColor, RoundedCornerShape(50))
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // ── Exact amount ─────────────────────────────────────────────────────
        Text(
            text = LocalCurrencyDisplay.current.format(amount),
            style = StatLabelStyle,
            fontSize = 13.sp,
            color = TextLabelGray,
            modifier = Modifier.widthIn(min = 76.dp)
        )
    }
}

@Composable
fun TopCategoriesCard(
    categories: List<CategoryStat>,
    modifier: Modifier = Modifier
) {
    if (categories.isEmpty()) return

    val barColors = listOf(
        Rank1Terracotta,
        Rank2Amber,
        Rank3SoftTeal,
        Rank4SoftBlue
    )

    val maxTotal = categories.maxOf { it.total }.coerceAtLeast(0.0001)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = CardWhite,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Top danh mục",
                style = StatSectionTitleStyle,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = TextPrimaryStat
            )

            Spacer(modifier = Modifier.height(8.dp))

            categories.forEachIndexed { index, stat ->
                val fraction  = (stat.total / maxTotal).toFloat()
                val barColor  = barColors[index % barColors.size]
                val trackColor = barColor.copy(alpha = 0.18f)

                CategoryStatItem(
                    name      = stat.name,
                    iconUrl   = stat.iconUrl,
                    imageUrl  = null,
                    amount    = stat.total,
                    fraction  = fraction,
                    barColor  = barColor,
                    trackColor = trackColor
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TopCategoriesCardPreview() {
    FoodTrackerTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MainBackground)
                .padding(16.dp)
        ) {
            TopCategoriesCard(
                categories = listOf(
                    CategoryStat(name = "Merch",  iconUrl = "💳", total = 300_000.0),
                    CategoryStat(name = "Drink",  iconUrl = "🥤", total =  20_000.0),
                    CategoryStat(name = "Cơm",    iconUrl = "🍚", total = 150_000.0)
                )
            )
        }
    }
}