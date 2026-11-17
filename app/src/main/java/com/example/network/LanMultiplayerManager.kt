package com.example.network

import com.example.model.ArenaMapId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.concurrent.atomic.AtomicReference

data class LanLobbyState(
    val localIp: String = "Unavailable",
    val port: Int = 47777,
    val isHosting: Boolean = false,
    val isJoined: Boolean = false,
    val isConnected: Boolean = false,
    val peerCallsign: String = "",
    val selectedMap: ArenaMapId = ArenaMapId.URBAN_DISTRICT,
    val pingMs: Int = 0,
    val statusMessage: String = "Idle • Ready to host or join on local Wi-Fi"
)

data class RemotePlayerPacket(
    val callsign: String = "RIVAL-LAN",
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 22f,
    val yawDeg: Float = 180f,
    val pitchDeg: Float = 0f,
    val hp: Int = 100,
    val isFiring: Boolean = false,
    val hitsClaimed: Int = 0,
    val kills: Int = 0,
    val mapId: ArenaMapId = ArenaMapId.URBAN_DISTRICT,
    val timestampMs: Long = 0L
)

class LanMultiplayerManager(private val scope: CoroutineScope) {
    private val _lobbyState = MutableStateFlow(LanLobbyState(localIp = resolveLocalIpv4()))
    val lobbyState: StateFlow<LanLobbyState> = _lobbyState.asStateFlow()

    val latestRemotePacket = AtomicReference<RemotePlayerPacket?>(null)

    private var socket: DatagramSocket? = null
    private var rxJob: Job? = null
    private var heartbeatJob: Job? = null
    private var peerAddress: InetAddress? = null
    private var peerPort: Int = 47777

    fun refreshLocalIp() {
        _lobbyState.value = _lobbyState.value.copy(localIp = resolveLocalIpv4())
    }

    fun startHost(mapId: ArenaMapId, localCallsign: String) {
        stopSession()
        val ip = resolveLocalIpv4()
        _lobbyState.value = LanLobbyState(
            localIp = ip,
            port = 47777,
            isHosting = true,
            selectedMap = mapId,
            statusMessage = "Hosting ${mapId.codeName} on $ip:47777 • Waiting for peer..."
        )
        launchSocketListener(bindPort = 47777, localCallsign = localCallsign, initialMap = mapId)
    }

    fun joinHost(hostIp: String, localCallsign: String) {
        val trimmed = hostIp.trim()
        if (trimmed.isEmpty()) {
            _lobbyState.value = _lobbyState.value.copy(statusMessage = "Enter host IPv4 address")
            return
        }
        stopSession()
        _lobbyState.value = LanLobbyState(
            localIp = resolveLocalIpv4(),
            port = 47777,
            isJoined = true,
            statusMessage = "Connecting to $trimmed:47777..."
        )
        scope.launch(Dispatchers.IO) {
            try {
                peerAddress = InetAddress.getByName(trimmed)
                peerPort = 47777
                launchSocketListener(bindPort = 47778, localCallsign = localCallsign, initialMap = _lobbyState.value.selectedMap)
                sendControl("HELLO|$localCallsign|${System.currentTimeMillis()}")
            } catch (e: Exception) {
                _lobbyState.value = _lobbyState.value.copy(
                    isJoined = false,
                    statusMessage = "Failed to resolve host: ${e.message}"
                )
            }
        }
    }

    fun updateHostedMap(mapId: ArenaMapId) {
        _lobbyState.value = _lobbyState.value.copy(selectedMap = mapId)
        if (_lobbyState.value.isConnected) {
            sendControl("MAP|${mapId.name}")
        }
    }

    fun sendGameState(
        callsign: String,
        x: Float,
        y: Float,
        z: Float,
        yawDeg: Float,
        pitchDeg: Float,
        hp: Int,
        isFiring: Boolean,
        hitsClaimed: Int,
        kills: Int,
        mapId: ArenaMapId
    ) {
        val target = peerAddress ?: return
        val sock = socket ?: return
        val msg = "STATE|$callsign|$x|$y|$z|$yawDeg|$pitchDeg|$hp|${if (isFiring) 1 else 0}|$hitsClaimed|$kills|${mapId.name}|${System.currentTimeMillis()}"
        val bytes = msg.toByteArray(Charsets.UTF_8)
        scope.launch(Dispatchers.IO) {
            try {
                sock.send(DatagramPacket(bytes, bytes.size, target, peerPort))
            } catch (_: Exception) {}
        }
    }

    private fun sendControl(payload: String) {
        val target = peerAddress ?: return
        val sock = socket ?: return
        val bytes = payload.toByteArray(Charsets.UTF_8)
        scope.launch(Dispatchers.IO) {
            try {
                sock.send(DatagramPacket(bytes, bytes.size, target, peerPort))
            } catch (_: Exception) {}
        }
    }

    private fun launchSocketListener(bindPort: Int, localCallsign: String, initialMap: ArenaMapId) {
        rxJob = scope.launch(Dispatchers.IO) {
            try {
                val sock = DatagramSocket(bindPort).apply {
                    soTimeout = 0
                    reuseAddress = true
                }
                socket = sock
                val buf = ByteArray(1024)
                while (isActive && !sock.isClosed) {
                    val packet = DatagramPacket(buf, buf.size)
                    sock.receive(packet)
                    peerAddress = packet.address
                    peerPort = packet.port
                    val text = String(packet.data, 0, packet.length, Charsets.UTF_8)
                    handleIncomingMessage(text, localCallsign, initialMap)
                }
            } catch (e: Exception) {
                if (isActive) {
                    _lobbyState.value = _lobbyState.value.copy(
                        isConnected = false,
                        statusMessage = "LAN socket closed: ${e.localizedMessage ?: "disconnected"}"
                    )
                }
            }
        }
        heartbeatJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                if (peerAddress != null) {
                    sendControl("PING|${System.currentTimeMillis()}")
                }
                delay(1000L)
            }
        }
    }

    private fun handleIncomingMessage(text: String, localCallsign: String, initialMap: ArenaMapId) {
        val parts = text.split("|")
        if (parts.isEmpty()) return
        when (parts[0]) {
            "HELLO" -> {
                val peerName = parts.getOrNull(1) ?: "LAN-OP"
                _lobbyState.value = _lobbyState.value.copy(
                    isConnected = true,
                    peerCallsign = peerName,
                    statusMessage = "Connected to $peerName"
                )
                sendControl("WELCOME|$localCallsign|${initialMap.name}")
            }
            "WELCOME" -> {
                val peerName = parts.getOrNull(1) ?: "HOST-OP"
                val mapName = parts.getOrNull(2)
                val syncedMap = ArenaMapId.entries.firstOrNull { it.name == mapName } ?: _lobbyState.value.selectedMap
                _lobbyState.value = _lobbyState.value.copy(
                    isConnected = true,
                    peerCallsign = peerName,
                    selectedMap = syncedMap,
                    statusMessage = "Connected to $peerName (${syncedMap.codeName})"
                )
            }
            "MAP" -> {
                val mapName = parts.getOrNull(1)
                val syncedMap = ArenaMapId.entries.firstOrNull { it.name == mapName } ?: return
                _lobbyState.value = _lobbyState.value.copy(selectedMap = syncedMap)
            }
            "PING" -> {
                val ts = parts.getOrNull(1) ?: return
                sendControl("PONG|$ts")
            }
            "PONG" -> {
                val sentTs = parts.getOrNull(1)?.toLongOrNull() ?: return
                val rtt = (System.currentTimeMillis() - sentTs).toInt().coerceIn(1, 999)
                _lobbyState.value = _lobbyState.value.copy(pingMs = rtt)
            }
            "STATE" -> {
                if (parts.size >= 13) {
                    val map = ArenaMapId.entries.firstOrNull { it.name == parts[11] } ?: ArenaMapId.URBAN_DISTRICT
                    val pkt = RemotePlayerPacket(
                        callsign = parts[1],
                        x = parts[2].toFloatOrNull() ?: 0f,
                        y = parts[3].toFloatOrNull() ?: 0f,
                        z = parts[4].toFloatOrNull() ?: 0f,
                        yawDeg = parts[5].toFloatOrNull() ?: 0f,
                        pitchDeg = parts[6].toFloatOrNull() ?: 0f,
                        hp = parts[7].toIntOrNull() ?: 100,
                        isFiring = parts[8] == "1",
                        hitsClaimed = parts[9].toIntOrNull() ?: 0,
                        kills = parts[10].toIntOrNull() ?: 0,
                        mapId = map,
                        timestampMs = parts[12].toLongOrNull() ?: 0L
                    )
                    latestRemotePacket.set(pkt)
                    if (!_lobbyState.value.isConnected) {
                        _lobbyState.value = _lobbyState.value.copy(
                            isConnected = true,
                            peerCallsign = pkt.callsign,
                            statusMessage = "Live match link with ${pkt.callsign}"
                        )
                    }
                }
            }
        }
    }

    fun stopSession() {
        rxJob?.cancel()
        heartbeatJob?.cancel()
        rxJob = null
        heartbeatJob = null
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null
        peerAddress = null
        latestRemotePacket.set(null)
        _lobbyState.value = LanLobbyState(localIp = resolveLocalIpv4())
    }

    private fun resolveLocalIpv4(): String {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                for (addr in intf.inetAddresses) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        return addr.hostAddress ?: "Unavailable"
                    }
                }
            }
            "127.0.0.1"
        } catch (_: Exception) {
            "Unavailable"
        }
    }
}
