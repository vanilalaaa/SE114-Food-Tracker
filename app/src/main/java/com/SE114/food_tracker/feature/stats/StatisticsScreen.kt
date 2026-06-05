package com.SE114.food_tracker.feature.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.core.designsystem.components.*
import com.SE114.food_tracker.feature.stats.components.*
import com.SE114.food_tracker.core.designsystem.theme.*

@Composable
fun StatisticsScreen() {
    val scrollState = rememberScrollState()

    // 4 tab thời gian: 0 = Ngày, 1 = Tuần, 2 = Tháng, 3 = Năm
    var selectedTab by remember { mutableStateOf(0) }

    // 2 tab nội dung: 0 = Chi tiêu, 1 = Phân tích
    var selectedContentTab by remember { mutableStateOf(0) }

    // Quản lý ngân sách lưu riêng biệt theo bảng Budget đề xuất
    var dailyBudget by remember { mutableStateOf(100000f) }
    var weeklyBudget by remember { mutableStateOf(500000f) }
    var monthlyBudget by remember { mutableStateOf(2000000f) }
    var yearlyBudget by remember { mutableStateOf(24000000f) }

    var showBudgetDialog by remember { mutableStateOf(false) }
    var budgetInput by remember { mutableStateOf("") }

    // Tính toán số liệu thực tế (Chỉ lấy các bữa ăn cá nhân, wallet_id IS NULL)
    val totalSpendingAmount = 120000f // Giả lập tổng chi hiện tại là 120K

    val currentBudget = when (selectedTab) {
        1 -> weeklyBudget
        2 -> monthlyBudget
        3 -> yearlyBudget
        else -> dailyBudget
    }

    val remainingBudget = currentBudget - totalSpendingAmount
    val isBudgetExceeded = remainingBudget < 0
    val isWeeklyMode = selectedTab != 0

    Scaffold(
        topBar = {
            StatisticsTopBar(
                dateText = when (selectedTab) {
                    1 -> "Tuần 23 (01/06 - 07/06)"
                    2 -> "Tháng 06/2026"
                    3 -> "Năm 2026"
                    else -> "03/04/2026"
                },
                onDateClick = {
                    budgetInput = currentBudget.toInt().toString()
                    showBudgetDialog = true
                },
                onNextClick = {},
                onPreviousClick = {}
            )
        },
        bottomBar = {
            BottomBar(onItemSelected = {})
        },
        containerColor = MainBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 4 Tab thời gian chọn kỳ qua DatePicker
            TimeRangeSelector(selectedTab = selectedTab, onTabSelected = { selectedTab = it })

            // 2 Tab nội dung
            TabRow(
                selectedTabIndex = selectedContentTab,
                containerColor = MainBackground,
                contentColor = StatPinkDark
            ) {
                Tab(
                    selected = selectedContentTab == 0,
                    onClick = { selectedContentTab = 0 },
                    text = { Text("Chi tiêu", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedContentTab == 1,
                    onClick = { selectedContentTab = 1 },
                    text = { Text("Phân tích", fontWeight = FontWeight.Bold) }
                )
            }

            // CẢNH BÁO TRỰC QUAN KHI VƯỢT NGÂN SÁCH (Theo đặc tả yêu cầu - Dòng nhắc nhở ngắn gọn)
            if (isBudgetExceeded) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF2F2)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚠️", fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
                        Text(
                            text = "Chi tiêu hiện tại đã vượt quá ngân sách kỳ này!",
                            color = Color(0xFF9B1C1C),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (selectedContentTab == 0) {
                // ================== CHI TIÊU ==================

                // SỬA ĐỔI: Bọc clickable lên toàn bộ Grid và truyền chuỗi giá trị thô gọn gàng vào 3 ô số liệu
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            budgetInput = currentBudget.toInt().toString()
                            showBudgetDialog = true
                        }
                ) {
                    StatisticsSummaryGrid(
                        totalMeals = "${currentBudget.toInt() / 1000}K ",
                        totalSpending = "${totalSpendingAmount.toInt() / 1000}K",
                        averagePerMeal = if (isBudgetExceeded) "Vượt ${-remainingBudget.toInt() / 1000}K 🚨" else "${remainingBudget.toInt() / 1000}K"
                    )
                }

                // Biểu đồ chi tiêu theo buổi (Cột)
                ChartCard(
                    title = "Chi tiêu theo buổi",
                    data = listOf(Pair("Sáng", 20), Pair("Trưa", 40), Pair("Chiều", 60), Pair("Tối", 0), Pair("Khuya", 0))
                )

                // Biểu đồ chi tiêu theo danh mục (Biểu đồ tròn thực tế)
                LocalDonutChartCard(
                    title = "Tỷ trọng chi tiêu theo danh mục",
                    categories = listOf(
                        DonutSegment("Mì & Phở", 70000f, Color(0xFFE8AEB4)),
                        DonutSegment("Cơm", 30000f, Color(0xFFFBE3B5)),
                        DonutSegment("Nước uống", 20000f, Color(0xFFAED9E0))
                    )
                )

                // Danh sách món ăn chi tiết
                PopularFoodCard(
                    foodList = listOf(PopularFoodData(name = "Phở Hà Nội", recordCount = "1"))
                )

                DetailCardSection(
                    dataGroups = listOf(
                        DayGroup(
                            dateLabel = "Thứ Sáu, 03/04",
                            meals = listOf(
                                MealRecord("15:20", "Phở Hà Nội", "Mì & Phở", "70k đ"),
                                MealRecord("18:45", "Cơm tấm", "Cơm", "30k đ")
                            )
                        )
                    ),
                    isWeeklyMode = isWeeklyMode
                )
            } else {
                // ================== PHÂN TÍCH ==================

                // Dự báo bằng biểu đồ xu hướng (Linear Regression) trực quan
                LocalLineTrendChartCard(
                    title = "Dự báo xu hướng (Linear Regression dữ liệu cũ)",
                    points = listOf(45f, 70f, 110f, 120f)
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = CardWhite,
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "PHÂN TÍCH CHUYÊN SÂU KỲ NÀY",
                            style = StatSectionTitleStyle,
                            color = StatPinkDark,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("💥 Kẻ hủy diệt ví tiền", fontWeight = FontWeight.Bold, color = TextPrimaryStat, fontSize = 14.sp)
                                Text("Món tốn tiền nhất của bạn", color = TextLabelGray, fontSize = 12.sp)
                            }
                            Text("Phở Hà Nội", fontWeight = FontWeight.Bold, color = StatPinkDark, fontSize = 14.sp)
                        }

                        Divider(color = MainBackground, thickness = 1.dp)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("🗂️ Top danh mục chi nhiều", fontWeight = FontWeight.Bold, color = TextPrimaryStat, fontSize = 14.sp)
                                Text("Danh mục chiếm tỷ trọng lớn nhất", color = TextLabelGray, fontSize = 12.sp)
                            }
                            Text("Mì & Phở", fontWeight = FontWeight.Bold, color = TextPrimaryStat, fontSize = 14.sp)
                        }

                        Divider(color = MainBackground, thickness = 1.dp)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("📈 So sánh với kỳ trước", fontWeight = FontWeight.Bold, color = TextPrimaryStat, fontSize = 14.sp)
                                Text("Biến động tổng chi tiêu", color = TextLabelGray, fontSize = 12.sp)
                            }
                            Text("+15.4% (Tăng)", fontWeight = FontWeight.Bold, color = Color(0xFFD39292), fontSize = 14.sp)
                        }
                    }
                }

                InsightCard(insightText = "Hệ thống nhận xét: Tốc độ chi dùng của bạn đang tăng nhẹ. Hãy giảm tần suất ăn danh mục Mì & Phở để kéo phần 'Còn lại' về mức an toàn.")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // --- DIALOG ĐẶT NGÂN SÁCH THEO TAB THỜI GIAN ---
    if (showBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = { Text("Đặt ngân sách " + when (selectedTab) { 1 -> "Tuần" 2 -> "Tháng" 3 -> "Năm" else -> "Ngày" }, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Hạn mức mong muốn (đ):")
                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { budgetInput = it },
                        label = { Text("Số tiền") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val value = budgetInput.toFloatOrNull() ?: 0f
                        when (selectedTab) {
                            1 -> weeklyBudget = value
                            2 -> monthlyBudget = value
                            3 -> yearlyBudget = value
                            else -> dailyBudget = value
                        }
                        showBudgetDialog = false
                    }
                ) { Text("Lưu lại") }
            },
            dismissButton = {
                Button(onClick = { showBudgetDialog = false }) { Text("Hủy") }
            }
        )
    }
}

// ================== COMPONENT CUSTOM BIỂU ĐỒ TRÒN ==================
data class DonutSegment(val label: String, val value: Float, val color: Color)

@Composable
fun LocalDonutChartCard(title: String, categories: List<DonutSegment>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = title, style = StatSectionTitleStyle, color = TextPrimaryStat, fontSize = 14.sp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val total = categories.sumOf { it.value.toDouble() }.toFloat()
                Canvas(modifier = Modifier.size(100.dp)) {
                    var startAngle = -90f
                    categories.forEach { segment ->
                        val sweepAngle = (segment.value / total) * 360f
                        drawArc(
                            color = segment.color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = 24f, cap = StrokeCap.Round)
                        )
                        startAngle += sweepAngle
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { segment ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(12.dp).background(segment.color, RoundedCornerShape(3.dp)))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "${segment.label} (${(segment.value/total*100).toInt()}%)", fontSize = 12.sp, color = TextLabelGray)
                        }
                    }
                }
            }
        }
    }
}

// ================== COMPONENT BIỂU ĐỒ ĐƯỜNG XU HƯỚNG ==================
@Composable
fun LocalLineTrendChartCard(title: String, points: List<Float>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = title, style = StatSectionTitleStyle, color = TextPrimaryStat, fontSize = 14.sp)

            Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                val widthInterval = size.width / (points.size - 1)
                val maxVal = points.maxOrNull() ?: 100f

                for (i in 0 until points.size - 1) {
                    val startX = i * widthInterval
                    val startY = size.height - (points[i] / maxVal * size.height * 0.7f)
                    val endX = (i + 1) * widthInterval
                    val endY = size.height - (points[i + 1] / maxVal * size.height * 0.7f)

                    drawLine(
                        color = if (i == points.size - 2) StatPinkDark else Color(0xFFAED9E0),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 6f
                    )
                }
            }
            Text(text = "👉 Điểm cuối thể hiện dự báo tuyến tính từ dữ liệu kỳ trước.", fontSize = 11.sp, color = TextLabelGray)
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun StatisticsScreenPreview() {
    FoodTrackerTheme {
        StatisticsScreen()
    }
}