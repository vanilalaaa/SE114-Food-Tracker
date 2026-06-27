package com.SE114.food_tracker.feature.profile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SE114.food_tracker.R
import com.SE114.food_tracker.core.designsystem.components.BottomBarContentPadding
import com.SE114.food_tracker.core.designsystem.theme.FoodTrackerTheme
import com.SE114.food_tracker.core.designsystem.theme.TextLabelGray
import com.SE114.food_tracker.data.local.dao.FeedPostDto
import com.SE114.food_tracker.feature.feed.components.FeedGridPostTile

@Composable
fun ProfilePostsTab(
    posts: List<FeedPostDto>,
    isLoading: Boolean,
    onPostClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            posts.isEmpty() -> {
                Text(
                    text = stringResource(R.string.profile_posts_empty),
                    color = TextLabelGray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 32.dp)
                )
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = BottomBarContentPadding),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    items(
                        items = posts,
                        key = { it.postId }
                    ) { post ->
                        FeedGridPostTile(
                            post = post,
                            onClick = { onPostClick(post.postId) }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfilePostsTabPreview() {
    FoodTrackerTheme {
        ProfilePostsTab(
            posts = listOf(
                FeedPostDto(
                    postId = "post-1",
                    ownerId = "user-1",
                    ownerName = "Thảo Uyên",
                    ownerAvatarUrl = null,
                    itemId = null,
                    itemName = "Phở bò",
                    categoryIconUrl = "🍜",
                    imageUrl = "",
                    caption = "Bữa sáng",
                    visibility = "public",
                    likeCount = 3,
                    commentCount = 1,
                    isLikedByMe = false,
                    createdAt = 0L
                )
            ),
            isLoading = false,
            onPostClick = {}
        )
    }
}
