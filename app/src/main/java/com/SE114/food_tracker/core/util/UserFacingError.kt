package com.SE114.food_tracker.core.util

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

fun Throwable.toUserFacingMessage(
    fallback: String = "Đã xảy ra lỗi, vui lòng thử lại."
): String {
    if (hasCause<UnknownHostException>() ||
        hasCause<ConnectException>() ||
        hasCause<SocketTimeoutException>()
    ) {
        return NetworkErrorMessage
    }

    val raw = sequenceOf(message, localizedMessage)
        .filterNotNull()
        .firstOrNull { it.isNotBlank() }
        .orEmpty()

    if (raw.looksLikeNetworkFailure()) return NetworkErrorMessage
    if (raw.looksLikeTechnicalHttpError()) return fallback

    return raw
        .takeIf { it.isNotBlank() && it.length <= MaxUserFacingErrorLength }
        ?: fallback
}

fun String.toUserFacingErrorMessage(
    fallback: String = "Đã xảy ra lỗi, vui lòng thử lại."
): String = when {
    looksLikeNetworkFailure() -> NetworkErrorMessage
    looksLikeTechnicalHttpError() -> fallback
    isBlank() -> fallback
    length > MaxUserFacingErrorLength -> fallback
    else -> this
}

private inline fun <reified T : Throwable> Throwable.hasCause(): Boolean {
    var current: Throwable? = this
    while (current != null) {
        if (current is T) return true
        current = current.cause
    }
    return false
}

private fun String.looksLikeNetworkFailure(): Boolean {
    val lower = lowercase()
    return lower.contains("unable to resolve host") ||
        lower.contains("no address associated with hostname") ||
        lower.contains("network is unreachable") ||
        lower.contains("failed to connect") ||
        lower.contains("connection refused") ||
        lower.contains("connection reset") ||
        lower.contains("connect timed out") ||
        lower.contains("timeout") ||
        lower.contains("java.net.unknownhostexception") ||
        lower.contains("java.net.connectexception") ||
        lower.contains("java.net.sockettimeoutexception") ||
        (lower.contains("http request to") && lower.contains("failed with message"))
}

private fun String.looksLikeTechnicalHttpError(): Boolean {
    val lower = lowercase()
    return lower.contains("http request to") ||
        lower.contains("https://") ||
        lower.contains("/rest/v1/") ||
        lower.contains("supabase.co")
}

private const val MaxUserFacingErrorLength = 96
private const val NetworkErrorMessage = "Không có kết nối mạng"
