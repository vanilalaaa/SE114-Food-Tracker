package com.SE114.food_tracker.feature.diary

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.SE114.food_tracker.core.designsystem.theme.*
import timber.log.Timber
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodSourceScreen(
    categories: List<DiaryCategory> = emptyList(),
    onBack: () -> Unit,
    onImageCaptured: (Uri) -> Unit, // Callback truyền Uri hình ảnh lên cho màn hình cha
    onPresetSelected: (DiaryCategory) -> Unit = {},
    onManualSelected: () -> Unit = {}
) {
    var showPresetSheet by remember { mutableStateOf(false) }

    // Trạng thái lưu Uri ảnh sau khi chụp hoặc chọn từ Gallery để kích hoạt chế độ Preview
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // ── XỬ LÝ PICK GALLERY ──────────────────────────────────────────────────
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri // Thay vì gọi thẳng callback, chuyển sang chế độ Preview
        }
    }

    // ── XỬ LÝ CHỤP CAMERA ──────────────────────────────────────────────────
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { isSuccess: Boolean ->
        if (isSuccess) {
            tempCameraUri?.let { uri ->
                selectedImageUri = uri // Thay vì gọi thẳng callback, chuyển sang chế độ Preview
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFFFF5E4)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            // Header bar chung cho cả 2 chế độ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedImageUri != null) "Xem trước ảnh" else "Thêm món ăn",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                IconButton(
                    onClick = {
                        if (selectedImageUri != null) {
                            selectedImageUri = null // Nếu đang preview thì nhấn Đóng sẽ quay lại chọn nguồn
                        } else {
                            onBack()
                        }
                    },
                    modifier = Modifier.background(Color.Transparent, CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Đóng", modifier = Modifier.size(28.dp))
                }
            }

            Spacer(Modifier.height(32.dp))

            // RẼ NHÁNH GIAO DIỆN CHÍNH
            if (selectedImageUri != null) {
                // 1. GIAO DIỆN XEM TRƯỚC ẢNH (IMAGE PREVIEW)
                ImagePreviewSection(
                    imageUri = selectedImageUri!!,
                    onRetry = { selectedImageUri = null },
                    onConfirm = {
                        onImageCaptured(selectedImageUri!!)
                    }
                )
            } else {
                // 2. GIAO DIỆN CHỌN NGUỒN BAN ĐẦU
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SourceCard(
                        label = "Chụp ảnh",
                        icon = Icons.Outlined.PhotoCamera,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            runCatching { createTempImageUri(context) }
                                .onSuccess { uri ->
                                    tempCameraUri = uri
                                    cameraLauncher.launch(uri)
                                }
                                .onFailure { e ->
                                    Timber.e(e, "Không tạo được file tạm để chụp ảnh")
                                }
                        }
                    )
                    SourceCard(
                        label = "Thư viện",
                        icon = Icons.Outlined.Image,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                }

                Spacer(Modifier.height(40.dp))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray.copy(alpha = 0.4f))
                    Text("HOẶC CHỌN NHANH", modifier = Modifier.padding(horizontal = 16.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray.copy(alpha = 0.4f))
                }

                Spacer(Modifier.height(24.dp))

                QuickActionItem(
                    title = "Món ăn có sẵn",
                    subtitle = "Chọn từ danh sách đã lưu",
                    icon = Icons.Outlined.RestaurantMenu,
                    iconBgColor = Color(0xFFF7C7BB),
                    onClick = { showPresetSheet = true }
                )
            }
        }
    }

    // ── BOTTOM SHEET MÓN ĂN CÓ SẴN ──────────────────────────────
    if (showPresetSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPresetSheet = false },
            containerColor = Color(0xFFFFF9F5),
            dragHandle = { BottomSheetDefaults.DragHandle() },
            modifier = Modifier.fillMaxHeight(0.85f)
        ) {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🍜", fontSize = 24.sp)
                        Spacer(Modifier.width(12.dp))
                        Text("Món ăn có sẵn", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { showPresetSheet = false }, modifier = Modifier.background(Color(0xFFF0F0F0), CircleShape).size(36.dp)) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(20.dp))
                    }
                }

                if (categories.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Chưa có món ăn nào trong danh mục.", color = Color.Gray)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize().padding(bottom = 32.dp)
                    ) {
                        items(categories) { category ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White)
                                    .clickable {
                                        showPresetSheet = false
                                        onPresetSelected(category)
                                        onManualSelected()
                                    }
                                    .padding(vertical = 12.dp, horizontal = 4.dp)
                            ) {
                                Text(category.iconUrl ?: "🍽️", fontSize = 26.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = category.name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    color = Color.DarkGray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── COMPOSABLE PHẦN XEM TRƯỚC ẢNH CHỤP ─────────────────────────────────────
@Composable
private fun ImagePreviewSection(
    imageUri: Uri,
    onRetry: () -> Unit,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Khung hình vuông chứa ảnh Center Crop
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .aspectRatio(1f) // Đảm bảo tỉ lệ khung là hình vuông tuyệt đối 1:1
                    .fillMaxWidth(0.85f), // Chiếm 85% chiều ngang màn hình
                color = Color.White,
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 4.dp
            ) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Ảnh xem trước món ăn",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Crop // Cắt góc dạng Center Crop
                )
            }
        }

        // Cụm nút chức năng ở dưới đáy màn hình
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SettingActionOrange), // Màu cam cam ấm phù hợp chủ đề
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp), tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Xác nhận & Tiếp tục", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            OutlinedButton(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.DarkGray),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Chụp lại / Chọn ảnh khác", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

private fun createTempImageUri(context: Context): Uri {
    val tempFile = File.createTempFile(
        "JPEG_${System.currentTimeMillis()}_",
        ".jpg",
        context.cacheDir
    ).apply {
        createNewFile()
        deleteOnExit()
    }
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        tempFile
    )
}

@Composable
fun SourceCard(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier
            .height(163.dp)
            .clickable { onClick() },
        color = Color(0xFFFCE4D6),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 0.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, modifier = Modifier.size(28.dp), tint = Color(0xFF4A4A4A))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF333333))
        }
    }
}

@Composable
fun QuickActionItem(title: String, subtitle: String, icon: ImageVector, iconBgColor: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(iconBgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, modifier = Modifier.size(24.dp), tint = Color.DarkGray)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                Text(subtitle, color = Color.Gray, fontSize = 14.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
        }
    }
}