package vn.tunglv.browser.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.contracts.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import vn.tunglv.browser.R
import vn.tunglv.browser.data.PROVINCES
import vn.tunglv.browser.data.Province
import vn.tunglv.browser.data.SettingsStore
import vn.tunglv.browser.data.Shortcut
import vn.tunglv.browser.data.ShortcutStore
import vn.tunglv.browser.data.WeatherRepository
import vn.tunglv.browser.data.WeatherUiState
import vn.tunglv.browser.data.getDeviceLocation
import vn.tunglv.browser.data.provinceById
import vn.tunglv.browser.data.weatherDescription

private fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
}

private fun normalizeUrl(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return ""
    return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        trimmed
    } else {
        "https://$trimmed"
    }
}

private fun greetingFor(hour: Int): String = when (hour) {
    in 5..10 -> "Chào buổi sáng"
    in 11..12 -> "Chào buổi trưa"
    in 13..17 -> "Chào buổi chiều"
    in 18..22 -> "Chào buổi tối"
    else -> "Khuya rồi, nghỉ ngơi nhé"
}

private fun weatherIcon(code: Int): ImageVector = when (code) {
    0, 1 -> Icons.Filled.WbSunny
    2, 3, 45, 48 -> Icons.Filled.Cloud
    51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> Icons.Filled.WaterDrop
    71, 73, 75, 77, 85, 86 -> Icons.Filled.AcUnit
    95, 96, 99 -> Icons.Filled.Thunderstorm
    else -> Icons.Filled.Cloud
}

@Composable
internal fun StartPage(
    addressField: @Composable (Modifier) -> Unit,
    onOpen: (String) -> Unit,
    showShortcuts: Boolean,
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val shortcuts by ShortcutStore.shortcuts.collectAsState()
    val settings by SettingsStore.settings.collectAsState()
    val weatherState by WeatherRepository.state.collectAsState()

    var editing by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(hasLocationPermission(context)) }
    var autoLocationFailed by remember { mutableStateOf(false) }
    var locationAttempt by remember { mutableStateOf(0) }
    var currentTarget by remember { mutableStateOf<Triple<Double, Double, String>?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            locationAttempt++
        } else {
            // Từ chối quyền thì mở chọn tỉnh/thành để vẫn xem được thời tiết.
            showLocationDialog = true
        }
    }

    // Xác định khu vực rồi tải thời tiết: tự động dùng vị trí máy trước,
    // không được thì chờ người dùng chọn tỉnh/thành.
    LaunchedEffect(settings.weatherLocationMode, settings.weatherProvinceId, locationAttempt) {
        hasPermission = hasLocationPermission(context)
        if (settings.weatherLocationMode == "manual") {
            val province = provinceById(settings.weatherProvinceId)
            if (province != null) {
                currentTarget = Triple(province.latitude, province.longitude, province.name)
                WeatherRepository.load(province.latitude, province.longitude, province.name)
            }
            return@LaunchedEffect
        }
        if (!hasPermission) return@LaunchedEffect
        val found = getDeviceLocation(context)
        if (found != null) {
            autoLocationFailed = false
            currentTarget = Triple(found.latitude, found.longitude, found.placeName)
            WeatherRepository.load(found.latitude, found.longitude, found.placeName)
        } else {
            autoLocationFailed = true
        }
    }

    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = LocalDateTime.now()
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier.widthIn(max = 520.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                color = colors.primaryContainer,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                ) {
                    if (showShortcuts) {
                        Image(
                            painter = painterResource(R.drawable.launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                        )
                        Text(
                            "VN Browser",
                            style = MaterialTheme.typography.headlineMedium,
                            color = colors.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                    addressField(Modifier.fillMaxWidth())
                }
            }
            if (showShortcuts) {
                Spacer(Modifier.height(16.dp))
                TimeWeatherCard(
                    now = now,
                    weatherState = weatherState,
                    needsPermission = settings.weatherLocationMode == "auto" && !hasPermission,
                    locationFailed = autoLocationFailed,
                    needsProvince = settings.weatherLocationMode == "manual" &&
                        provinceById(settings.weatherProvinceId) == null,
                    onClick = {
                        if (settings.weatherLocationMode == "auto" && !hasPermission) {
                            permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                        } else {
                            showLocationDialog = true
                        }
                    },
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Truy cập nhanh",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onBackground,
                        modifier = Modifier.weight(1f),
                    )
                    if (editing) {
                        TextButton(onClick = { editing = false }) { Text("Xong") }
                    }
                }
                Spacer(Modifier.height(8.dp))
                val tiles: List<Shortcut?> = if (editing) shortcuts + listOf(null) else shortcuts
                for (row in tiles.chunked(4)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for (item in row) {
                            if (item == null) {
                                AddTile(
                                    onClick = { showAddDialog = true },
                                    modifier = Modifier.weight(1f),
                                )
                            } else {
                                ShortcutTile(
                                    shortcut = item,
                                    editing = editing,
                                    onClick = { if (!editing) onOpen(item.url) },
                                    onLongClick = { editing = true },
                                    onDelete = { ShortcutStore.remove(item.id) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
                if (editing) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { ShortcutStore.reset() }) {
                        Text("Khôi phục 12 trang mặc định")
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddShortcutDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, url ->
                ShortcutStore.add(name, url)
                showAddDialog = false
            },
        )
    }

    if (showLocationDialog) {
        LocationDialog(
            currentMode = settings.weatherLocationMode,
            currentProvinceId = settings.weatherProvinceId,
            hasPermission = hasPermission,
            onPickAuto = {
                SettingsStore.update(weatherLocationMode = "auto")
                showLocationDialog = false
                if (!hasLocationPermission(context)) {
                    permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
            },
            onPickProvince = { province ->
                SettingsStore.update(
                    weatherLocationMode = "manual",
                    weatherProvinceId = province.id,
                )
                showLocationDialog = false
            },
            onRefresh = {
                currentTarget?.let { target ->
                    WeatherRepository.load(target.first, target.second, target.third, force = true)
                }
                showLocationDialog = false
            },
            onDismiss = { showLocationDialog = false },
        )
    }
}

@Composable
private fun TimeWeatherCard(
    now: LocalDateTime,
    weatherState: WeatherUiState,
    needsPermission: Boolean,
    locationFailed: Boolean,
    needsProvince: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val timeText = remember(now) { now.format(DateTimeFormatter.ofPattern("HH:mm")) }
    val dateText = remember(now) {
        now.format(DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", Locale("vi", "VN")))
    }
    Surface(
        color = colors.surfaceContainerLow,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    greetingFor(now.hour),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
                Text(
                    timeText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                )
                Text(
                    dateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            when {
                needsProvince -> {
                    Text(
                        "Chọn tỉnh/thành\nđể xem thời tiết",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.End,
                    )
                }
                needsPermission -> {
                    Text(
                        "Bấm để xem thời tiết\ntheo vị trí của bạn",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.End,
                    )
                }
                locationFailed -> {
                    Text(
                        "Không lấy được vị trí\nBấm để chọn tỉnh/thành",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.End,
                    )
                }
                weatherState is WeatherUiState.Ready -> {
                    val info = (weatherState as WeatherUiState.Ready).info
                    Icon(
                        weatherIcon(info.weatherCode),
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(36.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${info.temperature.toInt()}°",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface,
                        )
                        Text(
                            weatherDescription(info.weatherCode),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                        )
                        Text(
                            info.placeName,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                weatherState is WeatherUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
                else -> {
                    Text(
                        "Không tải được\nthời tiết",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShortcutTile(
    shortcut: Shortcut,
    editing: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 1.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(46.dp)) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(colors.surfaceContainerHighest, RoundedCornerShape(15.dp))
                    .clearAndSetSemantics { },
                contentAlignment = Alignment.Center,
            ) {
                FaviconOrLetter(shortcut)
            }
            if (editing) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .size(20.dp)
                        .background(Color(0xFFD32F2F), CircleShape)
                        .clickable(role = Role.Button, onClick = onDelete),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Xóa ${shortcut.title}",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            shortcut.title,
            style = MaterialTheme.typography.labelMedium,
            color = colors.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AddTile(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 1.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(colors.surfaceContainerHighest, RoundedCornerShape(15.dp))
                .border(1.dp, colors.outlineVariant, RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = colors.primary)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Thêm",
            style = MaterialTheme.typography.labelMedium,
            color = colors.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Icon chính thức của trang, tải qua dịch vụ favicon; lỗi thì hiện chữ cái đầu. */
@Composable
private fun FaviconOrLetter(shortcut: Shortcut) {
    val colors = MaterialTheme.colorScheme
    val host = remember(shortcut.url) { Uri.parse(shortcut.url).host.orEmpty() }
    val painter = rememberAsyncImagePainter(
        "https://www.google.com/s2/favicons?domain=$host&sz=128",
    )
    if (host.isNotBlank() && painter.state is AsyncImagePainter.State.Success) {
        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            contentScale = ContentScale.Fit,
        )
    } else {
        Text(
            shortcut.title.firstOrNull()?.uppercase().orEmpty(),
            color = colors.primary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun AddShortcutDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var urlError by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm trang") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it; urlError = false },
                    label = { Text("Địa chỉ (ví dụ: dantri.com.vn)") },
                    singleLine = true,
                    isError = urlError,
                    supportingText = {
                        if (urlError) Text("Địa chỉ chưa đúng, kiểm tra lại.")
                    },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val normalized = normalizeUrl(url)
                    if (Uri.parse(normalized).host.isNullOrBlank()) {
                        urlError = true
                        return@TextButton
                    }
                    onConfirm(name, normalized)
                },
            ) { Text("Thêm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        },
    )
}

@Composable
private fun LocationDialog(
    currentMode: String,
    currentProvinceId: String,
    hasPermission: Boolean,
    onPickAuto: () -> Unit,
    onPickProvince: (Province) -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var mode by remember { mutableStateOf(currentMode) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Khu vực xem thời tiết") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clickable { mode = "auto" }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = mode == "auto", onClick = { mode = "auto" })
                    Text("Tự động theo vị trí máy")
                }
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clickable { mode = "manual" }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = mode == "manual", onClick = { mode = "manual" })
                    Text("Chọn tỉnh/thành")
                }
                if (!hasPermission && mode == "auto") {
                    Text(
                        "Cần cấp quyền vị trí xấp xỉ để dùng chế độ tự động.",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
                if (mode == "manual") {
                    Spacer(Modifier.height(8.dp))
                    for (province in PROVINCES) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { onPickProvince(province) }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = province.id == currentProvinceId,
                                onClick = { onPickProvince(province) },
                            )
                            Text(province.name)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (mode == "auto") onPickAuto() else onDismiss()
                },
            ) { Text("Xong") }
        },
        dismissButton = {
            TextButton(onClick = onRefresh) { Text("Làm mới") }
        },
    )
}
