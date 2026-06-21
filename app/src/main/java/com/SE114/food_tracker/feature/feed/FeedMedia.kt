package com.SE114.food_tracker.feature.feed

private const val FeedEmojiImagePrefix = "emoji:"

fun String?.feedImageModelOrNull(): String? =
    this?.takeIf { it.isNotBlank() && !it.startsWith(FeedEmojiImagePrefix) }

fun String?.feedEmojiOrNull(): String? =
    this
        ?.takeIf { it.startsWith(FeedEmojiImagePrefix) }
        ?.removePrefix(FeedEmojiImagePrefix)
        ?.takeIf { it.isNotBlank() }

fun feedFallbackIcon(categoryIconUrl: String?, imageUrl: String?): String =
    imageUrl.feedEmojiOrNull()
        ?: categoryIconUrl?.takeIf { it.isNotBlank() }
        ?: "🍱"
