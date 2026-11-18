package vn.tunglv.browser.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vn.tunglv.browser.R

private data class Shortcut(val title: String, val url: String, val mark: String, val color: Color)

private val shortcuts = listOf(
    Shortcut("Google", "https://www.google.com", "G", Color(0xFF245BB9)),
    Shortcut("YouTube", "https://m.youtube.com", "", Color(0xFFC82127)),
    Shortcut("Facebook", "https://m.facebook.com", "f", Color(0xFF2156BA)),
    Shortcut("TikTok", "https://www.tiktok.com", "♪", Color(0xFF363047)),
    Shortcut("Zalo", "https://zalo.me", "Z", Color(0xFF145DB7)),
    Shortcut("Instagram", "https://www.instagram.com", "◎", Color(0xFFA62E75)),
    Shortcut("Shopee", "https://shopee.vn", "S", Color(0xFFAD421C)),
    Shortcut("Lazada", "https://www.lazada.vn", "L", Color(0xFF4E42A4)),
    Shortcut("VnExpress", "https://vnexpress.net", "V", Color(0xFFA22E4A)),
    Shortcut("Tuổi Trẻ", "https://tuoitre.vn", "T", Color(0xFFAB3038)),
    Shortcut("Wikipedia", "https://vi.wikipedia.org", "W", Color(0xFF424754)),
    Shortcut("Gmail", "https://mail.google.com", "M", Color(0xFFB13D32)),
)

@Composable
internal fun StartPage(
    addressField: @Composable (Modifier) -> Unit,
    onOpen: (String) -> Unit,
    showShortcuts: Boolean,
) {
    val colors = MaterialTheme.colorScheme
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
                Spacer(Modifier.height(24.dp))
                Text(
                    "Truy cập nhanh",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onBackground,
                    modifier = Modifier.align(Alignment.Start),
                )
                Spacer(Modifier.height(8.dp))
                for (row in shortcuts.chunked(4)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for (shortcut in row) {
                            ShortcutTile(shortcut, { onOpen(shortcut.url) }, Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortcutTile(shortcut: Shortcut, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
                .background(shortcut.color, RoundedCornerShape(15.dp))
                .clearAndSetSemantics { },
            contentAlignment = Alignment.Center,
        ) {
            if (shortcut.mark.isEmpty()) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
            } else {
                Text(shortcut.mark, color = Color.White, style = MaterialTheme.typography.titleLarge)
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
