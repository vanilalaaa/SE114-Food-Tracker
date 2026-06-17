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
import com.SE114.food_tracker.core.util.formatVndExact
import com.SE114.food_tracker.core.designsystem.theme.*
import com.SE114.food_tracker.feature.stats.WalletDestroyerItem

@Composable
fun WalletDestroyerCard(
    item: WalletDestroyerItem?,
    modifier: Modifier = Modifier
) {
    if (item == null) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = CardWhite,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Title row ────────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "🔥", fontSize = 20.sp)
                Text(
                    text = "KẺ HUỶ DIỆT VÍ TIỀN",
                    style = StatSectionTitleStyle,
                    color = TextPrimaryStat,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            // ── Content row ──────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Thumbnail
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFFCEAE8)),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.imageUrl != null) {
                        AsyncImage(
                            model = item.imageUrl,
                            contentDescription = item.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                        )
                    } else {
                        Text(
                            text = item.categoryIconUrl,
                            fontSize = 28.sp
                        )
                    }
                }

                // Metadata column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.name,
                        style = StatSectionTitleStyle,
                        color = TextPrimaryStat,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 2
                    )

                    Text(
                        text = "${item.recordCount} lần · Tổng ${(item.price * item.recordCount).formatVndExact()}",
                        style = StatLabelStyle,
                        color = TextLabelGray,
                        fontSize = 13.sp
                    )

                    if (item.percentageShare > 0.0) {
                        Text(
                            text = "Chiếm ${"%.0f".format(item.percentageShare)}% chi tiêu kỳ này",
                            style = StatLabelStyle,
                            color = Color(0xFFD05050),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WalletDestroyerCardPreview() {
    FoodTrackerTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MainBackground)
                .padding(16.dp)
        ) {
            WalletDestroyerCard(
                item = WalletDestroyerItem(
                    itemId          = "1",
                    name            = "card bo góc",
                    categoryName    = "Ăn vặt",
                    categoryIconUrl = "🍡",
                    price           = 300_000.0,
                    currencyCode    = "VND",
                    imageUrl        = null,
                    recordCount     = 1,
                    percentageShare = 93.0
                )
            )
        }
    }
}