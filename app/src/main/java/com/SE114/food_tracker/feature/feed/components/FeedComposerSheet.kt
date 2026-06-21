package com.SE114.food_tracker.feature.feed.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.SE114.food_tracker.core.designsystem.theme.CardWhite
import com.SE114.food_tracker.core.designsystem.theme.LightPeach
import com.SE114.food_tracker.core.designsystem.theme.MainBackground
import com.SE114.food_tracker.core.designsystem.theme.OrangeMain
import com.SE114.food_tracker.core.designsystem.theme.TextLabelGray
import com.SE114.food_tracker.core.designsystem.theme.TextPrimary
import com.SE114.food_tracker.data.local.dao.FeedSourceItemDto
import com.SE114.food_tracker.feature.feed.FeedUiState
import com.SE114.food_tracker.feature.feed.FeedVisibility

@Composable
fun FeedComposerSheet(
    uiState: FeedUiState,
    onPickImage: () -> Unit,
    onSelectSourceItem: (FeedSourceItemDto?) -> Unit,
    onCaptionChange: (String) -> Unit,
    onVisibilityChange: (FeedVisibility) -> Unit,
    onCreatePost: () -> Unit,
    onCancel: () -> Unit,
    onClearError: () -> Unit
) {
    val canCreate = !uiState.isCreatingPost &&
        (uiState.selectedSourceItem != null || uiState.pickedImageUri != null)

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tao bai viet",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Chon anh hoac mon trong nhat ky",
                        color = TextLabelGray,
                        fontSize = 13.sp
                    )
                }

                TextButton(onClick = onCancel) {
                    Text("Huy", color = TextLabelGray, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            FeedPickedSourcePreview(
                selectedSourceItem = uiState.selectedSourceItem,
                pickedImageUri = uiState.pickedImageUri,
                onPickImage = onPickImage
            )
        }

        item {
            OutlinedTextField(
                value = uiState.draftCaption,
                onValueChange = onCaptionChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Caption") },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(16.dp)
            )
        }

        item {
            FeedVisibilityPicker(
                selectedValue = uiState.draftVisibility,
                onVisibilityChange = onVisibilityChange
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tu nhat ky",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = onPickImage,
                    label = { Text("Chon anh") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }

        if (uiState.sourceItems.isEmpty()) {
            item {
                Text(
                    text = "Chua co mon nao trong nhat ky de dang.",
                    color = TextLabelGray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        } else {
            items(
                items = uiState.sourceItems,
                key = { it.itemId }
            ) { item ->
                FeedSourceItemRow(
                    item = item,
                    selected = uiState.selectedSourceItem?.itemId == item.itemId,
                    onClick = { onSelectSourceItem(item) }
                )
            }
        }

        uiState.error?.let { error ->
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.errorContainer,
                            RoundedCornerShape(14.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onClearError) {
                        Text("An")
                    }
                }
            }
        }

        item {
            Button(
                onClick = onCreatePost,
                enabled = canCreate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangeMain)
            ) {
                if (uiState.isCreatingPost) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = "Dang bai",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FeedPickedSourcePreview(
    selectedSourceItem: FeedSourceItemDto?,
    pickedImageUri: Uri?,
    onPickImage: () -> Unit
) {
    val previewModel = pickedImageUri ?: selectedSourceItem?.imageUrl
    val previewTitle = selectedSourceItem?.name ?: "Anh tu do"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardWhite, RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(74.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(LightPeach),
            contentAlignment = Alignment.Center
        ) {
            if (previewModel != null && previewModel.toString().isNotBlank()) {
                AsyncImage(
                    model = previewModel,
                    contentDescription = previewTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = null,
                    tint = OrangeMain,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = previewTitle,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (selectedSourceItem != null) "Tu nhat ky" else "Chua chon nguon",
                color = TextLabelGray,
                fontSize = 12.sp
            )
        }

        TextButton(onClick = onPickImage) {
            Text("Anh", color = OrangeMain, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FeedVisibilityPicker(
    selectedValue: String,
    onVisibilityChange: (FeedVisibility) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Quyen rieng tu",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FeedVisibility.values().forEach { visibility ->
                FilterChip(
                    selected = selectedValue == visibility.value,
                    onClick = { onVisibilityChange(visibility) },
                    label = { Text(visibility.label) }
                )
            }
        }
    }
}

@Composable
private fun FeedSourceItemRow(
    item: FeedSourceItemDto,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) LightPeach else CardWhite)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MainBackground),
            contentAlignment = Alignment.Center
        ) {
            if (!item.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Restaurant,
                    contentDescription = null,
                    tint = OrangeMain,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${item.price.toInt()} d",
                color = TextLabelGray,
                fontSize = 12.sp
            )
        }

        if (selected) {
            Text(
                text = "Da chon",
                color = OrangeMain,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
