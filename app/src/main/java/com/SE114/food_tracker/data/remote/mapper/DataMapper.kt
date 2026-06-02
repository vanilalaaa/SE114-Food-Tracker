package com.SE114.food_tracker.data.remote.mapper

import com.SE114.food_tracker.data.local.entities.Budget
import com.SE114.food_tracker.data.local.entities.Item
import com.SE114.food_tracker.data.local.entities.SyncStatus
import com.SE114.food_tracker.data.remote.dto.BudgetDTO
import com.SE114.food_tracker.data.remote.dto.ItemDTO
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneOffset
import org.threeten.bp.format.DateTimeFormatter

object DataMapper {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    // MAPPERS CHO MÓN ĂN (ITEM)
    fun ItemDTO.toEntity(): Item {
        // Chuyển "2026-06-03" -> Epoch Millis (UTC 00:00:00)
        val entryDateMillis = LocalDate.parse(this.entryDate, dateFormatter)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

        val createdAtMillis = Instant.parse(this.createdAt).toEpochMilli()
        val updatedAtMillis = Instant.parse(this.updatedAt).toEpochMilli()

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
        // Chuyển Millis -> "YYYY-MM-DD"
        val entryDateStr = Instant.ofEpochMilli(this.entryDate)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
            .format(dateFormatter)

        val createdAtStr = Instant.ofEpochMilli(this.createdAt).toString()
        val updatedAtStr = Instant.ofEpochMilli(this.updatedAt).toString()

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
    /**
     * Chuyển đổi từ BudgetDTO (Mạng) sang Budget (Room Entity)
     */
    fun BudgetDTO.toEntity(): Budget {
        val updatedAtMillis = Instant.parse(this.updatedAt).toEpochMilli()
        return Budget(
            userId = this.userId,
            daily = this.daily,
            weekly = this.weekly,
            monthly = this.monthly,
            yearly = this.yearly,
            updatedAt = updatedAtMillis
        )
    }

    /**
     * Chuyển đổi từ Budget (Room Entity) sang BudgetDTO (Mạng) để đẩy lên server
     */
    fun Budget.toDto(): BudgetDTO {
        val updatedAtStr = Instant.ofEpochMilli(this.updatedAt).toString()
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