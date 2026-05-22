package com.example

import android.Manifest
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.AppDatabase
import com.example.data.MediaItem
import com.example.data.MediaRepository
import com.example.ui.CameraPreview
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MediaViewModel
import com.example.viewmodel.MediaViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Init database & repository
        val database = AppDatabase.getDatabase(this)
        val repository = MediaRepository(database.mediaDao())
        val viewModelFactory = MediaViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, viewModelFactory)[MediaViewModel::class.java]

        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: MediaViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val isRendering by viewModel.isRendering.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF09090B), // Minimalist Dark theme background
        bottomBar = {
            if (!isRendering && viewModel.editorSelectedMedia.collectAsStateWithLifecycle().value == null) {
                BottomNavigationBar(
                    selectedTab = currentTab,
                    onTabSelected = { viewModel.setTab(it) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                "RECORDER" -> ScreenRecorderTab(viewModel = viewModel)
                "TIKTOK" -> TikTokDownloaderTab(viewModel = viewModel)
                "PHOTOBOOTH" -> PhotoBoothTab(viewModel = viewModel)
                "EDITOR" -> VideoEditorTab(viewModel = viewModel)
                "LIBRARY" -> LibraryTab(viewModel = viewModel)
                else -> ScreenRecorderTab(viewModel = viewModel)
            }

            // Exporter Pro progress blocker overlay
            if (isRendering) {
                RenderProgressOverlay(viewModel = viewModel)
            }
        }
    }
}

// --- STANDARD NAVIGATION BOTTOM BAR ---
@Composable
fun BottomNavigationBar(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars),
        color = Color(0xFF121214),
        tonalElevation = 8.dp,
        border = BorderStroke(1.dp, Color(0xFF27272A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                Triple("RECORDER", "Quay MH", Icons.Default.FiberManualRecord),
                Triple("TIKTOK", "Tải TikTok", Icons.Default.DownloadForOffline),
                Triple("PHOTOBOOTH", "Photo Booth", Icons.Default.CameraAlt),
                Triple("LIBRARY", "Thư thư viện", Icons.Default.VideoLibrary)
            )

            tabs.forEach { (tabId, label, icon) ->
                val isSelected = selectedTab == tabId || (tabId == "LIBRARY" && selectedTab == "EDITOR")
                val toneColor = if (isSelected) Color(0xFFE11D48) else Color(0xFF71717A)
                val scale = animateFloatAsState(if (isSelected) 1.05f else 0.95f, label = "tabScale")

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("nav_tab_$tabId")
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTabSelected(tabId) }
                        )
                        .scale(scale.value),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = toneColor,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = label,
                        color = toneColor,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )

                    // Active dot indicator
                    if (isSelected) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE11D48))
                        )
                    }
                }
            }
        }
    }
}


// ============== TAB 1: SCREEN RECORDER ==============
@Composable
fun ScreenRecorderTab(viewModel: MediaViewModel) {
    val context = LocalContext.current
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val recordingTime by viewModel.recordingTime.collectAsStateWithLifecycle()
    val recRes by viewModel.recResolution.collectAsStateWithLifecycle()
    val recFps by viewModel.recFps.collectAsStateWithLifecycle()
    val recAudio by viewModel.recAudioMode.collectAsStateWithLifecycle()
    val waveformLevels by viewModel.waveformLevels.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()

    var showResDialog by remember { mutableStateOf(false) }
    var showFpsDialog by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showProjectionConsentDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            // Elegant title header
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "BỘ GHI MÀN HÌNH CHUYÊN NGHIỆP",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Xuất video cực sắc nét, hỗ trợ chuẩn Ultra HD 4K rực rỡ",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        item {
            // Immersive Recorder Viewport (Dynamic dark core card)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF1E1B4B), Color(0xFF0F172A))
                        )
                    )
                    .border(1.dp, Color(0xFF312E81), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (isRecording) {
                        // Flashing recording indicator
                        val infiniteTransition = rememberInfiniteTransition()
                        val pulseAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.4f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ), label = "pulse"
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444))
                                    .alpha(pulseAlpha)
                            )
                            Text(
                                text = "ĐANG GHI MÀN HÌNH",
                                color = Color(0xFFEF4444),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        // Recording timer Display
                        Text(
                            text = String.format(
                                Locale.getDefault(),
                                "%02d:%02d",
                                recordingTime / 60,
                                recordingTime % 60
                            ),
                            color = Color.White,
                            fontSize = 54.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        // Reactive Waveform equalizer simulation
                        Row(
                            modifier = Modifier
                                .height(50.dp)
                                .fillMaxWidth(0.8f),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            waveformLevels.forEach { level ->
                                var heightState by remember { mutableStateOf(0.1f) }
                                LaunchedEffect(level) {
                                    heightState = level
                                }
                                val animatedHeight by animateFloatAsState(
                                    targetValue = heightState * 40f + 4f,
                                    animationSpec = tween(120), label = "waveformHeight"
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(animatedHeight.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(Color(0xFFEF4444), Color(0xFFF43F5E), Color(0xFFEC4899))
                                            )
                                        )
                                )
                            }
                        }
                    } else {
                        // Inactive visual interface
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Camera Icon",
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(64.dp)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Thiết bị đã sẵn sàng",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$recRes @ $recFps",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Large circular trigger Record Button
                    Button(
                        onClick = {
                            if (isRecording) {
                                viewModel.stopRecording(context)
                                Toast.makeText(context, "Đã lưu video quay màn hình vào Thư viện!", Toast.LENGTH_SHORT).show()
                                viewModel.setTab("LIBRARY")
                            } else {
                                showProjectionConsentDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording) Color(0xFFEF4444) else Color(0xFFE11D48)
                        ),
                        modifier = Modifier
                            .height(56.dp)
                            .testTag("record_action_btn")
                            .padding(horizontal = 24.dp),
                        shape = RoundedCornerShape(28.dp),
                        border = BorderStroke(2.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = "Record Trigger"
                            )
                            Text(
                                text = if (isRecording) "DỪNG VÀ KÈM FILE" else "BẮT ĐẦU GHI",
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            // Screen recorder parameter settings
            Text(
                text = "CẤU HÌNH THÔNG SỐ EXPORT",
                color = Color(0xFFCBD5E1),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp),
                letterSpacing = 1.sp
            )
        }

        item {
            // Configuration select tags UI
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Resolution Selector
                ConfigRowItem(
                    title = "Độ phân giải video",
                    value = recRes,
                    icon = Icons.Default.AspectRatio,
                    onClick = { showResDialog = true }
                )

                // FPS Selector
                ConfigRowItem(
                    title = "Tốc độ khung hình (FPS)",
                    value = recFps,
                    icon = Icons.Default.Timer,
                    onClick = { showFpsDialog = true }
                )

                // Audio Selector
                ConfigRowItem(
                    title = "Nguồn âm thanh",
                    value = recAudio,
                    icon = Icons.Default.VolumeUp,
                    onClick = { showAudioDialog = true }
                )
            }
        }

        item {
            // Tips banner
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1F22)),
                border = BorderStroke(1.dp, Color(0xFF2E2F32)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Tips",
                        tint = Color(0xFFF59E0B)
                    )
                    Column {
                        Text(
                            text = "Mẹo nâng cao cho chất lượng 4K",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Để xuất sản phẩm 4K không giật lag, hãy dọn dẹp các ứng dụng chạy ngầm trước khi thực hiện ghi hệ thống.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }

    // Resolution Choice Dialog
    if (showResDialog) {
        ParameterSelectionDialog(
            title = "Chọn độ phân giải ghi",
            options = listOf("4K UHD (2160p)", "Full HD (1080p)", "HD (720p)"),
            selectedValue = recRes,
            onDismiss = { showResDialog = false },
            onSelect = { viewModel.recResolution.value = it }
        )
    }

    // FPS Choice Dialog
    if (showFpsDialog) {
        ParameterSelectionDialog(
            title = "Tỷ lệ khung hình",
            options = listOf("60 FPS", "30 FPS", "24 FPS Cinema"),
            selectedValue = recFps,
            onDismiss = { showFpsDialog = false },
            onSelect = { viewModel.recFps.value = it }
        )
    }

    // Audio Choice Dialog
    if (showAudioDialog) {
        ParameterSelectionDialog(
            title = "Cài đặt ghi âm",
            options = listOf("System Audio & Mic", "Chỉ Mic (Microphone)", "Tắt tiếng âm thanh"),
            selectedValue = recAudio,
            onDismiss = { showAudioDialog = false },
            onSelect = { viewModel.recAudioMode.value = it }
        )
    }

    // Authentic Screen-Sharing Android System Dialog
    if (showProjectionConsentDialog) {
        Dialog(
            onDismissRequest = { showProjectionConsentDialog = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF202124))
                    .border(1.dp, Color(0xFF35363A), RoundedCornerShape(28.dp))
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Cast/ScreenShare icon indicator
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF303135))
                            .border(1.dp, Color(0xFF424348), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Cast Screen Icon",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Title
                    Text(
                        text = "Chia sẻ màn hình của bạn với VMedia Studio?",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        lineHeight = 28.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Selector Box option (replicated perfectly from image)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, Color(0xFF8E918F).copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                            .clickable { /* Tap to select options */ }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Chia sẻ toàn bộ màn hình",
                            color = Color(0xFFE3E2E6),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Dropdown Options",
                            tint = Color(0xFFC4C7C5),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Content warning description
                    Text(
                        text = "Khi bạn chia sẻ toàn bộ màn hình, VMedia Studio sẽ thấy được mọi nội dung trên màn hình của bạn. Vì vậy, hãy thận trọng để không làm lộ thông tin như mật khẩu, thông tin thanh toán, tin nhắn, ảnh, âm thanh và video.",
                        color = Color(0xFFC4C7C5),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Start
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Buttons Row (Thoát & Chia sẻ màn hình)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Exit ("Thoát")
                        Button(
                            onClick = { showProjectionConsentDialog = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
                            ),
                            border = BorderStroke(1.dp, Color(0xFF8E918F)),
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text(
                                text = "Thoát",
                                color = Color(0xFFA8C8FF),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Share Screen ("Chia sẻ màn hình")
                        Button(
                            onClick = {
                                showProjectionConsentDialog = false
                                viewModel.startRecording()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFA8C8FF)
                            ),
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text(
                                text = "Chia sẻ màn hình",
                                color = Color(0xFF003062),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConfigRowItem(
    title: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF18181B))
            .border(1.dp, Color(0xFF27272A), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                color = Color(0xFFF43F5E),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.Default.NavigateNext,
                contentDescription = "Open Settings",
                tint = Color(0xFF71717A),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun ParameterSelectionDialog(
    title: String,
    options: List<String>,
    selectedValue: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true),
        containerColor = Color(0xFF1E1E22),
        title = {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                options.forEach { option ->
                    val isChosen = option == selectedValue
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isChosen) Color(0xFFE11D48).copy(alpha = 0.15f) else Color.Transparent)
                            .clickable {
                                onSelect(option)
                                onDismiss()
                            }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option,
                            color = if (isChosen) Color(0xFFF43F5E) else Color.White,
                            fontSize = 14.sp,
                            fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium
                        )
                        if (isChosen) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active",
                                tint = Color(0xFFF43F5E),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng", color = Color(0xFF94A3B8))
            }
        }
    )
}


// ============== TAB 2: TIKTOK DOWNLOADER ==============
@Composable
fun TikTokDownloaderTab(viewModel: MediaViewModel) {
    val context = LocalContext.current
    val inputUrl by viewModel.tiktokInputUrl.collectAsStateWithLifecycle()
    val dlState by viewModel.tiktokUiState.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "TẢI TIKTOK KO LOGO",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Trích xuất video độ nét cao không hề dính watermark chất lượng cực định",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        item {
            // Elegant Input Field Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121214)),
                border = BorderStroke(1.dp, Color(0xFF27272A)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Nhập liên kết TikTok",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E1E22))
                            .border(1.dp, Color(0xFF3F3F46), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Link icon",
                            tint = Color(0xFF71717A),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = inputUrl,
                            onValueChange = { viewModel.tiktokInputUrl.value = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tiktok_url_input"),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = Color.White,
                                fontSize = 14.sp
                            ),
                            singleLine = true,
                            decorationBox = @Composable { innerTextField: @Composable () -> Unit ->
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    if (inputUrl.isEmpty()) {
                                        Text(
                                            text = "Dán url video tiktok tại đây...",
                                            color = Color(0xFF52525B),
                                            fontSize = 13.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        // Clear paste shortcut trigger
                        if (inputUrl.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.tiktokInputUrl.value = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    if (clipboard.hasPrimaryClip() && clipboard.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true) {
                                        val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                        if (clipText.contains("tiktok")) {
                                            viewModel.tiktokInputUrl.value = clipText
                                            Toast.makeText(context, "Đã dán link từ bộ nhớ tạm!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Không có liên kết TikTok hợp lệ trong bộ nhớ tạm.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Paste",
                                    tint = Color(0xFFE11D48),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Scan button
                    Button(
                        onClick = { viewModel.fetchTikTokVideo() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("analyze_tiktok_btn")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Parse stream link"
                            )
                            Text("PHÂN TÍCH LIÊN KẾT", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        }
                    }
                }
            }
        }

        // DYNAMIC PREVIEW STATES
        item {
            AnimatedContent(
                targetState = dlState,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                }, label = "dlStateAnim"
            ) { state ->
                when (state) {
                    is MediaViewModel.TikTokDlState.Idle -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = "Waiting",
                                tint = Color(0xFF27272A),
                                modifier = Modifier.size(72.dp)
                            )
                            Text(
                                text = "Sẵn sàng trích xuất video đỉnh cao không lo dính logo bản quyền",
                                color = Color(0xFF52525B),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 30.dp)
                            )
                        }
                    }

                    is MediaViewModel.TikTokDlState.Loading -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CircularProgressIndicator(color = Color(0xFFE11D48))
                            Text(
                                text = "Đang quét luồng video & giải mã chữ chìm logo...",
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                    }

                    is MediaViewModel.TikTokDlState.Found -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1F22)),
                            border = BorderStroke(1.dp, Color(0xFF3A3B40)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // User Avatar simulator
                                    Box(
                                        modifier = Modifier
                                            .size(45.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.radialGradient(
                                                    listOf(Color(0xFFE11D48), Color(0xFF2563EB))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = state.author.take(2).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = state.author,
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Nhà sáng tạo TikTok Trends",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Render video static thumbnail
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(state.coverUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "TikTok cover previews",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    // Simulated watermark free check symbol
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(10.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF10B981))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "KO LOGO BẢN QUYỀN",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.Default.PlayCircle,
                                        contentDescription = "Watch preview",
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(54.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = state.title,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.clearTikTokState() },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color(0xFF94A3B8)
                                        ),
                                        border = BorderStroke(1.dp, Color(0xFF3F3F46)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("HỦY KHUÔNG")
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.downloadTikTokWatermarkFree(
                                                context = context,
                                                title = state.title,
                                                author = state.author,
                                                playUrl = state.playUrl,
                                                coverUrl = state.coverUrl
                                            )
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        modifier = Modifier
                                            .weight(1.8f)
                                            .testTag("download_no_wm_btn")
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = "Download start file",
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text("TẢI KO LOGO (PRO)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    is MediaViewModel.TikTokDlState.Success -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B)),
                            border = BorderStroke(1.dp, Color(0xFF059669)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Tải xuống thành công",
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(48.dp)
                                )

                                Text(
                                    text = "ĐÃ TỔNG HỢP VIDEO THÀNH CÔNG",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = state.item.title,
                                    color = Color(0xFFA7F3D0),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.clearTikTokState() },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color.White
                                        ),
                                        border = BorderStroke(1.dp, Color(0xFF059669)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("TẢI TIẾP", fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = { viewModel.selectMediaForEditing(state.item) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                        modifier = Modifier.weight(1.5f)
                                    ) {
                                        Text("BIÊN TẬP 4K", color = Color(0xFF111827), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    is MediaViewModel.TikTokDlState.Error -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF451A03)),
                            border = BorderStroke(1.dp, Color(0xFF9A3412)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "Error",
                                    tint = Color(0xFFF97316)
                                )
                                Column {
                                    Text(
                                        text = "Nhận link thất bại",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = state.message,
                                        color = Color(0xFFFED7AA),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// ============== TAB 3: PHOTO BOOTH ==============
@Composable
fun PhotoBoothTab(viewModel: MediaViewModel) {
    val context = LocalContext.current
    val keyboardPhotos by viewModel.boothPhotos.collectAsStateWithLifecycle()
    val boothTimer by viewModel.boothTimer.collectAsStateWithLifecycle()
    val isBoothProcessing by viewModel.isBoothProcessing.collectAsStateWithLifecycle()
    val lastCollage by viewModel.lastCreatedCollage.collectAsStateWithLifecycle()

    val currentLayout by viewModel.boothLayout.collectAsStateWithLifecycle()
    val currentFilter by viewModel.boothFilter.collectAsStateWithLifecycle()
    val currentFrameColor by viewModel.boothFrameColor.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()

    // Permission tracking state
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "STUDIO PHOTO BOOTH",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Góc chụp ảnh dải hàn quốc, polaroid và lưu giữ kỷ niệm retro",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        item {
            // VIEWPORT VIEWFINDER BOX (Main Camera Canvas)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black)
                    .border(1.dp, Color(0xFF27272A), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (hasCameraPermission) {
                    if (lastCollage == null) {
                        // Live Viewfinder from CameraX
                        CameraPreview(modifier = Modifier.fillMaxSize())

                        // Apply screen filter preview tint
                        val filterOverlayColor = when (currentFilter) {
                            "Vintage Noir" -> Color.Black.copy(alpha = 0.35f)
                            "Classic Sepia" -> Color(0xFF8B5A2B).copy(alpha = 0.25f)
                            "Cyberpunk Neon" -> Color(0xFFE11D48).copy(alpha = 0.15f)
                            "Cosmic Dream" -> Color(0xFF7C3AED).copy(alpha = 0.2f)
                            else -> Color.Transparent
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(filterOverlayColor)
                        )

                        // If countdown is active, show giant pulsing countdown
                        if (boothTimer >= 0) {
                            val displayNum = if (boothTimer == 0) "CHỤP!" else boothTimer.toString()
                            val scaleMultiplier = remember { Animatable(3f) }
                            LaunchedEffect(boothTimer) {
                                scaleMultiplier.snapTo(3f)
                                scaleMultiplier.animateTo(
                                    targetValue = 1f,
                                    animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow)
                                )
                            }
                            // Flashing white overlay when boothTimer is 0 (Shutter flash simulation!)
                            if (boothTimer == 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.White)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = displayNum,
                                    color = if (boothTimer == 0) Color(0xFFEC4899) else Color.White,
                                    fontSize = if (boothTimer == 0) 32.sp else 64.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.scale(scaleMultiplier.value)
                                )
                            }
                        }

                        // Bottom badge of live photo count (e.g. 2/4 shots)
                        if (keyboardPhotos.isNotEmpty()) {
                            val maxShots = if (currentLayout.contains("Strip") || currentLayout.contains("Grid")) 4 else 1
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(14.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.7f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Đã tích lũy: ${keyboardPhotos.size} / $maxShots ảnh mẫu",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                    } else {
                        // Collage Created Result
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF121214)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(File(lastCollage!!))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Finished Booth Collage",
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(12.dp)
                                    .shadow(6.dp)
                            )

                            // Quick controls to reset or delete
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = { viewModel.resetBooth() },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.7f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Chụp lại",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Lock illustration & standard request permission button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Camera Locked",
                            tint = Color(0xFFF43F5E),
                            modifier = Modifier.size(54.dp)
                        )
                        Text(
                            text = "Yêu cầu quyền truy cập Camera",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "VMedia Studio cần truy cập camera để thực hiện chức năng chụp xếp dải và chế độ Polaroid.",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                        Button(
                            onClick = { launcher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("CẤP QUYỀN CAMERA", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        if (hasCameraPermission && lastCollage == null) {
            item {
                // Large shutter trigger button
                Button(
                    onClick = {
                        viewModel.capturePhotoBoothStrip(context) {
                            Toast.makeText(context, "Snaaaap! Đã bắt dải phim...", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("pb_shoot_btn"),
                    shape = RoundedCornerShape(12.dp),
                    enabled = boothTimer < 0 && !isBoothProcessing
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isBoothProcessing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            Text("ĐANG KẾT XUẤT COLAGE...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Camera,
                                contentDescription = "Shot"
                            )
                            Text("BẮT ĐẦU CHỤP HÀN QUỐC", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        }
                    }
                }
            }

            item {
                // Options Grid config selectors
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF121214)),
                    border = BorderStroke(1.dp, Color(0xFF27272A)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Grid configuration choices
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("1. Chọn bố cục dải phim", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val layouts = listOf("Strip Vertical (4)", "Square Grid (2x2)", "Polaroid Space")
                                items(layouts) { lay ->
                                    val isSelected = currentLayout == lay
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) Color(0xFFEC4899) else Color(0xFF1E1E22))
                                            .clickable { viewModel.boothLayout.value = lay }
                                            .padding(horizontal = 14.dp, vertical = 7.dp)
                                    ) {
                                        Text(lay, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Filter configuration choices
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("2. Chọn tông lọc ảnh", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val filters = listOf("Vintage Noir", "Classic Sepia", "Cyberpunk Neon", "Cosmic Dream", "Natural")
                                items(filters) { filt ->
                                    val isSelected = currentFilter == filt
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) Color(0xFFEC4899) else Color(0xFF1E1E22))
                                            .clickable { viewModel.boothFilter.value = filt }
                                            .padding(horizontal = 14.dp, vertical = 7.dp)
                                    ) {
                                        Text(filt, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Filter frame border choices
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("3. Màu viền khung ảnh", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val borders = listOf("Pearl White", "Midnight Black", "Peach Glow", "Neon Violet")
                                items(borders) { bord ->
                                    val isSelected = currentFrameColor == bord
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) Color(0xFFEC4899) else Color(0xFF1E1E22))
                                            .clickable { viewModel.boothFrameColor.value = bord }
                                            .padding(horizontal = 14.dp, vertical = 7.dp)
                                    ) {
                                        Text(bord, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (lastCollage != null) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            Toast.makeText(context, "Đã lưu tác phẩm collager vào hệ máy!", Toast.LENGTH_SHORT).show()
                            viewModel.setTab("LIBRARY")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("XONG & LƯU LẠI", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.resetBooth() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFF27272A)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("CHỤP BỨC ẢNH KHÁC")
                    }
                }
            }
        }
    }
}


// ============== TAB 4: LIBRARY ==============
@Composable
fun LibraryTab(viewModel: MediaViewModel) {
    val context = LocalContext.current
    val allMedia by viewModel.allMedia.collectAsStateWithLifecycle()
    val filterType by viewModel.filterType.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Simple header title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "BỘ SƯU TẬP PHƯƠNG TIỆN",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Các sản phẩm quay phim, tải và ảnh Photo booth của bạn",
                    color = Color(0xFF71717A),
                    fontSize = 11.sp
                )
            }
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = "Folder",
                tint = Color(0xFFF59E0B)
            )
        }

        // Category selections selectors
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val categories = listOf("ALL" to "Tất cả", "RECORDING" to "Quay MH", "TIKTOK" to "Tải TikTok", "PHOTOBOOTH" to "Photo Booth", "EDITED" to "Đã Sửa")
            categories.forEach { (key, name) ->
                val isSelected = filterType == key
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFFE11D48) else Color(0xFF18181B))
                        .clickable { viewModel.setFilter(key) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = name,
                        color = if (isSelected) Color.White else Color(0xFF71717A),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // List Grid View of files
        val filteredList = if (filterType == "ALL") {
            allMedia
        } else {
            allMedia.filter { it.type == filterType }
        }

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = "Empty",
                        tint = Color(0xFF27272A),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "Danh mục này hiện chưa có nội dung gốc nào",
                        color = Color(0xFF3F3F46),
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredList) { media ->
                    MediaGridCard(
                        media = media,
                        onEdit = { viewModel.selectMediaForEditing(media) },
                        onDelete = {
                            viewModel.deleteMediaItem(media)
                            Toast.makeText(context, "Đã xóa phần tử khỏi thư viện!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MediaGridCard(
    media: MediaItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121214)),
        border = BorderStroke(1.dp, Color(0xFF27272A)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
    ) {
        Column {
            // Visual Banner Preview depending on category
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (media.type == "PHOTOBOOTH" && media.filePath.contains("sample_booth").not()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(File(media.filePath))
                            .crossfade(true)
                            .build(),
                        contentDescription = media.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (!media.thumbnailPath.isNullOrEmpty()) {
                    val imgModel = if (media.thumbnailPath!!.startsWith("http")) {
                        media.thumbnailPath
                    } else {
                        java.io.File(media.thumbnailPath!!)
                    }
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imgModel)
                            .crossfade(true)
                            .build(),
                        contentDescription = media.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Default abstract gradient cards
                    val paletteGradients = when (media.type) {
                        "RECORDING" -> Brush.linearGradient(listOf(Color(0xFF312E81), Color(0xFF0F172A)))
                        "TIKTOK" -> Brush.linearGradient(listOf(Color(0xFF022C22), Color(0xFF0F172A)))
                        "PHOTOBOOTH" -> Brush.linearGradient(listOf(Color(0xFF4C0519), Color(0xFF0F172A)))
                        else -> Brush.linearGradient(listOf(Color(0xFF18181B), Color(0xFF0F172A)))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(paletteGradients),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (media.type) {
                                "RECORDING" -> Icons.Default.FiberManualRecord
                                "TIKTOK" -> Icons.Default.MusicVideo
                                "PHOTOBOOTH" -> Icons.Default.Camera
                                else -> Icons.Default.Movie
                            },
                            contentDescription = "Abstract format",
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Overlay tag specs
                if (media.durationText != "Photo") {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(media.durationText, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Format Aspect Ratio overlay indicator
                val ratioText = if (media.width > media.height) "16:9 4K" else if (media.width == media.height) "1:1 SD" else "9:16 FHD"
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFE11D48).copy(alpha = 0.85f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (media.type == "EDITED" || media.filePath.contains("4K") || media.title.contains("4K")) "4K UHD" else ratioText,
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Info details content
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = media.title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = media.creatorName ?: "VMedia Library",
                        color = Color(0xFF71717A),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = { onDelete() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete item",
                            tint = Color(0xFF71717A),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}


// ============== TAB 5: PROFESSIONAL VIDEO EDITOR ==============
@Composable
fun VideoEditorTab(viewModel: MediaViewModel) {
    val context = LocalContext.current
    val media by viewModel.editorSelectedMedia.collectAsStateWithLifecycle() ?: return

    val editStartTrim by viewModel.editStartTrim.collectAsStateWithLifecycle()
    val editEndTrim by viewModel.editEndTrim.collectAsStateWithLifecycle()
    val editSpeed by viewModel.editSpeed.collectAsStateWithLifecycle()
    val editMusic by viewModel.editMusic.collectAsStateWithLifecycle()
    val editMusicVolume by viewModel.editMusicVolume.collectAsStateWithLifecycle()
    val editFilter by viewModel.editFilter.collectAsStateWithLifecycle()
    val editSubtitle by viewModel.editSubtitle.collectAsStateWithLifecycle()
    val editRatio by viewModel.editAspectRatio.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()

    var isPlaying by remember { mutableStateOf(false) }
    var playbackProgress by remember { mutableStateOf(0f) }

    // Simulated local video play timeline ticker
    val activeRunningMedia = media
    LaunchedEffect(isPlaying) {
        if (isPlaying && activeRunningMedia != null) {
            while (isPlaying) {
                delay(100)
                playbackProgress += 0.02f
                if (playbackProgress >= editEndTrim) {
                    playbackProgress = editStartTrim
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Elegant Back & title navigation headers
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.closeEditor() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "VSTUDIO BIÊN TẬP CHUYÊN NGHIỆP",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Trình chỉnh sửa cao cấp hỗ trợ ghim luồng tệp 4K",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
            }
        }

        item {
            // Dynamic Aspect Ratio Preview Box Canvas!
            val dynamicWidthPercent = when (editRatio) {
                "16:9" -> 1f
                "9:16" -> 0.56f
                else -> 0.75f // 1:1 square ratio visual size bounds
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(dynamicWidthPercent)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                        .border(1.dp, Color(0xFF3F3F46), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Preview static/dynamic cover graphic loader
                    if (activeRunningMedia != null) {
                        if (activeRunningMedia.filePath.contains("sample_booth")) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF27272A)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Xem trước luồng ảnh Polaroid", color = Color(0xFF71717A), fontSize = 11.sp)
                            }
                        } else if (activeRunningMedia.type == "RECORDING" || activeRunningMedia.type == "EDITED") {
                            // High performance simulated real-time screen-sharing video stream canvas!
                            val infiniteTransition = rememberInfiniteTransition(label = "simRec")
                            val rotationAngle by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(4000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ), label = "rotateCircle"
                            )
                            val pulseAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.2f,
                                targetValue = 1.0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ), label = "pulseRec"
                            )
                            
                            val playbackSeconds = (playbackProgress * 15).toInt() // representing standard length progression

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val width = size.width
                                val height = size.height

                                // Draw modern dark space high tech video canvas
                                val grad = Brush.linearGradient(
                                    listOf(Color(0xFF0F172A), Color(0xFF020617))
                                )
                                drawRect(brush = grad, size = size)

                                // Tech network grid matrix
                                val gridColor = Color(0xFF1E293B)
                                val gridSpacing = 40f
                                for (x in 0..width.toInt() step gridSpacing.toInt()) {
                                    drawLine(
                                        color = gridColor,
                                        start = Offset(x.toFloat(), 0f),
                                        end = Offset(x.toFloat(), height),
                                        strokeWidth = 1f
                                    )
                                }
                                for (y in 0..height.toInt() step gridSpacing.toInt()) {
                                    drawLine(
                                        color = gridColor,
                                        start = Offset(0f, y.toFloat()),
                                        end = Offset(width, y.toFloat()),
                                        strokeWidth = 1f
                                    )
                                }

                                // Interactive central orbit representing camera capturing/recording
                                val finalAngle = if (isPlaying) rotationAngle else 45f
                                val centralColor = Color(0xFFE11D48)
                                drawCircle(
                                    color = centralColor.copy(alpha = 0.1f),
                                    radius = 110f,
                                    center = Offset(width / 2, height / 2)
                                )
                                drawCircle(
                                    color = centralColor,
                                    radius = 110f,
                                    center = Offset(width / 2, height / 2),
                                    style = Stroke(
                                        width = 4f,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 15f), finalAngle)
                                    )
                                )

                                // Pulsing central ring
                                val radiusPulse = if (isPlaying) 110f + pulseAlpha * 30f else 125f
                                drawCircle(
                                    color = Color(0xFFE11D48).copy(alpha = 0.05f * (if (isPlaying) pulseAlpha else 1f)),
                                    radius = radiusPulse,
                                    center = Offset(width / 2, height / 2)
                                )

                                // Real-time updated time ticker text
                                val textPaint = android.graphics.Paint().apply {
                                    color = android.graphics.Color.WHITE
                                    textSize = 28f
                                    isAntiAlias = true
                                    textAlign = android.graphics.Paint.Align.LEFT
                                    typeface = android.graphics.Typeface.MONOSPACE
                                }
                                val formatTime = String.format("%02d:%02d", playbackSeconds / 60, playbackSeconds % 60)
                                drawContext.canvas.nativeCanvas.drawText(
                                    "PLAY [4K] $formatTime",
                                    35f,
                                    55f,
                                    textPaint
                                )

                                // Pulsing/Blinking RED dot
                                val finalAlpha = if (isPlaying) pulseAlpha else 1f
                                drawCircle(
                                    color = Color(0xFFEF4444).copy(alpha = finalAlpha),
                                    radius = 8f,
                                    center = Offset(340f, 45f)
                                )

                                // Active running green audio equalizer wave stream
                                val wavePath = Path()
                                wavePath.moveTo(40f, height - 60f)
                                val finalWaveOffset = if (isPlaying) (playbackProgress * 1500f) else 0f
                                for (i in 0..20) {
                                    val pointX = 40f + i * (width - 80f) / 20f
                                    val waveShift = (i * 0.7f) + (finalWaveOffset / 100f)
                                    val pointY = height - 60f + (Math.sin(waveShift.toDouble()) * 30f).toFloat() * (if (isPlaying) 1f else 0.4f)
                                    wavePath.lineTo(pointX, pointY)
                                }
                                drawPath(
                                    path = wavePath,
                                    color = Color(0xFF10B981),
                                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                                )

                                // Bottom metadata helper display message status
                                val statusTextMsg = if (isPlaying) "Đang phát luồng video ghi màn hình 4K..." else "Tạm dừng phát"
                                val smallTextPaint = android.graphics.Paint().apply {
                                    color = android.graphics.Color.parseColor("#94A3B8")
                                    textSize = 20f
                                    isAntiAlias = true
                                    textAlign = android.graphics.Paint.Align.CENTER
                                }
                                drawContext.canvas.nativeCanvas.drawText(
                                    statusTextMsg,
                                    width / 2f,
                                    height - 20f,
                                    smallTextPaint
                                )
                            }
                        } else if (!activeRunningMedia.thumbnailPath.isNullOrEmpty()) {
                            val imgModel = if (activeRunningMedia.thumbnailPath!!.startsWith("http")) {
                                activeRunningMedia.thumbnailPath
                            } else {
                                java.io.File(activeRunningMedia.thumbnailPath!!)
                            }
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(imgModel)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Active preview editor",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // Video fallback preview card
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF312E81), Color(0xFF1E1B4B))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MovieFilter,
                                    contentDescription = "Movie preview",
                                    tint = Color.White.copy(alpha = 0.3f),
                                    modifier = Modifier.size(72.dp)
                                )
                            }
                        }
                    }

                    // Apply Image Film filter overlay dynamically on screen!
                    val previewColorFilter = when (editFilter) {
                        "Monochrome Noir" -> Color.Black.copy(alpha = 0.35f)
                        "Warm Retro Film" -> Color(0xFF8B5A2B).copy(alpha = 0.25f)
                        "Cyber Sunset" -> Color(0xFFD946EF).copy(alpha = 0.15f)
                        "Aqua Cold" -> Color(0xFF06B6D4).copy(alpha = 0.15f)
                        else -> Color.Transparent
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(previewColorFilter)
                    )

                    // Subtitle overlay text on top
                    if (editSubtitle.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 20.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.8f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = editSubtitle,
                                color = Color.Yellow,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Playback status indicators
                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play toggle preview",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        }

        item {
            // Visual Timeline Slider track
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Điểm cắt đầu: ${(editStartTrim * 100).toInt()}%", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Điểm cắt cuối: ${(editEndTrim * 100).toInt()}%", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E1E22))
                        .border(1.dp, Color(0xFF27272A), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.CenterStart
                ) {
                    // Frame sliders ranges highlighter block background
                    val startOffsetPercent = editStartTrim
                    val widthPercent = editEndTrim - editStartTrim
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(widthPercent)
                            .padding(horizontal = (startOffsetPercent * 100).dp)
                            .background(Color(0xFFE11D48).copy(alpha = 0.3f))
                            .border(2.dp, Color(0xFFE11D48), RoundedCornerShape(4.dp))
                    )

                    // Video ticker mark indicator
                    if (playbackProgress > editStartTrim && playbackProgress < editEndTrim) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(2.dp)
                                .padding(horizontal = (playbackProgress * 100).dp)
                                .background(Color.White)
                        )
                    }
                }

                // Interactive sliders to edit bounds
                Row {
                    Slider(
                        value = editStartTrim,
                        onValueChange = { viewModel.editStartTrim.value = it.coerceAtMost(editEndTrim - 0.1f) },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color(0xFFE11D48)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Slider(
                        value = editEndTrim,
                        onValueChange = { viewModel.editEndTrim.value = it.coerceAtLeast(editStartTrim + 0.1f) },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color(0xFFE11D48)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            // Detailed custom attributes controllers list
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121214)),
                border = BorderStroke(1.dp, Color(0xFF27272A)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Aspect ratio picker
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Bố cục khung tỷ lệ", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("16:9", "9:16", "1:1").forEach { ratio ->
                                val selected = editRatio == ratio
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) Color(0xFFE11D48) else Color(0xFF1E1E22))
                                        .border(1.dp, if (selected) Color.White.copy(alpha = 0.3f) else Color.Transparent, RoundedCornerShape(8.dp))
                                        .clickable { viewModel.editAspectRatio.value = ratio }
                                        .padding(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    Text(ratio, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Multiplier speed picker
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Tốc độ chuyển động", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0.5f to "0.5x", 1.0f to "1.0x (Gốc)", 1.5f to "1.5x", 2.0f to "2.0x").forEach { (speedValue, textStr) ->
                                val selected = editSpeed == speedValue
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) Color(0xFFE11D48) else Color(0xFF1E1E22))
                                        .clickable { viewModel.editSpeed.value = speedValue }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Text(textStr, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Music Soundtrack Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Âm thanh nền sành điệu", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val playlist = listOf("None", "Chill Lofi Beats", "Cyberpunk Beat", "Upbeat Vlog", "Cinematic Strings")
                            items(playlist) { song ->
                                val selected = editMusic == song
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) Color(0xFFE11D48) else Color(0xFF1E1E22))
                                    .clickable { viewModel.editMusic.value = song }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    Text(song, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Coloring Filters
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Bộ lọc tông màu nghệ thuật", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val filterSet = listOf("Vivid Original", "Monochrome Noir", "Warm Retro Film", "Cyber Sunset", "Aqua Cold")
                            items(filterSet) { filterName ->
                                val selected = editFilter == filterName
                                Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) Color(0xFFE11D48) else Color(0xFF1E1E22))
                                    .clickable { viewModel.editFilter.value = filterName }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    Text(filterName, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Custom overlay text input
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Chữ và phụ đề lồng ghép", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E1E22))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = editSubtitle,
                                onValueChange = { viewModel.editSubtitle.value = it },
                                modifier = Modifier.weight(1f),
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                                singleLine = true,
                                decorationBox = @Composable { innerTextField: @Composable () -> Unit ->
                                    Box {
                                        if (editSubtitle.isEmpty()) {
                                            Text("Thêm chữ lồng video (e.g. My Epic Vlog)...", color = Color(0xFF52525B), fontSize = 12.sp)
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        item {
            // Giant Professional action trigger to compile in 4K resolution!
            Button(
                onClick = {
                    viewModel.triggerProfessional4KExport(context) {
                        Toast.makeText(context, "Đã hoàn tất kết xuất định dạng 4K cực chất lượng cao!", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("export_4k_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = "4K Premium renderer",
                        tint = Color.Yellow
                    )
                    Text("XUẤT VIDEO LẬP CHẤT LƯỢNG 4K ULTRA HD", fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                }
            }
        }
    }
}


// ============== RENDER OVERLAY (BLOCKER POPUP) ==============
@Composable
fun RenderProgressOverlay(viewModel: MediaViewModel) {
    val progress by viewModel.renderProgress.collectAsStateWithLifecycle()
    val statusMsg by viewModel.renderStatusMsg.collectAsStateWithLifecycle()

    Dialog(
        onDismissRequest = {}, // absolute blocker dialog
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
            border = BorderStroke(1.dp, Color(0xFF3F3F46)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    color = Color(0xFF10B981),
                    strokeWidth = 6.dp,
                    trackColor = Color(0xFF27272A),
                    modifier = Modifier.size(72.dp)
                )

                Text(
                    text = "XUẤT VIDEO 4K CHUYÊN NGHIỆP",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                // Render metrics estimates details
                val percent = (progress * 100).toInt()
                Text(
                    text = "$percent%",
                    color = Color(0xFF10B981),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Băng thông: H.265", color = Color(0xFF71717A), fontSize = 10.sp)
                    Text("Preset: Ultra 4K UHD", color = Color(0xFF71717A), fontSize = 10.sp)
                    Text("Renderer: GPU Accelerated", color = Color(0xFF71717A), fontSize = 10.sp)
                }

                // Log steps outputs
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                        .padding(12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "VStudio Logs:\n> $statusMsg",
                        color = Color(0xFF10B981),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 15.sp,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text(
                    text = "Lưu ý không đóng ứng dụng hoặc tắt nguồn khi luồng chỉnh sửa hình ảnh 4K đang được kết xuất nén.",
                    color = Color(0xFF71717A),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
