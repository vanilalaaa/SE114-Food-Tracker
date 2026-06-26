package com.SE114.food_tracker.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.SE114.food_tracker.data.repository.AdminDashboardStats
import com.SE114.food_tracker.data.repository.AdminRepository
import com.SE114.food_tracker.data.repository.AuthError
import com.SE114.food_tracker.data.repository.AuthOutcome
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminDashboardUiState(
    val stats: AdminDashboardStats? = null,
    val isLoading: Boolean = true,
    val error: AuthError? = null
)

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AdminDashboardUiState())
    val state: StateFlow<AdminDashboardUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val outcome = adminRepository.dashboardStats()) {
                is AuthOutcome.Success ->
                    _state.update { it.copy(isLoading = false, stats = outcome.data) }
                is AuthOutcome.Failure ->
                    _state.update { it.copy(isLoading = false, error = outcome.error) }
            }
        }
    }
}
