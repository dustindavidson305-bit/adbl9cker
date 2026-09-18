package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DnsQueryLog
import com.example.ui.LogFilterType
import com.example.ui.theme.CardBackground
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldShield
import com.example.ui.theme.EmeraldShieldGlow
import com.example.ui.theme.RedBlocked
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun QueryLogScreen(
    logs: List<DnsQueryLog>,
    searchQuery: String,
    onSearchChanged: (String) -> Unit,
    activeFilter: LogFilterType,
    onFilterSelected: (LogFilterType) -> Unit,
    onWhitelistDomain: (String) -> Unit,
    onBlockDomain: (String) -> Unit,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Live DNS Stream",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "${logs.size} queries logged in memory",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400
                )
            }

            if (logs.isNotEmpty()) {
                IconButton(
                    onClick = onClearLogs,
                    modifier = Modifier.testTag("btn_clear_logs")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear Logs",
                        tint = Slate400
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChanged,
            placeholder = { Text("Search domains or categories...", color = Slate400) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Slate400
                )
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_query_input"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CardBackground,
                unfocusedContainerColor = CardBackground,
                focusedBorderColor = CyanPrimary,
                unfocusedBorderColor = CardBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = activeFilter == LogFilterType.ALL,
                onClick = { onFilterSelected(LogFilterType.ALL) },
                label = { Text("All Queries") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CyanPrimary,
                    selectedLabelColor = Slate900,
                    containerColor = Slate900,
                    labelColor = Slate400
                ),
                border = BorderStroke(1.dp, if (activeFilter == LogFilterType.ALL) CyanPrimary else Slate800)
            )

            FilterChip(
                selected = activeFilter == LogFilterType.BLOCKED_ONLY,
                onClick = { onFilterSelected(LogFilterType.BLOCKED_ONLY) },
                label = { Text("Blocked") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = RedBlocked,
                    selectedLabelColor = Color.White,
                    containerColor = Slate900,
                    labelColor = Slate400
                ),
                border = BorderStroke(1.dp, if (activeFilter == LogFilterType.BLOCKED_ONLY) RedBlocked else Slate800)
            )

            FilterChip(
                selected = activeFilter == LogFilterType.ALLOWED_ONLY,
                onClick = { onFilterSelected(LogFilterType.ALLOWED_ONLY) },
                label = { Text("Allowed") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldShield,
                    selectedLabelColor = Color.White,
                    containerColor = Slate900,
                    labelColor = Slate400
                ),
                border = BorderStroke(1.dp, if (activeFilter == LogFilterType.ALLOWED_ONLY) EmeraldShield else Slate800)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Slate800,
                        modifier = Modifier.size(54.dp)
                    )
                    Text(
                        text = "No DNS Queries Recorded",
                        style = MaterialTheme.typography.titleMedium,
                        color = Slate400
                    )
                    Text(
                        text = "Turn on the shield or tap 'Test Ad Blocker' on the Dashboard to inspect real-time lookups.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400.copy(alpha = 0.7f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs, key = { it.id }) { log ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("log_item_${log.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        border = BorderStroke(
                            1.dp,
                            if (log.isBlocked) RedBlocked.copy(alpha = 0.25f) else CardBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (log.isBlocked) RedBlocked else EmeraldShieldGlow)
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = log.domain,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = timeFormat.format(Date(log.timestamp)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Slate400
                                        )
                                        if (log.category != null) {
                                            Text(
                                                text = "• ${log.category}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (log.isBlocked) RedBlocked.copy(alpha = 0.8f) else CyanPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            // Action button
                            if (log.isBlocked) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = RedBlocked.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, RedBlocked.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "BLOCKED",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = RedBlocked,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = EmeraldShield.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, EmeraldShield.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "ALLOWED",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = EmeraldShieldGlow,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
