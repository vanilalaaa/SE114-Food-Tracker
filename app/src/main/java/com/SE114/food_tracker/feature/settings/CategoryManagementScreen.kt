package com.SE114.food_tracker.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.SE114.food_tracker.R
import com.SE114.food_tracker.core.designsystem.components.AppButton
import com.SE114.food_tracker.core.designsystem.components.ConfirmDialog
import com.SE114.food_tracker.core.designsystem.theme.FoodTrackerTheme

@Composable
fun CategoryManagementScreen(
    onBack: () -> Unit,
    viewModel: CategoryManagementViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CategoryManagementContent(
        state = state,
        onBack = onBack,
        onToggleVisibility = viewModel::toggleVisibility,
        onCreate = viewModel::createCategory,
        onEdit = viewModel::editCategory,
        onDelete = viewModel::deleteCategory,
        onClearError = viewModel::clearError
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryManagementContent(
    state: CategoryManagementUiState,
    onBack: () -> Unit,
    onToggleVisibility: (ManagedCategory) -> Unit,
    onCreate: (String, String) -> Unit,
    onEdit: (ManagedCategory, String, String) -> Unit,
    onDelete: (ManagedCategory) -> Unit,
    onClearError: () -> Unit
) {
    var formTarget by remember { mutableStateOf<CategoryForm?>(null) }
    var pendingDelete by remember { mutableStateOf<ManagedCategory?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_category_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_category_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { GroupHeader(stringResource(R.string.settings_category_system_group)) }
                items(state.systemCategories, key = { it.categoryId }) { category ->
                    CategoryRow(
                        category = category,
                        onToggleVisibility = { onToggleVisibility(category) }
                    )
                }

                item { GroupHeader(stringResource(R.string.settings_category_my_group)) }
                if (state.myCategories.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.settings_category_empty_mine),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                } else {
                    items(state.myCategories, key = { it.categoryId }) { category ->
                        CategoryRow(
                            category = category,
                            onToggleVisibility = { onToggleVisibility(category) },
                            onEdit = { formTarget = CategoryForm(category) },
                            onDelete = { pendingDelete = category }
                        )
                    }
                }
            }

            AppButton(
                text = stringResource(R.string.settings_category_create),
                onClick = { formTarget = CategoryForm(null) },
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
        }
    }

    formTarget?.let { form ->
        CategoryFormDialog(
            existing = form.category,
            onDismiss = { formTarget = null },
            onSave = { name, icon ->
                val target = form.category
                if (target == null) onCreate(name, icon) else onEdit(target, name, icon)
                formTarget = null
            }
        )
    }

    pendingDelete?.let { target ->
        ConfirmDialog(
            title = stringResource(R.string.settings_category_delete_title),
            body = stringResource(R.string.settings_category_delete_body, target.name),
            confirmLabel = stringResource(R.string.settings_category_delete_confirm),
            cancelLabel = stringResource(R.string.settings_category_cancel),
            onConfirm = {
                onDelete(target)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
            destructive = true
        )
    }

    state.error?.let { error ->
        val message = when (error) {
            CategoryActionError.IN_USE -> stringResource(R.string.settings_category_in_use)
            CategoryActionError.NAME_REQUIRED -> stringResource(R.string.settings_category_name_required)
            CategoryActionError.UNAUTHENTICATED -> stringResource(R.string.settings_category_unauthenticated)
            CategoryActionError.UNKNOWN -> stringResource(R.string.settings_category_error_generic)
        }
        ConfirmDialog(
            title = stringResource(R.string.settings_category_error_title),
            body = message,
            confirmLabel = stringResource(R.string.settings_category_error_ok),
            cancelLabel = stringResource(R.string.settings_category_cancel),
            onConfirm = onClearError,
            onDismiss = onClearError
        )
    }
}

@Composable
private fun GroupHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun CategoryRow(
    category: ManagedCategory,
    onToggleVisibility: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = category.iconUrl, fontSize = 22.sp)
            Spacer(Modifier.size(12.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (category.isHidden) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (onEdit != null) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.settings_category_edit))
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.settings_category_delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    imageVector = if (category.isHidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = stringResource(
                        if (category.isHidden) R.string.settings_category_show else R.string.settings_category_hide
                    )
                )
            }
        }
    }
}

private data class CategoryForm(val category: ManagedCategory?)

private val PRESET_ICONS = listOf(
    "🍚", "🍜", "🥖", "🥤", "🍰", "🍡", "🍕", "🍔",
    "🍣", "🍦", "☕", "🍺", "🥗", "🍇", "🍩", "🌮"
)

@Composable
private fun CategoryFormDialog(
    existing: ManagedCategory?,
    onDismiss: () -> Unit,
    onSave: (name: String, icon: String) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var icon by remember { mutableStateOf(existing?.iconUrl ?: PRESET_ICONS.first()) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = stringResource(
                        if (existing == null) R.string.settings_category_create_title
                        else R.string.settings_category_edit_title
                    ),
                    style = MaterialTheme.typography.titleMedium
                )

                com.SE114.food_tracker.core.designsystem.components.AppTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.settings_category_name_label)
                )

                Text(
                    text = stringResource(R.string.settings_category_icon_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconPickerGrid(selected = icon, onSelect = { icon = it })

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.align(Alignment.End)) {
                    AppButton(
                        text = stringResource(R.string.settings_category_cancel),
                        onClick = onDismiss,
                        variant = com.SE114.food_tracker.core.designsystem.components.AppButtonVariant.Text
                    )
                    AppButton(
                        text = stringResource(R.string.settings_category_save),
                        onClick = { onSave(name.trim(), icon) },
                        enabled = name.isNotBlank()
                    )
                }
            }
        }
    }
}

@Composable
private fun IconPickerGrid(selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PRESET_ICONS.chunked(8).forEach { rowIcons ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowIcons.forEach { emoji ->
                    val isSelected = emoji == selected
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { onSelect(emoji) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoryManagementContentPreview() {
    FoodTrackerTheme {
        CategoryManagementContent(
            state = CategoryManagementUiState(
                systemCategories = listOf(
                    ManagedCategory("1", "Cơm", "🍚", isSystem = true, isHidden = false),
                    ManagedCategory("2", "Mì & Phở", "🍜", isSystem = true, isHidden = true)
                ),
                myCategories = listOf(
                    ManagedCategory("3", "Cà phê", "☕", isSystem = false, isHidden = false)
                )
            ),
            onBack = {},
            onToggleVisibility = {},
            onCreate = { _, _ -> },
            onEdit = { _, _, _ -> },
            onDelete = {},
            onClearError = {}
        )
    }
}
