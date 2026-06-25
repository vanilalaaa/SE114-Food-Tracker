package com.SE114.food_tracker.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.SE114.food_tracker.data.repository.AdminRepository
import com.SE114.food_tracker.data.repository.AdminUser
import com.SE114.food_tracker.data.repository.AuthError
import com.SE114.food_tracker.data.repository.AuthOutcome
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUsersUiState(
    val search: String = "",
    val users: List<AdminUser> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = false,
    val error: AuthError? = null,
    val busyIds: Set<String> = emptySet(),
    val actionError: AuthError? = null
)

@HiltViewModel
class AdminUsersViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AdminUsersUiState())
    val state: StateFlow<AdminUsersUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init { reload() }

    fun onSearchChange(query: String) {
        _state.update { it.copy(search = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            reload()
        }
    }

    fun reload() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val outcome = adminRepository.listUsers(_state.value.search.trim(), PAGE_SIZE, 0)) {
                is AuthOutcome.Success -> _state.update {
                    it.copy(
                        isLoading = false,
                        users = outcome.data,
                        canLoadMore = outcome.data.size >= PAGE_SIZE
                    )
                }
                is AuthOutcome.Failure ->
                    _state.update { it.copy(isLoading = false, error = outcome.error) }
            }
        }
    }

    fun loadMore() {
        val current = _state.value
        if (current.isLoading || current.isLoadingMore || !current.canLoadMore) return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true) }
            when (val outcome = adminRepository.listUsers(current.search.trim(), PAGE_SIZE, current.users.size)) {
                is AuthOutcome.Success -> _state.update {
                    it.copy(
                        isLoadingMore = false,
                        users = it.users + outcome.data,
                        canLoadMore = outcome.data.size >= PAGE_SIZE
                    )
                }
                is AuthOutcome.Failure ->
                    _state.update { it.copy(isLoadingMore = false, actionError = outcome.error) }
            }
        }
    }

    fun setAdmin(user: AdminUser, admin: Boolean) =
        runUserAction(user.id) {
            when (val outcome = adminRepository.setAdmin(user.id, admin)) {
                is AuthOutcome.Success -> updateUser(user.id) { it.copy(isAdmin = admin) }
                is AuthOutcome.Failure -> _state.update { it.copy(actionError = outcome.error) }
            }
        }

    fun setBanned(user: AdminUser, banned: Boolean) =
        runUserAction(user.id) {
            when (val outcome = adminRepository.setBanned(user.id, banned)) {
                is AuthOutcome.Success -> updateUser(user.id) { it.copy(isBanned = banned) }
                is AuthOutcome.Failure -> _state.update { it.copy(actionError = outcome.error) }
            }
        }

    fun setDeleted(user: AdminUser, deleted: Boolean) =
        runUserAction(user.id) {
            when (val outcome = adminRepository.setDeleted(user.id, deleted)) {
                is AuthOutcome.Success -> updateUser(user.id) { it.copy(isDeleted = deleted) }
                is AuthOutcome.Failure -> _state.update { it.copy(actionError = outcome.error) }
            }
        }

    fun clearActionError() = _state.update { it.copy(actionError = null) }

    private fun runUserAction(userId: String, action: suspend () -> Unit) {
        if (userId in _state.value.busyIds) return
        _state.update { it.copy(busyIds = it.busyIds + userId) }
        viewModelScope.launch {
            try {
                action()
            } finally {
                _state.update { it.copy(busyIds = it.busyIds - userId) }
            }
        }
    }

    private fun updateUser(userId: String, transform: (AdminUser) -> AdminUser) =
        _state.update { st ->
            st.copy(users = st.users.map { if (it.id == userId) transform(it) else it })
        }

    private companion object {
        const val PAGE_SIZE = 20
        const val SEARCH_DEBOUNCE_MS = 400L
    }
}
