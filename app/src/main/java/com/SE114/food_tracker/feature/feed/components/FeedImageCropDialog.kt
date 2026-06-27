package com.SE114.food_tracker.feature.feed.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.exifinterface.media.ExifInterface
import com.SE114.food_tracker.core.designsystem.theme.CardWhite
import com.SE114.food_tracker.core.designsystem.theme.OrangeMain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

private const val FeedPostAspectRatio = 0.82f
private const val MaxCropZoom = 4f
private const val FeedPostImageWidth = 960
private const val FeedPostImageQuality = 80
private const val MinFeedPostImageWidth = 640
private const val MinFeedPostImageQuality = 60
private const val MaxFeedPostImageBytes = 1 * 1024 * 1024
private const val MaxFeedCropDecodeSize = 2048

@Composable
fun FeedImageCropDialog(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onConfirm: (Uri) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bitmap by remember(imageUri) { mutableStateOf<Bitmap?>(null) }
    var error by remember(imageUri) { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var cropSize by remember { mutableStateOf(IntSize.Zero) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val navigationBarBottom = with(density) {
        WindowInsets.navigationBars.getBottom(density).toDp()
    }

    LaunchedEffect(imageUri) {
        error = null
        bitmap = withContext(Dispatchers.IO) {
            runCatching { context.decodeFeedCropBitmap(imageUri) }
                .onFailure { error = it.message ?: "Không đọc được ảnh" }
                .getOrNull()
        }
        zoom = 1f
        offset = Offset.Zero
    }

    fun clampOffset(candidate: Offset, candidateZoom: Float = zoom): Offset {
        val source = bitmap ?: return Offset.Zero
        if (cropSize == IntSize.Zero) return Offset.Zero

        val baseScale = max(
            cropSize.width / source.width.toFloat(),
            cropSize.height / source.height.toFloat()
        )
        val scaledWidth = source.width * baseScale * candidateZoom
        val scaledHeight = source.height * baseScale * candidateZoom
        val maxX = max(0f, (scaledWidth - cropSize.width) / 2f)
        val maxY = max(0f, (scaledHeight - cropSize.height) / 2f)
        return Offset(
            x = candidate.x.coerceIn(-maxX, maxX),
            y = candidate.y.coerceIn(-maxY, maxY)
        )
    }

    Dialog(
        onDismissRequest = {
            if (!isSaving) onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding()
                .padding(
                    start = 22.dp,
                    top = 18.dp,
                    end = 22.dp,
                    bottom = navigationBarBottom.coerceAtLeast(28.dp) + 18.dp
                )
        ) {
            IconButton(
                onClick = onDismiss,
                enabled = !isSaving,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Đóng",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 46.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Căn ảnh bài viết",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Kéo ảnh để căn khung, chụm hai ngón để phóng to",
                        color = Color.White.copy(alpha = 0.64f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(FeedPostAspectRatio)
                        .clip(RoundedCornerShape(34.dp))
                        .background(Color(0xFF191713))
                        .onSizeChanged {
                            cropSize = it
                            offset = clampOffset(offset)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val source = bitmap
                    when {
                        source != null -> {
                            Image(
                                bitmap = source.asImageBitmap(),
                                contentDescription = "Ảnh xem trước",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = zoom
                                        scaleY = zoom
                                        translationX = offset.x
                                        translationY = offset.y
                                    }
                                    .pointerInput(source, cropSize) {
                                        detectTransformGestures { _, pan, gestureZoom, _ ->
                                            val nextZoom = (zoom * gestureZoom).coerceIn(1f, MaxCropZoom)
                                            zoom = nextZoom
                                            offset = clampOffset(offset + pan, nextZoom)
                                        }
                                    }
                            )
                        }

                        error != null -> Text(
                            text = error.orEmpty(),
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(18.dp)
                        )

                        else -> CircularProgressIndicator(color = OrangeMain)
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            enabled = !isSaving,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Hủy", color = Color.White.copy(alpha = 0.76f))
                        }

                        Button(
                            onClick = {
                                val source = bitmap ?: return@Button
                                if (cropSize == IntSize.Zero) return@Button

                                scope.launch {
                                    isSaving = true
                                    runCatching {
                                        withContext(Dispatchers.IO) {
                                            context.saveFeedCrop(source, cropSize, zoom, offset)
                                        }
                                    }.onSuccess { croppedUri ->
                                        onConfirm(croppedUri)
                                    }.onFailure {
                                        error = it.message ?: "Không cắt được ảnh"
                                    }
                                    isSaving = false
                                }
                            },
                            enabled = bitmap != null && !isSaving,
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeMain),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = CardWhite,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Xác nhận", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Context.decodeFeedCropBitmap(uri: Uri): Bitmap {
    val rawBytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?: error("Không đọc được ảnh")
    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, boundsOptions)

    val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = calculateInSampleSize(boundsOptions, MaxFeedCropDecodeSize)
    }
    var decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOptions)
        ?: error("Ảnh không hợp lệ")

    val rotationDegrees = contentResolver.openInputStream(uri)?.use { input ->
        val orientation = ExifInterface(input).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    } ?: 0f

    if (rotationDegrees != 0f) {
        val rotated = Bitmap.createBitmap(
            decoded,
            0,
            0,
            decoded.width,
            decoded.height,
            Matrix().apply { postRotate(rotationDegrees) },
            true
        )
        if (rotated != decoded) {
            decoded.recycle()
            decoded = rotated
        }
    }

    return decoded
}

private fun calculateInSampleSize(
    options: BitmapFactory.Options,
    maxSize: Int
): Int {
    var inSampleSize = 1
    val halfWidth = options.outWidth / 2
    val halfHeight = options.outHeight / 2

    while (halfWidth / inSampleSize >= maxSize || halfHeight / inSampleSize >= maxSize) {
        inSampleSize *= 2
    }

    return inSampleSize
}

private fun Context.saveFeedCrop(
    bitmap: Bitmap,
    cropSize: IntSize,
    zoom: Float,
    offset: Offset
): Uri {
    val baseScale = max(
        cropSize.width / bitmap.width.toFloat(),
        cropSize.height / bitmap.height.toFloat()
    )
    val totalScale = baseScale * zoom

    val sourceLeft = bitmap.width / 2f + (-cropSize.width / 2f - offset.x) / totalScale
    val sourceTop = bitmap.height / 2f + (-cropSize.height / 2f - offset.y) / totalScale
    val sourceWidth = cropSize.width / totalScale
    val sourceHeight = cropSize.height / totalScale

    val left = sourceLeft.roundToInt().coerceIn(0, bitmap.width - 1)
    val top = sourceTop.roundToInt().coerceIn(0, bitmap.height - 1)
    val width = sourceWidth.roundToInt().coerceIn(1, bitmap.width - left)
    val height = sourceHeight.roundToInt().coerceIn(1, bitmap.height - top)

    val cropped = Bitmap.createBitmap(bitmap, left, top, width, height)
    val targetWidth = FeedPostImageWidth
    val targetHeight = (targetWidth / FeedPostAspectRatio).roundToInt()
    val output = Bitmap.createScaledBitmap(cropped, targetWidth, targetHeight, true)
    if (output != cropped) cropped.recycle()

    val directory = File(cacheDir, "feed_post_crops").apply { mkdirs() }
    val file = File(directory, "feed_crop_${System.currentTimeMillis()}.jpg")
    file.outputStream().use { out ->
        out.write(output.compressFeedPostJpeg())
    }
    output.recycle()
    return Uri.fromFile(file)
}

private fun Bitmap.compressFeedPostJpeg(): ByteArray {
    var current = this
    var currentWidth = width
    var lastBytes = ByteArray(0)

    while (true) {
        var quality = FeedPostImageQuality
        while (quality >= MinFeedPostImageQuality) {
            val stream = ByteArrayOutputStream()
            current.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            lastBytes = stream.toByteArray()

            if (lastBytes.size <= MaxFeedPostImageBytes) {
                if (current !== this) current.recycle()
                return lastBytes
            }

            quality -= 5
        }

        if (currentWidth <= MinFeedPostImageWidth) {
            if (current !== this) current.recycle()
            return lastBytes
        }

        val nextWidth = (currentWidth * 0.85f)
            .roundToInt()
            .coerceAtLeast(MinFeedPostImageWidth)
        val nextHeight = (nextWidth / FeedPostAspectRatio).roundToInt()
        val resized = Bitmap.createScaledBitmap(current, nextWidth, nextHeight, true)
        if (current !== this) current.recycle()
        current = resized
        currentWidth = nextWidth
    }
}
