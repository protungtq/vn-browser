package vn.tunglv.browser.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import vn.tunglv.browser.BrowserViewModel
import vn.tunglv.browser.data.BookmarkStore
import vn.tunglv.browser.data.SettingsStore
import vn.tunglv.browser.data.searchEngineById
import vn.tunglv.browser.filter.FilterStore
import vn.tunglv.browser.web.HOME_URL
import vn.tunglv.browser.web.TabManager
import vn.tunglv.browser.web.resolveInput

private enum class AppScreen {
    BROWSER,
    TABS,
    HISTORY,
    BOOKMARKS,
    SETTINGS,
}

@Composable
fun BrowserApp(viewModel: BrowserViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tabManager = remember {
        TabManager(context, context.applicationContext, scope)
    }
    var screen by remember { mutableStateOf(AppScreen.BROWSER) }
    val settings by SettingsStore.settings.collectAsState()
    val externalUrl by viewModel.externalUrl.collectAsState()
    val crashReport by viewModel.crashReport.collectAsState()

    LaunchedEffect(Unit) {
        tabManager.restoreIfNeeded()
        FilterStore.refreshIfNeeded(context)
    }

    // Link từ app khác gửi sang: mở thành tab mới.
    LaunchedEffect(externalUrl) {
        val text = externalUrl ?: return@LaunchedEffect
        val target = resolveInput(text, searchEngineById(settings.searchEngineId))
        if (target.isNotBlank()) {
            tabManager.createTab(target, activate = true)
            screen = AppScreen.BROWSER
        }
        viewModel.consumeExternalUrl()
    }

    DisposableEffect(Unit) {
        onDispose { tabManager.destroy() }
    }

    VnBrowserTheme(themeMode = settings.themeMode) {
        when (screen) {
            AppScreen.TABS -> TabSwitcherScreen(
                tabs = tabManager.tabs,
                activeTabId = tabManager.activeTabId.value,
                onSelect = { id ->
                    tabManager.selectTab(id)
                    screen = AppScreen.BROWSER
                },
                onClose = tabManager::closeTab,
                onNewTab = {
                    tabManager.createTab(HOME_URL, activate = true)
                    screen = AppScreen.BROWSER
                },
                onCloseAll = {
                    tabManager.closeAllTabs()
                    screen = AppScreen.BROWSER
                },
                onBack = { screen = AppScreen.BROWSER },
            )
            AppScreen.HISTORY -> HistoryScreen(
                onOpen = { url ->
                    tabManager.createTab(url, activate = true)
                    screen = AppScreen.BROWSER
                },
                onBack = { screen = AppScreen.BROWSER },
            )
            AppScreen.BOOKMARKS -> BookmarksScreen(
                onOpen = { url ->
                    tabManager.createTab(url, activate = true)
                    screen = AppScreen.BROWSER
                },
                onBack = { screen = AppScreen.BROWSER },
            )
            AppScreen.SETTINGS -> SettingsScreen(
                onBack = { screen = AppScreen.BROWSER },
            )
            AppScreen.BROWSER -> BrowserMainScreen(
                tabManager = tabManager,
                onOpenTabs = { screen = AppScreen.TABS },
                onOpenHistory = { screen = AppScreen.HISTORY },
                onOpenBookmarks = { screen = AppScreen.BOOKMARKS },
                onOpenSettings = { screen = AppScreen.SETTINGS },
            )
        }
        crashReport?.let { report ->
            CrashReportDialog(report = report, onClose = viewModel::dismissCrashReport)
        }
    }
}

@Composable
private fun BrowserMainScreen(
    tabManager: TabManager,
    onOpenTabs: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val activeTab = tabManager.activeTab
    val settings by SettingsStore.settings.collectAsState()
    val blockedCount by FilterStore.blockedCount.collectAsState()
    val bookmarks by BookmarkStore.bookmarks.collectAsState()

    var addressText by remember { mutableStateOf(TextFieldValue()) }
    var addressFocused by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var downloadSheet by remember { mutableStateOf<Pair<String, String?>?>(null) }

    val tabUrl = activeTab?.url?.value ?: ""
    val tabIsStartPage = activeTab?.isStartPage?.value == true
    val tabTitle = activeTab?.title?.value ?: ""
    val tabProgress = activeTab?.progress?.intValue ?: 100
    val tabCanGoBack = activeTab?.canGoBack?.value ?: false
    val tabCanGoForward = activeTab?.canGoForward?.value ?: false

    val isWebPage = !tabIsStartPage && (tabUrl.startsWith("https://") || tabUrl.startsWith("http://"))
    // Cuộn trang xuống thì ẩn 2 thanh, cuộn lên thì hiện lại. Lúc đang
    // sửa địa chỉ thì luôn hiện thanh địa chỉ để người dùng nhìn thấy.
    val hideBars = activeTab?.barsHidden?.value == true && isWebPage && !addressFocused
    val bookmarked = isWebPage && bookmarks.any { it.url == tabUrl }
    val engine = searchEngineById(settings.searchEngineId)
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val colors = MaterialTheme.colorScheme

    LaunchedEffect(activeTab?.id) { focusManager.clearFocus() }

    // Địa chỉ trang đổi thì thanh địa chỉ hiện theo, trừ lúc đang gõ.
    LaunchedEffect(activeTab?.id, tabUrl, tabIsStartPage, addressFocused) {
        if (!addressFocused) {
            addressText = TextFieldValue(if (tabIsStartPage) "" else tabUrl)
        }
    }

    fun goTo(input: String) {
        val tab = activeTab ?: return
        if (input.isBlank()) return
        focusManager.clearFocus()
        tabManager.navigate(tab, input, engine)
    }

    BackHandler(enabled = addressFocused || tabCanGoBack) {
        if (addressFocused) {
            focusManager.clearFocus()
        } else {
            activeTab?.let(tabManager::goBack)
        }
    }

    val addressField: @Composable (Modifier) -> Unit = { modifier ->
        val fieldShape = RoundedCornerShape(18.dp)
        BasicTextField(
            value = addressText,
            onValueChange = { addressText = it },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
            cursorBrush = SolidColor(colors.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { goTo(addressText.text) }),
            modifier = modifier
                .onFocusChanged {
                    if (it.isFocused && !addressFocused && addressText.text.isNotEmpty()) {
                        addressText = addressText.copy(selection = TextRange(0, addressText.text.length))
                    }
                    addressFocused = it.isFocused
                }
                .semantics { contentDescription = "Địa chỉ hoặc từ khóa" },
            decorationBox = { innerTextField ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .heightIn(min = 54.dp)
                        .background(if (addressFocused) colors.surface else colors.surfaceContainerLow, fieldShape)
                        .border(1.dp, if (addressFocused) colors.primary else colors.outlineVariant, fieldShape)
                        .padding(start = 16.dp, end = 4.dp),
                ) {
                    Icon(
                        if (isWebPage && !addressFocused && tabUrl.startsWith("https://")) {
                            Icons.Outlined.Lock
                        } else {
                            Icons.Outlined.Search
                        },
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (addressFocused) colors.primary else colors.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.weight(1f).padding(vertical = 8.dp)) {
                        if (addressText.text.isEmpty()) {
                            Text(
                                if (tabIsStartPage) "Tìm với ${engine.name}" else "Tìm kiếm hoặc nhập địa chỉ",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = colors.onSurfaceVariant,
                            )
                        }
                        innerTextField()
                    }
                    if (addressFocused && addressText.text.isNotEmpty()) {
                        IconButton(onClick = { addressText = TextFieldValue() }) {
                            Icon(Icons.Default.Close, contentDescription = "Xóa nội dung")
                        }
                    } else if (isWebPage && !addressFocused) {
                        IconButton(onClick = { activeTab?.let(tabManager::reload) }) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Tải lại trang")
                        }
                    }
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .statusBarsPadding()
            .imePadding(),
    ) {
        if (!tabIsStartPage && activeTab != null) {
            AnimatedVisibility(
                visible = !hideBars,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.surface)
                            .padding(
                                start = 12.dp,
                                end = if (isWebPage && !addressFocused) 4.dp else 12.dp,
                                top = 8.dp,
                                bottom = 8.dp,
                            ),
                    ) {
                        addressField(Modifier.weight(1f))
                        if (isWebPage && !addressFocused) {
                            MediaDownloadButton(activeTab.capturedMedia.size) {
                                val cookies = try {
                                    CookieManager.getInstance().getCookie(tabUrl)
                                } catch (_: Exception) {
                                    null
                                }
                                downloadSheet = tabUrl to cookies
                            }
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(2.dp)) {
                        if (tabProgress in 1..99) {
                            LinearProgressIndicator(
                                progress = { tabProgress / 100f },
                                modifier = Modifier.fillMaxSize(),
                                color = colors.primary,
                                trackColor = Color.Transparent,
                            )
                        } else {
                            HorizontalDivider(Modifier.align(Alignment.BottomCenter), color = colors.outlineVariant)
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            if (activeTab == null) {
                CircularProgressIndicator()
            } else {
                AndroidView(
                    factory = { ctx -> FrameLayout(ctx) },
                    update = { container ->
                        val webView = tabManager.ensureWebView(activeTab)
                        if (webView.parent !== container) {
                            (webView.parent as? ViewGroup)?.removeView(webView)
                            container.removeAllViews()
                            container.addView(
                                webView,
                                FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                ),
                            )
                        }
                        container.visibility = if (tabIsStartPage) View.INVISIBLE else View.VISIBLE
                    },
                    onRelease = { it.removeAllViews() },
                    modifier = Modifier.fillMaxSize(),
                )
                if (tabIsStartPage) {
                    StartPage(addressField, ::goTo, showShortcuts = !keyboardVisible)
                }
            }
        }

        if (!keyboardVisible) {
            AnimatedVisibility(
                visible = !hideBars,
                enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
            ) {
            Column(
                modifier = Modifier.fillMaxWidth().background(colors.surface).navigationBarsPadding(),
            ) {
                HorizontalDivider(color = colors.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BottomBarButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        label = "Lùi",
                        enabled = tabCanGoBack,
                        onClick = {
                            focusManager.clearFocus()
                            activeTab?.let(tabManager::goBack)
                        },
                        modifier = Modifier.weight(1f),
                    )
                    BottomBarButton(
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        label = "Tiến",
                        enabled = tabCanGoForward,
                        onClick = {
                            focusManager.clearFocus()
                            activeTab?.let(tabManager::goForward)
                        },
                        modifier = Modifier.weight(1f),
                    )
                    BottomBarButton(
                        icon = Icons.Outlined.Home,
                        label = "Trang chủ",
                        enabled = activeTab != null,
                        active = tabIsStartPage,
                        onClick = {
                            focusManager.clearFocus()
                            if (!tabIsStartPage) activeTab?.let(tabManager::openStartPage)
                        },
                        modifier = Modifier.weight(1f),
                    )
                    val tabCount = tabManager.tabs.size
                    BottomBarButton(
                        label = "Tab",
                        description = "Xem $tabCount tab đang mở",
                        tabCount = tabCount,
                        onClick = onOpenTabs,
                        modifier = Modifier.weight(1f),
                    )
                    Box(Modifier.weight(1f)) {
                        BottomBarButton(
                            icon = Icons.Default.MoreVert,
                            label = "Menu",
                            active = menuExpanded,
                            onClick = {
                                focusManager.clearFocus()
                                menuExpanded = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        BrowserMenu(
                            expanded = menuExpanded,
                            onDismiss = { menuExpanded = false },
                            tabUrl = tabUrl.takeIf { isWebPage }.orEmpty(),
                            bookmarked = bookmarked,
                            adBlockEnabled = settings.adBlockEnabled,
                            blockedCount = blockedCount,
                            onNewTab = { tabManager.createTab(HOME_URL, activate = true) },
                            onHistory = onOpenHistory,
                            onBookmarks = onOpenBookmarks,
                            onSettings = onOpenSettings,
                            onSavePage = {
                                val nowMarked = BookmarkStore.toggle(tabUrl, tabTitle)
                                Toast.makeText(
                                    context,
                                    if (nowMarked) "Đã thêm dấu trang." else "Đã gỡ dấu trang.",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            },
                            onToggleAdBlock = {
                                val enabled = !settings.adBlockEnabled
                                SettingsStore.update(adBlockEnabled = enabled)
                                Toast.makeText(
                                    context,
                                    if (enabled) "Đã bật chặn quảng cáo." else "Đã tắt chặn quảng cáo.",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            },
                        )
                    }
                }
            }
            }
        }
    }
    downloadSheet?.let { (sheetUrl, sheetCookies) ->
        val tabCaptures = activeTab?.capturedMedia?.toList().orEmpty()
        DownloadSheet(
            pageUrl = sheetUrl,
            cookieString = sheetCookies,
            captures = tabCaptures,
            pageTitle = tabTitle,
            onClose = { downloadSheet = null },
            onDeepScan = { activeTab?.let { tabManager.deepScanMedia(it) } },
        )
    }
}

@Composable
private fun MediaDownloadButton(captureCount: Int, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        BadgedBox(badge = { if (captureCount > 0) Badge { Text(captureCount.toString()) } }) {
            Icon(
                Icons.Outlined.Download,
                contentDescription = if (captureCount > 0) {
                    "Tải media, $captureCount liên kết phát hiện được"
                } else {
                    "Tải media của trang"
                },
            )
        }
    }
}

@Composable
private fun BottomBarButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    description: String = label,
    tabCount: Int = 0,
    enabled: Boolean = true,
    active: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val tint = when {
        !enabled -> colors.onSurfaceVariant.copy(alpha = 0.38f)
        active -> colors.primary
        else -> colors.onSurfaceVariant
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(vertical = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 46.dp, height = 32.dp)
                .background(if (active) colors.primaryContainer else Color.Transparent, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            } else {
                Box(
                    modifier = Modifier.size(25.dp).border(1.5.dp, tint, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (tabCount > 99) "99+" else tabCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = tint,
                        maxLines = 1,
                        modifier = Modifier.clearAndSetSemantics { },
                    )
                }
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}

@Composable
private fun BrowserMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    tabUrl: String,
    bookmarked: Boolean,
    adBlockEnabled: Boolean,
    blockedCount: Int,
    onNewTab: () -> Unit,
    onHistory: () -> Unit,
    onBookmarks: () -> Unit,
    onSettings: () -> Unit,
    onSavePage: () -> Unit,
    onToggleAdBlock: () -> Unit,
) {
    val context = LocalContext.current
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        BrowserMenuItem(Icons.Outlined.Add, "Tab mới", onDismiss, onNewTab)
        BrowserMenuItem(Icons.Outlined.History, "Lịch sử", onDismiss, onHistory)
        BrowserMenuItem(Icons.Outlined.StarBorder, "Dấu trang", onDismiss, onBookmarks)
        BrowserMenuItem(Icons.Outlined.Settings, "Cài đặt", onDismiss, onSettings)
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Chặn quảng cáo") },
            leadingIcon = { Icon(Icons.Outlined.Shield, contentDescription = null) },
            trailingIcon = {
                Text(
                    if (adBlockEnabled) "Bật · $blockedCount" else "Tắt",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            onClick = {
                onDismiss()
                onToggleAdBlock()
            },
        )
        if (tabUrl.isNotEmpty()) {
            HorizontalDivider()
            BrowserMenuItem(
                if (bookmarked) Icons.Default.Star else Icons.Outlined.StarBorder,
                if (bookmarked) "Gỡ dấu trang này" else "Lưu trang này",
                onDismiss,
                onSavePage,
            )
            BrowserMenuItem(Icons.Outlined.ContentCopy, "Sao chép liên kết", onDismiss) {
                val clip = ClipData.newPlainText("Liên kết", tabUrl)
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                Toast.makeText(context, "Đã sao chép liên kết.", Toast.LENGTH_SHORT).show()
            }
            BrowserMenuItem(Icons.Outlined.Share, "Chia sẻ liên kết", onDismiss) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, tabUrl)
                }
                context.startActivity(Intent.createChooser(intent, "Chia sẻ liên kết"))
            }
            BrowserMenuItem(Icons.AutoMirrored.Outlined.OpenInNew, "Mở bằng ứng dụng khác", onDismiss) {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(tabUrl)))
                } catch (_: Exception) {
                    Toast.makeText(context, "Không mở được liên kết này.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

@Composable
private fun BrowserMenuItem(icon: ImageVector, label: String, onDismiss: () -> Unit, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        onClick = {
            onDismiss()
            onClick()
        },
    )
}

/** Hộp hiện chi tiết lỗi làm app tự đóng ở lần mở trước, để sao chép gửi đi. */
@Composable
private fun CrashReportDialog(report: String, onClose: () -> Unit) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onClose) {
        Card {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "App gặp lỗi lần mở trước",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Lần mở trước, app tự đóng vì lỗi. Chi tiết ở dưới — " +
                        "sao chép rồi gửi cho người làm app để sửa nhé.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    report,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState()),
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = {
                        val clip = ClipData.newPlainText("Chi tiết lỗi VN Browser", report)
                        (context.getSystemService(Context.CLIPBOARD_SERVICE)
                            as ClipboardManager).setPrimaryClip(clip)
                        Toast.makeText(
                            context,
                            "Đã sao chép chi tiết lỗi.",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }) { Text("Sao chép") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onClose) { Text("Đóng") }
                }
            }
        }
    }
}
