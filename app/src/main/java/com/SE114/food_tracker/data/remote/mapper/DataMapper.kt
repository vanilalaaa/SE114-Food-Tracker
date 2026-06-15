package com.SE114.food_tracker.data.remote.mapper

import com.SE114.food_tracker.data.local.entities.Budget
import com.SE114.food_tracker.data.local.entities.Category
import com.SE114.food_tracker.data.local.entities.Item
import com.SE114.food_tracker.core.sync.SyncStatus
import com.SE114.food_tracker.data.remote.dto.BudgetDTO
import com.SE114.food_tracker.data.remote.dto.CategoryDTO
import com.SE114.food_tracker.data.remote.dto.ItemDTO
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

object DataMapper {

    fun ItemDTO.toEntity(): Item {
        val entryDateMillis = LocalDate.parse(this.entryDate)
            .atStartOfDayIn(TimeZone.UTC)
            .toEpochMilliseconds()

        val createdAtMillis = Instant.parse(this.createdAt).toEpochMilliseconds()
        val updatedAtMillis = Instant.parse(this.updatedAt).toEpochMilliseconds()

        return Item(
            itemId       = this.id,
            categoryId   = this.categoryId,
            name         = this.name,
            timeType     = this.timeType,
            price        = this.price,
            currencyCode = this.currencyCode,
            rating       = this.rating,
            note         = this.note,
            imageUrl     = this.imageUrl,
            isShared     = this.isShared,
            walletId     = this.walletId,
            syncStatus   = SyncStatus.SYNCED.name,
            entryDate    = entryDateMillis,
            createdAt    = createdAtMillis,
            updatedAt    = updatedAtMillis,
            isDeleted    = this.isDeleted
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
            id           = this.itemId,
            ownerId      = ownerId,
            categoryId   = this.categoryId,
            name         = this.name,
            price        = this.price,
            currencyCode = this.currencyCode,
            timeType     = this.timeType,
            entryDate    = entryDateStr,
            rating       = this.rating,
            note         = this.note,
            imageUrl     = this.imageUrl,
            isShared     = this.isShared,
            walletId     = this.walletId,
            createdAt    = createdAtStr,
            updatedAt    = updatedAtStr,
            isDeleted    = this.isDeleted
        )
    }

    // ── CATEGORY mappers ──────────────────────────────────────────────────────

    fun CategoryDTO.toEntity(): Category {
        val createdAtMillis = Instant.parse(this.createdAt).toEpochMilliseconds()
        return Category(
            categoryId = this.id,
            ownerId    = this.ownerId,
            name       = this.name,
            iconUrl    = this.iconUrl,
            isHidden   = this.isHidden,
            isSystem   = this.isSystem,
            isDeleted  = this.isDeleted,
            syncStatus = SyncStatus.SYNCED.name,
            createdAt  = createdAtMillis,
            updatedAt  = createdAtMillis
        )
    }

    fun Category.toDto(): CategoryDTO {
        val createdAtStr = Instant.fromEpochMilliseconds(this.createdAt).toString()
        return CategoryDTO(
            id        = this.categoryId,
            ownerId   = this.ownerId,
            name      = this.name,
            iconUrl   = this.iconUrl,
            isHidden  = this.isHidden,
            isSystem  = this.isSystem,
            isDeleted = this.isDeleted,
            createdAt = createdAtStr
        )
    }

    // ── BUDGET mappers ────────────────────────────────────────────────────────

    fun BudgetDTO.toEntity(): Budget {
        val updatedAtMillis = Instant.parse(this.updatedAt).toEpochMilliseconds()
        return Budget(
            userId     = this.userId,
            daily      = this.daily,
            weekly     = this.weekly,
            monthly    = this.monthly,
            yearly     = this.yearly,
            syncStatus = SyncStatus.SYNCED.name,
            updatedAt  = updatedAtMillis,
            isDeleted  = this.isDeleted
        )
    }

    fun Budget.toDto(): BudgetDTO {
        val updatedAtStr = Instant.fromEpochMilliseconds(this.updatedAt).toString()
        return BudgetDTO(
            userId    = this.userId,
            daily     = this.daily,
            weekly    = this.weekly,
            monthly   = this.monthly,
            yearly    = this.yearly,
            updatedAt = updatedAtStr,
            isDeleted = this.isDeleted
        )
    }
}