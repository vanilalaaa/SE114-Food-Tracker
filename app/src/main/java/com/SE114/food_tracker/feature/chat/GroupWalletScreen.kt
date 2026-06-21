package com.SE114.food_tracker.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.SE114.food_tracker.core.designsystem.theme.*
import com.SE114.food_tracker.feature.chat.components.TransactionItem
import com.SE114.food_tracker.feature.chat.components.WalletTransactionDialog

// DATA CLASS ĐỂ ĐỊNH HƯỚNG DỮ LIỆU HIỂN THỊ TRÊN UI
data class GroupWalletUiTxModel(
    val id: String,
    val actorName: String,
    val type: String,
    val amount: Double,
    val note: String,
    val createdAt: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupWalletScreen(
    conversationId: String,
    viewModel: ChatViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {}
) {
    // 1. Theo dõi thông tin phòng chat Realtime từ local Room DB (Tên cuộc hội thoại thật)
    val conversationState by viewModel.getConversationState(conversationId)
        .collectAsState(initial = null)

    // 2. Lắng nghe lịch sử giao dịch thực tế lấy từ bảng wallet_transaction trên máy chủ về
    val realTransactions by viewModel.walletTransactions.collectAsState()

    // Tự động load số dư và lịch sử giao dịch thật từ server ngay khi khởi chạy màn hình
    LaunchedEffect(conversationId) {
        viewModel.loadWalletData(conversationId)
    }

    // Gán dữ liệu thật từ ViewModel
    val walletBalance = viewModel.walletBalance
    val walletName = conversationState?.name?.let { "Quỹ của $it" } ?: "Quỹ Nhóm Tài Chính"
    val hasWallet = conversationState?.walletId != null

    GroupWalletScreenContent(
        walletName = walletName,
        walletBalance = walletBalance,
        hasWallet = hasWallet,
        realTransactions = realTransactions,
        onBackClick = onBackClick,
        onConfirmTransaction = { amount: Double, isDeposit: Boolean, note: String ->
            viewModel.executeWalletTransaction(
                conversationId = conversationId,
                amount = amount,
                isDeposit = isDeposit,
                note = note
            )
        }
    )
}

// HÀM CONTENT RIÊNG BIỆT: CHỈ CHỨA GIAO DIỆN THUẦN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupWalletScreenContent(
    walletName: String,
    walletBalance: Double,
    hasWallet: Boolean,
    realTransactions: List<Map<String, kotlinx.serialization.json.JsonElement>>,
    onBackClick: () -> Unit,
    onConfirmTransaction: (Double, Boolean, String) -> Unit
) {
    var showTransactionDialog by remember { mutableStateOf<String?>(null) }
    var selectedFilter by remember { mutableStateOf("Tất cả") }

    // Chuyển đổi Map<String, JsonElement> từ Supabase sang Object giao diện UI
    val cleanTransactions = remember(realTransactions) {
        realTransactions.map { tx ->
            val txId = tx["id"]?.toString()?.replace("\"", "") ?: ""
            val type = tx["type"]?.toString()?.replace("\"", "") ?: "deposit"
            val amount = tx["amount"]?.toString()?.toDoubleOrNull() ?: 0.0
            val note = tx["note"]?.toString()?.replace("\"", "") ?: ""
            val createdAt = tx["created_at"]?.toString()?.replace("\"", "") ?: "Vừa xong"
            val actorName = if (amount > 0) "Thành viên" else "Admin nhóm"

            GroupWalletUiTxModel(txId, actorName, type, amount, note, createdAt)
        }
    }

    val filteredTransactions = cleanTransactions.filter {
        when (selectedFilter) {
            "Nộp tiền" -> it.type == "deposit"
            "Chi tiêu" -> it.type == "withdrawal" || it.type == "purchase"
            else -> true
        }
    }

    if (showTransactionDialog != null) {
        WalletTransactionDialog(
            transactionType = showTransactionDialog!!,
            onDismissRequest = { showTransactionDialog = null },
            onConfirm = { amount, note ->
                onConfirmTransaction(amount, showTransactionDialog == "deposit", note)
                showTransactionDialog = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Quỹ Nhóm Tài Chính",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MainBackground)
            )
        },
        containerColor = MainBackground
    ) { innerPadding ->

        if (!hasWallet) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("💰", fontSize = 48.sp)
                        Text(
                            text = "Cuộc trò chuyện này chưa được thiết lập Quỹ nhóm",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding()
                    )
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Khối 1: Card hiển thị tổng số dư quỹ
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = walletName,
                                fontSize = 14.sp,
                                color = TextLabelGray,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${String.format("%,.0f", walletBalance)} VND",
                                fontSize = 28.sp, fontWeight = FontWeight.Bold, color = StatPinkDark
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { showTransactionDialog = "deposit" },
                                    colors = ButtonDefaults.buttonColors(containerColor = LightGreenStat),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Text(
                                        "📥 Nộp tiền",
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Button(
                                    onClick = { showTransactionDialog = "withdrawal" },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatPinkLight),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Text(
                                        "📤 Rút quỹ",
                                        color = StatPinkDark,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Khối 2: Thanh Filter Chips lựa chọn bộ lọc phân loại
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Tất cả", "Nộp tiền", "Chi tiêu").forEach { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text(filter) },
                                shape = RoundedCornerShape(24.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StatPinkLight,
                                    selectedLabelColor = StatPinkDark,
                                    disabledContainerColor = CardWhite,
                                    disabledLabelColor = HintGray
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selectedFilter == filter,
                                    borderColor = Color(0xFFE0E0E0),
                                    selectedBorderColor = StatPinkDark,
                                    borderWidth = 1.dp,
                                    selectedBorderWidth = 1.dp
                                )
                            )
                        }
                    }
                }

                // Khối 3: Dòng tiêu đề nhỏ của khu vực Lịch sử
                item {
                    Text(
                        "Lịch sử giao dịch quỹ (Mới nhất)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextLabelGray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Khối 4: Hiển thị danh sách lịch sử giao dịch hoặc báo trống động
                if (filteredTransactions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Chưa có lịch sử giao dịch nào.",
                                color = HintGray,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    items(filteredTransactions, key = { it.id }) { tx ->
                        TransactionItem(
                            actorName = tx.actorName,
                            type = tx.type,
                            amount = tx.amount,
                            note = tx.note,
                            createdAt = tx.createdAt
                        )
                    }
                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun GroupWalletScreenPreview() {
    FoodTrackerTheme {
        GroupWalletScreenContent(
            walletName = "Quỹ của Nhóm Quỹ Thực Tế 🥑 (Preview)",
            walletBalance = 680000.0,
            hasWallet = true,
            realTransactions = emptyList(),
            onBackClick = {},
            onConfirmTransaction = { _, _, _ -> }
        )
    }
}