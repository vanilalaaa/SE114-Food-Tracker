package com.SE114.food_tracker.ui.screens.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.ui.theme.*
import com.SE114.food_tracker.data.local.entities.Category
import com.SE114.food_tracker.data.local.entities.Item

object DiaryMockData {
    val categories = listOf(
        Category(categoryId = 1, name = "Cơm", iconUrl = ""),
        Category(categoryId = 2, name = "Mì & Phở", iconUrl = ""),
        Category(categoryId = 3, name = "Đồ uống", iconUrl = "")
    )

    val items = listOf(
        Item(
            itemId = 1,
            categoryId = 2,
            name = "Phở Hà Nội",
            timeType = 1,
            price = 30000.0,
            entryDate = 1713830400000L
        )
    )

    const val STREAK = "1"
    const val DISPLAY_MONTH = "thg 4 2026"
}

@Composable
fun DiaryHeader(onMonthClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Nhật ký", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.Whatshot, contentDescription = null, tint = Orange)
            Text(
                DiaryMockData.STREAK, color = Orange, fontWeight = FontWeight.Bold, fontSize = 24.sp
            )
        }
        Surface(
            modifier = Modifier.clickable { onMonthClick() },
            shape = RoundedCornerShape(9999.dp),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(DiaryMockData.DISPLAY_MONTH, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun OptionMenuSection() {
    var isMenuExpanded by remember { mutableStateOf(false) }
    var selectedSubMenu by remember { mutableIntStateOf(0) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(26.dp,20.dp,26.dp,5.dp)
            .height(200.dp),
        shape = RoundedCornerShape(30.dp),
        color = LightPinkBG,
        shadowElevation = 20.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            IconButton(
                onClick = {
                    isMenuExpanded = !isMenuExpanded
                    if (!isMenuExpanded) selectedSubMenu = 0
                }, modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.Black)
            }

            if (isMenuExpanded) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 48.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (selectedSubMenu) {
                        1 -> SubMenuContent(DiaryMockData.categories.map { "${it.name} (1)" })
                        2 -> SubMenuContent(listOf("Đơn giản", "Nước"))
                        3 -> SubMenuContent(listOf("Trong hộp", "Trong lịch"))
                    }

                    Surface(
                        modifier = Modifier.width(150.dp),
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 8.dp,
                        color = Color.White
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            OptionMenuItem(
                                Icons.Default.FilterList,
                                "Lọc loại",
                                onClick = { selectedSubMenu = 1 })
                            OptionMenuItem(
                                Icons.Default.ColorLens,
                                "Đổi skin",
                                onClick = { selectedSubMenu = 2 })
                            OptionMenuItem(
                                Icons.Default.Straighten,
                                "Kích thước",
                                onClick = { selectedSubMenu = 3 })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OptionMenuItem(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, fontSize = 12.sp)
        }
        Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(14.dp))
    }
}

@Composable
fun SubMenuContent(items: List<String>) {
    var sliderValue1 by remember { mutableFloatStateOf(0.7f) }
    var sliderValue2 by remember { mutableFloatStateOf(0.7f) }

    Surface(
        modifier = Modifier.width(if (items.contains("Trang tập")) 180.dp else 130.dp),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 8.dp,
        color = Color.White
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (items.contains("Trang tập")) {
                Text(
                    "Kích thước",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                SizeSliderItem("Trong hộp", sliderValue1) { sliderValue1 = it }
                Spacer(Modifier.height(8.dp))
                SizeSliderItem("Trong lịch", sliderValue2) { sliderValue2 = it }
            } else {
                items.forEach { text ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text, fontSize = 12.sp)
                        if (text == "Nước" || text == "Đơn giản") {
                            Icon(
                                Icons.Default.Check,
                                null,
                                modifier = Modifier.size(14.dp),
                                tint = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SizeSliderItem(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.EditNote, null, modifier = Modifier.size(14.dp), tint = Color.Gray
                )
                Spacer(Modifier.width(4.dp))
                Text(label, fontSize = 11.sp, color = Color.Gray)
            }
            Text("${(value * 100).toInt()}%", fontSize = 11.sp, color = Color.Gray)
        }
        Slider(
            value = value, onValueChange = onValueChange, colors = SliderDefaults.colors(
                thumbColor = Color.DarkGray,
                activeTrackColor = Color.DarkGray,
                inactiveTrackColor = Color.LightGray
            ), modifier = Modifier.height(24.dp)
        )
    }
}

@Composable
fun CalendarSection(
    onDateClick: () -> Unit,
    onItemClick: (Item) -> Unit
) {
    val days = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
    val dates = (1..30).toList()
    val itemOnDay23 = DiaryMockData.items.firstOrNull()

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(26.dp, 15.dp, 26.dp, 35.dp),
        shape = RoundedCornerShape(25.dp),
        color = CalendarHighlight,
        shadowElevation = 10.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                days.forEach {
                    Text(
                        it,
                        color = DarkPink,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(5.dp), horizontalArrangement = Arrangement.spacedBy(4.dp),) {
                items(dates) { date ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(32.dp)
                        ) {
                            if (date == 23) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(LightGreen, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        date.toString(),
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Text(date.toString(), color = Color.DarkGray, fontSize = 14.sp)
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                        val hasData = (date == 23)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    if (hasData) Color.White else Color.Transparent,
                                    CircleShape
                                )
                                .clip(CircleShape)
                                .clickable(enabled = hasData) { itemOnDay23?.let { onItemClick(it) } },
                            contentAlignment = Alignment.Center
                        ) {
                            if (hasData) {
                                Icon(
                                    Icons.Default.Restaurant,
                                    null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Orange
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiaryScreen() {
    var showDetailSheet by remember { mutableStateOf(false) }
    var showEntryScreen by remember { mutableStateOf(false) }
    var showSourceScreen by remember { mutableStateOf(false) }
    var selectedItemForEdit by remember { mutableStateOf<Item?>(null) }

    Scaffold(
        bottomBar = { FoodTrackerBottomBar() },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedItemForEdit = null
                    showSourceScreen = true
                },
                containerColor = MintGreen,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFFFF5E4))
        ) {
            DiaryHeader(onMonthClick = {
                showDetailSheet = true
            })

            OptionMenuSection()
            Spacer(modifier = Modifier.height(16.dp))

            CalendarSection(
                onDateClick = {},
                onItemClick = { item ->
                    selectedItemForEdit = item
                    showEntryScreen = true
                }
            )
        }

        if (showDetailSheet) {
            DayDetailBottomSheet(
                onDismiss = { showDetailSheet = false },
                items = DiaryMockData.items,
                categories = DiaryMockData.categories,
                onEditItem = { item ->
                    selectedItemForEdit = item
                    showDetailSheet = false
                    showEntryScreen = true
                }
            )
        }

        if (showSourceScreen) {
            AddFoodSourceScreen(
                onBack = { showSourceScreen = false },
                onSourceSelected = {
                    showSourceScreen = false
                    showEntryScreen = true
                }
            )
        }

        if (showEntryScreen) {
            FoodEntryScreen(
                item = selectedItemForEdit,
                categories = DiaryMockData.categories,
                onDismiss = { showEntryScreen = false },
                onSave = { showEntryScreen = false }
            )
        }
    }
}

@Composable
fun FoodTrackerBottomBar() {
    NavigationBar(
        containerColor = Color(0xFFFFF5E4), tonalElevation = 0.dp, modifier = Modifier.height(80.dp) .padding(bottom = 8.dp)
    ) {
        val icons = listOf(
            Icons.Filled.MenuBook,
            Icons.Outlined.BarChart,
            Icons.Outlined.Payments,
            Icons.Outlined.Settings
        )
        icons.forEachIndexed { index, icon ->
            NavigationBarItem(
                selected = index == 0,
                onClick = { },
                icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(30.dp)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Black,
                    unselectedIconColor = Color.Gray,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=411dp,height=891dp")
@Composable
fun DiaryScreenPreview() {
    FoodTrackerTheme {
        DiaryScreen()
    }
}