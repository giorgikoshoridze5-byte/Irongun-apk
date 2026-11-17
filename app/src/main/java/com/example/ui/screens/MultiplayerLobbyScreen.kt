package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.ArenaMapId
import com.example.network.LanLobbyState
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.CyanAllied
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.GunmetalSurface
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SteelBorder
import com.example.ui.theme.TacticalSlate
import com.example.ui.theme.TextSecondary

@Composable
fun MultiplayerLobbyScreen(
    lobbyState: LanLobbyState,
    selectedMap: ArenaMapId,
    onSelectMap: (ArenaMapId) -> Unit,
    onHostMatch: () -> Unit,
    onJoinMatch: (String) -> Unit,
    onStopSession: () -> Unit,
    onLaunchLanMatch: () -> Unit,
    modifier: Modifier = Modifier
) {
    var hostIpInput by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("REAL-TIME LAN MULTIPLAYER", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Low-latency UDP socket match hosting (port 47777) for devices on the same Wi-Fi network.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            // Honest Cloud Hosting Status
            item {
                Surface(
                    color = GunmetalSurface,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, SteelBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Filled.CloudOff, contentDescription = null, tint = AmberPrimary)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Cloud Match Hosting Offline", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Firebase backend provisioning was declined, so internet cloud lobbies are disabled. Use Wi-Fi LAN PvP below or AI Tactical Skirmishes on the Command tab.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Local Network Status Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GunmetalSurface),
                    border = BorderStroke(1.dp, if (lobbyState.isConnected) EmeraldReady else SteelBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Wifi,
                                contentDescription = null,
                                tint = if (lobbyState.isConnected) EmeraldReady else CyanAllied
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "LOCAL IP: ${lobbyState.localIp}:${lobbyState.port}",
                                style = MaterialTheme.typography.labelMedium,
                                color = CyanAllied
                            )
                            Spacer(Modifier.weight(1f))
                            if (lobbyState.pingMs > 0) {
                                Text(
                                    text = "${lobbyState.pingMs} ms",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EmeraldReady
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(lobbyState.statusMessage, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Map Selection for Hosted Match
            item {
                Text("HOSTED MAP", style = MaterialTheme.typography.labelLarge, color = AmberPrimary)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ArenaMapId.entries.forEach { map ->
                        val active = map == selectedMap
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSelectMap(map) },
                            color = if (active) TacticalSlate else GunmetalSurface,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (active) AmberPrimary else SteelBorder)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(map.codeName, style = MaterialTheme.typography.labelSmall, color = AmberPrimary)
                                Text(map.displayName.substringBefore(":"), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }

            // Host / Join Actions
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onHostMatch,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAllied, contentColor = ObsidianBg),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("lan_host_button")
                    ) {
                        Icon(Icons.Filled.Lan, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("HOST MATCH")
                    }
                    OutlinedButton(
                        onClick = onStopSession,
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("lan_stop_button")
                    ) {
                        Text("RESET")
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GunmetalSurface),
                    border = BorderStroke(1.dp, SteelBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("JOIN BY HOST IP", style = MaterialTheme.typography.labelLarge, color = AmberPrimary)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = hostIpInput,
                                onValueChange = { hostIpInput = it },
                                label = { Text("Host IPv4 (e.g. 192.168.1.42)") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("lan_ip_input")
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { onJoinMatch(hostIpInput) },
                                modifier = Modifier
                                    .height(52.dp)
                                    .testTag("lan_join_button")
                            ) {
                                Text("JOIN")
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = onLaunchLanMatch,
                    enabled = lobbyState.isHosting || lobbyState.isConnected,
                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary, contentColor = ObsidianBg),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("lan_deploy_button")
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("ENTER LAN ARENA (${selectedMap.codeName})")
                }
            }
        }
    }
}
