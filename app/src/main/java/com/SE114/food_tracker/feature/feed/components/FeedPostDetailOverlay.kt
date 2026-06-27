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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalDensity
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
import kotlin.math.max

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FeedPostDetailOverlay(
    uiState: FeedUiState,
    onClose: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onSelectPostAt: (Int) -> Unit,
    onToggleLike: (String) -> Unit,
    onHidePost: (String) -> Unit,
    onDownloadPost: (FeedPostDto) -> Unit,
    onDeletePost: (String) -> Unit,
    onAddComment: (String, String, String?) -> Unit,
    onEditComment: (String, String) -> Unit,
    onDeleteComment: (String) -> Unit,
    onSetCommentHidden: (String, Boolean) -> Unit,
    onClearError: () -> Unit
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
        var replyingToCommentId by rememberSaveable { mutableStateOf<String?>(null) }
        var replyingToDisplayName by rememberSaveable { mutableStateOf<String?>(null) }
        var replyingToBody by rememberSaveable { mutableStateOf<String?>(null) }
        var editingCommentId by rememberSaveable { mutableStateOf<String?>(null) }
        var editingCommentPreview by rememberSaveable { mutableStateOf<String?>(null) }
        var pendingScrollCommentId by rememberSaveable { mutableStateOf<String?>(null) }
        var pendingScrollToBottom by rememberSaveable { mutableStateOf(false) }
        var isCommentsSheetOpen by rememberSaveable { mutableStateOf(false) }
        var pendingDeletePostId by rememberSaveable { mutableStateOf<String?>(null) }
        var actionPost by remember { mutableStateOf<FeedPostDto?>(null) }
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(uiState.error) {
            val error = uiState.error
            if (error != null) {
                snackbarHostState.showSnackbar(error)
                onClearError()
            }
        }

        LaunchedEffect(selectedIndex, posts.size) {
            val targetPage = selectedIndex.coerceIn(posts.indices)
            if (pagerState.currentPage != targetPage) {
                pagerState.scrollToPage(targetPage)
            }
        }

        LaunchedEffect(pagerState, posts.size) {
            snapshotFlow { pagerState.currentPage }
                .distinctUntilChanged()
                .collect { page ->
                    replyingToCommentId = null
                    replyingToDisplayName = null
                    replyingToBody = null
                    editingCommentId = null
                    editingCommentPreview = null
                    pendingScrollCommentId = null
                    pendingScrollToBottom = false
                    onSelectPostAt(page)
                }
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
                        onOpenPostActions = { post -> actionPost = post },
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

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
                )

                if (isCommentsSheetOpen) {
                    FeedCommentsBottomSheet(
                        comments = uiState.selectedComments,
                        currentUserId = uiState.currentUserId,
                        postOwnerId = uiState.selectedPost?.ownerId.orEmpty(),
                        commentText = commentText,
                        replyingToDisplayName = replyingToDisplayName,
                        replyingToBody = replyingToBody,
                        isEditingComment = editingCommentId != null,
                        editingCommentPreview = editingCommentPreview,
                        scrollToCommentId = pendingScrollCommentId,
                        scrollToBottom = pendingScrollToBottom,
                        onScrollHandled = {
                            pendingScrollCommentId = null
                            pendingScrollToBottom = false
                        },
                        onCommentTextChange = { commentText = it },
                        onCancelReply = {
                            replyingToCommentId = null
                            replyingToDisplayName = null
                            replyingToBody = null
                            editingCommentId = null
                            editingCommentPreview = null
                        },
                        onNavigateToProfile = { profileId ->
                            isCommentsSheetOpen = false
                            onClose()
                            onNavigateToProfile(profileId)
                        },
                        onReply = { comment ->
                            replyingToCommentId = comment.commentId
                            replyingToDisplayName = comment.displayName
                            replyingToBody = comment.body
                            editingCommentId = null
                            editingCommentPreview = null
                            if (commentText.isBlank()) {
                                commentText = ""
                            }
                        },
                        onEditComment = { comment ->
                            editingCommentId = comment.commentId
                            replyingToCommentId = null
                            replyingToDisplayName = null
                            replyingToBody = null
                            commentText = comment.body
                            editingCommentPreview = comment.body
                        },
                        onDeleteComment = onDeleteComment,
                        onSetCommentHidden = onSetCommentHidden,
                        onSend = {
                            val post = uiState.selectedPost
                            if (post != null && commentText.isNotBlank()) {
                                val editId = editingCommentId
                                val replyId = replyingToCommentId
                                if (editId != null) {
                                    onEditComment(editId, commentText)
                                    pendingScrollCommentId = editId
                                    pendingScrollToBottom = false
                                } else {
                                    onAddComment(post.postId, commentText, replyId)
                                    pendingScrollCommentId = replyId
                                    pendingScrollToBottom = replyId == null
                                }
                                commentText = ""
                                replyingToCommentId = null
                                replyingToDisplayName = null
                                replyingToBody = null
                                editingCommentId = null
                                editingCommentPreview = null
                            }
                        },
                        onDismiss = {
                            isCommentsSheetOpen = false
                            replyingToCommentId = null
                            replyingToDisplayName = null
                            replyingToBody = null
                            editingCommentId = null
                            editingCommentPreview = null
                            pendingScrollCommentId = null
                            pendingScrollToBottom = false
                        }
                    )
                }

                pendingDeletePostId?.let { postId ->
                    AlertDialog(
                        onDismissRequest = { pendingDeletePostId = null },
                        title = { Text(text = "Xóa bài viết?") },
                        text = { Text(text = "Bài viết sẽ bị ẩn khỏi bảng tin.") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    pendingDeletePostId = null
                                    onDeletePost(postId)
                                }
                            ) {
                                Text(text = "Xóa")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { pendingDeletePostId = null }) {
                                Text(text = "Hủy")
                            }
                        }
                    )
                }

                actionPost?.let { post ->
                    FeedPostActionSheet(
                        post = post,
                        currentUserId = uiState.currentUserId,
                        onHide = {
                            actionPost = null
                            onHidePost(post.postId)
                        },
                        onDownload = {
                            actionPost = null
                            onDownloadPost(post)
                        },
                        onDelete = {
                            actionPost = null
                            pendingDeletePostId = post.postId
                        },
                        onDismiss = { actionPost = null }
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
    onOpenPostActions: (FeedPostDto) -> Unit,
    onOpenComments: () -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    val density = LocalDensity.current
    val navigationBarBottom = with(density) {
        WindowInsets.navigationBars.getBottom(density).toDp()
    }
    val actionBarBottomPadding = navigationBarBottom.coerceAtLeast(32.dp) + 56.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.90f))
            .statusBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 22.dp,
                    top = 22.dp,
                    end = 22.dp,
                    bottom = actionBarBottomPadding
                ),
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
                commentCount = comments.size,
                onToggleLike = onToggleLike,
                onOpenPostActions = onOpenPostActions,
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
            Spacer(Modifier.width(8.dp))
            Text(
                text = "• ${formatCommentAge(post.createdAt)}",
                color = Color.White.copy(alpha = 0.46f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
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
    commentCount: Int,
    onToggleLike: (String) -> Unit,
    onOpenPostActions: (FeedPostDto) -> Unit,
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

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onToggleLike(post.postId) }
        ) {
            if (post.likeCount > 0) {
                Text(
                    text = post.likeCount.toString(),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(3.dp))
            }
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

        IconButton(onClick = { onOpenPostActions(post) }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Tùy chọn bài viết",
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedPostActionSheet(
    post: FeedPostDto,
    currentUserId: String,
    onHide: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val isOwner = post.ownerId == currentUserId

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF202229).copy(alpha = 0.96f),
        scrimColor = Color.Black.copy(alpha = 0.45f),
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 18.dp, bottom = 18.dp)
        ) {
            if (!isOwner) {
                FeedPostActionRow(
                    text = "Ẩn",
                    textColor = Color.White,
                    icon = { Icon(Icons.Outlined.VisibilityOff, contentDescription = null, tint = Color.White) },
                    onClick = onHide
                )
            }
            FeedPostActionRow(
                text = "Tải về",
                textColor = Color.White,
                icon = { Icon(Icons.Outlined.FileDownload, contentDescription = null, tint = Color.White) },
                onClick = onDownload
            )
            if (isOwner) {
                FeedPostActionRow(
                    text = "Xóa",
                    textColor = Color(0xFFFF5B6B),
                    icon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = Color(0xFFFF5B6B)) },
                    onClick = onDelete
                )
            }
            FeedPostActionRow(
                text = "Hủy",
                textColor = Color.White.copy(alpha = 0.88f),
                icon = null,
                onClick = onDismiss
            )
        }
    }
}

@Composable
private fun FeedPostActionRow(
    text: String,
    textColor: Color,
    icon: (@Composable () -> Unit)?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 28.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            icon()
            Spacer(Modifier.width(14.dp))
        }
        Text(
            text = text,
            color = textColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedCommentsBottomSheet(
    comments: List<FeedCommentDto>,
    currentUserId: String,
    postOwnerId: String,
    commentText: String,
    replyingToDisplayName: String?,
    replyingToBody: String?,
    isEditingComment: Boolean,
    editingCommentPreview: String?,
    scrollToCommentId: String?,
    scrollToBottom: Boolean,
    onScrollHandled: () -> Unit,
    onCommentTextChange: (String) -> Unit,
    onCancelReply: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onReply: (FeedCommentDto) -> Unit,
    onEditComment: (FeedCommentDto) -> Unit,
    onDeleteComment: (String) -> Unit,
    onSetCommentHidden: (String, Boolean) -> Unit,
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
                .fillMaxHeight(0.88f)
                .imePadding()
                .navigationBarsPadding()
                .padding(start = 18.dp, end = 18.dp, bottom = 24.dp)
        ) {
            FeedCommentsList(
                comments = comments,
                currentUserId = currentUserId,
                postOwnerId = postOwnerId,
                onNavigateToProfile = onNavigateToProfile,
                onReply = onReply,
                onEditComment = onEditComment,
                onDeleteComment = onDeleteComment,
                onSetCommentHidden = onSetCommentHidden,
                scrollToCommentId = scrollToCommentId,
                scrollToBottom = scrollToBottom,
                onScrollHandled = onScrollHandled,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Spacer(Modifier.height(12.dp))

            FeedCommentInput(
                value = commentText,
                replyingToDisplayName = replyingToDisplayName,
                replyingToBody = replyingToBody,
                isEditingComment = isEditingComment,
                editingCommentPreview = editingCommentPreview,
                onValueChange = onCommentTextChange,
                onCancelReply = onCancelReply,
                onSend = onSend
            )
        }
    }
}

@Composable
private fun FeedCommentsList(
    comments: List<FeedCommentDto>,
    currentUserId: String,
    postOwnerId: String,
    onNavigateToProfile: (String) -> Unit,
    onReply: (FeedCommentDto) -> Unit,
    onEditComment: (FeedCommentDto) -> Unit,
    onDeleteComment: (String) -> Unit,
    onSetCommentHidden: (String, Boolean) -> Unit,
    scrollToCommentId: String?,
    scrollToBottom: Boolean,
    onScrollHandled: () -> Unit,
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

    val commentIds = comments.map { it.commentId }.toSet()
    val rootComments = comments.filter { it.parentCommentId == null || it.parentCommentId !in commentIds }
    val repliesByParentId = comments
        .filter { it.parentCommentId != null && it.parentCommentId in commentIds }
        .groupBy { it.parentCommentId }
    val displayComments = rootComments.flatMap { root ->
        listOf(root to 0) + flattenReplies(root.commentId, repliesByParentId).map { it to 1 }
    }
    val listState = rememberLazyListState()
    var previousCommentCount by remember { mutableStateOf(comments.size) }
    var actionComment by remember { mutableStateOf<FeedCommentDto?>(null) }

    LaunchedEffect(scrollToCommentId, scrollToBottom, comments.size, displayComments.size) {
        val targetCommentId = scrollToCommentId
        when {
            targetCommentId != null -> {
                val targetIndex = displayComments.indexOfFirst { it.first.commentId == targetCommentId }
                if (targetIndex >= 0) {
                    listState.animateScrollToItem(targetIndex)
                    onScrollHandled()
                }
            }

            scrollToBottom && comments.size > previousCommentCount && displayComments.isNotEmpty() -> {
                listState.animateScrollToItem(displayComments.lastIndex)
                onScrollHandled()
            }
        }
        previousCommentCount = comments.size
    }

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(displayComments, key = { it.first.commentId }) { (comment, replyDepth) ->
            val canEdit = comment.userId == currentUserId
            val canDelete = canEdit
            val canToggleHidden = postOwnerId == currentUserId && !canEdit
            Box(modifier = Modifier.fillMaxWidth()) {
                FeedCommentRow(
                    comment = comment,
                    replyDepth = replyDepth,
                    onNavigateToProfile = onNavigateToProfile,
                    onReply = onReply,
                    canToggleHidden = canToggleHidden,
                    onToggleHidden = { onSetCommentHidden(comment.commentId, !comment.isHidden) },
                    onOpenActions = if (canEdit || canDelete) {
                        { actionComment = comment }
                    } else {
                        null
                    }
                )
                FeedCommentActionMenu(
                    expanded = actionComment?.commentId == comment.commentId,
                    canEdit = canEdit,
                    canDelete = canDelete,
                    onEdit = {
                        actionComment = null
                        onEditComment(comment)
                    },
                    onDelete = {
                        actionComment = null
                        onDeleteComment(comment.commentId)
                    },
                    onDismiss = { actionComment = null }
                )
            }
        }
    }
}

private fun flattenReplies(
    parentCommentId: String,
    repliesByParentId: Map<String?, List<FeedCommentDto>>
): List<FeedCommentDto> {
    val replies = repliesByParentId[parentCommentId].orEmpty()
    return replies.flatMap { reply ->
        listOf(reply) + flattenReplies(reply.commentId, repliesByParentId)
    }
}

@Composable
private fun FeedCommentActionMenu(
    expanded: Boolean,
    canEdit: Boolean,
    canDelete: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF202229).copy(alpha = 0.88f),
        shape = RoundedCornerShape(24.dp)
    ) {
        if (canEdit) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Sửa",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                leadingIcon = {
                    Icon(Icons.Outlined.Edit, contentDescription = null, tint = Color.White)
                },
                onClick = onEdit
            )
        }
        if (canDelete) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Xóa",
                        color = Color(0xFFFF5B6B),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                leadingIcon = {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = Color(0xFFFF5B6B))
                },
                onClick = onDelete
            )
        }
    }
}

@Composable
private fun FeedCommentActionDialog(
    canEdit: Boolean,
    canDelete: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .width(250.dp)
                    .clickable(enabled = false) {},
                color = Color(0xFF202229).copy(alpha = 0.82f),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 14.dp)) {
                    if (canEdit) {
                        FeedCommentActionRow(
                            text = "Sửa",
                            icon = { Icon(Icons.Outlined.Edit, contentDescription = null, tint = Color.White) },
                            textColor = Color.White,
                            onClick = onEdit
                        )
                    }
                    if (canDelete) {
                        FeedCommentActionRow(
                            text = "Xóa",
                            icon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = Color(0xFFFF5B6B)) },
                            textColor = Color(0xFFFF5B6B),
                            onClick = onDelete
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedCommentActionRow(
    text: String,
    icon: @Composable () -> Unit,
    textColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(Modifier.width(16.dp))
        Text(
            text = text,
            color = textColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FeedCommentRow(
    comment: FeedCommentDto,
    replyDepth: Int,
    onNavigateToProfile: (String) -> Unit,
    onReply: (FeedCommentDto) -> Unit = {},
    canToggleHidden: Boolean = false,
    onToggleHidden: () -> Unit = {},
    onOpenActions: (() -> Unit)? = null
) {
    val longPressModifier = if (onOpenActions != null) {
        Modifier.clickable(onClick = onOpenActions)
    } else {
        Modifier
    }
    val avatarModifier = if (onOpenActions != null) {
        Modifier.clickable { onNavigateToProfile(comment.userId) }
    } else {
        Modifier.clickable { onNavigateToProfile(comment.userId) }
    }
    val nameModifier = if (onOpenActions != null) {
        Modifier.clickable { onNavigateToProfile(comment.userId) }
    } else {
        Modifier.clickable { onNavigateToProfile(comment.userId) }
    }
    val replyModifier = if (onOpenActions != null) {
        Modifier.clickable { onReply(comment) }
    } else {
        Modifier.clickable { onReply(comment) }
    }
    val contentAlpha = if (comment.isHidden) 0.42f else 1f
    val isEdited = comment.updatedAt > comment.createdAt + 1_000L

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (replyDepth.coerceAtMost(1) * 42).dp)
            .then(longPressModifier),
        verticalAlignment = Alignment.Top
    ) {
        ProfileAvatar(
            avatarUrl = comment.avatarUrl,
            modifier = Modifier
                .size(42.dp)
                .then(avatarModifier)
        )

        Spacer(Modifier.width(8.dp))

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.displayName,
                    color = Color.White.copy(alpha = contentAlpha),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = nameModifier
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatCommentAge(comment.createdAt),
                    color = Color.White.copy(alpha = 0.36f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isEdited) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Đã chỉnh sửa",
                        color = Color.White.copy(alpha = 0.30f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Text(
                text = comment.body,
                color = Color.White.copy(alpha = contentAlpha),
                fontSize = 15.sp
            )
            Row(
                modifier = Modifier.padding(top = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!comment.isHidden) {
                    Text(
                        text = "Trả lời",
                        color = Color.White.copy(alpha = 0.36f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = replyModifier
                    )
                }
                if (canToggleHidden) {
                    Text(
                        text = if (comment.isHidden) "Hiện" else "Ẩn",
                        color = Color.White.copy(alpha = 0.52f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(onClick = onToggleHidden)
                    )
                }
            }
        }
    }
}

private fun formatCommentAge(createdAt: Long): String {
    val elapsedMillis = max(0L, System.currentTimeMillis() - createdAt)
    val minute = 60_000L
    val hour = 60 * minute
    val day = 24 * hour
    val week = 7 * day
    val month = 30 * day
    val year = 365 * day

    return when {
        elapsedMillis < minute -> "Vừa xong"
        elapsedMillis < hour -> "${elapsedMillis / minute} phút"
        elapsedMillis < day -> "${elapsedMillis / hour} giờ"
        elapsedMillis < week -> "${elapsedMillis / day} ngày"
        elapsedMillis < month -> "${elapsedMillis / week} tuần"
        elapsedMillis < year -> "${elapsedMillis / month} tháng"
        else -> "${elapsedMillis / year} năm"
    }
}

@Composable
private fun FeedCommentInput(
    value: String,
    replyingToDisplayName: String?,
    replyingToBody: String?,
    isEditingComment: Boolean,
    editingCommentPreview: String?,
    onValueChange: (String) -> Unit,
    onCancelReply: () -> Unit,
    onSend: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (isEditingComment) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.10f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Đang sửa",
                        color = Color.White.copy(alpha = 0.88f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = editingCommentPreview.orEmpty(),
                        color = Color.White.copy(alpha = 0.62f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "Hủy",
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onCancelReply)
                )
            }
        }

        if (replyingToDisplayName != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.10f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Đang trả lời $replyingToDisplayName",
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = replyingToBody.orEmpty(),
                        color = Color.White.copy(alpha = 0.50f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "Hủy",
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onCancelReply)
                )
            }
        }

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
            textStyle = LocalTextStyle.current.copy(color = Color.White),
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
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Gửi",
                tint = Color.White
            )
        }
    }
    }
}
