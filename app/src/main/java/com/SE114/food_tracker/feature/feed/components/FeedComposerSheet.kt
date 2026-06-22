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
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Send
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
import androidx.compose.ui.graphics.vector.ImageVector
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
    onTakePhoto: () -> Unit,
    onSelectSourceItem: (FeedSourceItemDto?) -> Unit,
    onFreeImageTitleChange: (String) -> Unit,
    onCaptionChange: (String) -> Unit,
    onVisibilityChange: (FeedVisibility) -> Unit,
    onCreatePost: () -> Unit,
    onCancel: () -> Unit,
    onClearError: () -> Unit
) {
    val hasRequiredContent = uiState.selectedSourceItem != null ||
        (uiState.pickedImageUri != null && uiState.draftFreeImageTitle.isNotBlank())
    val canCreate = !uiState.isCreatingPost && hasRequiredContent

    Column(
        modifier = Modifier
            .fillMaxHeight(0.90f)
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        FeedComposerHeader(onCancel = onCancel)

        FeedPickedSourcePreview(
            selectedSourceItem = uiState.selectedSourceItem,
            pickedImageUri = uiState.pickedImageUri,
            freeImageTitle = uiState.draftFreeImageTitle,
            onFreeImageTitleChange = onFreeImageTitleChange
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
                colors = composerTextFieldColors()
            )
        }

        FeedVisibilityPicker(
            selectedValue = uiState.draftVisibility,
            onVisibilityChange = onVisibilityChange
        )

        FeedSourceHeader()

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
            FeedComposerError(
                error = error,
                onClearError = onClearError
            )
        }

        FeedComposerActionBar(
            showCreate = hasRequiredContent,
            isCreatingPost = uiState.isCreatingPost,
            onPickImage = onPickImage,
            onTakePhoto = onTakePhoto,
            onCreatePost = { if (canCreate) onCreatePost() }
        )

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun FeedComposerHeader(onCancel: () -> Unit) {
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
}

@Composable
private fun FeedPickedSourcePreview(
    selectedSourceItem: FeedSourceItemDto?,
    pickedImageUri: Uri?,
    freeImageTitle: String,
    onFreeImageTitleChange: (String) -> Unit
) {
    val previewModel = pickedImageUri ?: selectedSourceItem?.imageUrl
    val isFreeImage = selectedSourceItem == null

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
                        contentDescription = selectedSourceItem?.name ?: freeImageTitle.ifBlank { "Ảnh tự do" },
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (selectedSourceItem != null) {
                    Text(
                        text = selectedSourceItem.categoryIconUrl?.takeIf { it.isNotBlank() } ?: "🍱",
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
                if (isFreeImage) {
                    OutlinedTextField(
                        value = freeImageTitle,
                        onValueChange = onFreeImageTitleChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Tên loại ảnh", color = TextLabelGray) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = composerTextFieldColors()
                    )
                    Text(
                        text = if (pickedImageUri == null) "Chưa chọn nguồn" else "Ảnh tự do",
                        color = TextLabelGray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                } else {
                    Text(
                        text = selectedSourceItem?.name.orEmpty(),
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Từ nhật ký",
                        color = TextLabelGray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedSourceHeader() {
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
    }
}

@Composable
private fun FeedComposerActionBar(
    showCreate: Boolean,
    isCreatingPost: Boolean,
    onPickImage: () -> Unit,
    onTakePhoto: () -> Unit,
    onCreatePost: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            FeedRoundIconButton(
                icon = Icons.Outlined.PhotoLibrary,
                contentDescription = "Chọn ảnh từ thư viện",
                size = 52,
                containerColor = CardWhite.copy(alpha = 0.96f),
                contentColor = DarkPink,
                onClick = onPickImage
            )
        }

        FeedRoundIconButton(
            icon = Icons.Outlined.PhotoCamera,
            contentDescription = "Chụp ảnh",
            size = 68,
            containerColor = MintGreen,
            contentColor = Color.White,
            onClick = onTakePhoto
        )

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd
        ) {
            if (showCreate) {
                FeedRoundIconButton(
                    icon = Icons.Outlined.Send,
                    contentDescription = "Đăng bài",
                    size = 52,
                    containerColor = DarkPink,
                    contentColor = Color.White,
                    onClick = onCreatePost,
                    loading = isCreatingPost
                )
            } else {
                Spacer(Modifier.size(52.dp))
            }
        }
    }
}

@Composable
private fun FeedRoundIconButton(
    icon: ImageVector,
    contentDescription: String,
    size: Int,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    loading: Boolean = false
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = containerColor,
        shadowElevation = 4.dp,
        modifier = Modifier.size(size.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (loading) {
                CircularProgressIndicator(
                    color = contentColor,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = contentColor,
                    modifier = Modifier.size((size * 0.42f).dp)
                )
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
private fun FeedComposerError(
    error: String,
    onClearError: () -> Unit
) {
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

@Composable
private fun FeedSourceItemRow(
    item: FeedSourceItemDto,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
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

@Composable
private fun composerTextFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color.Transparent,
        unfocusedBorderColor = Color.Transparent,
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        cursorColor = DarkPink
    )
