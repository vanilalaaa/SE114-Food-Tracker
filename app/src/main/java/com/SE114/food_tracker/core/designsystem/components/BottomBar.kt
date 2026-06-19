package com.SE114.food_tracker.core.designsystem.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.SE114.food_tracker.core.designsystem.theme.*

@Composable
fun BottomBar(selectedIndex: Int = 0, onItemSelected: (Int) -> Unit) {
    NavigationBar(
        containerColor = MainBackground,
        tonalElevation = 0.dp,
        modifier = Modifier.height(80.dp)
    ) {
        val icons = listOf(
            Icons.Filled.MenuBook,
            Icons.Outlined.BarChart,
            Icons.Outlined.Article,
            Icons.Outlined.Forum,
            Icons.Outlined.Settings
        )
        icons.forEachIndexed { index, icon ->
            NavigationBarItem(
                selected = index == selectedIndex,
                onClick = { onItemSelected(index) },
                icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = TextPrimary,
                    unselectedIconColor = Color.Gray,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

@Preview
@Composable
fun BottomBarPreview() {
    FoodTrackerTheme {
        BottomBar(onItemSelected = {})
    }
}
