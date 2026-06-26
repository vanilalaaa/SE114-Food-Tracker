package com.SE114.food_tracker.feature.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.SE114.food_tracker.R
import com.SE114.food_tracker.core.designsystem.theme.*
import com.SE114.food_tracker.core.util.AppCurrency

/**
 * Display-currency picker. Choosing a currency only updates the display preference —
 * there is deliberately no batch conversion of stored amounts (that would corrupt history).
 */
@Composable
fun CurrencySelectionDialog(
    currencies: List<AppCurrency>,
    selected: AppCurrency,
    ratesStale: Boolean,
    onSelect: (AppCurrency) -> Unit,
    onDismissRequest: () -> Unit
) {
    // Selection is staged locally; the display currency only changes when the user confirms.
    var pending by remember(selected) { mutableStateOf(selected) }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = CardWhite,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.settings_currency_title),
                    style = AppTypography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (ratesStale) {
                    OutdatedRateWarningBanner()
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    currencies.forEach { currency ->
                        CurrencyRadioItem(
                            currencyCode = "${currency.symbol} ${currency.code}",
                            currencyName = currency.displayName,
                            isSelected = currency == pending,
                            onClick = { pending = currency }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                SettingActionButton(
                    text = stringResource(R.string.settings_currency_confirm),
                    onClick = {
                        onSelect(pending)
                        onDismissRequest()
                    }
                )
            }
        }
    }
}

@Preview
@Composable
fun CurrencySelectionDialogPreview() {
    FoodTrackerTheme {
        CurrencySelectionDialog(
            currencies = AppCurrency.entries,
            selected = AppCurrency.VND,
            ratesStale = true,
            onSelect = {},
            onDismissRequest = {}
        )
    }
}
