package com.SE114.food_tracker.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.SE114.food_tracker.core.sync.SyncStatus
import com.SE114.food_tracker.data.local.entities.Category
import com.SE114.food_tracker.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ManagedCategory(
    val categoryId: String,
    val name: String,
    val iconUrl: String,
    val isSystem: Boolean,
    val isHidden: Boolean
)

/** Errors are surfaced as a type so the UI owns the localized strings (no hardcoded text here). */
enum class CategoryActionError { IN_USE, NAME_REQUIRED, UNAUTHENTICATED, UNKNOWN }

data class CategoryManagementUiState(
    val systemCategories: List<ManagedCategory> = emptyList(),
    val myCategories: List<ManagedCategory> = emptyList(),
    val error: CategoryActionError? = null
)

@HiltViewModel
class CategoryManagementViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _error = MutableStateFlow<CategoryActionError?>(null)

    val uiState: StateFlow<CategoryManagementUiState> =
        combine(categoryRepository.getAllCategories(), _error) { categories, error ->
            val managed = categories.map { it.toManaged() }
            CategoryManagementUiState(
                systemCategories = managed.filter { it.isSystem },
                myCategories = managed.filter { !it.isSystem },
                error = error
            )
        }.catch { emit(CategoryManagementUiState(error = CategoryActionError.UNKNOWN)) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoryManagementUiState())

    fun createCategory(name: String, iconUrl: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) { _error.value = CategoryActionError.NAME_REQUIRED; return }
        val ownerId = categoryRepository.getCurrentUserId()
        if (ownerId == null) { _error.value = CategoryActionError.UNAUTHENTICATED; return }
        viewModelScope.launch {
            runCatching {
                val now = System.currentTimeMillis()
                categoryRepository.insertCategory(
                    Category(
                        ownerId = ownerId,
                        name = trimmed,
                        iconUrl = iconUrl,
                        isHidden = false,
                        isSystem = false,
                        syncStatus = SyncStatus.PENDING.name,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }.onFailure { _error.value = CategoryActionError.UNKNOWN }
        }
    }

    fun editCategory(category: ManagedCategory, newName: String, newIconUrl: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) { _error.value = CategoryActionError.NAME_REQUIRED; return }
        viewModelScope.launch {
            runCatching {
                categoryRepository.updateCustomCategoryDetails(category.categoryId, trimmed, newIconUrl)
            }.onFailure { _error.value = CategoryActionError.UNKNOWN }
        }
    }

    fun toggleVisibility(category: ManagedCategory) {
        viewModelScope.launch {
            runCatching {
                categoryRepository.updateCategoryVisibility(category.categoryId, !category.isHidden)
            }.onFailure { _error.value = CategoryActionError.UNKNOWN }
        }
    }

    fun deleteCategory(category: ManagedCategory) {
        if (category.isSystem) return
        viewModelScope.launch {
            runCatching {
                // DB FK is RESTRICT; block deletion while any active item still references it.
                if (categoryRepository.countActiveItemsForCategory(category.categoryId) > 0) {
                    _error.value = CategoryActionError.IN_USE
                } else {
                    categoryRepository.softDeleteCategory(category.categoryId)
                }
            }.onFailure { _error.value = CategoryActionError.UNKNOWN }
        }
    }

    fun clearError() { _error.value = null }

    private fun Category.toManaged() = ManagedCategory(
        categoryId = categoryId,
        name = name,
        iconUrl = iconUrl,
        isSystem = isSystem,
        isHidden = isHidden
    )
}
