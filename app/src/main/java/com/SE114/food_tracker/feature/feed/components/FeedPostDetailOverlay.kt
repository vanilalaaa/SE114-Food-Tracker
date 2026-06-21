package com.SE114.food_tracker.feature.feed.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.SE114.food_tracker.core.designsystem.theme.CardWhite
import com.SE114.food_tracker.core.designsystem.theme.LightPeach
import com.SE114.food_tracker.core.designsystem.theme.OrangeMain
import com.SE114.food_tracker.core.designsystem.theme.TextLabelGray
import com.SE114.food_tracker.core.designsystem.theme.TextPrimary
import com.SE114.food_tracker.data.local.dao.FeedCommentDto
import com.SE114.food_tracker.data.local.dao.FeedPostDto
import com.SE114.food_tracker.feature.feed.FeedUiState
import com.SE114.food_tracker.feature.feed.feedFallbackIcon
import com.SE114.food_tracker.feature.feed.feedImageModelOrNull
import com.SE114.food_tracker.feature.friend.components.ProfileAvatar
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeedPostDetailOverlay(
    uiState: FeedUiState,
    onClose: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onSelectPostAt: (Int) -> Unit,
    onToggleLike: (String) -> Unit,
    onAddComment: (String, String) -> Unit
) {
    val posts = uiState.posts
    val selectedIndex = uiState.selectedPostIndex

    if (posts.isEmpty() || selectedIndex !in posts.indices) return

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val pagerState = rememberPagerState(
            initialPage = selectedIndex.coerceIn(posts.indices),
            pageCount = { posts.size }
        )
        var commentText by rememberSaveable { mutableStateOf("") }

        LaunchedEffect(selectedIndex, posts.size) {
            val targetPage = selectedIndex.coerceIn(posts.indices)
            if (pagerState.currentPage != targetPage) {
                pagerState.scrollToPage(targetPage)
            }
        }

        LaunchedEffect(pagerState, posts.size) {
            snapshotFlow { pagerState.currentPage }
                .distinctUntilChanged()
                .collect { page -> onSelectPostAt(page) }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    FeedPostDetailPage(
                        post = posts[page],
                        comments = if (page == uiState.selectedPostIndex) {
                            uiState.selectedComments
                        } else {
                            emptyList()
                        },
                        commentText = if (page == uiState.selectedPostIndex) commentText else "",
                        onCommentTextChange = { commentText = it },
                        onToggleLike = onToggleLike,
                        onNavigateToProfile = { profileId ->
                            onClose()
                            onNavigateToProfile(profileId)
                        },
                        onAddComment = { postId, body ->
                            onAddComment(postId, body)
                            commentText = ""
                        }
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Đóng",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedPostDetailPage(
    post: FeedPostDto,
    comments: List<FeedCommentDto>,
    commentText: String,
    onCommentTextChange: (String) -> Unit,
    onToggleLike: (String) -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onAddComment: (String, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        FeedDetailImage(
            post = post,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        FeedDetailPanel(
            post = post,
            comments = comments,
            commentText = commentText,
            onCommentTextChange = onCommentTextChange,
            onToggleLike = onToggleLike,
            onNavigateToProfile = onNavigateToProfile,
            onAddComment = onAddComment
        )
    }
}

@Composable
private fun FeedDetailImage(
    post: FeedPostDto,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val imageModel = post.imageUrl.feedImageModelOrNull()
        if (imageModel != null) {
            AsyncImage(
                model = imageModel,
                contentDescription = post.caption.ifBlank { post.itemName ?: "Bài viết" },
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .background(LightPeach, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = feedFallbackIcon(post.categoryIconUrl, post.imageUrl),
                        fontSize = 36.sp
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = post.itemName ?: post.caption.ifBlank { "Bài viết món ăn" },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
private fun FeedDetailPanel(
    post: FeedPostDto,
    comments: List<FeedCommentDto>,
    commentText: String,
    onCommentTextChange: (String) -> Unit,
    onToggleLike: (String) -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onAddComment: (String, String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CardWhite,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.clickable { onNavigateToProfile(post.ownerId) }
                ) {
                    ProfileAvatar(
                        avatarUrl = post.ownerAvatarUrl,
                        hasStory = false
                    )
                }

                Spacer(Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.ownerName,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (post.caption.isNotBlank()) {
                        Text(
                            text = post.caption,
                            color = TextLabelGray,
                            fontSize = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                FeedActionButton(
                    selected = post.isLikedByMe,
                    count = post.likeCount,
                    onClick = { onToggleLike(post.postId) }
                )

                Spacer(Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = null,
                        tint = TextLabelGray,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = post.commentCount.toString(),
                        color = TextLabelGray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            FeedCommentsList(
                comments = comments,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            )

            Spacer(Modifier.height(10.dp))

            FeedCommentInput(
                value = commentText,
                onValueChange = onCommentTextChange,
                onSend = {
                    if (commentText.isNotBlank()) {
                        onAddComment(post.postId, commentText)
                    }
                }
            )
        }
    }
}

@Composable
private fun FeedActionButton(
    selected: Boolean,
    count: Int,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = if (selected) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Thích",
                tint = if (selected) OrangeMain else TextLabelGray,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = count.toString(),
            color = if (selected) OrangeMain else TextLabelGray,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun FeedCommentsList(
    comments: List<FeedCommentDto>,
    modifier: Modifier = Modifier
) {
    if (comments.isEmpty()) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFF7F4ED)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Chưa có bình luận nào",
                color = TextLabelGray,
                fontSize = 13.sp
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF7F4ED)),
        contentPadding = PaddingValues(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(comments, key = { it.commentId }) { comment ->
            FeedCommentRow(comment = comment)
        }
    }
}

@Composable
private fun FeedCommentRow(comment: FeedCommentDto) {
    Column {
        Text(
            text = comment.displayName,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
        Text(
            text = comment.body,
            color = TextLabelGray,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun FeedCommentInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Viết bình luận...") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        IconButton(
            onClick = onSend,
            modifier = Modifier
                .size(48.dp)
                .background(OrangeMain, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Gửi",
                tint = Color.White
            )
        }
    }
}
