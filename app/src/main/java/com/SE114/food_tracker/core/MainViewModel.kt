package com.SE114.food_tracker.core

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.SE114.food_tracker.core.sync.LocalDataCleaner
import com.SE114.food_tracker.core.sync.SyncScheduler
import com.SE114.food_tracker.data.repository.AuthOutcome
import com.SE114.food_tracker.data.repository.AuthRepository
import com.SE114.food_tracker.data.repository.ChatRepository
import com.SE114.food_tracker.data.repository.FeedRepository
import com.SE114.food_tracker.data.repository.FriendRepository
import com.SE114.food_tracker.data.repository.ProfileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val chatRepository: ChatRepository,
    private val feedRepository: FeedRepository,
    private val friendRepository: FriendRepository,
    private val localDataCleaner: LocalDataCleaner,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val sessionStatus: StateFlow<SessionStatus?> = authRepository.currentSessionFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Non-null once a banned/soft-deleted account is signed out; carried to Login as the reason.
    private val _blockedReason = MutableStateFlow<String?>(null)
    val blockedReason: StateFlow<String?> = _blockedReason.asStateFlow()

    private var guardJob: Job? = null

    init {
        // Check on every session resolve (app launch with a restored session, or right after login).
        viewModelScope.launch {
            authRepository.currentSessionFlow()
                .filterIsInstance<SessionStatus.Authenticated>()
                .collect {
                    enforceActive()
                    // Auth is ready (login or restored session) — drain any chat messages queued
                    // offline or left PENDING by a previous session/crash.
                    chatRepository.enqueuePendingMessageSync()
                    SyncScheduler.triggerImmediateSync(context)
                }
        }
    }

    /** Re-checks the account on app resume; admin actions on another device take effect here. */
    fun recheckActive() {
        if (authRepository.hasSession()) {
            enforceActive()
            chatRepository.enqueuePendingMessageSync()
            SyncScheduler.triggerImmediateSync(context)
        }
    }

    private fun enforceActive() {
        guardJob?.cancel()
        guardJob = viewModelScope.launch {
            when (val outcome = profileRepository.amIActive()) {
                is AuthOutcome.Success ->
                    if (outcome.data) _blockedReason.value = null else blockAndSignOut()
                // Network/lookup failure: keep the user signed in; re-checked on the next resume.
                is AuthOutcome.Failure -> Unit
            }
        }
    }

    private suspend fun blockAndSignOut() {
        // Set the reason before sign-out so the session guard navigates to Login with it.
        _blockedReason.value = REASON_BLOCKED
        localDataCleaner.clearUserOwnedData()
        chatRepository.resetChatState()
        feedRepository.resetFeedRealtime()
        friendRepository.resetFriendshipRealtime()
        authRepository.signOut()
    }

    companion object {
        const val REASON_BLOCKED = "blocked"
    }
}
