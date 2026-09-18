package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdBlockState
import com.example.ui.components.MetricCard
import com.example.ui.theme.AmberPaused
import com.example.ui.theme.CardBackground
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldShield
import com.example.ui.theme.EmeraldShieldGlow
import com.example.ui.theme.RedBlocked
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import java.util.Locale

@Composable
fun DashboardScreen(
    state: AdBlockState,
    onToggleVpn: () -> Unit,
    onRunTest: (String) -> Unit,
    onNavigateToLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero Shield & Power Control
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(top = 16.dp, bottom = 24.dp)
                .size(220.dp)
        ) {
            if (state.isVpnActive) {
                // Glowing outer pulse ring
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    EmeraldShield.copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            // Power button outer ring
            Box(
                modifier = Modifier
                    .size(170.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = if (state.isVpnActive) {
                                listOf(EmeraldShield.copy(alpha = 0.3f), CyanPrimary.copy(alpha = 0.2f))
                            } else {
                                listOf(Slate800, Slate900)
                            }
                        )
                    )
                    .border(
                        BorderStroke(
                            2.dp,
                            if (state.isVpnActive) EmeraldShieldGlow else CardBorder
                        ),
                        CircleShape
                    )
                    .clickable { onToggleVpn() }
                    .testTag("power_toggle_button"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (state.isVpnActive) Icons.Default.Shield else Icons.Default.PowerSettingsNew,
                        contentDescription = "Power status",
                        tint = if (state.isVpnActive) EmeraldShieldGlow else AmberPaused,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (state.isVpnActive) "ACTIVE" else "START",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        ),
                        color = if (state.isVpnActive) EmeraldShieldGlow else Color.White
                    )
                }
            }
        }

        // Status description pill
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (state.isVpnActive) EmeraldShield.copy(alpha = 0.15f) else AmberPaused.copy(alpha = 0.15f),
            border = BorderStroke(
                1.dp,
                if (state.isVpnActive) EmeraldShield.copy(alpha = 0.4f) else AmberPaused.copy(alpha = 0.4f)
            ),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (state.isVpnActive) EmeraldShieldGlow else AmberPaused)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (state.isVpnActive) "PROTECTION RUNNING" else "SHIELD DISCONNECTED",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (state.isVpnActive) EmeraldShieldGlow else AmberPaused
                )
            }
        }

        Text(
            text = if (state.isVpnActive) {
                "Blocking ads & trackers in mobile games and installed apps"
            } else {
                "Tap the shield above to enable on-device DNS ad blocking"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Slate400,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Metrics Grid (2x2)
        val savedFormatted = formatBytes(state.estimatedBytesSaved)
        val activeRulesCount = state.categories
            .filter { it.isEnabled }
            .sumOf { it.domains.size } + state.customBlockedDomains.size

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Ads Blocked",
                value = state.blockedCount.toString(),
                icon = Icons.Default.Block,
                accentColor = RedBlocked,
                modifier = Modifier.weight(1f),
                testTag = "metric_blocked"
            )
            MetricCard(
                title = "Total Inspected",
                value = state.totalQueriesCount.toString(),
                icon = Icons.Default.Speed,
                accentColor = CyanPrimary,
                modifier = Modifier.weight(1f),
                testTag = "metric_inspected"
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Data Saved",
                value = savedFormatted,
                icon = Icons.Default.CheckCircle,
                accentColor = EmeraldShieldGlow,
                modifier = Modifier.weight(1f),
                testTag = "metric_saved"
            )
            MetricCard(
                title = "Active Rules",
                value = activeRulesCount.toString(),
                icon = Icons.Default.Security,
                accentColor = CyanSecondary,
                modifier = Modifier.weight(1f),
                testTag = "metric_rules"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Simulation & Ad Blocker Test Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("test_simulation_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Test Game Ad Blocking",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "Simulate real ad network requests to verify filtering",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onRunTest("unityads.unity3d.com") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_test_unity"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CyanAccent)
                    ) {
                        Text("Unity Ads", color = CyanPrimary, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = { onRunTest("doubleclick.net") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_test_doubleclick"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CyanAccent)
                    ) {
                        Text("DoubleClick", color = CyanPrimary, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = { onRunTest("applovin.com") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_test_applovin"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CyanAccent)
                    ) {
                        Text("AppLovin", color = CyanPrimary, fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Android 15 Privacy Specs Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.6f)),
            border = BorderStroke(1.dp, Slate800)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(EmeraldShieldGlow)
                    )
                    Text(
                        text = "Android 15 Engine Specifications",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "• 100% Local Packet Inspection: Zero DNS queries leave device to external proxy servers\n" +
                            "• Game Interstitial & Reward Video Blocker for Unity, AppLovin, AdMob, Vungle\n" +
                            "• Foreground Service Type 'specialUse' compliant with Android 14 & 15\n" +
                            "• No Root Required: Operates securely through Android VpnService TUN interface",
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                    color = Slate400
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format(Locale.US, "%.2f MB", mb)
}
