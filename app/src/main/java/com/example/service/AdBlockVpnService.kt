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
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.concurrent.Executors

/**
 * Local DNS-filtering VPN.
 *
 * Design (the standard on-device DNS filter pattern):
 * - The TUN interface only claims the virtual DNS server addresses
 *   (10.0.0.1 and fd00::1). No general traffic route is added, so apps,
 *   games and browsing are never tunnelled and cannot be broken by us.
 * - Every DNS query the system sends to those servers lands in the TUN.
 * - Blocked domains get an immediate NXDOMAIN answer written back into
 *   the TUN. Allowed queries are forwarded over a protect()ed UDP socket
 *   to the real upstream resolver and the genuine answer is written back.
 */
class AdBlockVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnThread: Thread? = null
    @Volatile
    private var isRunning = false

    private var outputStream: FileOutputStream? = null
    private val writeLock = Any()
    private val executor = Executors.newCachedThreadPool()

    companion object {
        const val ACTION_START = "com.example.service.START"
        const val ACTION_STOP = "com.example.service.STOP"
        private const val CHANNEL_ID = "adblock_vpn_channel"
        private const val NOTIFICATION_ID = 1001
        private const val TAG = "AdBlockVpnService"

        private val VIRTUAL_DNS_V4 = byteArrayOf(10, 0, 0, 1)
        private const val VIRTUAL_HOST_V4 = "10.0.0.2"
        private val VIRTUAL_DNS_V6 =
            byteArrayOf(0xfd.toByte(), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1)
        private const val VIRTUAL_HOST_V6 = "fd00::2"

        private val UPSTREAM_DNS = listOf("8.8.8.8", "1.1.1.1")
        private const val DNS_PORT = 53
        private const val UPSTREAM_TIMEOUT_MS = 4000
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopVpn()
            return START_NOT_STICKY
        }
        if (isRunning) {
            return START_STICKY
        }
        AdBlockRepository.init(applicationContext)
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
                .addAddress(VIRTUAL_HOST_V4, 32)
                .addAddress(VIRTUAL_HOST_V6, 128)
                .addDnsServer("10.0.0.1")
                .addDnsServer("fd00::1")
                // Route ONLY the virtual DNS servers into the tunnel.
                .addRoute("10.0.0.1", 32)
                .addRoute("fd00::1", 128)
                .setMtu(1500)

            vpnInterface = builder.establish()

            if (vpnInterface != null) {
                isRunning = true
                AdBlockRepository.setVpnActive(true)
                vpnThread = Thread({ runVpnLoop() }, "AdBlockVpnThread").apply { start() }
                Log.i(TAG, "AdBlock DNS filter established")
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
        val out = FileOutputStream(pfd.fileDescriptor)
        outputStream = out
        val buffer = ByteArray(32767)

        try {
            while (isRunning) {
                val length = inputStream.read(buffer)
                if (length > 0) {
                    val packet = buffer.copyOf(length)
                    handlePacket(packet)
                } else if (length < 0) {
                    break
                }
            }
        } catch (e: Exception) {
            if (isRunning) {
                Log.w(TAG, "VPN loop interrupted: ${e.message}")
            }
        } finally {
            try {
                inputStream.close()
                out.close()
            } catch (_: Exception) {}
        }
    }

    /** One intercepted IP packet. Only UDP DNS queries are expected here. */
    private fun handlePacket(packet: ByteArray) {
        if (packet.isEmpty()) return
        when (packet[0].toInt() ushr 4) {
            4 -> handleIpv4(packet)
            6 -> handleIpv6(packet)
        }
    }

    private fun handleIpv4(packet: ByteArray) {
        if (packet.size < 28) return
        val ihl = (packet[0].toInt() and 0x0F) * 4
        if (packet.size < ihl + 8) return
        if (packet[9].toInt() and 0xFF != 17) return // UDP only

        val srcIp = packet.copyOfRange(12, 16)
        val dstIp = packet.copyOfRange(16, 20)
        if (!dstIp.contentEquals(VIRTUAL_DNS_V4)) return

        val srcPort = u16(packet, ihl)
        val dstPort = u16(packet, ihl + 2)
        if (dstPort != DNS_PORT) return

        val dnsStart = ihl + 8
        val dnsLen = packet.size - dnsStart
        if (dnsLen < 12) return
        val dns = packet.copyOfRange(dnsStart, packet.size)

        onDnsQuery(dns) { responseDns ->
            val response = buildIpv4Packet(srcIp, dstIp, dstPort, srcPort, responseDns)
            writeToTun(response)
        }
    }

    private fun handleIpv6(packet: ByteArray) {
        if (packet.size < 48) return
        if (packet[6].toInt() and 0xFF != 17) return // UDP only, no extension headers expected

        val srcIp = packet.copyOfRange(8, 24)
        val dstIp = packet.copyOfRange(24, 40)
        if (!dstIp.contentEquals(VIRTUAL_DNS_V6)) return

        val srcPort = u16(packet, 40)
        val dstPort = u16(packet, 42)
        if (dstPort != DNS_PORT) return

        val dnsStart = 48
        if (packet.size - dnsStart < 12) return
        val dns = packet.copyOfRange(dnsStart, packet.size)

        onDnsQuery(dns) { responseDns ->
            val response = buildIpv6Packet(srcIp, dstIp, dstPort, srcPort, responseDns)
            writeToTun(response)
        }
    }

    /**
     * Decide the fate of one DNS message. [respond] is called exactly once
     * with the DNS response payload to hand back to the querying app.
     */
    private fun onDnsQuery(dns: ByteArray, respond: (ByteArray) -> Unit) {
        val domain = extractDomain(dns)
        if (domain.isNullOrBlank()) {
            // Can't classify it — forward untouched rather than break it.
            forwardUpstream(dns, respond)
            return
        }

        val (isBlocked, category) = AdBlockRepository.isDomainBlocked(domain)
        AdBlockRepository.recordQuery(domain, isBlocked, category)

        if (isBlocked) {
            respond(buildErrorResponse(dns, rcode = 3)) // NXDOMAIN
        } else {
            forwardUpstream(dns, respond)
        }
    }

    private fun forwardUpstream(dns: ByteArray, respond: (ByteArray) -> Unit) {
        executor.execute {
            var answered: ByteArray? = null
            for (server in UPSTREAM_DNS) {
                try {
                    val socket = DatagramSocket()
                    protect(socket)
                    socket.soTimeout = UPSTREAM_TIMEOUT_MS
                    val address = InetAddress.getByName(server)
                    socket.send(DatagramPacket(dns, dns.size, address, DNS_PORT))
                    val buf = ByteArray(4096)
                    val reply = DatagramPacket(buf, buf.size)
                    socket.receive(reply)
                    answered = buf.copyOf(reply.length)
                    socket.close()
                    break
                } catch (e: Exception) {
                    Log.d(TAG, "Upstream $server failed: ${e.message}")
                }
            }
            respond(answered ?: buildErrorResponse(dns, rcode = 2)) // SERVFAIL
        }
    }

    /** Turn a query into an error response (NXDOMAIN / SERVFAIL), keeping the question. */
    private fun buildErrorResponse(query: ByteArray, rcode: Int): ByteArray {
        val questionEnd = questionEndOffset(query)
        val response = query.copyOf(questionEnd)
        // Flags: QR=1, keep opcode+RD from query, RA=1, RCODE as given.
        val queryFlags = ((query[2].toInt() and 0xFF) shl 8) or (query[3].toInt() and 0xFF)
        val flags = 0x8000 or (queryFlags and 0x7900) or 0x0080 or rcode
        response[2] = (flags ushr 8).toByte()
        response[3] = (flags and 0xFF).toByte()
        // ANCOUNT, NSCOUNT, ARCOUNT = 0 (QDCOUNT stays as in the query).
        for (i in 6..11) response[i] = 0
        return response
    }

    /** Offset just past the question section of a DNS message. */
    private fun questionEndOffset(dns: ByteArray): Int {
        var pos = 12
        while (pos < dns.size) {
            val len = dns[pos].toInt() and 0xFF
            if (len == 0) {
                pos++
                break
            }
            if (len >= 0xC0) { // compression pointer (shouldn't appear in a question)
                pos += 2
                break
            }
            pos += 1 + len
        }
        // Skip QTYPE + QCLASS.
        return minOf(pos + 4, dns.size)
    }

    private fun extractDomain(dns: ByteArray): String? {
        if (dns.size < 13) return null
        val sb = StringBuilder()
        var pos = 12
        while (pos < dns.size) {
            val len = dns[pos].toInt() and 0xFF
            if (len == 0) break
            if (len > 63 || pos + 1 + len > dns.size) return null
            pos++
            for (i in 0 until len) {
                sb.append((dns[pos++].toInt() and 0xFF).toChar())
            }
            sb.append('.')
        }
        if (sb.isNotEmpty() && sb.last() == '.') sb.setLength(sb.length - 1)
        return sb.toString().lowercase()
    }

    // ------------------------------------------------------------------
    // Packet building
    // ------------------------------------------------------------------

    private fun buildIpv4Packet(
        dstIp: ByteArray, // original source (the device)
        srcIp: ByteArray, // original destination (virtual DNS server)
        srcPort: Int,
        dstPort: Int,
        dnsPayload: ByteArray
    ): ByteArray {
        val totalLen = 20 + 8 + dnsPayload.size
        val packet = ByteArray(totalLen)
        packet[0] = 0x45 // version + IHL
        packet[1] = 0
        packet[2] = (totalLen ushr 8).toByte()
        packet[3] = (totalLen and 0xFF).toByte()
        packet[4] = 0
        packet[5] = 0
        packet[6] = 0x40 // don't fragment
        packet[7] = 0
        packet[8] = 64 // TTL
        packet[9] = 17 // UDP
        System.arraycopy(srcIp, 0, packet, 12, 4)
        System.arraycopy(dstIp, 0, packet, 16, 4)
        val ipChecksum = checksum(packet, 0, 20)
        packet[10] = (ipChecksum ushr 8).toByte()
        packet[11] = (ipChecksum and 0xFF).toByte()

        writeUdpHeader(packet, 20, srcPort, dstPort, dnsPayload)
        System.arraycopy(dnsPayload, 0, packet, 28, dnsPayload.size)
        // UDP checksum with IPv4 pseudo-header.
        val pseudo = ByteBuffer.allocate(12)
        pseudo.put(srcIp)
        pseudo.put(dstIp)
        pseudo.put(0.toByte())
        pseudo.put(17.toByte())
        pseudo.putShort((8 + dnsPayload.size).toShort())
        val udpChecksum = checksumSegments(pseudo.array(), packet.copyOfRange(20, totalLen))
        packet[26] = (udpChecksum ushr 8).toByte()
        packet[27] = (udpChecksum and 0xFF).toByte()
        return packet
    }

    private fun buildIpv6Packet(
        dstIp: ByteArray,
        srcIp: ByteArray,
        srcPort: Int,
        dstPort: Int,
        dnsPayload: ByteArray
    ): ByteArray {
        val payloadLen = 8 + dnsPayload.size
        val packet = ByteArray(40 + payloadLen)
        packet[0] = 0x60 // version 6
        packet[4] = (payloadLen ushr 8).toByte()
        packet[5] = (payloadLen and 0xFF).toByte()
        packet[6] = 17 // next header: UDP
        packet[7] = 64 // hop limit
        System.arraycopy(srcIp, 0, packet, 8, 16)
        System.arraycopy(dstIp, 0, packet, 24, 16)

        writeUdpHeader(packet, 40, srcPort, dstPort, dnsPayload)
        System.arraycopy(dnsPayload, 0, packet, 48, dnsPayload.size)
        // UDP checksum with IPv6 pseudo-header.
        val pseudo = ByteBuffer.allocate(40)
        pseudo.put(srcIp)
        pseudo.put(dstIp)
        pseudo.putInt(payloadLen)
        pseudo.put(0.toByte())
        pseudo.put(0.toByte())
        pseudo.put(0.toByte())
        pseudo.put(17.toByte())
        val udpChecksum = checksumSegments(pseudo.array(), packet.copyOfRange(40, packet.size))
        packet[46] = (udpChecksum ushr 8).toByte()
        packet[47] = (udpChecksum and 0xFF).toByte()
        return packet
    }

    private fun writeUdpHeader(packet: ByteArray, offset: Int, srcPort: Int, dstPort: Int, payload: ByteArray) {
        packet[offset] = (srcPort ushr 8).toByte()
        packet[offset + 1] = (srcPort and 0xFF).toByte()
        packet[offset + 2] = (dstPort ushr 8).toByte()
        packet[offset + 3] = (dstPort and 0xFF).toByte()
        val len = 8 + payload.size
        packet[offset + 4] = (len ushr 8).toByte()
        packet[offset + 5] = (len and 0xFF).toByte()
        packet[offset + 6] = 0
        packet[offset + 7] = 0
    }

    private fun u16(data: ByteArray, offset: Int): Int =
        ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)

    private fun checksum(data: ByteArray, offset: Int, length: Int): Int =
        checksumSegments(data.copyOfRange(offset, offset + length))

    private fun checksumSegments(vararg segments: ByteArray): Int {
        var sum = 0L
        var carryByte = -1
        for (segment in segments) {
            var i = 0
            if (carryByte >= 0 && segment.isNotEmpty()) {
                sum += (carryByte shl 8) or (segment[0].toInt() and 0xFF)
                carryByte = -1
                i = 1
            }
            while (i + 1 < segment.size) {
                sum += ((segment[i].toInt() and 0xFF) shl 8) or (segment[i + 1].toInt() and 0xFF)
                i += 2
            }
            if (i < segment.size) {
                carryByte = segment[i].toInt() and 0xFF
            }
        }
        if (carryByte >= 0) {
            sum += carryByte shl 8
        }
        while (sum ushr 16 != 0L) {
            sum = (sum and 0xFFFF) + (sum ushr 16)
        }
        val result = sum.inv().toInt() and 0xFFFF
        // A computed UDP checksum of 0 is transmitted as 0xFFFF.
        return if (result == 0) 0xFFFF else result
    }

    private fun writeToTun(packet: ByteArray) {
        val out = outputStream ?: return
        synchronized(writeLock) {
            try {
                out.write(packet)
                out.flush()
            } catch (e: Exception) {
                Log.d(TAG, "TUN write failed: ${e.message}")
            }
        }
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
        executor.shutdownNow()
        super.onDestroy()
    }
}
