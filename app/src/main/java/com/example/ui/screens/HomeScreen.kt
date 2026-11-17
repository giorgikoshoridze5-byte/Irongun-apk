package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.PlayerProfileEntity
import com.example.data.WeaponLoadoutEntity
import com.example.model.ArenaMapId
import com.example.model.GameMode
import com.example.model.MuzzleAttachment
import com.example.model.OpticAttachment
import com.example.model.RankTier
import com.example.model.WeaponType
import com.example.ui.theme.AmberMuted
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.CyanAllied
import com.example.ui.theme.GunmetalSurface
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SteelBorder
import com.example.ui.theme.TacticalSlate
import com.example.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    profile: PlayerProfileEntity,
    loadouts: List<WeaponLoadoutEntity>,
    activeLoadout: WeaponLoadoutEntity,
    selectedMap: ArenaMapId,
    selectedMode: GameMode,
    onSelectLoadoutSlot: (Int) -> Unit,
    onOpenArmory: () -> Unit,
    onSelectMap: (ArenaMapId) -> Unit,
    onSelectMode: (GameMode) -> Unit,
    onDeploy: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rank = RankTier.fromMmr(profile.mmr)
    val nextProgress = ((profile.mmr - rank.minMmr).toFloat() / (rank.maxMmr - rank.minMmr + 1).coerceAtLeast(1)).coerceIn(0f, 1f)
    val primary = WeaponType.primaries.firstOrNull { it.name == activeLoadout.primaryWeapon } ?: WeaponType.M4A1
    val secondary = WeaponType.secondaries.firstOrNull { it.name == activeLoadout.secondaryWeapon } ?: WeaponType.P226
    val optic = OpticAttachment.entries.firstOrNull { it.name == activeLoadout.primaryOptic } ?: OpticAttachment.HOLO_CYAN
    val muzzle = MuzzleAttachment.entries.firstOrNull { it.name == activeLoadout.primaryMuzzle } ?: MuzzleAttachment.STANDARD
    val kd = if (profile.totalDeaths > 0) String.format("%.2f", profile.totalKills.toFloat() / profile.totalDeaths) else "${profile.totalKills}.00"

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Tactical Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, SteelBorder, RoundedCornerShape(16.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_warzone),
                        contentDescription = stringResource(R.string.cd_hero_banner),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0x550A0E14), Color(0xEE0A0E14))
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = AmberMuted,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, AmberPrimary)
                        ) {
                            Text(
                                text = "OPENGL ES 3.0 • 60 FPS",
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xAA121821))
                                .testTag("settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = stringResource(R.string.cd_settings),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_avatar_operator),
                            contentDescription = stringResource(R.string.cd_operator_avatar),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(58.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(2.dp, rank.color, RoundedCornerShape(12.dp))
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.callsign,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.MilitaryTech,
                                    contentDescription = null,
                                    tint = rank.color,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "${rank.title} • ${profile.mmr} MMR",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = rank.color
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { nextProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = rank.color,
                                trackColor = TacticalSlate
                            )
                        }
                    }
                }
            }

            // Quick Telemetry Strip
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatPill("K/D RATIO", kd, CyanAllied, Modifier.weight(1f))
                    StatPill("WINS", "${profile.matchesWon}/${profile.matchesPlayed}", AmberPrimary, Modifier.weight(1f))
                    StatPill("LOADOUT", activeLoadout.slotName, MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
                }
            }

            // Pre-Match Active Weapon Loadout Quick Selector
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GunmetalSurface),
                    border = BorderStroke(1.dp, SteelBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("PRE-MATCH LOADOUT", style = MaterialTheme.typography.labelLarge, color = AmberPrimary)
                            TextButton(onClick = onOpenArmory, modifier = Modifier.testTag("customize_loadout_btn")) {
                                Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("CUSTOMIZE")
                            }
                        }

                        if (loadouts.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                loadouts.forEach { slot ->
                                    val selected = slot.slotIndex == activeLoadout.slotIndex
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onSelectLoadoutSlot(slot.slotIndex) }
                                            .testTag("home_loadout_slot_${slot.slotIndex}"),
                                        color = if (selected) AmberMuted else TacticalSlate,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, if (selected) AmberPrimary else SteelBorder)
                                    ) {
                                        Text(
                                            text = slot.slotName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (selected) AmberPrimary else TextSecondary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                color = TacticalSlate,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("PRIMARY", style = MaterialTheme.typography.labelSmall, color = CyanAllied)
                                    Text(primary.displayName, style = MaterialTheme.typography.titleMedium)
                                    Text("${optic.displayName} • ${muzzle.displayName.substringBefore(" ")}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                }
                            }
                            Surface(
                                color = TacticalSlate,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("SECONDARY", style = MaterialTheme.typography.labelSmall, color = AmberPrimary)
                                    Text(secondary.displayName, style = MaterialTheme.typography.titleMedium)
                                    Text(secondary.caliber, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }

            // Map Selection (2 Distinct Tactical 3D Maps)
            item {
                Text(
                    text = "SELECT COMBAT THEATER (2 MAPS)",
                    style = MaterialTheme.typography.labelLarge,
                    color = AmberPrimary
                )
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ArenaMapId.entries.forEach { map ->
                        val isSelected = map == selectedMap
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectMap(map) }
                                .testTag("map_card_${map.name.lowercase()}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) TacticalSlate else GunmetalSurface
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) AmberPrimary else SteelBorder
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (map == ArenaMapId.URBAN_DISTRICT) Icons.Outlined.LocationCity else Icons.Outlined.Landscape,
                                            contentDescription = null,
                                            tint = if (isSelected) AmberPrimary else CyanAllied,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = map.displayName,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Surface(
                                        color = if (isSelected) AmberMuted else Color(0x2206B6D4),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = map.codeName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) AmberPrimary else CyanAllied,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = map.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "TACTICAL: ${map.engagementProfile} • Barrels, Breakable Barriers, Hackable Turrets",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyanAllied
                                )
                            }
                        }
                    }
                }
            }

            // Combat Mode Selection
            item {
                Text(
                    text = "OPERATION MODE",
                    style = MaterialTheme.typography.labelLarge,
                    color = AmberPrimary
                )
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        GameMode.TEAM_DEATHMATCH,
                        GameMode.ZONE_DOMINATION,
                        GameMode.WAVE_SURVIVAL
                    ).forEach { mode ->
                        val active = mode == selectedMode
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectMode(mode) }
                                .testTag("mode_${mode.name.lowercase()}"),
                            color = if (active) TacticalSlate else GunmetalSurface,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (active) CyanAllied else SteelBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (mode == GameMode.ZONE_DOMINATION) Icons.Filled.Shield else Icons.Filled.GpsFixed,
                                    contentDescription = null,
                                    tint = if (active) CyanAllied else TextSecondary
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mode.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = mode.subtitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Deploy CTA
            item {
                Button(
                    onClick = onDeploy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("deploy_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberPrimary,
                        contentColor = ObsidianBg
                    )
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.deploy_now),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Honest Cloud Status Card
            item {
                Surface(
                    color = GunmetalSurface,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, SteelBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudOff,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.cloud_offline_title),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.cloud_offline_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = GunmetalSurface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, SteelBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Spacer(Modifier.height(2.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, color = accent)
        }
    }
}
