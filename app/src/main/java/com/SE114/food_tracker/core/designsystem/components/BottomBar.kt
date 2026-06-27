package com.SE114.food_tracker.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DynamicFeed
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.R
import com.SE114.food_tracker.core.designsystem.theme.CardWhite
import com.SE114.food_tracker.core.designsystem.theme.FoodTrackerTheme
import com.SE114.food_tracker.core.designsystem.theme.HintGrayStat
import com.SE114.food_tracker.core.designsystem.theme.MainBackground
import com.SE114.food_tracker.core.designsystem.theme.StatPinkDark
import com.SE114.food_tracker.core.designsystem.theme.TextSecondary

val BottomBarContentPadding = 112.dp

@Composable
fun BottomBar(
    selectedIndex: Int = 0,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .height(68.dp),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val items = listOf(
                BottomBarItem(Icons.Rounded.RestaurantMenu, R.string.bottom_bar_diary),
                BottomBarItem(Icons.Rounded.QueryStats, R.string.bottom_bar_stats),
                BottomBarItem(Icons.Rounded.DynamicFeed, R.string.bottom_bar_feed),
                BottomBarItem(Icons.Rounded.Forum, R.string.bottom_bar_chat),
                BottomBarItem(Icons.Rounded.Settings, R.string.bottom_bar_settings)
            )
            items.forEachIndexed { index, item ->
                BottomBarButton(
                    item = item,
                    selected = index == selectedIndex,
                    onClick = { onItemSelected(index) }
                )
            }
        }
    }
}

@Composable
private fun BottomBarButton(
    item: BottomBarItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val activeColor = StatPinkDark
    val inactiveColor = HintGrayStat
    val contentColor = if (selected) activeColor else inactiveColor

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(width = 58.dp, height = 34.dp),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = if (selected) MainBackground else CardWhite,
                contentColor = contentColor
            )
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = stringResource(item.labelRes),
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = stringResource(item.labelRes),
            color = if (selected) TextSecondary else inactiveColor,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

private data class BottomBarItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val labelRes: Int
)

@Preview
@Composable
fun BottomBarPreview() {
    FoodTrackerTheme {
        BottomBar(onItemSelected = {})
    }
}
