package com.SE114.food_tracker.feature.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.SE114.food_tracker.core.sync.SyncStatus
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
            .map { categories -> categories.map { it.toDiaryCategory() } }
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
                        ownerId    = userId,
                        name       = name,
                        iconUrl    = iconUrl,
                        isHidden   = false,
                        isSystem   = false,
                        syncStatus = SyncStatus.PENDING.name,
                        createdAt  = now,
                        updatedAt  = now
                    )
                )
            }.onFailure { _error.value = it.message }
        }
    }

    fun editCategory(category: DiaryCategory, newName: String, newIconUrl: String) {
        if (category.isSystem) {
            _error.value = "Danh mục hệ thống không thể chỉnh sửa."
            return
        }
        val trimmedName = newName.trim()
        if (trimmedName.isBlank()) {
            _error.value = "Tên danh mục không được để trống."
            return
        }
        viewModelScope.launch {
            runCatching {
                val existing = categoryRepository.getCategoryByIdOneShot(category.categoryId)
                    ?: error("Không tìm thấy danh mục để cập nhật")
                categoryRepository.updateCategory(
                    existing.copy(
                        name       = trimmedName,
                        iconUrl    = newIconUrl,
                        syncStatus = SyncStatus.PENDING.name,
                        updatedAt  = System.currentTimeMillis()
                    )
                )
            }.onFailure { _error.value = it.message }
        }
    }

    fun toggleVisibility(category: DiaryCategory) {
        viewModelScope.launch {
            runCatching {
                val existing = categoryRepository.getCategoryByIdOneShot(category.categoryId)
                    ?: error("Không tìm thấy danh mục để cập nhật")
                categoryRepository.updateCategory(
                    existing.copy(
                        isHidden   = !existing.isHidden,
                        syncStatus = SyncStatus.PENDING.name,
                        updatedAt  = System.currentTimeMillis()
                    )
                )
            }.onFailure { _error.value = it.message }
        }
    }

    fun deleteCategory(category: DiaryCategory) {
        if (category.isSystem) {
            _error.value = "Danh mục hệ thống không thể xóa. Hãy ẩn nó thay thế."
            return
        }
        viewModelScope.launch {
            runCatching {
                val linkedCount = categoryRepository.countActiveItemsForCategory(category.categoryId)
                if (linkedCount > 0) {
                    _error.value = "Không thể xóa: còn $linkedCount món ăn đang dùng danh mục này."
                    return@runCatching
                }
                categoryRepository.softDeleteCategory(category.categoryId)
            }.onFailure { _error.value = it.message }
        }
    }

    fun clearError() { _error.value = null }

    private fun Category.toDiaryCategory(): DiaryCategory =
        DiaryCategory(
            categoryId = categoryId,
            name       = name,
            iconUrl    = iconUrl,
            isHidden   = isHidden,
            isSystem   = isSystem
        )

    private fun DiaryCategory.toEntity(): Category =
        Category(
            categoryId = categoryId,
            name       = name,
            iconUrl    = iconUrl,
            isHidden   = isHidden,
            isSystem   = isSystem
        )
}