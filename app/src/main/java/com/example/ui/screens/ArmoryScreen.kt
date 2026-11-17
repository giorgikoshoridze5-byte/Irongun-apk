package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.PlayerProfileEntity
import com.example.data.WeaponLoadoutEntity
import com.example.model.MuzzleAttachment
import com.example.model.OpticAttachment
import com.example.model.TacticalPerk
import com.example.model.WeaponCamo
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
fun ArmoryScreen(
    profile: PlayerProfileEntity,
    loadouts: List<WeaponLoadoutEntity>,
    activeLoadout: WeaponLoadoutEntity,
    onSelectSlot: (Int) -> Unit,
    onRenameSlot: (Int, String) -> Unit,
    onSetPrimaryWeapon: (Int, WeaponType) -> Unit,
    onSetSecondaryWeapon: (Int, WeaponType) -> Unit,
    onSetOptic: (Int, OpticAttachment) -> Unit,
    onSetMuzzle: (Int, MuzzleAttachment) -> Unit,
    onSetPrimaryCamo: (Int, WeaponCamo) -> Unit,
    onSetSecondaryCamo: (Int, WeaponCamo) -> Unit,
    onSetPerk: (Int, TacticalPerk) -> Unit,
    onSaveCallsign: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var callsignInput by remember(profile.callsign) { mutableStateOf(profile.callsign) }
    var slotNameInput by remember(activeLoadout.slotIndex, activeLoadout.slotName) { mutableStateOf(activeLoadout.slotName) }

    val primary = WeaponType.primaries.firstOrNull { it.name == activeLoadout.primaryWeapon } ?: WeaponType.M4A1
    val secondary = WeaponType.secondaries.firstOrNull { it.name == activeLoadout.secondaryWeapon } ?: WeaponType.P226
    val activeOptic = OpticAttachment.entries.firstOrNull { it.name == activeLoadout.primaryOptic } ?: OpticAttachment.HOLO_CYAN
    val activeMuzzle = MuzzleAttachment.entries.firstOrNull { it.name == activeLoadout.primaryMuzzle } ?: MuzzleAttachment.STANDARD
    val activePrimaryCamo = WeaponCamo.entries.firstOrNull { it.name == activeLoadout.primaryCamo } ?: WeaponCamo.OBSIDIAN_STEEL
    val activeSecondaryCamo = WeaponCamo.entries.firstOrNull { it.name == activeLoadout.secondaryCamo } ?: WeaponCamo.OBSIDIAN_STEEL
    val activePerk = TacticalPerk.entries.firstOrNull { it.name == activeLoadout.perk } ?: TacticalPerk.KEVLAR_PLATING

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
                Text("TACTICAL ARMORY & LOADOUTS", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Customize 3 persistent Room loadout slots with Primary & Secondary firearms, optics, muzzles, and camos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            // Callsign Editor
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GunmetalSurface),
                    border = BorderStroke(1.dp, SteelBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = callsignInput,
                            onValueChange = { if (it.length <= 14) callsignInput = it },
                            label = { Text("Operator Callsign") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("callsign_input")
                        )
                        Spacer(Modifier.width(10.dp))
                        Button(
                            onClick = { onSaveCallsign(callsignInput) },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary, contentColor = ObsidianBg),
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("save_callsign_button")
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("SET")
                        }
                    }
                }
            }

            // Loadout Slot Tabs + Slot Name Editor
            item {
                Text("LOADOUT SLOTS (ROOM PERSISTED)", style = MaterialTheme.typography.labelLarge, color = AmberPrimary)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    (if (loadouts.isNotEmpty()) loadouts else listOf(activeLoadout)).forEach { slot ->
                        val selected = slot.slotIndex == activeLoadout.slotIndex
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clickable { onSelectSlot(slot.slotIndex) }
                                .testTag("armory_slot_${slot.slotIndex}"),
                            color = if (selected) TacticalSlate else GunmetalSurface,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.5.dp, if (selected) AmberPrimary else SteelBorder)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("SLOT 0${slot.slotIndex}", style = MaterialTheme.typography.labelSmall, color = if (selected) AmberPrimary else TextSecondary)
                                Text(slot.slotName, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = slotNameInput,
                        onValueChange = { if (it.length <= 12) slotNameInput = it },
                        label = { Text("Loadout Slot Name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("slot_name_input")
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onRenameSlot(activeLoadout.slotIndex, slotNameInput) },
                        modifier = Modifier.height(52.dp).testTag("save_slot_name_button")
                    ) {
                        Text("RENAME")
                    }
                }
            }

            // Primary Firearms
            item {
                Text("PRIMARY FIREARM", style = MaterialTheme.typography.labelLarge, color = CyanAllied)
            }

            WeaponType.primaries.forEach { weapon ->
                item(key = "p_${weapon.name}") {
                    WeaponSelectionCard(
                        weapon = weapon,
                        selected = weapon == primary,
                        accent = CyanAllied,
                        onClick = { onSetPrimaryWeapon(activeLoadout.slotIndex, weapon) },
                        testTag = "primary_${weapon.name.lowercase()}"
                    )
                }
            }

            // Primary Attachments & Camo
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GunmetalSurface),
                    border = BorderStroke(1.dp, SteelBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("PRIMARY OPTIC", style = MaterialTheme.typography.labelLarge, color = AmberPrimary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OpticAttachment.entries.forEach { optic ->
                                val selected = optic == activeOptic
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onSetOptic(activeLoadout.slotIndex, optic) }
                                        .testTag("optic_${optic.name.lowercase()}"),
                                    color = if (selected) TacticalSlate else ObsidianBg,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (selected) CyanAllied else SteelBorder)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(optic.displayName, style = MaterialTheme.typography.labelSmall, color = if (selected) CyanAllied else MaterialTheme.colorScheme.onSurface)
                                        Text(optic.description.substringBefore(" •"), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    }
                                }
                            }
                        }

                        Text("PRIMARY MUZZLE", style = MaterialTheme.typography.labelLarge, color = AmberPrimary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            MuzzleAttachment.entries.forEach { muzzle ->
                                val selected = muzzle == activeMuzzle
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onSetMuzzle(activeLoadout.slotIndex, muzzle) }
                                        .testTag("muzzle_${muzzle.name.lowercase()}"),
                                    color = if (selected) TacticalSlate else ObsidianBg,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (selected) AmberPrimary else SteelBorder)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(muzzle.displayName.substringAfter(" "), style = MaterialTheme.typography.labelSmall, color = if (selected) AmberPrimary else MaterialTheme.colorScheme.onSurface)
                                        Text(muzzle.description.substringBefore(" •"), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    }
                                }
                            }
                        }

                        CamoPickerRow(
                            title = "PRIMARY CAMO FINISH",
                            tagPrefix = "primary_camo",
                            activeCamo = activePrimaryCamo,
                            onSelect = { onSetPrimaryCamo(activeLoadout.slotIndex, it) }
                        )
                    }
                }
            }

            // Secondary Sidearms
            item {
                Text("SECONDARY SIDEARM", style = MaterialTheme.typography.labelLarge, color = AmberPrimary)
            }

            WeaponType.secondaries.forEach { weapon ->
                item(key = "s_${weapon.name}") {
                    WeaponSelectionCard(
                        weapon = weapon,
                        selected = weapon == secondary,
                        accent = AmberPrimary,
                        onClick = { onSetSecondaryWeapon(activeLoadout.slotIndex, weapon) },
                        testTag = "secondary_${weapon.name.lowercase()}"
                    )
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GunmetalSurface),
                    border = BorderStroke(1.dp, SteelBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        CamoPickerRow(
                            title = "SIDEARM CAMO FINISH",
                            tagPrefix = "secondary_camo",
                            activeCamo = activeSecondaryCamo,
                            onSelect = { onSetSecondaryCamo(activeLoadout.slotIndex, it) }
                        )
                    }
                }
            }

            // Perks
            item {
                Text("COMBAT PERK", style = MaterialTheme.typography.labelLarge, color = CyanAllied)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TacticalPerk.entries.forEach { perk ->
                        val selected = perk == activePerk
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSetPerk(activeLoadout.slotIndex, perk) }
                                .testTag("perk_${perk.name.lowercase()}"),
                            color = if (selected) TacticalSlate else GunmetalSurface,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (selected) CyanAllied else SteelBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(perk.displayName, style = MaterialTheme.typography.titleMedium)
                                    Text(perk.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                }
                                if (selected) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = "Equipped perk", tint = CyanAllied)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeaponSelectionCard(
    weapon: WeaponType,
    selected: Boolean,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) TacticalSlate else GunmetalSurface
        ),
        border = BorderStroke(1.5.dp, if (selected) accent else SteelBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(weapon.displayName, style = MaterialTheme.typography.titleMedium)
                    Text("${weapon.caliber} • ${weapon.role}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
                if (selected) {
                    Surface(color = AmberMuted, shape = RoundedCornerShape(6.dp)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.CheckCircle, null, tint = accent, modifier = Modifier.height(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("EQUIPPED", style = MaterialTheme.typography.labelSmall, color = accent)
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            WeaponStatBar("DAMAGE", (weapon.damage * weapon.pellets).coerceAtMost(132) / 132f, accent)
            WeaponStatBar("FIRE RATE", (weapon.fireRateRpm.toFloat() / 1000f).coerceIn(0.08f, 1f), accent)
            WeaponStatBar("RANGE", (weapon.rangeMeters / 150f).coerceIn(0.15f, 1f), accent)
            WeaponStatBar("MOBILITY", ((weapon.moveSpeedMult - 0.7f) / 0.5f).coerceIn(0.2f, 1f), accent)
        }
    }
}

@Composable
private fun CamoPickerRow(
    title: String,
    tagPrefix: String,
    activeCamo: WeaponCamo,
    onSelect: (WeaponCamo) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            WeaponCamo.entries.forEach { camo ->
                val selected = camo == activeCamo
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clickable { onSelect(camo) }
                        .testTag("${tagPrefix}_${camo.name.lowercase()}"),
                    color = if (selected) TacticalSlate else ObsidianBg,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (selected) AmberPrimary else SteelBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(camo.swatchColor)
                                .border(1.dp, androidx.compose.ui.graphics.Color.White, CircleShape)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(camo.displayName.substringAfter(" "), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeaponStatBar(label: String, fraction: Float, accent: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier.width(82.dp)
        )
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = accent,
            trackColor = ObsidianBg
        )
    }
}
