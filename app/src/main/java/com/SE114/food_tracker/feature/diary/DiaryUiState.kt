package com.SE114.food_tracker.feature.diary

import android.net.Uri
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

data class DiaryCategory(
    val categoryId: String,
    val name: String,
    val iconUrl: String,
    val isHidden: Boolean = false,
    val isSystem: Boolean = false
)

data class DiaryItem(
    val itemId: String,
    val categoryId: String,
    val categoryName: String,
    val categoryIconUrl: String,
    val name: String,
    val timeType: Int,
    val timeLabel: String,
    val price: Double,
    val currencyCode: String,
    val rating: Int? = null,
    val note: String? = null,
    val imageUrl: String? = null,
    val isShared: Boolean = false,
    val walletId: String? = null,
    val entryDate: Long,
    val createdAt: Long,
    val updatedAt: Long
)

data class DiaryUiState(
    val items: List<DiaryItem> = emptyList(),
    val monthlyItems: List<DiaryItem> = emptyList(),
    val categories: List<DiaryCategory> = emptyList(),
    val selectedDate: LocalDate = currentLocalDate(),
    val selectedCategoryId: String? = null,
    val datesWithData: Set<Int> = emptySet(),
    val totalSpend: Double = 0.0,
    val itemCount: Int = 0,
    val streak: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    // Local URI of a picked/captured image waiting to be saved with the next item.
    // Null when no image has been selected. Shown as a preview in FoodEntryScreen.
    val pendingImageUri: Uri? = null
) {
    val selectedYear: Int = selectedDate.year
    val selectedMonth: Int = selectedDate.monthNumber
    val selectedDayOfMonth: Int = selectedDate.dayOfMonth
    val filteredItems: List<DiaryItem> =
        selectedCategoryId?.let { categoryId ->
            items.filter { it.categoryId == categoryId }
        } ?: items
}

private fun currentLocalDate(): LocalDate =
    Clock.System.todayIn(TimeZone.currentSystemDefault())