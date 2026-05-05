package com.SE114.food_tracker.ui.screens.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DiaryHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Nhật ký", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.Whatshot, contentDescription = null, tint = Color.Red)
            Text("1", fontWeight = FontWeight.Bold)
    }
}