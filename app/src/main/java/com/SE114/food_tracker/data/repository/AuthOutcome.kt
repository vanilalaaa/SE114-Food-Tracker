package com.SE114.food_tracker.data.repository

sealed interface AuthOutcome<out T> {
    data class Success<T>(val data: T) : AuthOutcome<T>
    data class Failure(val error: AuthError) : AuthOutcome<Nothing>
}
