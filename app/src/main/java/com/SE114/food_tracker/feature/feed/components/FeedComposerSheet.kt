package com.SE114.food_tracker.feature.feed.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import com.SE114.food_tracker.core.designsystem.theme.DarkPink
import com.SE114.food_tracker.core.designsystem.theme.LightPeach
import com.SE114.food_tracker.core.designsystem.theme.MainBackground
import com.SE114.food_tracker.core.designsystem.theme.MintGreen
import com.SE114.food_tracker.core.designsystem.theme.StatPinkLight
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

    Column(
        modifier = Modifier
            .fillMaxHeight(0.90f)
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Tạo bài viết",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Chọn ảnh hoặc món trong nhật ký",
                    color = TextLabelGray,
                    fontSize = 13.sp
                )
            }

            TextButton(onClick = onCancel) {
                Text("Hủy", color = TextLabelGray, fontWeight = FontWeight.Bold)
            }
        }

        FeedPickedSourcePreview(
            selectedSourceItem = uiState.selectedSourceItem,
            pickedImageUri = uiState.pickedImageUri,
            onPickImage = onPickImage
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = CardWhite.copy(alpha = 0.96f),
            shadowElevation = 5.dp
        ) {
            OutlinedTextField(
                value = uiState.draftCaption,
                onValueChange = onCaptionChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Chú thích", color = TextLabelGray) },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = DarkPink
                )
            )
        }

        FeedVisibilityPicker(
            selectedValue = uiState.draftVisibility,
            onVisibilityChange = onVisibilityChange
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Từ nhật ký",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Surface(
                onClick = onPickImage,
                shape = RoundedCornerShape(16.dp),
                color = CardWhite.copy(alpha = 0.96f),
                shadowElevation = 4.dp
            ) {
            Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhotoLibrary,
                    contentDescription = null,
                    tint = MintGreen,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Chọn ảnh",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.sourceItems.isEmpty()) {
                item {
                    Text(
                        text = "Chưa có món nào trong nhật ký để đăng.",
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
        }

        uiState.error?.let { error ->
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
                    Text("Ẩn")
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = Color.Transparent,
            shadowElevation = 0.dp
        ) {
            Button(
                onClick = onCreatePost,
                enabled = canCreate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MintGreen,
                    disabledContainerColor = MintGreen.copy(alpha = 0.48f),
                    disabledContentColor = TextPrimary.copy(alpha = 0.54f)
                )
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
                text = "Đăng bài",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun FeedPickedSourcePreview(
    selectedSourceItem: FeedSourceItemDto?,
    pickedImageUri: Uri?,
    onPickImage: () -> Unit
) {
    val previewModel = pickedImageUri ?: selectedSourceItem?.imageUrl
    val previewTitle = selectedSourceItem?.name ?: "Ảnh tự do"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = CardWhite.copy(alpha = 0.98f),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
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
            } else if (selectedSourceItem != null) {
                Text(
                    text = selectedSourceItem?.categoryIconUrl?.takeIf { it.isNotBlank() } ?: "🍱",
                    fontSize = 28.sp
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = null,
                    tint = DarkPink,
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
                text = if (selectedSourceItem != null) "Từ nhật ký" else "Chưa chọn nguồn",
                color = TextLabelGray,
                fontSize = 12.sp
            )
        }

        TextButton(onClick = onPickImage) {
            Text("Ảnh", color = DarkPink, fontWeight = FontWeight.Bold)
        }
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
            text = "Quyền riêng tư",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FeedVisibility.values().forEach { visibility ->
                val selected = selectedValue == visibility.value
                Surface(
                    onClick = { onVisibilityChange(visibility) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) StatPinkLight else CardWhite.copy(alpha = 0.94f),
                    shadowElevation = if (selected) 5.dp else 3.dp
                ) {
                    Text(
                        text = visibility.label,
                        color = if (selected) DarkPink else TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 17.dp, vertical = 10.dp)
                    )
                }
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
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (selected) StatPinkLight else CardWhite.copy(alpha = 0.98f),
        shadowElevation = if (selected) 6.dp else 3.dp
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
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
                    Text(
                        text = item.categoryIconUrl?.takeIf { it.isNotBlank() } ?: "🍱",
                        fontSize = 24.sp
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
                    text = "${item.price.toInt()} đ",
                    color = TextLabelGray,
                    fontSize = 12.sp
                )
            }

            if (selected) {
                Text(
                    text = "Đã chọn",
                    color = DarkPink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
