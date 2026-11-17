package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.PlayerSettings
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.GunmetalSurface

@Composable
fun SettingsDialog(
    settings: PlayerSettings,
    onUpdateSettings: ((PlayerSettings) -> PlayerSettings) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GunmetalSurface,
        title = {
            Text("COMBAT SETTINGS", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Aim Sensitivity: ${String.format("%.2fx", settings.lookSensitivity)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = AmberPrimary
                )
                Slider(
                    value = settings.lookSensitivity,
                    onValueChange = { v -> onUpdateSettings { it.copy(lookSensitivity = v) } },
                    valueRange = 0.4f..2.2f
                )

                SettingToggleRow(
                    title = "High-Quality Shaders & Fog",
                    checked = settings.highQualityGraphics,
                    onCheckedChange = { c -> onUpdateSettings { it.copy(highQualityGraphics = c) } }
                )
                SettingToggleRow(
                    title = "Tactical Aim Assist",
                    checked = settings.aimAssist,
                    onCheckedChange = { c -> onUpdateSettings { it.copy(aimAssist = c) } }
                )
                SettingToggleRow(
                    title = "Invert Look Y-Axis",
                    checked = settings.invertY,
                    onCheckedChange = { c -> onUpdateSettings { it.copy(invertY = c) } }
                )
                SettingToggleRow(
                    title = "Synthesized Weapon Audio",
                    checked = settings.audioEnabled,
                    onCheckedChange = { c -> onUpdateSettings { it.copy(audioEnabled = c) } }
                )
                SettingToggleRow(
                    title = "Combat Haptics",
                    checked = settings.hapticsEnabled,
                    onCheckedChange = { c -> onUpdateSettings { it.copy(hapticsEnabled = c) } }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("DONE", color = AmberPrimary)
            }
        }
    )
}

@Composable
private fun SettingToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Spacer(Modifier.height(4.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
