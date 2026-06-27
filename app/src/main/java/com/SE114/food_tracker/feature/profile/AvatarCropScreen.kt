package com.SE114.food_tracker.feature.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas as GraphicsCanvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.exifinterface.media.ExifInterface
import com.SE114.food_tracker.R
import com.SE114.food_tracker.core.designsystem.theme.FoodTrackerTheme
import com.SE114.food_tracker.core.designsystem.theme.MintGreen
import com.SE114.food_tracker.core.designsystem.theme.OrangeMain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.min
import kotlin.math.roundToInt

private val CROP_MIN_FRAME = 72.dp
private val CROP_HANDLE_TOUCH = 28.dp
private val CROP_HANDLE_LENGTH = 22.dp
private val CROP_HANDLE_STROKE = 3.dp
private val CROP_FRAME_STROKE = 1.5.dp
private val CROP_CIRCLE_STROKE = 2.dp
private const val CROP_INITIAL_FRACTION = 0.9f
private const val CROP_DIM_ALPHA = 0.5f
private const val MAX_AVATAR_PX = 1024
private const val AVATAR_JPEG_QUALITY = 90

/** Square crop region expressed in the source bitmap's own pixel coordinates. */
data class CropRect(val left: Int, val top: Int, val size: Int)

private enum class DragMode { None, Move, TopLeft, TopRight, BottomLeft, BottomRight }

private data class SquareFrame(val left: Float, val top: Float, val size: Float)

/** Letterbox geometry of a fit-scaled image inside its container, in container pixels. */
private data class CropMetrics(
    val scale: Float,
    val dispLeft: Float,
    val dispTop: Float,
    val dispWidth: Float,
    val dispHeight: Float
)

/**
 * Full-screen crop step shown between picking an image and uploading it. Loads the picked [imageUri]
 * as an upright bitmap, lets the user frame a square, and hands the cropped JPEG back via [onConfirm].
 */
@Composable
fun AvatarCropScreen(
    imageUri: Uri,
    onCancel: () -> Unit,
    onConfirm: (ByteArray) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var source by remember(imageUri) { mutableStateOf<Bitmap?>(null) }
    var failed by remember(imageUri) { mutableStateOf(false) }
    var processing by remember { mutableStateOf(false) }

    // Read system-bar insets HERE, in the activity composition — a Dialog's own window often
    // reports zero navigation-bar inset, which is why the buttons sank under the nav bar.
    val systemBarsInsets = WindowInsets.systemBars.asPaddingValues()

    LaunchedEffect(imageUri) {
        val loaded = withContext(Dispatchers.IO) {
            runCatching { decodeOrientedBitmap(context, imageUri) }
                .onFailure { Timber.tag("Profile").e(it, "avatar crop decode failed") }
                .getOrNull()
        }
        if (loaded == null) failed = true else source = loaded
    }

    Dialog(
        onDismissRequest = { if (!processing) onCancel() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim),
            contentAlignment = Alignment.Center
        ) {
            val current = source
            when {
                current != null -> AvatarCropContent(
                    image = remember(current) { current.asImageBitmap() },
                    processing = processing,
                    contentInsets = systemBarsInsets,
                    onCancel = onCancel,
                    onConfirm = { rect ->
                        scope.launch {
                            processing = true
                            val bytes = withContext(Dispatchers.Default) {
                                runCatching { cropToJpegBytes(current, rect) }
                                    .onFailure { Timber.tag("Profile").e(it, "avatar crop failed") }
                                    .getOrNull()
                            }
                            if (bytes != null) {
                                onConfirm(bytes)
                            } else {
                                processing = false
                                failed = true
                            }
                        }
                    }
                )

                failed -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = stringResource(R.string.profile_crop_error),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.inverseOnSurface
                    )
                    TextButton(onClick = onCancel) {
                        Text(stringResource(R.string.profile_crop_cancel))
                    }
                }

                else -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

/**
 * Stateless crop surface: shows [image] fit-to-screen with a draggable, resizable square frame and an
 * inscribed circle preview. [onConfirm] receives the frame mapped back to source-pixel coordinates.
 */
@Composable
private fun AvatarCropContent(
    image: ImageBitmap,
    processing: Boolean,
    contentInsets: PaddingValues,
    onCancel: () -> Unit,
    onConfirm: (CropRect) -> Unit
) {
    val density = LocalDensity.current
    val minSizePx = with(density) { CROP_MIN_FRAME.toPx() }
    val touchPx = with(density) { CROP_HANDLE_TOUCH.toPx() }
    val bracketPx = with(density) { CROP_HANDLE_LENGTH.toPx() }
    val handleStrokePx = with(density) { CROP_HANDLE_STROKE.toPx() }
    val frameStrokePx = with(density) { CROP_FRAME_STROKE.toPx() }
    val circleStrokePx = with(density) { CROP_CIRCLE_STROKE.toPx() }

    val dimColor = MaterialTheme.colorScheme.scrim.copy(alpha = CROP_DIM_ALPHA)
    val frameColor = MaterialTheme.colorScheme.inverseOnSurface
    val circleColor = MaterialTheme.colorScheme.primary

    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var frame by remember(image) { mutableStateOf<SquareFrame?>(null) }
    var dragMode by remember { mutableStateOf(DragMode.None) }

    val metrics = remember(containerSize, image) {
        if (containerSize.width == 0 || containerSize.height == 0) {
            null
        } else {
            val iw = image.width.toFloat()
            val ih = image.height.toFloat()
            val scale = min(containerSize.width / iw, containerSize.height / ih)
            val dispW = iw * scale
            val dispH = ih * scale
            CropMetrics(
                scale = scale,
                dispLeft = (containerSize.width - dispW) / 2f,
                dispTop = (containerSize.height - dispH) / 2f,
                dispWidth = dispW,
                dispHeight = dispH
            )
        }
    }

    LaunchedEffect(metrics, minSizePx) {
        val m = metrics ?: return@LaunchedEffect
        frame = initialFrame(m, minSizePx)
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentInsets)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.profile_crop_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.inverseOnSurface
            )
            Text(
                text = stringResource(R.string.profile_crop_instruction),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.7f)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onSizeChanged { containerSize = it }
            ) {
                Image(
                    bitmap = image,
                    contentDescription = stringResource(R.string.profile_avatar_desc),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(metrics) {
                            detectDragGestures(
                                onDragStart = { pos ->
                                    val f = frame
                                    dragMode = if (f == null) DragMode.None else hitTest(pos, f, touchPx)
                                },
                                onDragEnd = { dragMode = DragMode.None },
                                onDragCancel = { dragMode = DragMode.None },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val f = frame
                                    val m = metrics
                                    if (f != null && m != null && dragMode != DragMode.None) {
                                        frame = if (dragMode == DragMode.Move) {
                                            moveFrame(f, dragAmount, m)
                                        } else {
                                            resizeFrame(f, dragMode, dragAmount, m, minSizePx)
                                        }
                                    }
                                }
                            )
                        }
                ) {
                    val f = frame ?: return@Canvas
                    val radius = f.size / 2f
                    val center = Offset(f.left + radius, f.top + radius)

                    val dimPath = Path().apply {
                        addRect(Rect(0f, 0f, size.width, size.height))
                        addOval(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))
                        fillType = PathFillType.EvenOdd
                    }
                    drawPath(dimPath, color = dimColor)

                    drawRect(
                        color = frameColor,
                        topLeft = Offset(f.left, f.top),
                        size = Size(f.size, f.size),
                        style = Stroke(width = frameStrokePx)
                    )
                    drawCircle(
                        color = circleColor,
                        radius = radius,
                        center = center,
                        style = Stroke(width = circleStrokePx)
                    )
                    drawCornerBrackets(f, bracketPx, handleStrokePx, frameColor)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onCancel,
                    enabled = !processing,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.profile_crop_cancel))
                }
                Button(
                    onClick = {
                        val m = metrics
                        val f = frame
                        if (m != null && f != null) {
                            onConfirm(toCropRect(f, m, image.width, image.height))
                        }
                    },
                    enabled = frame != null && !processing,
                    modifier = Modifier.weight(1f)
                ) {
                    if (processing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(R.string.profile_crop_confirm))
                    }
                }
            }
        }
    }
}

private fun initialFrame(m: CropMetrics, minSizePx: Float): SquareFrame {
    val maxSide = min(m.dispWidth, m.dispHeight)
    val side = (maxSide * CROP_INITIAL_FRACTION).coerceIn(min(minSizePx, maxSide), maxSide)
    return SquareFrame(
        left = m.dispLeft + (m.dispWidth - side) / 2f,
        top = m.dispTop + (m.dispHeight - side) / 2f,
        size = side
    )
}

private fun hitTest(pos: Offset, f: SquareFrame, touchPx: Float): DragMode {
    val right = f.left + f.size
    val bottom = f.top + f.size
    val touchSq = touchPx * touchPx
    fun near(cornerX: Float, cornerY: Float): Boolean {
        val dx = pos.x - cornerX
        val dy = pos.y - cornerY
        return dx * dx + dy * dy <= touchSq
    }
    return when {
        near(f.left, f.top) -> DragMode.TopLeft
        near(right, f.top) -> DragMode.TopRight
        near(f.left, bottom) -> DragMode.BottomLeft
        near(right, bottom) -> DragMode.BottomRight
        pos.x in f.left..right && pos.y in f.top..bottom -> DragMode.Move
        else -> DragMode.None
    }
}

private fun moveFrame(f: SquareFrame, drag: Offset, m: CropMetrics): SquareFrame {
    val maxLeft = m.dispLeft + m.dispWidth - f.size
    val maxTop = m.dispTop + m.dispHeight - f.size
    return f.copy(
        left = (f.left + drag.x).coerceIn(m.dispLeft, maxLeft),
        top = (f.top + drag.y).coerceIn(m.dispTop, maxTop)
    )
}

/**
 * Resize the square by dragging one corner; the opposite corner stays pinned. Side length grows by the
 * average of the drag's two outward components so the frame tracks the finger while staying square,
 * clamped to the min size and the displayed image bounds.
 */
private fun resizeFrame(
    f: SquareFrame,
    mode: DragMode,
    drag: Offset,
    m: CropMetrics,
    minSizePx: Float
): SquareFrame {
    val right = f.left + f.size
    val bottom = f.top + f.size
    val dispRight = m.dispLeft + m.dispWidth
    val dispBottom = m.dispTop + m.dispHeight

    return when (mode) {
        DragMode.BottomRight -> {
            val maxS = min(dispRight - f.left, dispBottom - f.top)
            val ns = (f.size + (drag.x + drag.y) / 2f).coerceIn(min(minSizePx, maxS), maxS)
            f.copy(size = ns)
        }
        DragMode.BottomLeft -> {
            val maxS = min(right - m.dispLeft, dispBottom - f.top)
            val ns = (f.size + (-drag.x + drag.y) / 2f).coerceIn(min(minSizePx, maxS), maxS)
            f.copy(left = right - ns, size = ns)
        }
        DragMode.TopRight -> {
            val maxS = min(dispRight - f.left, bottom - m.dispTop)
            val ns = (f.size + (drag.x - drag.y) / 2f).coerceIn(min(minSizePx, maxS), maxS)
            f.copy(top = bottom - ns, size = ns)
        }
        DragMode.TopLeft -> {
            val maxS = min(right - m.dispLeft, bottom - m.dispTop)
            val ns = (f.size + (-drag.x - drag.y) / 2f).coerceIn(min(minSizePx, maxS), maxS)
            f.copy(left = right - ns, top = bottom - ns, size = ns)
        }
        else -> f
    }
}

/** Map the on-screen square back to source-bitmap pixels, clamped so the rect always lies inside the image. */
private fun toCropRect(f: SquareFrame, m: CropMetrics, imageWidth: Int, imageHeight: Int): CropRect {
    val left = ((f.left - m.dispLeft) / m.scale).roundToInt().coerceIn(0, imageWidth - 1)
    val top = ((f.top - m.dispTop) / m.scale).roundToInt().coerceIn(0, imageHeight - 1)
    val size = (f.size / m.scale).roundToInt().coerceIn(1, min(imageWidth - left, imageHeight - top))
    return CropRect(left, top, size)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCornerBrackets(
    f: SquareFrame,
    length: Float,
    strokeWidth: Float,
    color: androidx.compose.ui.graphics.Color
) {
    val right = f.left + f.size
    val bottom = f.top + f.size
    fun bracket(x: Float, y: Float, dx: Float, dy: Float) {
        drawLine(color, Offset(x, y), Offset(x + dx, y), strokeWidth, cap = StrokeCap.Round)
        drawLine(color, Offset(x, y), Offset(x, y + dy), strokeWidth, cap = StrokeCap.Round)
    }
    bracket(f.left, f.top, length, length)
    bracket(right, f.top, -length, length)
    bracket(f.left, bottom, length, -length)
    bracket(right, bottom, -length, -length)
}

/** Decode [uri] into an upright bitmap, applying the EXIF orientation cameras record for portrait shots. */
private fun decodeOrientedBitmap(context: Context, uri: Uri): Bitmap {
    val raw = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?: error("avatar image stream unavailable")
    val decoded = BitmapFactory.decodeByteArray(raw, 0, raw.size) ?: error("avatar image not decodable")

    val orientation = ByteArrayInputStream(raw).use { stream ->
        ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    }
    val degrees = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    if (degrees == 0f) return decoded

    val rotated = Bitmap.createBitmap(
        decoded, 0, 0, decoded.width, decoded.height,
        Matrix().apply { postRotate(degrees) }, true
    )
    if (rotated !== decoded) decoded.recycle()
    return rotated
}

/** Crop the square [rect] out of [source], downscale to the avatar cap, and encode as JPEG bytes. */
private fun cropToJpegBytes(source: Bitmap, rect: CropRect): ByteArray {
    val cropped = Bitmap.createBitmap(source, rect.left, rect.top, rect.size, rect.size)
    val target = min(rect.size, MAX_AVATAR_PX)
    val scaled = if (target != cropped.width) Bitmap.createScaledBitmap(cropped, target, target, true) else cropped
    if (scaled !== cropped && cropped !== source) cropped.recycle()

    val bytes = ByteArrayOutputStream().use { out ->
        scaled.compress(Bitmap.CompressFormat.JPEG, AVATAR_JPEG_QUALITY, out)
        out.toByteArray()
    }
    if (scaled !== source) scaled.recycle()
    return bytes
}

@Preview(showBackground = true)
@Composable
private fun AvatarCropContentPreview() {
    FoodTrackerTheme {
        AvatarCropContent(
            image = previewImage(),
            processing = false,
            contentInsets = PaddingValues(0.dp),
            onCancel = {},
            onConfirm = {}
        )
    }
}

@Composable
private fun previewImage(): ImageBitmap = remember {
    val bmp = ImageBitmap(900, 600)
    val canvas = GraphicsCanvas(bmp)
    val paint = Paint().apply { color = OrangeMain }
    canvas.drawRect(0f, 0f, 900f, 600f, paint)
    paint.color = MintGreen
    canvas.drawRect(180f, 120f, 720f, 480f, paint)
    bmp
}
