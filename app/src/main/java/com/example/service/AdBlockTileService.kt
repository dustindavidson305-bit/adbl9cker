package com.example.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.AdBlockRepository

/**
 * Quick Settings tile: one tap in the swipe-down panel toggles the
 * blocker. If VPN consent hasn't been granted yet, the tile opens the
 * app instead (only the app can show the system consent dialog).
 */
class AdBlockTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        AdBlockRepository.init(applicationContext)
        val active = AdBlockRepository.state.value.isVpnActive
        when {
            active -> {
                val intent = Intent(this, AdBlockVpnService::class.java).apply {
                    action = AdBlockVpnService.ACTION_STOP
                }
                startService(intent)
            }
            VpnService.prepare(this) == null -> {
                val intent = Intent(this, AdBlockVpnService::class.java).apply {
                    action = AdBlockVpnService.ACTION_START
                }
                ContextCompat.startForegroundService(this, intent)
            }
            else -> {
                val launch = Intent(this, MainActivity::class.java).apply {
                    action = MainActivity.ACTION_TOGGLE
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                @Suppress("DEPRECATION")
                unlockAndRun { startActivity(launch) }
            }
        }
        refreshTile()
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        val active = AdBlockRepository.state.value.isVpnActive
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "AdBlock"
        tile.updateTile()
    }

    companion object {
        /** Asks the system to refresh the tile (no-op if the user hasn't added it). */
        fun requestUpdate(context: Context) {
            try {
                requestListeningState(
                    context,
                    ComponentName(context, AdBlockTileService::class.java)
                )
            } catch (e: Exception) {
                // Tile not placed yet — nothing to refresh.
            }
        }
    }
}
