package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import com.example.MainActivity
import com.example.data.AdBlockRepository
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

class AdBlockVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnThread: Thread? = null
    @Volatile
    private var isRunning = false

    companion object {
        const val ACTION_START = "com.example.service.START"
        const val ACTION_STOP = "com.example.service.STOP"
        private const val CHANNEL_ID = "adblock_vpn_channel"
        private const val NOTIFICATION_ID = 1001
        private const val TAG = "AdBlockVpnService"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopVpn()
            return START_NOT_STICKY
        }

        if (isRunning) {
            return START_STICKY
        }

        startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        createNotificationChannel()
        val notification = buildNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        try {
            val builder = Builder()
                .setSession("AdBlock Shield")
                .addAddress("10.0.0.2", 32)
                .addDnsServer("8.8.8.8")
                .addDnsServer("1.1.1.1")
                .addRoute("0.0.0.0", 0)
                .setMtu(1500)

            vpnInterface = builder.establish()

            if (vpnInterface != null) {
                isRunning = true
                AdBlockRepository.setVpnActive(true)

                vpnThread = Thread({
                    runVpnLoop()
                }, "AdBlockVpnThread").apply {
                    start()
                }
                Log.i(TAG, "AdBlock VPN established and running")
            } else {
                Log.e(TAG, "Failed to establish VPN interface")
                stopSelf()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting VPN", e)
            stopSelf()
        }
    }

    private fun runVpnLoop() {
        val pfd = vpnInterface ?: return
        val inputStream = FileInputStream(pfd.fileDescriptor)
        val outputStream = FileOutputStream(pfd.fileDescriptor)
        val packet = ByteBuffer.allocate(32767)

        try {
            while (isRunning) {
                val length = inputStream.read(packet.array())
                if (length > 0) {
                    packet.limit(length)
                    packet.position(0)

                    val isAdBlocked = processPacket(packet)

                    if (isAdBlocked) {
                        // Drop packet - prevent ad server lookup from reaching network
                        packet.clear()
                        continue
                    }

                    // Forward legitimate packet
                    try {
                        outputStream.write(packet.array(), 0, length)
                    } catch (ioe: Exception) {
                        // Connection might be closing or buffer full
                    }
                    packet.clear()
                } else if (length < 0) {
                    break
                }
            }
        } catch (e: Exception) {
            if (isRunning) {
                Log.w(TAG, "VPN read/write interrupted: ${e.message}")
            }
        } finally {
            try {
                inputStream.close()
                outputStream.close()
            } catch (_: Exception) {}
        }
    }

    private fun processPacket(packet: ByteBuffer): Boolean {
        // Need at least 20 bytes for IPv4 header and 8 bytes for UDP header
        if (packet.remaining() < 28) return false

        val versionAndIhl = packet.get(0).toInt() and 0xFF
        val ipVersion = versionAndIhl ushr 4
        if (ipVersion != 4) return false

        val ipHeaderLen = (versionAndIhl and 0x0F) * 4
        if (packet.remaining() < ipHeaderLen + 8) return false

        // Check protocol (17 = UDP)
        val protocol = packet.get(9).toInt() and 0xFF
        if (protocol != 17) return false

        // Check UDP ports (DNS is port 53)
        val srcPort = ((packet.get(ipHeaderLen).toInt() and 0xFF) shl 8) or
                (packet.get(ipHeaderLen + 1).toInt() and 0xFF)
        val dstPort = ((packet.get(ipHeaderLen + 2).toInt() and 0xFF) shl 8) or
                (packet.get(ipHeaderLen + 3).toInt() and 0xFF)

        if (dstPort != 53 && srcPort != 53) return false

        // DNS header is 8 bytes into UDP header
        val dnsOffset = ipHeaderLen + 8
        if (packet.remaining() < dnsOffset + 12) return false

        // Extract queried domain starting after the 12-byte DNS header
        val domain = extractDomain(packet, dnsOffset + 12) ?: return false
        if (domain.isBlank()) return false

        val (isBlocked, category) = AdBlockRepository.isDomainBlocked(domain)
        AdBlockRepository.recordQuery(domain, isBlocked, category)

        return isBlocked
    }

    private fun extractDomain(packet: ByteBuffer, offset: Int): String? {
        val sb = StringBuilder()
        var pos = offset
        val limit = packet.limit()

        while (pos < limit) {
            val len = packet.get(pos).toInt() and 0xFF
            if (len == 0) break
            // DNS label length should not exceed 63
            if (len > 63 || pos + 1 + len > limit) return null

            pos++
            for (i in 0 until len) {
                val b = packet.get(pos++).toInt().toChar()
                sb.append(b)
            }
            sb.append('.')
        }

        if (sb.isNotEmpty() && sb.last() == '.') {
            sb.setLength(sb.length - 1)
        }

        return sb.toString().lowercase()
    }

    private fun stopVpn() {
        isRunning = false
        AdBlockRepository.setVpnActive(false)
        try {
            vpnThread?.interrupt()
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing VPN interface", e)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AdBlock VPN Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows ongoing protection status of AdBlock VPN"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AdBlockVpnService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        builder.setContentTitle("AdBlock Shield Active")
            .setContentText("Protecting games & apps from ads and trackers")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .addAction(
                Notification.Action.Builder(
                    null,
                    "Turn Off",
                    stopPendingIntent
                ).build()
            )

        return builder.build()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }
}
