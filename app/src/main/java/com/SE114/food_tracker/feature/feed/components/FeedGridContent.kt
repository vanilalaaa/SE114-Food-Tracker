package com.SE114.food_tracker.feature.feed.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.SE114.food_tracker.core.designsystem.theme.CardWhite
import com.SE114.food_tracker.core.designsystem.theme.HintGray
import com.SE114.food_tracker.core.designsystem.theme.LightPeach
import com.SE114.food_tracker.core.designsystem.theme.MainBackground
import com.SE114.food_tracker.core.designsystem.theme.MintGreen
import com.SE114.food_tracker.core.designsystem.theme.OrangeMain
import com.SE114.food_tracker.core.designsystem.theme.TextLabelGray
import com.SE114.food_tracker.core.designsystem.theme.TextPrimary
import com.SE114.food_tracker.data.local.dao.FeedPostDto
import com.SE114.food_tracker.feature.feed.FeedUiState
import com.SE114.food_tracker.feature.feed.feedFallbackIcon
import com.SE114.food_tracker.feature.feed.feedImageModelOrNull
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun FeedGridContent(
    uiState: FeedUiState,
    gridState: LazyGridState,
    onNavigateToFriend: () -> Unit,
    onPostClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        FeedHeader(
            postCount = uiState.posts.size,
            onNavigateToFriend = onNavigateToFriend
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when {
                uiState.posts.isEmpty() && uiState.isLoading -> FeedLoadingState()
                uiState.posts.isEmpty() -> FeedEmptyState(error = uiState.error)
                else -> FeedGrid(
                    posts = uiState.posts,
                    gridState = gridState,
                    onPostClick = onPostClick
                )
            }
        }
    }
}

@Composable
fun FeedPagingEffect(
    gridState: LazyGridState,
    uiState: FeedUiState,
    onLoadNextPage: () -> Unit
) {
    LaunchedEffect(gridState, uiState.posts.size, uiState.canLoadMore) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .distinctUntilChanged()
            .collect { lastVisibleIndex ->
                val shouldLoadMore = uiState.canLoadMore &&
                    lastVisibleIndex >= uiState.posts.lastIndex - 6

                if (shouldLoadMore) {
                    onLoadNextPage()
                }
            }
    }
}

@Composable
private fun FeedHeader(
    postCount: Int,
    onNavigateToFriend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Newsfeed",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
            Text(
                text = "$postCount bài viết",
                color = TextLabelGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        IconButton(onClick = onNavigateToFriend) {
            Icon(
                imageVector = Icons.Outlined.People,
                contentDescription = "Bạn bè",
                tint = TextPrimary
            )
        }
    }
}

@Composable
private fun FeedGrid(
    posts: List<FeedPostDto>,
    gridState: LazyGridState,
    onPostClick: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        items(
            items = posts,
            key = { it.postId }
        ) { post ->
            FeedPostTile(
                post = post,
                onClick = { onPostClick(post.postId) }
            )
        }
    }
}

@Composable
private fun FeedPostTile(
    post: FeedPostDto,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(LightPeach)
            .clickable(onClick = onClick)
    ) {
        val imageModel = post.imageUrl.feedImageModelOrNull()
        if (imageModel != null) {
            AsyncImage(
                model = imageModel,
                contentDescription = post.caption.ifBlank { post.itemName ?: "Bài viết" },
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            FeedTilePlaceholder(
                title = post.itemName ?: post.caption.ifBlank { "Bài viết món ăn" },
                fallbackIcon = feedFallbackIcon(post.categoryIconUrl, post.imageUrl)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.52f)
                        ),
                        startY = 110f
                    )
                )
        )

        FeedTileFooter(
            post = post,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(7.dp)
        )
    }
}

@Composable
private fun FeedTilePlaceholder(
    title: String,
    fallbackIcon: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(CardWhite.copy(alpha = 0.75f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = fallbackIcon,
                fontSize = 20.sp
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun FeedTileFooter(
    post: FeedPostDto,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = post.ownerName,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (post.caption.isNotBlank()) {
            Text(
                text = post.caption,
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FeedMetric(
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                },
                count = post.likeCount
            )
            FeedMetric(
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                },
                count = post.commentCount
            )
        }
    }
}

@Composable
private fun FeedMetric(
    icon: @Composable () -> Unit,
    count: Int
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.24f), RoundedCornerShape(8.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            icon()
        }
        Text(
            text = count.toString(),
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun FeedLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Đang tải bảng tin...",
            color = TextLabelGray,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun FeedEmptyState(error: String?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MintGreen.copy(alpha = 0.24f)
        ) {
            Icon(
                imageVector = Icons.Outlined.Image,
                contentDescription = null,
                tint = MintGreen,
                modifier = Modifier
                    .padding(18.dp)
                    .size(34.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = error ?: "Chưa có bài viết nào",
            color = if (error == null) TextPrimary else MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Đăng bài viết đầu tiên từ nhật ký hoặc ảnh của bạn.",
            color = HintGray,
            fontSize = 13.sp
        )
    }
}
