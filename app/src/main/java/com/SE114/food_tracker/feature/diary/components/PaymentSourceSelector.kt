package com.SE114.food_tracker.feature.diary.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.core.designsystem.theme.FoodTrackerTheme
import com.SE114.food_tracker.core.designsystem.theme.MintGreen
import com.SE114.food_tracker.data.repository.ChatRepository.WalletWithRole
import com.SE114.food_tracker.core.designsystem.theme.StatPinkDark


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentSourceSelector(
    availableWallets: List<WalletWithRole>,
    selectedWalletId: String?,          // null = personal
    onSelectionChange: (walletId: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val isGroupSelected = selectedWalletId != null
    var dropdownExpanded by remember { mutableStateOf(false) }

    // When switching back to personal, clear wallet selection
    fun selectPersonal() {
        dropdownExpanded = false
        onSelectionChange(null)
    }

    fun selectGroup() {
        if (availableWallets.isEmpty()) return
        // Pre-select first owner wallet if nothing selected yet
        if (selectedWalletId == null) {
            val firstOwner = availableWallets.firstOrNull { it.role == "owner" }
            onSelectionChange(firstOwner?.walletId)
        }
        dropdownExpanded = true
    }

    val selectedWallet = availableWallets.find { it.walletId == selectedWalletId }

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text       = "NGUỒN THANH TOÁN",
            fontSize   = 13.sp,
            fontWeight = FontWeight.Bold,
            color      = Color.Black
        )

        Spacer(Modifier.height(8.dp))

        // ── Pill toggle row ───────────────────────────────────────────────
        Row(
            modifier            = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PillChip(
                label     = "💵 Cá nhân",
                selected  = !isGroupSelected,
                color     = MintGreen,
                modifier  = Modifier.weight(1f),
                onClick   = ::selectPersonal
            )
            PillChip(
                label    = "💰 Quỹ nhóm",
                selected = isGroupSelected,
                color    = StatPinkDark,
                enabled  = availableWallets.isNotEmpty(),
                modifier = Modifier.weight(1f),
                onClick  = ::selectGroup
            )
        }

        if (availableWallets.isEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text     = "Bạn chưa tham gia quỹ nhóm nào",
                fontSize = 12.sp,
                color    = Color.Gray,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        // ── Animated wallet dropdown ──────────────────────────────────────
        AnimatedVisibility(
            visible = isGroupSelected && availableWallets.isNotEmpty(),
            enter   = expandVertically(),
            exit    = shrinkVertically()
        ) {
            Column {
                Spacer(Modifier.height(10.dp))

                ExposedDropdownMenuBox(
                    expanded         = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it }
                ) {
                    // Trigger field
                    Surface(
                        modifier        = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        color           = Color.White,
                        shape           = RoundedCornerShape(14.dp),
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier            = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment   = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text       = selectedWallet?.walletName ?: "Chọn quỹ nhóm",
                                fontSize   = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = if (selectedWallet != null) Color(0xFF333333) else Color.Gray
                            )
                            Icon(
                                imageVector        = Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint               = Color.Gray
                            )
                        }
                    }

                    ExposedDropdownMenu(
                        expanded         = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier         = Modifier.background(Color.White)
                    ) {
                        availableWallets.forEach { wallet ->
                            val isOwner  = wallet.role == "owner"
                            val rowAlpha = if (isOwner) 1f else 0.5f

                            DropdownMenuItem(
                                text = {
                                    Column(modifier = Modifier.alpha(rowAlpha)) {
                                        Text(
                                            text       = wallet.walletName,
                                            fontSize   = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color      = Color(0xFF333333)
                                        )
                                        if (!isOwner) {
                                            Text(
                                                text     = "Chỉ trưởng nhóm mới chi được từ quỹ này",
                                                fontSize = 11.sp,
                                                color    = Color(0xFFE57373)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    if (isOwner) {
                                        onSelectionChange(wallet.walletId)
                                        dropdownExpanded = false
                                    }
                                    // non-owner: click is silently ignored (item stays non-interactive)
                                },
                                enabled  = isOwner,
                                modifier = Modifier.alpha(rowAlpha)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PillChip(
    label:    String,
    selected: Boolean,
    color:    Color,
    modifier: Modifier = Modifier,
    enabled:  Boolean  = true,
    onClick:  () -> Unit
) {
    val bgColor     = if (selected && enabled) color else Color.White
    val textColor   = if (selected && enabled) Color.White else if (!enabled) Color.LightGray else color
    val borderColor = if (enabled) color else Color.LightGray

    Surface(
        modifier        = modifier
            .height(44.dp)
            .border(1.5.dp, borderColor, RoundedCornerShape(22.dp))
            .clickable(enabled = enabled, onClick = onClick),
        color           = bgColor,
        shape           = RoundedCornerShape(22.dp),
        shadowElevation = if (selected) 2.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text       = label,
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color      = textColor
            )
        }
    }
}

// ── Preview ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFFFFF5E4)
@Composable
private fun PaymentSourceSelectorPersonalPreview() {
    FoodTrackerTheme {
        PaymentSourceSelector(
            availableWallets  = listOf(
                WalletWithRole("w1", "Quỹ Ăn Trưa Nhóm 4", "owner"),
                WalletWithRole("w2", "Quỹ Đi Chơi", "member")
            ),
            selectedWalletId  = null,
            onSelectionChange = {},
            modifier          = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF5E4)
@Composable
private fun PaymentSourceSelectorGroupPreview() {
    FoodTrackerTheme {
        PaymentSourceSelector(
            availableWallets  = listOf(
                WalletWithRole("w1", "Quỹ Ăn Trưa Nhóm 4", "owner"),
                WalletWithRole("w2", "Quỹ Đi Chơi", "member")
            ),
            selectedWalletId  = "w1",
            onSelectionChange = {},
            modifier          = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF5E4)
@Composable
private fun PaymentSourceSelectorNoWalletPreview() {
    FoodTrackerTheme {
        PaymentSourceSelector(
            availableWallets  = emptyList(),
            selectedWalletId  = null,
            onSelectionChange = {},
            modifier          = Modifier.padding(16.dp)
        )
    }
}