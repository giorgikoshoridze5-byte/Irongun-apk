package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.PlayerSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "ironfront_settings")

class SettingsStore(private val context: Context) {
    private object Keys {
        val LOOK_SENSITIVITY = floatPreferencesKey("look_sensitivity")
        val INVERT_Y = booleanPreferencesKey("invert_y")
        val AIM_ASSIST = booleanPreferencesKey("aim_assist")
        val HIGH_QUALITY = booleanPreferencesKey("high_quality")
        val AUDIO_ENABLED = booleanPreferencesKey("audio_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
    }

    val settingsFlow: Flow<PlayerSettings> = context.dataStore.data.map { prefs ->
        PlayerSettings(
            lookSensitivity = prefs[Keys.LOOK_SENSITIVITY] ?: 1.0f,
            invertY = prefs[Keys.INVERT_Y] ?: false,
            aimAssist = prefs[Keys.AIM_ASSIST] ?: true,
            highQualityGraphics = prefs[Keys.HIGH_QUALITY] ?: true,
            audioEnabled = prefs[Keys.AUDIO_ENABLED] ?: true,
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true
        )
    }

    suspend fun updateSettings(transform: (PlayerSettings) -> PlayerSettings) {
        context.dataStore.edit { prefs ->
            val current = PlayerSettings(
                lookSensitivity = prefs[Keys.LOOK_SENSITIVITY] ?: 1.0f,
                invertY = prefs[Keys.INVERT_Y] ?: false,
                aimAssist = prefs[Keys.AIM_ASSIST] ?: true,
                highQualityGraphics = prefs[Keys.HIGH_QUALITY] ?: true,
                audioEnabled = prefs[Keys.AUDIO_ENABLED] ?: true,
                hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true
            )
            val next = transform(current)
            prefs[Keys.LOOK_SENSITIVITY] = next.lookSensitivity
            prefs[Keys.INVERT_Y] = next.invertY
            prefs[Keys.AIM_ASSIST] = next.aimAssist
            prefs[Keys.HIGH_QUALITY] = next.highQualityGraphics
            prefs[Keys.AUDIO_ENABLED] = next.audioEnabled
            prefs[Keys.HAPTICS_ENABLED] = next.hapticsEnabled
        }
    }
}
