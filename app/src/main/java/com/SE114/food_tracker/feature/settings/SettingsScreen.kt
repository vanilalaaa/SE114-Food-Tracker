package com.SE114.food_tracker.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.SE114.food_tracker.R
import com.SE114.food_tracker.core.designsystem.components.AppButton
import com.SE114.food_tracker.core.designsystem.components.AppButtonVariant
import com.SE114.food_tracker.core.designsystem.components.AppScaffold
import com.SE114.food_tracker.core.designsystem.components.ConfirmDialog
import com.SE114.food_tracker.core.designsystem.theme.CardWhite
import com.SE114.food_tracker.core.designsystem.theme.FoodTrackerTheme
import com.SE114.food_tracker.core.designsystem.theme.HintGrayStat
import com.SE114.food_tracker.core.util.AppCurrency
import com.SE114.food_tracker.data.repository.Profile
import com.SE114.food_tracker.feature.settings.components.CurrencySelectionDialog

@Composable
fun SettingsScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onChangePassword: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    currencyViewModel: CurrencyViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val currencyState by currencyViewModel.uiState.collectAsStateWithLifecycle()

    SettingsContent(
        profile = profile,
        displayCurrency = currencyState.displayCurrency,
        currencies = currencyState.currencies,
        ratesStale = currencyState.ratesStale,
        onProfileClick = onNavigateToProfile,
        onCategoriesClick = onNavigateToCategories,
        onChangePasswordClick = onChangePassword,
        onCurrencyDialogOpen = currencyViewModel::refreshRates,
        onSelectCurrency = currencyViewModel::selectCurrency,
        onLogout = viewModel::logout
    )
}

@Composable
private fun SettingsContent(
    profile: Profile?,
    displayCurrency: AppCurrency,
    currencies: List<AppCurrency>,
    ratesStale: Boolean,
    onProfileClick: () -> Unit,
    onCategoriesClick: () -> Unit,
    onChangePasswordClick: () -> Unit,
    onCurrencyDialogOpen: () -> Unit,
    onSelectCurrency: (AppCurrency) -> Unit,
    onLogout: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }

    AppScaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            ProfileHeaderCard(profile = profile, onClick = onProfileClick)

            SettingsRow(
                icon = Icons.Outlined.Payments,
                title = stringResource(R.string.settings_currency),
                subtitle = "${displayCurrency.symbol} ${displayCurrency.code}",
                onClick = {
                    onCurrencyDialogOpen()
                    showCurrencyDialog = true
                }
            )

            SettingsRow(
                icon = Icons.Outlined.Category,
                title = stringResource(R.string.settings_category_manage),
                onClick = onCategoriesClick
            )

            SettingsRow(
                icon = Icons.Outlined.Lock,
                title = stringResource(R.string.settings_change_password),
                onClick = onChangePasswordClick
            )

            Spacer(Modifier.height(24.dp))

            AppButton(
                text = stringResource(R.string.settings_logout),
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth(),
                variant = AppButtonVariant.Destructive
            )
        }
    }

    if (showCurrencyDialog) {
        CurrencySelectionDialog(
            currencies = currencies,
            selected = displayCurrency,
            ratesStale = ratesStale,
            onSelect = onSelectCurrency,
            onDismissRequest = { showCurrencyDialog = false }
        )
    }

    if (showLogoutDialog) {
        ConfirmDialog(
            title = stringResource(R.string.settings_logout_confirm_title),
            body = stringResource(R.string.settings_logout_confirm_body),
            confirmLabel = stringResource(R.string.settings_logout_confirm_yes),
            cancelLabel = stringResource(R.string.settings_logout_confirm_cancel),
            onConfirm = {
                showLogoutDialog = false
                onLogout()
            },
            onDismiss = { showLogoutDialog = false },
            destructive = true
        )
    }
}

@Composable
private fun ProfileHeaderCard(profile: Profile?, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = CardWhite,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProfileAvatar(avatarUrl = profile?.avatarUrl, size = 56.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile?.displayName?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.settings_default_name),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = profile?.userId?.let { "@$it" }
                        ?: stringResource(R.string.settings_no_user_id),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.settings_open_profile),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProfileAvatar(avatarUrl: String?, size: androidx.compose.ui.unit.Dp) {
    val shape = Modifier.size(size).clip(CircleShape)
    if (avatarUrl.isNullOrBlank()) {
        Surface(modifier = Modifier.size(size), shape = CircleShape, color = HintGrayStat) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = CardWhite,
                modifier = Modifier.padding(12.dp)
            )
        }
    } else {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(avatarUrl).crossfade(true).build(),
            contentDescription = stringResource(R.string.settings_avatar_desc),
            contentScale = ContentScale.Crop,
            modifier = shape
        )
    }
}

/** Reusable settings option row. */
@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Surface(
        onClick = onClick,
        color = CardWhite,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsContentPreview() {
    FoodTrackerTheme {
        SettingsContent(
            profile = Profile(id = "u1", displayName = "An Nguyễn", userId = "an.nguyen", avatarUrl = null),
            displayCurrency = AppCurrency.VND,
            currencies = AppCurrency.entries,
            ratesStale = false,
            onProfileClick = {},
            onCategoriesClick = {},
            onChangePasswordClick = {},
            onCurrencyDialogOpen = {},
            onSelectCurrency = {},
            onLogout = {}
        )
    }
}
