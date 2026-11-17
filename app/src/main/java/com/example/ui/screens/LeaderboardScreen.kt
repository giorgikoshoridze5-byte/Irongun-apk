package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.LeaderboardEntryEntity
import com.example.data.MatchHistoryEntity
import com.example.model.RankTier
import com.example.ui.theme.AmberMuted
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.CrimsonThreat
import com.example.ui.theme.CyanAllied
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.GunmetalSurface
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SteelBorder
import com.example.ui.theme.TacticalSlate
import com.example.ui.theme.TextSecondary

@Composable
fun LeaderboardScreen(
    leaderboard: List<LeaderboardEntryEntity>,
    history: List<MatchHistoryEntity>,
    modifier: Modifier = Modifier
) {
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("COMPETITIVE STANDINGS", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Local competitive MMR ladder & match telemetry (persisted on-device).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            itemsIndexed(leaderboard, key = { _, item -> item.callsign }) { idx, entry ->
                val tier = RankTier.fromMmr(entry.mmr)
                val kd = if (entry.deaths > 0) String.format("%.2f", entry.kills.toFloat() / entry.deaths) else "${entry.kills}.00"
                Surface(
                    color = if (entry.isLocalPlayer) TacticalSlate else GunmetalSurface,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        width = if (entry.isLocalPlayer) 1.5.dp else 1.dp,
                        color = if (entry.isLocalPlayer) AmberPrimary else SteelBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "#${idx + 1}",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (idx < 3) AmberPrimary else TextSecondary,
                            modifier = Modifier.width(34.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(entry.callsign, style = MaterialTheme.typography.titleMedium)
                                if (entry.isLocalPlayer) {
                                    Spacer(Modifier.width(6.dp))
                                    Surface(color = AmberMuted, shape = RoundedCornerShape(4.dp)) {
                                        Text(
                                            "YOU",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AmberPrimary,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${tier.title} • K/D $kd • ${entry.wins}W",
                                style = MaterialTheme.typography.bodyMedium,
                                color = tier.color
                            )
                        }
                        Text(
                            text = "${entry.mmr} MMR",
                            style = MaterialTheme.typography.labelMedium,
                            color = CyanAllied
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.History, contentDescription = null, tint = AmberPrimary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("RECENT MATCH LOG", style = MaterialTheme.typography.labelLarge, color = AmberPrimary)
                }
            }

            if (history.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = GunmetalSurface),
                        border = BorderStroke(1.dp, SteelBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("No Combat Logs Yet", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Deploy to District 09 or Kandahar Ridge to record competitive MMR.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(history, key = { it.id }) { match ->
                    val deltaSign = if (match.mmrDelta >= 0) "+${match.mmrDelta}" else "${match.mmrDelta}"
                    Card(
                        colors = CardDefaults.cardColors(containerColor = GunmetalSurface),
                        border = BorderStroke(1.dp, if (match.isVictory) EmeraldReady.copy(alpha = 0.45f) else CrimsonThreat.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${if (match.isVictory) "VICTORY" else "DEFEAT"} • ${match.gameMode}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (match.isVictory) EmeraldReady else CrimsonThreat
                                )
                                Text(match.mapName, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "${match.kills}K / ${match.deaths}D • ${match.headshots} HS • ${match.accuracyPercent}% ACC",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                text = "$deltaSign MMR",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (match.mmrDelta >= 0) EmeraldReady else CrimsonThreat
                            )
                        }
                    }
                }
            }
        }
    }
}
