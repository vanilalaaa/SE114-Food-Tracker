package com.SE114.food_tracker.feature.profile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.R
import com.SE114.food_tracker.core.designsystem.theme.FoodTrackerTheme
import com.SE114.food_tracker.core.designsystem.theme.TextLabelGray
import com.SE114.food_tracker.data.model.ProfileSharedItem
import com.SE114.food_tracker.feature.diary.components.DayItem
import kotlinx.datetime.LocalDate

@Composable
fun ProfileDiaryTab(
    items: List<ProfileSharedItem>,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = error, color = TextLabelGray, fontSize = 14.sp)
                    TextButton(onClick = onRetry) {
                        Text(stringResource(R.string.profile_retry))
                    }
                }
            }

            items.isEmpty() -> {
                ProfileDiaryEmptyState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp)
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items, key = { it.itemId }) { item ->
                        Column {
                            Text(
                                text = item.entryDate.toDisplayDate(),
                                color = TextLabelGray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            DayItem(
                                name = item.name,
                                category = item.categoryName,
                                price = item.price,
                                time = item.timeLabel,
                                categoryIcon = item.categoryIcon,
                                imageUrl = item.imageUrl,
                                onClick = {}
                            )
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ProfileDiaryEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.profile_diary_empty),
            color = TextLabelGray,
            fontSize = 14.sp
        )
    }
}

private fun String.toDisplayDate(): String =
    runCatching {
        val date = LocalDate.parse(this)
        "${date.dayOfMonth}/${date.monthNumber}/${date.year}"
    }.getOrDefault(this)

@Preview(showBackground = true)
@Composable
private fun ProfileDiaryTabPreview() {
    FoodTrackerTheme {
        ProfileDiaryTab(
            items = listOf(
                ProfileSharedItem(
                    itemId = "1",
                    name = "Phở Bò",
                    categoryName = "Mì & Phở",
                    categoryIcon = "🍜",
                    price = 45_000.0,
                    timeLabel = "Sáng",
                    imageUrl = null,
                    entryDate = "2026-06-07"
                ),
                ProfileSharedItem(
                    itemId = "2",
                    name = "Trà sữa",
                    categoryName = "Đồ uống",
                    categoryIcon = "🥤",
                    price = 35_000.0,
                    timeLabel = "Chiều",
                    imageUrl = null,
                    entryDate = "2026-06-06"
                )
            ),
            isLoading = false,
            error = null,
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileDiaryTabEmptyPreview() {
    FoodTrackerTheme {
        ProfileDiaryTab(
            items = emptyList(),
            isLoading = false,
            error = null,
            onRetry = {}
        )
    }
}
