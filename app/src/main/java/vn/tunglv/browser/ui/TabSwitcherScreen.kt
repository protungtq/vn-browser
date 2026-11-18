package vn.tunglv.browser.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vn.tunglv.browser.web.TabSession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabSwitcherScreen(
    tabs: List<TabSession>,
    activeTabId: Long?,
    onSelect: (Long) -> Unit,
    onClose: (Long) -> Unit,
    onNewTab: () -> Unit,
    onCloseAll: () -> Unit,
    onBack: () -> Unit,
) {
    var showCloseDialog by rememberSaveable { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    BackHandler(onBack = onBack)
    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("${tabs.size} tab đang mở", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại trình duyệt")
                    }
                },
                actions = {
                    TextButton(onClick = { showCloseDialog = true }, enabled = tabs.isNotEmpty()) {
                        Text("Đóng tất cả")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surfaceContainerLow),
            )
        },
        bottomBar = {
            Surface(color = colors.background) {
                Button(
                    onClick = onNewTab,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp).heightIn(min = 48.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Mở tab mới")
                }
            }
        },
    ) { innerPadding ->
        if (tabs.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 32.dp),
                ) {
                    Icon(
                        Icons.Outlined.Home,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Không có tab nào đang mở.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Bấm nút Mở tab mới ở dưới để bắt đầu duyệt web.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 144.dp),
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(tabs, key = { it.id }) { tab ->
                    TabCard(tab, tab.id == activeTabId, onSelect, onClose)
                }
            }
        }
    }
    if (showCloseDialog) {
        AlertDialog(
            onDismissRequest = { showCloseDialog = false },
            title = { Text("Đóng tất cả tab?") },
            text = { Text("Bạn sẽ quay về trang chủ trong một tab mới.") },
            dismissButton = {
                TextButton(onClick = { showCloseDialog = false }) { Text("Hủy") }
            },
            confirmButton = {
                TextButton(onClick = {
                    showCloseDialog = false
                    onCloseAll()
                }) { Text("Đóng tất cả", color = colors.error) }
            },
        )
    }
}

@Composable
private fun TabCard(tab: TabSession, isActive: Boolean, onSelect: (Long) -> Unit, onClose: (Long) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val isHome = tab.isStartPage.value
    val url = tab.url.value.ifBlank { tab.startUrl }
    val domain = if (isHome) "VN Browser" else Uri.parse(url).host?.removePrefix("www.") ?: url
    val title = if (isHome) "Trang chủ" else tab.title.value.ifBlank { domain }
    Surface(
        onClick = { onSelect(tab.id) },
        shape = RoundedCornerShape(20.dp),
        color = if (isActive) colors.primaryContainer else colors.surface,
        contentColor = if (isActive) colors.onPrimaryContainer else colors.onSurface,
        border = BorderStroke(1.dp, if (isActive) colors.primary else colors.outlineVariant),
        modifier = Modifier.semantics { selected = isActive },
    ) {
        Column(Modifier.padding(start = 12.dp, top = 4.dp, end = 4.dp, bottom = 12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).background(
                        if (isActive) colors.primary else colors.surfaceContainerHigh,
                        RoundedCornerShape(12.dp),
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (isHome) Icons.Outlined.Home else Icons.Outlined.Language,
                        contentDescription = null,
                        tint = if (isActive) colors.onPrimary else colors.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { onClose(tab.id) }) {
                    Icon(Icons.Default.Close, contentDescription = "Đóng tab $title", modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Column(Modifier.padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (isActive) "Đang xem · $domain" else domain,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isActive) colors.onPrimaryContainer else colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
