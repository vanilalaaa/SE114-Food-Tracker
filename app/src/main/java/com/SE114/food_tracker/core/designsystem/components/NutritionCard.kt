package com.SE114.food_tracker.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.SE114.food_tracker.core.designsystem.theme.*
import com.SE114.food_tracker.feature.diary.DiaryCategory
import com.SE114.food_tracker.feature.diary.DiaryItem
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class MenuState { MAIN, FILTER, SIZE }

@Composable
fun NutritionCard(
    unfilteredItems: List<DiaryItem> = emptyList(),
    filteredItemCount: Int = 0,
    categories: List<DiaryCategory> = emptyList(),
    selectedCategoryId: String? = null,
    onCategorySelect: (String?) -> Unit = {},
    boxScale: Float = 1f,
    calendarScale: Float = 1f,
    onBoxScaleChange: (Float) -> Unit = {},
    onCalendarScaleChange: (Float) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    var menuState by remember { mutableStateOf(MenuState.MAIN) }

    val categoryCounts = remember(unfilteredItems) {
        unfilteredItems.groupingBy { it.categoryId }.eachCount()
    }
    val availableCategories = categories.filter { categoryCounts.containsKey(it.categoryId) }

    val filteredItems = remember(unfilteredItems, selectedCategoryId) {
        selectedCategoryId?.let { catId ->
            unfilteredItems.filter { it.categoryId == catId }
        } ?: unfilteredItems
    }

    val categoriesById = remember(categories) { categories.associateBy { it.categoryId } }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(horizontal = 24.dp),
        color = LightPinkBG,
        shape = RoundedCornerShape(30.dp),
        shadowElevation = 2.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // ── Menu Dropdown ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .zIndex(2f)
            ) {
                IconButton(onClick = {
                    expanded = true
                    menuState = MenuState.MAIN
                }) {
                    Icon(
                        Icons.Default.MoreVert, "Menu",
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                }

                MaterialTheme(
                    shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(20.dp))
                ) {
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier
                            .background(Color.White)
                            .width(230.dp)
                    ) {
                        when (menuState) {
                            MenuState.MAIN -> {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Lọc loại",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.Black
                                        )
                                    },
                                    leadingIcon = { Icon(Icons.Outlined.Tune, null, tint = Color.Black) },
                                    trailingIcon = {
                                        Icon(Icons.Default.KeyboardArrowRight, null, tint = Color.Black)
                                    },
                                    onClick = { menuState = MenuState.FILTER },
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = Color(0xFFF0F0F0)
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Kích thước",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.Black
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.AspectRatio, null, tint = Color.Black)
                                    },
                                    onClick = { menuState = MenuState.SIZE },
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }

                            MenuState.FILTER -> {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Lọc loại",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    },
                                    leadingIcon = { Icon(Icons.Outlined.Tune, null, tint = Color.Black) },
                                    trailingIcon = {
                                        Icon(Icons.Default.KeyboardArrowRight, null, tint = Color.Black)
                                    },
                                    onClick = { menuState = MenuState.MAIN },
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = Color(0xFFF0F0F0)
                                )
                                DropdownMenuItem(
                                    text = { Text("Tất cả", fontSize = 15.sp, color = Color.Black) },
                                    leadingIcon = {
                                        if (selectedCategoryId == null)
                                            Icon(
                                                Icons.Default.Check, null,
                                                tint = Color.Black,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        else Spacer(modifier = Modifier.width(20.dp))
                                    },
                                    onClick = { onCategorySelect(null) }
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = Color(0xFFF0F0F0)
                                )
                                availableCategories.forEach { cat ->
                                    val count = categoryCounts[cat.categoryId] ?: 0
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "${cat.name}($count)",
                                                fontSize = 15.sp,
                                                color = Color.Black
                                            )
                                        },
                                        leadingIcon = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (selectedCategoryId == cat.categoryId) {
                                                    Icon(
                                                        Icons.Default.Check, null,
                                                        tint = Color.Black,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                } else {
                                                    Spacer(modifier = Modifier.width(20.dp))
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(cat.iconUrl ?: "🍽️", fontSize = 18.sp)
                                            }
                                        },
                                        onClick = { onCategorySelect(cat.categoryId) }
                                    )
                                }
                            }

                            MenuState.SIZE -> {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Kích thước",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.AspectRatio, null, tint = Color.Black)
                                    },
                                    onClick = { menuState = MenuState.MAIN },
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = Color(0xFFF0F0F0)
                                )
                                Column(
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Outlined.Inbox, null,
                                            modifier = Modifier.size(16.dp),
                                            tint = Color.Black
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("Trong hộp", fontSize = 12.sp, color = Color.Black)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Slider(
                                            value = boxScale,
                                            onValueChange = onBoxScaleChange,
                                            valueRange = 0.5f..1.5f,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(24.dp),
                                            colors = SliderDefaults.colors(
                                                thumbColor = Color.Gray,
                                                activeTrackColor = Color.DarkGray,
                                                inactiveTrackColor = Color.LightGray
                                            )
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "${(boxScale * 100).toInt()}%",
                                            fontSize = 10.sp,
                                            color = Color.Black
                                        )
                                    }
                                    Spacer(Modifier.height(16.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Outlined.CalendarToday, null,
                                            modifier = Modifier.size(16.dp),
                                            tint = Color.Black
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("Trong lịch", fontSize = 12.sp, color = Color.Black)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Slider(
                                            value = calendarScale,
                                            onValueChange = onCalendarScaleChange,
                                            valueRange = 0.5f..1.5f,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(24.dp),
                                            colors = SliderDefaults.colors(
                                                thumbColor = Color.Gray,
                                                activeTrackColor = Color.DarkGray,
                                                inactiveTrackColor = Color.LightGray
                                            )
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "${(calendarScale * 100).toInt()}%",
                                            fontSize = 10.sp,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Physics Engine ─────────────────────────────────────────────────
            val density = LocalDensity.current
            val baseSizePx = with(density) { 44.dp.toPx() }
            val stickerSizePx = baseSizePx * boxScale
            val radius = stickerSizePx / 2f
            val stickers = remember { mutableStateListOf<StickerNode>() }
            var boxSize by remember { mutableStateOf(IntSize.Zero) }

            LaunchedEffect(filteredItems, boxSize, selectedCategoryId) {
                if (boxSize == IntSize.Zero) return@LaunchedEffect

                val currentIds = filteredItems.map { it.itemId }.toSet()

                // Step A: Remove stickers no longer in the filtered list
                stickers.removeAll { node -> node.id !in currentIds }

                val spawnWidthRange = (boxSize.width - stickerSizePx.toInt()).coerceAtLeast(0)

                // Step B: Count how many NEW nodes we are about to add (for stagger index)
                val existingIds = stickers.map { it.id }.toSet()
                val newItems = filteredItems.filter { it.itemId !in existingIds }
                var spawnIndex = 0

                filteredItems.forEach { item ->
                    val existingNode = stickers.find { it.id == item.itemId }
                    if (existingNode != null) {
                        // Update imageUrl if DB just finished writing it (null → url)
                        if (existingNode.imageUrl != item.imageUrl) {
                            existingNode.imageUrl = item.imageUrl
                        }
                    } else {
                        val categoryEmoji = categoriesById[item.categoryId]?.iconUrl ?: "🍱"

                        // ── STAGGER FIX ──────────────────────────────────────
                        val staggerOffsetY = spawnIndex * stickerSizePx * 0.8f

                        // Small random horizontal push for dynamic single inserts so the
                        // new sticker doesn't fall in a dead-vertical column onto a settled pile.
                        val initialVx = if (newItems.size == 1) {
                            ((-3..3).random()).toFloat()
                        } else {
                            0f
                        }

                        stickers.add(
                            StickerNode(
                                id = item.itemId,
                                initialX = if (spawnWidthRange > 0)
                                    (0..spawnWidthRange).random().toFloat()
                                else 0f,
                                initialY = -(100f + staggerOffsetY),
                                emoji = categoryEmoji,
                                imageUrl = item.imageUrl,
                                initialVx = initialVx
                            )
                        )
                        spawnIndex++
                    }
                }
            }

            // ── Physics loop: gravity + wall bounce + circle-to-circle collision ──
            LaunchedEffect(boxSize, boxScale) {
                if (boxSize == IntSize.Zero) return@LaunchedEffect
                while (isActive) {
                    withFrameNanos {
                        val maxX = boxSize.width - stickerSizePx
                        val maxY = boxSize.height - stickerSizePx

                        // 1. Gravity + movement + boundary collision
                        stickers.forEach { s ->
                            if (!s.isDragging) {
                                s.vy += 2.0f
                                s.x += s.vx
                                s.y += s.vy

                                if (s.y >= maxY) {
                                    s.y = maxY
                                    s.vy = -s.vy * 0.2f
                                    s.vx *= 0.75f
                                }
                                if (s.x <= 0f) { s.x = 0f; s.vx = -s.vx * 0.4f }
                                else if (s.x >= maxX) { s.x = maxX; s.vx = -s.vx * 0.4f }
                            }
                        }

                        // 2. Circle-to-circle collision
                        for (i in stickers.indices) {
                            for (j in i + 1 until stickers.size) {
                                val s1 = stickers[i]
                                val s2 = stickers[j]

                                val dx = (s1.x + radius) - (s2.x + radius)
                                val dy = (s1.y + radius) - (s2.y + radius)
                                val dist = sqrt(dx * dx + dy * dy)

                                if (dist < stickerSizePx && dist > 0f) {
                                    val overlap = stickerSizePx - dist
                                    val nx = dx / dist
                                    val ny = dy / dist

                                    // A: Separate overlapping positions
                                    val pushX = nx * overlap * 0.5f
                                    val pushY = ny * overlap * 0.5f
                                    if (!s1.isDragging) { s1.x += pushX; s1.y += pushY }
                                    if (!s2.isDragging) { s2.x -= pushX; s2.y -= pushY }

                                    // B: Impulse along collision normal
                                    val rvx = s1.vx - s2.vx
                                    val rvy = s1.vy - s2.vy
                                    val velAlongNormal = rvx * nx + rvy * ny

                                    if (velAlongNormal < 0) {
                                        val restitution = 0.1f
                                        val impulseScalar = -(1f + restitution) * velAlongNormal
                                        val impulseX = (impulseScalar / 2f) * nx
                                        val impulseY = (impulseScalar / 2f) * ny

                                        if (!s1.isDragging) { s1.vx += impulseX; s1.vy += impulseY }
                                        if (!s2.isDragging) { s2.vx -= impulseX; s2.vy -= impulseY }
                                    }

                                    // C: Surface contact friction
                                    val contactFriction = 0.82f
                                    if (!s1.isDragging) s1.vx *= contactFriction
                                    if (!s2.isDragging) s2.vx *= contactFriction
                                }
                            }
                        }
                    }
                }
            }

            // ── Render ────────────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxSize().onSizeChanged { boxSize = it }) {
                stickers.forEach { node ->
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(node.x.roundToInt(), node.y.roundToInt()) }
                            .pointerInput(boxScale) {
                                detectDragGestures(
                                    onDragStart = {
                                        node.isDragging = true
                                        node.vx = 0f
                                        node.vy = 0f
                                    },
                                    onDragEnd = { node.isDragging = false },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        node.x = (node.x + dragAmount.x)
                                            .coerceIn(0f, boxSize.width - stickerSizePx)
                                        node.y = (node.y + dragAmount.y)
                                            .coerceIn(0f, boxSize.height - stickerSizePx)
                                        node.vx = dragAmount.x * 0.8f
                                        node.vy = dragAmount.y * 0.8f
                                    }
                                )
                            }
                            .size((44 * boxScale).dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        FoodStickerAvatar(
                            imageUrl = node.imageUrl,
                            emoji = node.emoji,
                            scale = boxScale
                        )
                    }
                }
            }
        }
    }
}

// ── Avatar: Supabase image or emoji fallback ───────────────────────────────────
@Composable
private fun FoodStickerAvatar(imageUrl: String?, emoji: String, scale: Float) {
    if (!imageUrl.isNullOrBlank()) {
        val context = LocalContext.current
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
            .build()
        AsyncImage(
            model = request,
            contentDescription = emoji,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji.ifBlank { "🍱" }, fontSize = (22 * scale).sp)
        }
    }
}

// ── Physics node ──────────────────────────────────────────────────────────────
class StickerNode(
    val id: String,
    initialX: Float,
    initialY: Float,
    val emoji: String,
    imageUrl: String?,
    // Optional initial horizontal velocity — used for single dynamic inserts so
    // the new sticker deflects off the settled pile instead of falling dead-vertical.
    initialVx: Float = 0f
) {
    var x by mutableFloatStateOf(initialX)
    var y by mutableFloatStateOf(initialY)
    var vx = initialVx
    var vy = 0f
    var isDragging = false

    var imageUrl by mutableStateOf(imageUrl)
}