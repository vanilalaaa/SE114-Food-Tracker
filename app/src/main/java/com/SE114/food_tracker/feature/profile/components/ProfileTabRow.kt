package com.SE114.food_tracker.feature.profile.components

import androidx.annotation.StringRes
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.R
import com.SE114.food_tracker.core.designsystem.theme.FoodTrackerTheme
import com.SE114.food_tracker.core.designsystem.theme.MainBackground
import com.SE114.food_tracker.core.designsystem.theme.OrangeMain
import com.SE114.food_tracker.core.designsystem.theme.TextLabelGray
import com.SE114.food_tracker.core.designsystem.theme.TextPrimary
import com.SE114.food_tracker.feature.profile.ProfileTab

@Composable
fun ProfileTabRow(
    selectedTab: ProfileTab,
    onTabSelected: (ProfileTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(ProfileTab.DIARY, ProfileTab.POSTS)

    TabRow(
        selectedTabIndex = tabs.indexOf(selectedTab).coerceAtLeast(0),
        modifier = modifier,
        containerColor = MainBackground,
        contentColor = OrangeMain,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[tabs.indexOf(selectedTab).coerceAtLeast(0)]),
                color = OrangeMain
            )
        }
    ) {
        tabs.forEach { tab ->
            val selected = selectedTab == tab
            Tab(
                selected = selected,
                onClick = { onTabSelected(tab) },
                selectedContentColor = TextPrimary,
                unselectedContentColor = TextLabelGray,
                text = {
                    Text(
                        text = stringResource(tab.labelResId),
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                    )
                }
            )
        }
    }
}

private val ProfileTab.labelResId: Int
    @StringRes get() = when (this) {
        ProfileTab.DIARY -> R.string.profile_tab_diary
        ProfileTab.POSTS -> R.string.profile_tab_posts
    }

@Preview(showBackground = true)
@Composable
private fun ProfileTabRowPreview() {
    FoodTrackerTheme {
        ProfileTabRow(
            selectedTab = ProfileTab.DIARY,
            onTabSelected = {}
        )
    }
}
