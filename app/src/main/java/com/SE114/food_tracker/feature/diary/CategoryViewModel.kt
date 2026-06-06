package com.SE114.food_tracker.feature.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.SE114.food_tracker.data.local.entities.Category
import com.SE114.food_tracker.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    val visibleCategories: StateFlow<List<DiaryCategory>> =
        categoryRepository.getVisibleCategories()
            .map { categories ->
                categories.map { category -> category.toDiaryCategory() }
            }
            .catch { throwable ->
                _error.value = throwable.message
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun addCategory(name: String, iconUrl: String) {
        val userId = categoryRepository.getCurrentUserId()
        if (userId == null) {
            _error.value = "User unauthenticated"
            return
        }

        viewModelScope.launch {
            runCatching {
                val now = System.currentTimeMillis()
                categoryRepository.insertCategory(
                    Category(
                        ownerId = userId,
                        name = name,
                        iconUrl = iconUrl,
                        isHidden = false,
                        isSystem = false,
                        syncStatus = "PENDING",
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }.onFailure { throwable ->
                _error.value = throwable.message
            }
        }
    }

    fun toggleVisibility(category: DiaryCategory) {
        viewModelScope.launch {
            runCatching {
                categoryRepository.updateCategory(
                    category.toEntity().copy(isHidden = !category.isHidden)
                )
            }.onFailure { throwable ->
                _error.value = throwable.message
            }
        }
    }

    fun deleteCategory(category: DiaryCategory) {
        if (category.isSystem) {
            _error.value = "System categories cannot be deleted"
            return
        }

        viewModelScope.launch {
            runCatching {
                categoryRepository.deleteCategory(category.toEntity())
            }.onFailure { throwable ->
                _error.value = throwable.message
            }
        }
    }

    private fun ensureAuthenticated(): Boolean =
        categoryRepository.getCurrentUserId() != null

    private fun Category.toDiaryCategory(): DiaryCategory =
        DiaryCategory(
            categoryId = categoryId,
            name = name,
            iconUrl = iconUrl,
            isHidden = isHidden,
            isSystem = isSystem
        )

    private fun DiaryCategory.toEntity(): Category =
        Category(
            categoryId = categoryId,
            name = name,
            iconUrl = iconUrl,
            isHidden = isHidden,
            isSystem = isSystem
        )
}
