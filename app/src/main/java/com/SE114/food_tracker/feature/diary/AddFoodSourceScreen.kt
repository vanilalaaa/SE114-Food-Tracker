package com.SE114.food_tracker.feature.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodSourceScreen(
    onBack: () -> Unit, onSourceSelected: () -> Unit
) {
    Scaffold(
        containerColor = Color(0xFFFFF5E4)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Thêm món ăn",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                IconButton(
                    onClick = onBack, modifier = Modifier.background(Color.Transparent, CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                }
            }

            Spacer(Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SourceCard(
                    label = "Chụp ảnh",
                    icon = Icons.Outlined.PhotoCamera,
                    modifier = Modifier.weight(1f),
                    onClick = onSourceSelected
                )
                SourceCard(
                    label = "Thư viện",
                    icon = Icons.Outlined.Image,
                    modifier = Modifier.weight(1f),
                    onClick = onSourceSelected
                )
            }

            Spacer(Modifier.height(40.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f), color = Color.LightGray.copy(alpha = 0.4f)
                )
                Text(
                    "HOẶC CHỌN NHANH",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight(400),
                    color = Color(0xFF000000)
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f), color = Color.LightGray.copy(alpha = 0.4f)
                )
            }

            Spacer(Modifier.height(24.dp))

            QuickActionItem(
                title = "Món ăn có sẵn",
                subtitle = "Chọn từ danh sách đã lưu",
                icon = Icons.Outlined.RestaurantMenu,
                iconBgColor = Color(0xFFF7C7BB)
            )
            Spacer(Modifier.height(16.dp))
            QuickActionItem(
                title = "Gần đây",
                subtitle = "2 giờ trước",
                icon = Icons.Outlined.History,
                iconBgColor = Color(0xFFFCE0BA)
            )
        }
    }
}

@Composable
fun SourceCard(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier
            .height(163.dp)
            .clickable { onClick() },
        color = Color(0xFFFCDFCF),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 4.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                color = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, modifier = Modifier.size(28.dp), tint = Color.Black)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun QuickActionItem(title: String, subtitle: String, icon: ImageVector, iconBgColor: Color) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(iconBgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(subtitle, color = Color.Gray, fontSize = 14.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=411dp,height=891dp")
@Composable
fun AddFoodSourcePreview() {
    AddFoodSourceScreen(onBack = {}, onSourceSelected = {})
}