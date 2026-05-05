package com.SE114.food_tracker.ui.screens.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.*
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.IconButton
import com.SE114.food_tracker.ui.theme.BottomBarBG
import com.SE114.food_tracker.ui.theme.CalendarHighlight
import com.SE114.food_tracker.ui.theme.DarkGreen
import com.SE114.food_tracker.ui.theme.FoodTrackerTheme
import com.SE114.food_tracker.ui.theme.LightGreenBG
import com.SE114.food_tracker.ui.theme.MainGreen
import com.SE114.food_tracker.ui.theme.LightGrayBG

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
            Icon(Icons.Default.Whatshot, contentDescription = null, tint = MainGreen)
            Text("1", color = MainGreen, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        }
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 2.dp) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("thg 4 2026", fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun OptionMenuSection() {
    var isMenuExpanded by remember { mutableStateOf(false) }
    var selectedSubMenu by remember { mutableIntStateOf(0) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(180.dp)
            .background(LightGrayBG, RoundedCornerShape(24.dp))
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .size(100.dp)
                .background(Color.LightGray, CircleShape)
        )

        IconButton(
            onClick = {
                isMenuExpanded = !isMenuExpanded
                if (!isMenuExpanded) selectedSubMenu = 0
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.Gray)
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
                    3 -> SubMenuContent(listOf("Trang tập", "Trang ghi"))
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
    Surface(
        modifier = Modifier.width(130.dp),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 8.dp,
        color = Color.White
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
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
                            tint = MainGreen
                        )
                    }
                }
            }
        }
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
        color = Color.White,
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
                        color = LightGreenBG,
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
                            Surface(
                                shape = CircleShape,
                                color = CalendarHighlight,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(date.toString(), color = Color.White, fontSize = 16.sp)
                                }
                            }
                        } else {
                            Text(date.toString(), color = Color.DarkGray, fontSize = 16.sp)
                        }

                        if (date == 3 || date == 4) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.LightGray)
                            )
                        }
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
                containerColor = LightGreenBG,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = DarkGreen)
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
        ) {
            DiaryHeader()

            OptionMenuSection()

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 16.dp)
            ) {
                CalendarSection()
            }
        }
    }
}

@Composable
fun FoodTrackerBottomBar() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 22.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(BottomBarBG)
    ) {
        NavigationBar(
            containerColor = BottomBarBG,
            tonalElevation = 0.dp,
            windowInsets = WindowInsets(0, 0, 0, 0),
            modifier = Modifier.height(70.dp)
        ) {
            val items = listOf("Nhật ký", "Thống kê", "Dinh dưỡng", "Chi tiêu", "Cài đặt")
            val icons = listOf(
                Icons.Default.MenuBook,
                Icons.Default.BarChart,
                Icons.Default.Eco,
                Icons.Default.Payments,
                Icons.Default.Settings
            )

            items.forEachIndexed { index, item ->
                NavigationBarItem(
                    selected = index == 0,
                    onClick = { },
                    label = {
                        Text(
                            item,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    },
                    icon = {
                        Icon(
                            icons[index],
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DarkGreen,
                        indicatorColor = LightGreenBG,
                        unselectedIconColor = DarkGreen,
                        selectedTextColor = DarkGreen
                    )
                )
            }
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