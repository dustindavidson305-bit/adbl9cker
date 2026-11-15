package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AdBlockRepository
import com.example.ui.AdBlockViewModel
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.CustomRulesScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.QueryLogScreen
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldShield
import com.example.ui.theme.EmeraldShieldGlow
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.RedBlocked
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950

class MainActivity : ComponentActivity() {

    private val viewModel: AdBlockViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdBlockRepository.init(applicationContext)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                AdBlockMainApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdBlockMainApp(viewModel: AdBlockViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchFilter by viewModel.searchFilter.collectAsStateWithLifecycle()
    val logFilter by viewModel.logFilter.collectAsStateWithLifecycle()
    val filteredLogs by viewModel.filteredLogs.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showInfoDialog by remember { mutableStateOf(false) }

    // Launcher for VPN permission dialog
    val vpnPrepareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.startVpnService(context)
        }
    }

    // Launcher for Android 13+ Notification permission
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Proceed with VPN setup
    }

    fun handleToggleVpn() {
        if (uiState.isVpnActive) {
            viewModel.stopVpnService(context)
        } else {
            // Request notification permission if needed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            // Prepare VPN
            val vpnIntent = VpnService.prepare(context)
            if (vpnIntent != null) {
                vpnPrepareLauncher.launch(vpnIntent)
            } else {
                viewModel.startVpnService(context)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Slate950,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (uiState.isVpnActive) EmeraldShield.copy(alpha = 0.2f) else Slate800,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = if (uiState.isVpnActive) EmeraldShieldGlow else CyanPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "AdBlock VPN",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Slate800,
                                    border = BorderStroke(1.dp, CardBorder)
                                ) {
                                    Text(
                                        text = "Android 15",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = CyanPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (uiState.isVpnActive) "Protection Running" else "Protection Paused",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = if (uiState.isVpnActive) EmeraldShieldGlow else Slate400
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.clearStats() },
                        modifier = Modifier.testTag("btn_reset_stats")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Stats",
                            tint = Slate400
                        )
                    }
                    IconButton(
                        onClick = { showInfoDialog = true },
                        modifier = Modifier.testTag("btn_info")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About Engine",
                            tint = Slate400
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Slate950
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Slate900,
                contentColor = Slate400
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Shield"
                        )
                    },
                    label = { Text("Shield") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyanPrimary,
                        selectedTextColor = CyanPrimary,
                        unselectedIconColor = Slate400,
                        unselectedTextColor = Slate400,
                        indicatorColor = Slate800
                    ),
                    modifier = Modifier.testTag("nav_tab_shield")
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filters"
                        )
                    },
                    label = { Text("Filters") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyanPrimary,
                        selectedTextColor = CyanPrimary,
                        unselectedIconColor = Slate400,
                        unselectedTextColor = Slate400,
                        indicatorColor = Slate800
                    ),
                    modifier = Modifier.testTag("nav_tab_filters")
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (uiState.blockedCount > 0) {
                                    Badge(
                                        containerColor = RedBlocked,
                                        contentColor = Color.White
                                    ) {
                                        Text(uiState.blockedCount.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "Query Stream"
                            )
                        }
                    },
                    label = { Text("Log") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyanPrimary,
                        selectedTextColor = CyanPrimary,
                        unselectedIconColor = Slate400,
                        unselectedTextColor = Slate400,
                        indicatorColor = Slate800
                    ),
                    modifier = Modifier.testTag("nav_tab_logs")
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = "Custom Rules"
                        )
                    },
                    label = { Text("Rules") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyanPrimary,
                        selectedTextColor = CyanPrimary,
                        unselectedIconColor = Slate400,
                        unselectedTextColor = Slate400,
                        indicatorColor = Slate800
                    ),
                    modifier = Modifier.testTag("nav_tab_rules")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> DashboardScreen(
                    state = uiState,
                    onToggleVpn = { handleToggleVpn() },
                    onRunTest = { domain -> viewModel.runAdBlockTest(domain) },
                    onNavigateToLogs = { selectedTab = 2 }
                )
                1 -> CategoriesScreen(
                    categories = uiState.categories,
                    onToggleCategory = { id, enabled -> viewModel.toggleCategory(id, enabled) }
                )
                2 -> QueryLogScreen(
                    logs = filteredLogs,
                    searchQuery = searchFilter,
                    onSearchChanged = { viewModel.setSearchFilter(it) },
                    activeFilter = logFilter,
                    onFilterSelected = { viewModel.setLogFilter(it) },
                    onWhitelistDomain = { viewModel.addWhitelist(it) },
                    onBlockDomain = { viewModel.addCustomBlocked(it) },
                    onClearLogs = { viewModel.clearStats() }
                )
                3 -> CustomRulesScreen(
                    customBlocked = uiState.customBlockedDomains,
                    whitelisted = uiState.whitelistedDomains,
                    onAddBlocked = { viewModel.addCustomBlocked(it) },
                    onRemoveBlocked = { viewModel.removeCustomBlocked(it) },
                    onAddWhitelist = { viewModel.addWhitelist(it) },
                    onRemoveWhitelist = { viewModel.removeWhitelist(it) }
                )
            }
        }
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = CyanPrimary
                    )
                    Text(
                        text = "Android 15 AdBlock Engine",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "How it protects you:",
                        fontWeight = FontWeight.SemiBold,
                        color = CyanPrimary
                    )
                    Text(
                        text = "• Uses the local Android VpnService TUN interface to route port 53 DNS queries through on-device inspection.\n" +
                                "• When games or apps request known ad or tracking servers (UnityAds, AppLovin, AdMob, Vungle, etc.), the request is intercepted and dropped locally.\n" +
                                "• Zero remote proxy servers are used: your browsing and gaming stay 100% private on your device.\n" +
                                "• Android 15 Compliant: Declares foregroundServiceType='specialUse' with full system exemption compliance.",
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                        color = Slate400
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showInfoDialog = false }
                ) {
                    Text("Got It", color = CyanPrimary, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Slate900,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
