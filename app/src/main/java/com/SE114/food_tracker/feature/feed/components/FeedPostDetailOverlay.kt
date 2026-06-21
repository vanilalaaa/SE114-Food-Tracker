package com.SE114.food_tracker.feature.feed.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.SE114.food_tracker.core.designsystem.theme.LightPeach
import com.SE114.food_tracker.core.designsystem.theme.OrangeMain
import com.SE114.food_tracker.data.local.dao.FeedCommentDto
import com.SE114.food_tracker.data.local.dao.FeedPostDto
import com.SE114.food_tracker.feature.feed.FeedUiState
import com.SE114.food_tracker.feature.feed.feedFallbackIcon
import com.SE114.food_tracker.feature.feed.feedImageModelOrNull
import com.SE114.food_tracker.feature.friend.components.ProfileAvatar
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FeedPostDetailOverlay(
    uiState: FeedUiState,
    onClose: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onSelectPostAt: (Int) -> Unit,
    onToggleLike: (String) -> Unit,
    onDeletePost: (String) -> Unit,
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
        var isCommentsSheetOpen by rememberSaveable { mutableStateOf(false) }

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
            color = Color.Black.copy(alpha = 0.88f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    FeedPostDetailPage(
                        post = posts[page],
                        currentUserId = uiState.currentUserId,
                        comments = if (page == uiState.selectedPostIndex) {
                            uiState.selectedComments
                        } else {
                            emptyList()
                        },
                        onToggleLike = onToggleLike,
                        onDeletePost = onDeletePost,
                        onOpenComments = { isCommentsSheetOpen = true },
                        onNavigateToProfile = { profileId ->
                            onClose()
                            onNavigateToProfile(profileId)
                        },
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

                if (isCommentsSheetOpen) {
                    FeedCommentsBottomSheet(
                        comments = uiState.selectedComments,
                        commentText = commentText,
                        onCommentTextChange = { commentText = it },
                        onNavigateToProfile = { profileId ->
                            isCommentsSheetOpen = false
                            onClose()
                            onNavigateToProfile(profileId)
                        },
                        onSend = {
                            val post = uiState.selectedPost
                            if (post != null && commentText.isNotBlank()) {
                                onAddComment(post.postId, commentText)
                                commentText = ""
                            }
                        },
                        onDismiss = { isCommentsSheetOpen = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedPostDetailPage(
    post: FeedPostDto,
    currentUserId: String,
    comments: List<FeedCommentDto>,
    onToggleLike: (String) -> Unit,
    onDeletePost: (String) -> Unit,
    onOpenComments: () -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.90f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(0.58f))

            FeedPostImageCard(
                post = post,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.82f)
                    .heightIn(max = 520.dp)
            )

            FeedAuthorBlock(
                post = post,
                onNavigateToProfile = onNavigateToProfile
            )

            Spacer(Modifier.weight(1f))

            FeedStoryActionBar(
                post = post,
                canDelete = post.ownerId == currentUserId,
                commentCount = comments.size,
                onToggleLike = onToggleLike,
                onDeletePost = onDeletePost,
                onOpenComments = onOpenComments
            )
        }
    }
}

@Composable
private fun FeedPostImageCard(
    post: FeedPostDto,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(34.dp))
            .background(Color(0xFF191713)),
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

        FeedPostTypeBadge(
            post = post,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(14.dp)
        )
    }
}

@Composable
private fun FeedPostTypeBadge(
    post: FeedPostDto,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.34f),
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = post.itemName ?: "Ảnh tự do",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun FeedAuthorBlock(
    post: FeedPostDto,
    onNavigateToProfile: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, start = 4.dp, end = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.clickable { onNavigateToProfile(post.ownerId) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            ProfileAvatar(
                avatarUrl = post.ownerAvatarUrl,
                hasStory = false,
                modifier = Modifier.size(42.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = post.ownerName,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (post.caption.isNotBlank()) {
            Text(
                text = post.caption,
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )
        }
    }
}

@Composable
private fun FeedStoryActionBar(
    post: FeedPostDto,
    canDelete: Boolean,
    commentCount: Int,
    onToggleLike: (String) -> Unit,
    onDeletePost: (String) -> Unit,
    onOpenComments: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .padding(start = 18.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Viết bình luận...",
            color = Color.White.copy(alpha = 0.72f),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onOpenComments)
        )

        IconButton(onClick = { onToggleLike(post.postId) }) {
            Icon(
                imageVector = if (post.isLikedByMe) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Thích",
                tint = if (post.isLikedByMe) Color(0xFFFF7A8A) else Color.White,
                modifier = Modifier.size(30.dp)
            )
        }

        IconButton(onClick = onOpenComments) {
            Box(contentAlignment = Alignment.TopEnd) {
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = "Bình luận",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
                if (commentCount > 0) {
                    Text(
                        text = commentCount.toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(OrangeMain, CircleShape)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }

        if (canDelete) {
            IconButton(onClick = { onDeletePost(post.postId) }) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Xóa bài viết",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedCommentsBottomSheet(
    comments: List<FeedCommentDto>,
    commentText: String,
    onCommentTextChange: (String) -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onSend: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF3F4248).copy(alpha = 0.96f),
        scrimColor = Color.Black.copy(alpha = 0.72f),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 14.dp, bottom = 8.dp)
                    .size(width = 44.dp, height = 4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.48f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(start = 18.dp, end = 18.dp, bottom = 18.dp)
        ) {
            FeedCommentsList(
                comments = comments,
                onNavigateToProfile = onNavigateToProfile,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 280.dp, max = 430.dp)
            )

            Spacer(Modifier.height(12.dp))

            FeedCommentInput(
                value = commentText,
                onValueChange = onCommentTextChange,
                onSend = onSend
            )
        }
    }
}

@Composable
private fun FeedCommentsList(
    comments: List<FeedCommentDto>,
    onNavigateToProfile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (comments.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Chưa có bình luận nào",
                color = Color.White.copy(alpha = 0.70f),
                fontSize = 13.sp
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(comments, key = { it.commentId }) { comment ->
            FeedCommentRow(
                comment = comment,
                onNavigateToProfile = onNavigateToProfile
            )
        }
    }
}

@Composable
private fun FeedCommentRow(
    comment: FeedCommentDto,
    onNavigateToProfile: (String) -> Unit
) {
    Row(verticalAlignment = Alignment.Top) {
        ProfileAvatar(
            avatarUrl = comment.avatarUrl,
            hasStory = false,
            modifier = Modifier
                .size(42.dp)
                .clickable { onNavigateToProfile(comment.userId) }
        )

        Spacer(Modifier.width(8.dp))

        Column(
            modifier = Modifier.clickable { onNavigateToProfile(comment.userId) }
        ) {
            Text(
                text = comment.displayName,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = comment.body,
                color = Color.White,
                fontSize = 15.sp
            )
            Text(
                text = "Trả lời",
                color = Color.White.copy(alpha = 0.36f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}

@Composable
private fun FeedCommentInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(Color.Black.copy(alpha = 0.22f))
            .padding(start = 16.dp, top = 4.dp, end = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(
                    text = "Viết bình luận...",
                    color = Color.White.copy(alpha = 0.58f)
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(26.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color.White,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )
        )
        IconButton(
            onClick = onSend,
            modifier = Modifier
                .size(46.dp)
                .background(Color.White.copy(alpha = 0.18f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Gửi",
                tint = Color.White
            )
        }
    }
}
