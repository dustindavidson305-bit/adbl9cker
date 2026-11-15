package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FilterCategory
import com.example.model.InstalledApp
import com.example.ui.theme.CardBackground
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldShield
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

@Composable
fun CategoriesScreen(
    categories: List<FilterCategory>,
    onToggleCategory: (String, Boolean) -> Unit,
    excludedApps: Set<String> = emptySet(),
    installedApps: List<InstalledApp> = emptyList(),
    onToggleExcludedApp: (String, Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                Text(
                    text = "Protection Filter Lists",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Manage domain blocklists active in Android 15 DNS tunnel",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate400
                )
            }
        }

        item {
            SilentModeCard()
        }

        items(categories, key = { it.id }) { category ->
            CategoryCard(
                category = category,
                onToggle = { enabled -> onToggleCategory(category.id, enabled) }
            )
        }

        item {
            Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                Text(
                    text = "Stealth — App Exclusions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Excluded apps bypass the tunnel completely: they never see a VPN, and nothing in them is filtered. Changes apply the next time the shield restarts — toggle it off and back on.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400
                )
            }
        }

        if (installedApps.isEmpty()) {
            item {
                Text(
                    text = "Loading your apps…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate400
                )
            }
        } else {
            items(installedApps, key = { it.packageName }) { app ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = Color.White
                            )
                            Text(
                                text = app.packageName,
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate400
                            )
                        }
                        Switch(
                            checked = app.packageName in excludedApps,
                            onCheckedChange = { onToggleExcludedApp(app.packageName, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldShield,
                                uncheckedThumbColor = Slate400,
                                uncheckedTrackColor = Slate800
                            )
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SilentModeCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var copiedHost by remember { mutableStateOf<String?>(null) }

    val hosts = listOf(
        "dns.adguard-dns.com" to "AdGuard — blocks ads + trackers",
        "adblock.dns.mullvad.net" to "Mullvad — blocks ads + trackers"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CyanPrimary.copy(alpha = 0.15f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Silent Mode — No VPN",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                    Text(
                        text = "Private DNS · nothing to detect",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldShield
                    )
                }
            }

            Text(
                text = "Android's built-in Private DNS blocks ads and trackers system-wide with no VPN at all: no key icon in the status bar, and no VPN for apps or websites to detect. Android doesn't let apps change this setting themselves, so: tap a hostname to copy it, open the setting, choose \"Private DNS provider hostname\" and paste.",
                style = MaterialTheme.typography.bodySmall,
                color = Slate400
            )

            hosts.forEach { (host, label) ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate900,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            clipboard.setText(AnnotatedString(host))
                            copiedHost = host
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = host,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = Color.White
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate400
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy hostname",
                            tint = CyanPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (copiedHost != null) {
                Text(
                    text = "Copied $copiedHost — paste it in the Private DNS setting.",
                    style = MaterialTheme.typography.labelMedium,
                    color = EmeraldShield
                )
            }

            Button(
                onClick = { openPrivateDnsSettings(context) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanPrimary,
                    contentColor = Slate900
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Private DNS Settings", fontWeight = FontWeight.Bold)
            }

            Text(
                text = "Use one mode at a time: while the Shield (VPN) is running it takes over DNS, so turn the Shield off when you use Silent Mode.",
                style = MaterialTheme.typography.labelSmall,
                color = Slate400
            )
        }
    }
}

private fun openPrivateDnsSettings(context: Context) {
    val candidates = listOf(
        Intent(Settings.ACTION_WIRELESS_SETTINGS),
        Intent(Settings.ACTION_SETTINGS)
    )
    for (intent in candidates) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (context.packageManager.resolveActivity(intent, 0) != null) {
            context.startActivity(intent)
            return
        }
    }
}

@Composable
fun CategoryCard(
    category: FilterCategory,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val icon = when (category.id) {
        "gaming" -> Icons.Default.SportsEsports
        "trackers" -> Icons.Default.Security
        else -> Icons.Default.TravelExplore
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("category_card_${category.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, if (category.isEnabled) CyanAccent.copy(alpha = 0.4f) else CardBorder)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (category.isEnabled) CyanPrimary.copy(alpha = 0.15f) else Slate800,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (category.isEnabled) CyanPrimary else Slate400,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )
                        Text(
                            text = "${category.domains.size} blocked domains",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (category.isEnabled) EmeraldShield else Slate400
                        )
                    }
                }

                Switch(
                    checked = category.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = EmeraldShield,
                        uncheckedThumbColor = Slate400,
                        uncheckedTrackColor = Slate800
                    ),
                    modifier = Modifier.testTag("switch_${category.id}")
                )
            }

            Text(
                text = category.description,
                style = MaterialTheme.typography.bodySmall,
                color = Slate400,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            )

            HorizontalDivider(color = Slate800, thickness = 1.dp)

            // Expansion footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) "Hide Domain List" else "Inspect Blocked Domains (${category.domains.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = CyanPrimary
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = CyanPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    category.domains.forEach { domain ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Slate900,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = domain,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                    color = Slate400
                                )
                                Text(
                                    text = "BLOCKED",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (category.isEnabled) EmeraldShield else Slate700
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
