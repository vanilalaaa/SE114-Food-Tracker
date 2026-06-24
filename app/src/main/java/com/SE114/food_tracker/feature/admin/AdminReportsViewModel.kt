package com.SE114.food_tracker.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.SE114.food_tracker.data.repository.AdminReport
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

enum class ReportStatusFilter(val value: String) {
    PENDING("pending"),
    RESOLVED("resolved"),
    DISMISSED("dismissed")
}

data class AdminReportsUiState(
    val filter: ReportStatusFilter = ReportStatusFilter.PENDING,
    val reports: List<AdminReport> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = false,
    val error: AuthError? = null,
    val busyIds: Set<String> = emptySet(),
    val actionError: AuthError? = null
)

@HiltViewModel
class AdminReportsViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AdminReportsUiState())
    val state: StateFlow<AdminReportsUiState> = _state.asStateFlow()

    init { reload() }

    fun onFilterChange(filter: ReportStatusFilter) {
        if (filter == _state.value.filter) return
        _state.update { it.copy(filter = filter) }
        reload()
    }

    fun reload() {
        val filter = _state.value.filter
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val outcome = adminRepository.listReports(filter.value, PAGE_SIZE, 0)) {
                is AuthOutcome.Success -> _state.update {
                    it.copy(
                        isLoading = false,
                        reports = outcome.data,
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
            when (val outcome =
                adminRepository.listReports(current.filter.value, PAGE_SIZE, current.reports.size)) {
                is AuthOutcome.Success -> _state.update {
                    it.copy(
                        isLoadingMore = false,
                        reports = it.reports + outcome.data,
                        canLoadMore = outcome.data.size >= PAGE_SIZE
                    )
                }
                is AuthOutcome.Failure ->
                    _state.update { it.copy(isLoadingMore = false, actionError = outcome.error) }
            }
        }
    }

    /** Resolve or dismiss [report]; when [alsoBanTarget] is set, ban the reported user too. */
    fun resolve(report: AdminReport, status: String, alsoBanTarget: Boolean) {
        if (report.id in _state.value.busyIds) return
        _state.update { it.copy(busyIds = it.busyIds + report.id) }
        viewModelScope.launch {
            try {
                if (alsoBanTarget) {
                    when (val ban = adminRepository.setBanned(report.targetId, true)) {
                        is AuthOutcome.Success -> Unit
                        is AuthOutcome.Failure -> {
                            _state.update { it.copy(actionError = ban.error) }
                            return@launch
                        }
                    }
                }
                when (val outcome = adminRepository.resolveReport(report.id, status)) {
                    // The report leaves the current filtered list once its status changes.
                    is AuthOutcome.Success ->
                        _state.update { st -> st.copy(reports = st.reports.filterNot { it.id == report.id }) }
                    is AuthOutcome.Failure ->
                        _state.update { it.copy(actionError = outcome.error) }
                }
            } finally {
                _state.update { it.copy(busyIds = it.busyIds - report.id) }
            }
        }
    }

    fun clearActionError() = _state.update { it.copy(actionError = null) }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
