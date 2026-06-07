package com.SE114.food_tracker.data.remote.mapper

import com.SE114.food_tracker.data.local.entities.Budget
import com.SE114.food_tracker.data.local.entities.Item
import com.SE114.food_tracker.data.local.entities.SyncStatus
import com.SE114.food_tracker.data.remote.dto.BudgetDTO
import com.SE114.food_tracker.data.remote.dto.ItemDTO
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

object DataMapper {

    // MAPPERS CHO MÓN ĂN (ITEM)
    fun ItemDTO.toEntity(): Item {
        // entry_date là ngày thuần (không giờ); chuẩn hoá về mốc 00:00 UTC để Last-Write-Wins so sánh nhất quán
        val entryDateMillis = LocalDate.parse(this.entryDate)
            .atStartOfDayIn(TimeZone.UTC)
            .toEpochMilliseconds()

        val createdAtMillis = Instant.parse(this.createdAt).toEpochMilliseconds()
        val updatedAtMillis = Instant.parse(this.updatedAt).toEpochMilliseconds()

        return Item(
            itemId = this.id,
            categoryId = this.categoryId,
            name = this.name,
            timeType = this.timeType,
            price = this.price,
            currencyCode = this.currencyCode,
            rating = this.rating,
            note = this.note,
            imageUrl = this.imageUrl,
            isShared = this.isShared,
            walletId = this.walletId,
            syncStatus = SyncStatus.SYNCED.name,
            entryDate = entryDateMillis,
            createdAt = createdAtMillis,
            updatedAt = updatedAtMillis
        )
    }

    fun Item.toDto(ownerId: String): ItemDTO {
        val entryDateStr = Instant.fromEpochMilliseconds(this.entryDate)
            .toLocalDateTime(TimeZone.UTC)
            .date
            .toString()

        val createdAtStr = Instant.fromEpochMilliseconds(this.createdAt).toString()
        val updatedAtStr = Instant.fromEpochMilliseconds(this.updatedAt).toString()

        return ItemDTO(
            id = this.itemId,
            ownerId = ownerId,
            categoryId = this.categoryId,
            name = this.name,
            price = this.price,
            currencyCode = this.currencyCode,
            timeType = this.timeType,
            entryDate = entryDateStr,
            rating = this.rating,
            note = this.note,
            imageUrl = this.imageUrl,
            isShared = this.isShared,
            walletId = this.walletId,
            createdAt = createdAtStr,
            updatedAt = updatedAtStr
        )
    }

    // MAPPERS CHO NGÂN SÁCH (BUDGET)
    fun BudgetDTO.toEntity(): Budget {
        val updatedAtMillis = Instant.parse(this.updatedAt).toEpochMilliseconds()
        return Budget(
            userId = this.userId,
            daily = this.daily,
            weekly = this.weekly,
            monthly = this.monthly,
            yearly = this.yearly,
            updatedAt = updatedAtMillis
        )
    }

    fun Budget.toDto(): BudgetDTO {
        val updatedAtStr = Instant.fromEpochMilliseconds(this.updatedAt).toString()
        return BudgetDTO(
            userId = this.userId,
            daily = this.daily,
            weekly = this.weekly,
            monthly = this.monthly,
            yearly = this.yearly,
            updatedAt = updatedAtStr
        )
    }
}
