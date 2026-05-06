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

@Composable
fun DiaryHeader() {
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
            Text("1", color = Orange, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        }
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 2.dp) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("thg 4 2026", fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
            .padding(horizontal = 24.dp)
            .height(180.dp),
        shape = RoundedCornerShape(32.dp),
        color = LightPinkBG,
        shadowElevation = 4.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            IconButton(
                onClick = {
                    isMenuExpanded = !isMenuExpanded
                    if (!isMenuExpanded) selectedSubMenu = 0
                },
                modifier = Modifier
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
                        1 -> SubMenuContent(listOf("Tất cả", "Cơm (1)", "Mì & Phở (1)"))
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
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
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
                    Icons.Default.EditNote,
                    null,
                    modifier = Modifier.size(14.dp),
                    tint = Color.Gray
                )
                Spacer(Modifier.width(4.dp))
                Text(label, fontSize = 11.sp, color = Color.Gray)
            }
            Text("${(value * 100).toInt()}%", fontSize = 11.sp, color = Color.Gray)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(
                thumbColor = Color.DarkGray,
                activeTrackColor = Color.DarkGray,
                inactiveTrackColor = Color.LightGray
            ),
            modifier = Modifier.height(24.dp)
        )
    }
}

@Composable
fun CalendarSection() {
    val days = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
    val dates = (1..30).toList()

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(32.dp),
        color = CalendarHighlight,
        shadowElevation = 2.dp
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
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly,
                userScrollEnabled = false
            ) {
                items(dates) { date ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (date == 23) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(LightGreen, CircleShape)
                            ) {
                                Text(
                                    date.toString(),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Text(date.toString(), color = Color.DarkGray, fontSize = 14.sp)
                        }

                        Spacer(Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.White, CircleShape)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DiaryScreen() {
    Scaffold(
        bottomBar = { FoodTrackerBottomBar() },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {},
                containerColor = MintGreen,
                shape = CircleShape,
                modifier = Modifier.offset(y = 5.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
            }
        }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(Color.White)) {
            DiaryHeader()
            OptionMenuSection()
            Spacer(modifier = Modifier.height(16.dp))
            CalendarSection()
        }
    }
}

@Composable
fun FoodTrackerBottomBar() {
    NavigationBar(
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        modifier = Modifier.height(80.dp)
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
                icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp)) },
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