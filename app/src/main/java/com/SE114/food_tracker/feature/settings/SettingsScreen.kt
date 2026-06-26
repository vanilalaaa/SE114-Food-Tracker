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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
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
    onNavigateToAdmin: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    currencyViewModel: CurrencyViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val currencyState by currencyViewModel.uiState.collectAsStateWithLifecycle()

    SettingsContent(
        profile = profile,
        isAdmin = isAdmin,
        displayCurrency = currencyState.displayCurrency,
        currencies = currencyState.currencies,
        ratesStale = currencyState.ratesStale,
        onProfileClick = onNavigateToProfile,
        onCategoriesClick = onNavigateToCategories,
        onChangePasswordClick = onChangePassword,
        onAdminClick = onNavigateToAdmin,
        onCurrencyDialogOpen = currencyViewModel::refreshRates,
        onSelectCurrency = currencyViewModel::selectCurrency,
        onLogout = viewModel::logout
    )
}

@Composable
private fun SettingsContent(
    profile: Profile?,
    isAdmin: Boolean,
    displayCurrency: AppCurrency,
    currencies: List<AppCurrency>,
    ratesStale: Boolean,
    onProfileClick: () -> Unit,
    onCategoriesClick: () -> Unit,
    onChangePasswordClick: () -> Unit,
    onAdminClick: () -> Unit,
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

            ProfileInfoBlock(profile = profile, onClick = onProfileClick)

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

            if (isAdmin) {
                SettingsRow(
                    icon = Icons.Outlined.AdminPanelSettings,
                    title = stringResource(R.string.settings_admin),
                    onClick = onAdminClick
                )
            }

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

/** Centered profile block: tapping it (or the avatar's pencil) opens the profile editor. */
@Composable
private fun ProfileInfoBlock(profile: Profile?, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ProfileAvatar(avatarUrl = profile?.avatarUrl, size = 96.dp)
        Text(
            text = profile?.displayName?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.settings_default_name),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = profile?.userId?.takeIf { it.isNotBlank() }?.let { "@$it" }
                ?: stringResource(R.string.settings_no_user_id),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Circular avatar with an edit-pencil badge. Only the image is clipped, so the badge shows fully. */
@Composable
private fun ProfileAvatar(avatarUrl: String?, size: Dp) {
    Box(modifier = Modifier.size(size)) {
        Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(HintGrayStat)) {
            if (avatarUrl.isNullOrBlank()) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = CardWhite,
                    modifier = Modifier.align(Alignment.Center).size(size / 2)
                )
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(avatarUrl).crossfade(true).build(),
                    contentDescription = stringResource(R.string.settings_avatar_desc),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            border = androidx.compose.foundation.BorderStroke(2.dp, CardWhite),
            modifier = Modifier.align(Alignment.BottomEnd).size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = stringResource(R.string.settings_open_profile),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(6.dp)
            )
        }
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
            isAdmin = true,
            displayCurrency = AppCurrency.VND,
            currencies = AppCurrency.entries,
            ratesStale = false,
            onProfileClick = {},
            onCategoriesClick = {},
            onChangePasswordClick = {},
            onAdminClick = {},
            onCurrencyDialogOpen = {},
            onSelectCurrency = {},
            onLogout = {}
        )
    }
}
