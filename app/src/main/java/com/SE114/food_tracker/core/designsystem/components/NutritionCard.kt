package com.SE114.food_tracker.core.designsystem.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.SE114.food_tracker.core.designsystem.theme.*

// ── Data model ───────────────────────────────────────────────────────────────

/**
 * Lightweight model for one food item rendered as a draggable avatar inside
 * [NutritionCard]. Pass a non-null [imageUrl] to show a photo, otherwise the
 * [emoji] fallback (category icon) is displayed.
 */
data class DraggableFoodItem(
    val id: String,
    val imageUrl: String?,
    val emoji: String          // e.g. "🍚"  — shown when imageUrl is null/blank
)

// ── Constants ────────────────────────────────────────────────────────────────

private val AVATAR_SIZE  = 46.dp   // diameter of each circular avatar
private val MENU_SAFE_PX = 56      // px reserved in top-end corner for the MoreVert button

// ── Component ────────────────────────────────────────────────────────────────

@Composable
fun NutritionCard(
    onMenuClick: () -> Unit,
    items: List<DraggableFoodItem> = emptyList()
) {
    // Pixel size of the card, populated by onSizeChanged once laid out
    var cardSize by remember { mutableStateOf(IntSize.Zero) }

    // One Offset per item — keyed by item.id so drag state survives recomposition
    val positions = remember { mutableStateMapOf<String, Offset>() }

    val density = LocalDensity.current
    val avatarPx = with(density) { AVATAR_SIZE.toPx() }

    // Assign initial (random) positions when the item list or card size first arrives
    LaunchedEffect(items, cardSize) {
        if (cardSize == IntSize.Zero) return@LaunchedEffect
        items.forEach { item ->
            if (!positions.containsKey(item.id)) {
                positions[item.id] = randomInitialOffset(
                    cardWidth  = cardSize.width.toFloat(),
                    cardHeight = cardSize.height.toFloat(),
                    avatarSize = avatarPx,
                    menuSafePx = MENU_SAFE_PX.toFloat()
                )
            }
        }
        // Remove stale entries for items that are no longer in the list
        positions.keys.retainAll(items.map { it.id }.toSet())
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(horizontal = 24.dp)
            .onSizeChanged { cardSize = it },
        color         = LightPinkBG,
        shape         = RoundedCornerShape(30.dp),
        shadowElevation = 4.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // ── Draggable avatars ──────────────────────────────────────────
            items.forEach { item ->
                val offset = positions[item.id] ?: return@forEach

                Box(
                    modifier = Modifier
                        .size(AVATAR_SIZE)
                        // Position from top-left of the card
                        .offset(
                            x = with(density) { offset.x.toDp() },
                            y = with(density) { offset.y.toDp() }
                        )
                        .pointerInput(item.id, cardSize) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val current = positions[item.id] ?: Offset.Zero
                                positions[item.id] = clampOffset(
                                    raw        = current + dragAmount,
                                    cardWidth  = cardSize.width.toFloat(),
                                    cardHeight = cardSize.height.toFloat(),
                                    avatarSize = avatarPx,
                                    menuSafePx = MENU_SAFE_PX.toFloat()
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    FoodAvatar(item = item)
                }
            }

            // ── MoreVert menu button (always on top) ───────────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                IconButton(
                    onClick  = onMenuClick,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector      = Icons.Default.MoreVert,
                        contentDescription = "Thêm tùy chọn",
                        tint             = TextPrimary
                    )
                }
            }
        }
    }
}

// ── Avatar composable ─────────────────────────────────────────────────────────

@Composable
private fun FoodAvatar(item: DraggableFoodItem) {
    if (!item.imageUrl.isNullOrBlank()) {
        AsyncImage(
            model              = item.imageUrl,
            contentDescription = item.emoji,
            modifier           = Modifier
                .fillMaxSize()
                .clip(CircleShape),
            contentScale       = ContentScale.Crop
        )
    } else {
        Surface(
            modifier      = Modifier.fillMaxSize(),
            shape         = CircleShape,
            color         = CardWhite,
            shadowElevation = 2.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = item.emoji.ifBlank { "🍱" }, fontSize = 22.sp)
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

/**
 * Clamps [raw] so the avatar stays fully inside the card bounds.
 * The top-end corner is protected by an additional [menuSafePx] exclusion zone
 * so avatars don't pile up on the MoreVert button.
 */
private fun clampOffset(
    raw: Offset,
    cardWidth: Float,
    cardHeight: Float,
    avatarSize: Float,
    menuSafePx: Float
): Offset {
    val maxX = cardWidth  - avatarSize
    val maxY = cardHeight - avatarSize

    var x = raw.x.coerceIn(0f, maxX)
    var y = raw.y.coerceIn(0f, maxY)

    // Keep the avatar out of the top-end MoreVert area
    val menuLeft   = cardWidth  - menuSafePx
    val menuBottom = menuSafePx
    if (x + avatarSize > menuLeft && y < menuBottom) {
        // Push left if we'd overlap the button horizontally while near the top
        x = (menuLeft - avatarSize).coerceAtLeast(0f)
    }

    return Offset(x, y)
}

/**
 * Generates a random initial [Offset] that is guaranteed to sit within the
 * card's safe area (respects avatar size and the MoreVert exclusion zone).
 */
private fun randomInitialOffset(
    cardWidth: Float,
    cardHeight: Float,
    avatarSize: Float,
    menuSafePx: Float
): Offset {
    val maxX = (cardWidth  - avatarSize).coerceAtLeast(0f)
    val maxY = (cardHeight - avatarSize).coerceAtLeast(0f)

    // Try up to 20 times to find a spot outside the menu zone
    repeat(20) {
        val x = (0f..maxX).random()
        val y = (0f..maxY).random()
        val inMenuZone = x + avatarSize > cardWidth - menuSafePx && y < menuSafePx
        if (!inMenuZone) return Offset(x, y)
    }
    // Fallback: place in the bottom-left quadrant
    return Offset(avatarSize * 0.5f, maxY * 0.6f)
}

private fun ClosedFloatingPointRange<Float>.random(): Float =
    start + (endInclusive - start) * kotlin.random.Random.nextFloat()

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
fun NutritionCardPreview() {
    FoodTrackerTheme {
        Box(modifier = Modifier.padding(vertical = 20.dp)) {
            NutritionCard(
                onMenuClick = {},
                items = listOf(
                    DraggableFoodItem("1", null,  "🍚"),
                    DraggableFoodItem("2", null,  "🍜"),
                    DraggableFoodItem("3", null,  "🥤"),
                    DraggableFoodItem("4", null,  "🍰"),
                    DraggableFoodItem("5", null,  "🥖")
                )
            )
        }
    }
}