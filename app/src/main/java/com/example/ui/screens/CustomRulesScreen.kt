package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardBackground
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldShield
import com.example.ui.theme.RedBlocked
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

@Composable
fun CustomRulesScreen(
    customBlocked: Set<String>,
    whitelisted: Set<String>,
    onAddBlocked: (String) -> Boolean,
    onRemoveBlocked: (String) -> Unit,
    onAddWhitelist: (String) -> Boolean,
    onRemoveWhitelist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Blocklist, 1 = Whitelist
    var inputDomain by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.padding(bottom = 12.dp)) {
            Text(
                text = "Custom Domain Rules",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = "Add custom ad domains or bypass specific websites",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate400
            )
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Slate900,
            contentColor = CyanPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = if (selectedTab == 0) RedBlocked else EmeraldShield
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0; errorMessage = null },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            tint = if (selectedTab == 0) RedBlocked else Slate400,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Custom Blocked (${customBlocked.size})",
                            color = if (selectedTab == 0) Color.White else Slate400
                        )
                    }
                },
                modifier = Modifier.testTag("tab_custom_blocked")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1; errorMessage = null },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (selectedTab == 1) EmeraldShield else Slate400,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Whitelisted (${whitelisted.size})",
                            color = if (selectedTab == 1) Color.White else Slate400
                        )
                    }
                },
                modifier = Modifier.testTag("tab_custom_whitelist")
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input field and Add button
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = inputDomain,
                    onValueChange = {
                        inputDomain = it.trim().lowercase()
                        errorMessage = null
                    },
                    placeholder = {
                        Text(
                            text = if (selectedTab == 0) "e.g. adserver.game.com" else "e.g. allowed-site.com",
                            color = Slate400
                        )
                    },
                    singleLine = true,
                    isError = errorMessage != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_custom_domain"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Slate900,
                        unfocusedContainerColor = Slate900,
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = RedBlocked
                    )
                }

                Button(
                    onClick = {
                        val cleaned = inputDomain.trim().lowercase()
                        if (cleaned.isBlank() || !cleaned.contains(".")) {
                            errorMessage = "Please enter a valid domain (e.g., example.com)"
                        } else {
                            val success = if (selectedTab == 0) {
                                onAddBlocked(cleaned)
                            } else {
                                onAddWhitelist(cleaned)
                            }
                            if (success) {
                                inputDomain = ""
                                errorMessage = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTab == 0) RedBlocked else EmeraldShield
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_add_rule")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = if (selectedTab == 0) "Block Domain" else "Whitelist Domain",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Domains list
        val currentList = if (selectedTab == 0) customBlocked.toList() else whitelisted.toList()

        if (currentList.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (selectedTab == 0) "No custom blocked domains added" else "No whitelisted domains",
                    style = MaterialTheme.typography.titleMedium,
                    color = Slate400
                )
                Text(
                    text = if (selectedTab == 0) {
                        "Domains entered here will be intercepted and dropped locally across all Android 15 apps."
                    } else {
                        "Whitelisted domains will bypass the ad filter even if they exist in blocklists."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400.copy(alpha = 0.7f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(currentList, key = { it }) { domain ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_rule_item_$domain"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (selectedTab == 0) Icons.Default.Block else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (selectedTab == 0) RedBlocked else EmeraldShield,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = domain,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = Color.White
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (selectedTab == 0) onRemoveBlocked(domain) else onRemoveWhitelist(domain)
                                },
                                modifier = Modifier.testTag("btn_delete_$domain")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove",
                                    tint = Slate400
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
