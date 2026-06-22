package com.SE114.food_tracker.feature.feed

import com.SE114.food_tracker.data.local.dao.FeedPostDto

fun buildFreeImageCaption(title: String, note: String): String {
    val cleanTitle = title.trim()
    val cleanNote = note.trim()
    return if (cleanNote.isBlank()) cleanTitle else "$cleanTitle\n$cleanNote"
}

fun FeedPostDto.feedDisplayTitle(): String =
    itemName?.takeIf { it.isNotBlank() }
        ?: caption.lineSequence().firstOrNull()?.trim()?.takeIf { it.isNotBlank() }
        ?: "Ảnh tự do"

fun FeedPostDto.feedDisplayCaption(): String {
    if (!itemName.isNullOrBlank()) return caption.trim()
    return caption.lines()
        .drop(1)
        .joinToString("\n")
        .trim()
}

fun FeedPostDto.feedContentDescription(): String =
    feedDisplayCaption().ifBlank { feedDisplayTitle() }
