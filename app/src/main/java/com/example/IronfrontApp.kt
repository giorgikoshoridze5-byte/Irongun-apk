package com.example

import android.app.Application
import com.example.audio.TacticalAudioEngine
import com.example.data.GameRepository
import com.example.data.IronfrontDatabase
import com.example.data.SettingsStore

class IronfrontApp : Application() {
    lateinit var repository: GameRepository
        private set

    lateinit var audioEngine: TacticalAudioEngine
        private set

    override fun onCreate() {
        super.onCreate()
        val db = IronfrontDatabase.getInstance(this)
        val settingsStore = SettingsStore(this)
        repository = GameRepository(db.dao(), settingsStore)
        audioEngine = TacticalAudioEngine()
    }
}
