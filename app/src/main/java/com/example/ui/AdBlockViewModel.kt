package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AdBlockRepository
import com.example.data.AdBlockState
import com.example.model.InstalledApp
import com.example.service.AdBlockVpnService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdBlockViewModel : ViewModel() {

    private val _searchFilter = MutableStateFlow("")
    val searchFilter: StateFlow<String> = _searchFilter

    private val _logFilter = MutableStateFlow(LogFilterType.ALL)
    val logFilter: StateFlow<LogFilterType> = _logFilter

    val uiState: StateFlow<AdBlockState> = AdBlockRepository.state
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AdBlockRepository.state.value
        )

    val filteredLogs = combine(uiState, _searchFilter, _logFilter) { state, query, filter ->
        state.recentLogs.filter { log ->
            val matchesSearch = query.isBlank() || log.domain.contains(query, ignoreCase = true) ||
                    (log.category?.contains(query, ignoreCase = true) == true)

            val matchesType = when (filter) {
                LogFilterType.ALL -> true
                LogFilterType.BLOCKED_ONLY -> log.isBlocked
                LogFilterType.ALLOWED_ONLY -> !log.isBlocked
                LogFilterType.BEACONS_ONLY -> log.isBeacon
            }

            matchesSearch && matchesType
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps

    /** Loads the list of launchable apps once, for the exclusions picker. */
    fun loadInstalledApps(context: Context) {
        if (_installedApps.value.isNotEmpty()) return
        val appContext = context.applicationContext
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pm = appContext.packageManager
                val mainIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                val resolved = pm.queryIntentActivities(mainIntent, 0)
                val apps = resolved
                    .mapNotNull { it.activityInfo?.packageName }
                    .filter { it != appContext.packageName }
                    .distinct()
                    .map { pkg ->
                        val label = try {
                            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                        } catch (e: Exception) {
                            pkg
                        }
                        InstalledApp(packageName = pkg, label = label)
                    }
                    .sortedBy { it.label.lowercase() }
                _installedApps.value = apps
            } catch (e: Exception) {
                // Leave the list empty; the exclusions UI explains itself.
            }
        }
    }

    fun toggleExcludedApp(packageName: String, excluded: Boolean) {
        AdBlockRepository.toggleExcludedApp(packageName, excluded)
    }

    fun setSearchFilter(query: String) {
        _searchFilter.value = query
    }

    fun setLogFilter(filter: LogFilterType) {
        _logFilter.value = filter
    }

    fun startVpnService(context: Context) {
        val intent = Intent(context, AdBlockVpnService::class.java).apply {
            action = AdBlockVpnService.ACTION_START
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopVpnService(context: Context) {
        val intent = Intent(context, AdBlockVpnService::class.java).apply {
            action = AdBlockVpnService.ACTION_STOP
        }
        context.startService(intent)
    }

    fun toggleCategory(categoryId: String, enabled: Boolean) {
        AdBlockRepository.toggleCategory(categoryId, enabled)
    }

    fun addCustomBlocked(domain: String): Boolean {
        return AdBlockRepository.addCustomBlocked(domain)
    }

    fun removeCustomBlocked(domain: String) {
        AdBlockRepository.removeCustomBlocked(domain)
    }

    fun addWhitelist(domain: String): Boolean {
        return AdBlockRepository.addWhitelist(domain)
    }

    fun removeWhitelist(domain: String) {
        AdBlockRepository.removeWhitelist(domain)
    }

    fun clearStats() {
        AdBlockRepository.clearStats()
    }

    fun runAdBlockTest(domain: String = "unityads.unity3d.com") {
        viewModelScope.launch {
            AdBlockRepository.simulateTestQuery(domain)
        }
    }
}

enum class LogFilterType {
    ALL,
    BLOCKED_ONLY,
    ALLOWED_ONLY,
    BEACONS_ONLY
}
