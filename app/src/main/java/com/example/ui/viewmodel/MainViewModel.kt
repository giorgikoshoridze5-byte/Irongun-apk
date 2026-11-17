package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.IronfrontApp
import com.example.data.LeaderboardEntryEntity
import com.example.data.MatchHistoryEntity
import com.example.data.PlayerProfileEntity
import com.example.data.WeaponLoadoutEntity
import com.example.engine.GameSimulation
import com.example.model.ArenaMapId
import com.example.model.GameHudState
import com.example.model.GameMode
import com.example.model.MuzzleAttachment
import com.example.model.OpticAttachment
import com.example.model.PlayerSettings
import com.example.model.TacticalPerk
import com.example.model.WeaponCamo
import com.example.model.WeaponType
import com.example.network.LanLobbyState
import com.example.network.LanMultiplayerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as IronfrontApp
    private val repository = app.repository
    val audioEngine = app.audioEngine
    val lanManager = LanMultiplayerManager(viewModelScope)

    val profile: StateFlow<PlayerProfileEntity> = repository.profileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), PlayerProfileEntity())

    val loadouts: StateFlow<List<WeaponLoadoutEntity>> = repository.loadoutsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val activeLoadout: StateFlow<WeaponLoadoutEntity> = repository.activeLoadoutFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), WeaponLoadoutEntity(1, "ASSAULT"))

    val matchHistory: StateFlow<List<MatchHistoryEntity>> = repository.matchHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val leaderboard: StateFlow<List<LeaderboardEntryEntity>> = repository.leaderboardFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val settings: StateFlow<PlayerSettings> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), PlayerSettings())

    val lanLobbyState: StateFlow<LanLobbyState> = lanManager.lobbyState

    private val _selectedMode = MutableStateFlow(GameMode.TEAM_DEATHMATCH)
    val selectedMode: StateFlow<GameMode> = _selectedMode.asStateFlow()

    private val _selectedMap = MutableStateFlow(ArenaMapId.URBAN_DISTRICT)
    val selectedMap: StateFlow<ArenaMapId> = _selectedMap.asStateFlow()

    private val _hudState = MutableStateFlow(GameHudState())
    val hudState: StateFlow<GameHudState> = _hudState.asStateFlow()

    private val _activeSimulation = MutableStateFlow<GameSimulation?>(null)
    val activeSimulation: StateFlow<GameSimulation?> = _activeSimulation.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureInitialized()
        }
        viewModelScope.launch {
            settings.collect { s ->
                audioEngine.isEnabled = s.audioEnabled
                _activeSimulation.value?.settings = s
            }
        }
    }

    fun selectMode(mode: GameMode) {
        _selectedMode.value = mode
    }

    fun selectMap(mapId: ArenaMapId) {
        _selectedMap.value = mapId
        lanManager.updateHostedMap(mapId)
    }

    fun updateCallsign(callsign: String) {
        viewModelScope.launch { repository.updateCallsign(callsign) }
    }

    fun selectLoadoutSlot(slotIndex: Int) {
        viewModelScope.launch { repository.selectActiveLoadoutSlot(slotIndex) }
    }

    fun renameLoadoutSlot(slotIndex: Int, name: String) {
        val cleaned = name.trim().uppercase().take(12).ifEmpty { "LOADOUT $slotIndex" }
        viewModelScope.launch {
            repository.updateLoadout(slotIndex) { it.copy(slotName = cleaned) }
        }
    }

    fun setPrimaryWeapon(slotIndex: Int, weapon: WeaponType) {
        viewModelScope.launch {
            repository.updateLoadout(slotIndex) { it.copy(primaryWeapon = weapon.name) }
        }
    }

    fun setSecondaryWeapon(slotIndex: Int, weapon: WeaponType) {
        viewModelScope.launch {
            repository.updateLoadout(slotIndex) { it.copy(secondaryWeapon = weapon.name) }
        }
    }

    fun setPrimaryOptic(slotIndex: Int, optic: OpticAttachment) {
        viewModelScope.launch {
            repository.updateLoadout(slotIndex) { it.copy(primaryOptic = optic.name) }
        }
    }

    fun setPrimaryMuzzle(slotIndex: Int, muzzle: MuzzleAttachment) {
        viewModelScope.launch {
            repository.updateLoadout(slotIndex) { it.copy(primaryMuzzle = muzzle.name) }
        }
    }

    fun setPrimaryCamo(slotIndex: Int, camo: WeaponCamo) {
        viewModelScope.launch {
            repository.updateLoadout(slotIndex) { it.copy(primaryCamo = camo.name) }
        }
    }

    fun setSecondaryCamo(slotIndex: Int, camo: WeaponCamo) {
        viewModelScope.launch {
            repository.updateLoadout(slotIndex) { it.copy(secondaryCamo = camo.name) }
        }
    }

    fun setLoadoutPerk(slotIndex: Int, perk: TacticalPerk) {
        viewModelScope.launch {
            repository.updateLoadout(slotIndex) { it.copy(perk = perk.name) }
        }
    }

    fun updateSettings(transform: (PlayerSettings) -> PlayerSettings) {
        viewModelScope.launch { repository.updateSettings(transform) }
    }

    fun startHostLanMatch() {
        lanManager.startHost(_selectedMap.value, profile.value.callsign)
    }

    fun joinLanMatch(ip: String) {
        lanManager.joinHost(ip, profile.value.callsign)
    }

    fun stopLanSession() {
        lanManager.stopSession()
    }

    fun launchMatch(mode: GameMode = _selectedMode.value, mapId: ArenaMapId = _selectedMap.value) {
        val prof = profile.value
        val loadout = activeLoadout.value
        val primary = WeaponType.primaries.firstOrNull { it.name == loadout.primaryWeapon } ?: WeaponType.M4A1
        val secondary = WeaponType.secondaries.firstOrNull { it.name == loadout.secondaryWeapon } ?: WeaponType.P226
        val optic = OpticAttachment.entries.firstOrNull { it.name == loadout.primaryOptic } ?: OpticAttachment.HOLO_CYAN
        val muzzle = MuzzleAttachment.entries.firstOrNull { it.name == loadout.primaryMuzzle } ?: MuzzleAttachment.STANDARD
        val pCamo = WeaponCamo.entries.firstOrNull { it.name == loadout.primaryCamo } ?: WeaponCamo.OBSIDIAN_STEEL
        val sCamo = WeaponCamo.entries.firstOrNull { it.name == loadout.secondaryCamo } ?: WeaponCamo.OBSIDIAN_STEEL
        val perk = TacticalPerk.entries.firstOrNull { it.name == loadout.perk } ?: TacticalPerk.KEVLAR_PLATING

        val sim = GameSimulation(
            mapId = mapId,
            gameMode = mode,
            playerCallsign = prof.callsign,
            primaryWeapon = primary,
            secondaryWeapon = secondary,
            primaryOptic = optic,
            primaryMuzzle = muzzle,
            primaryCamo = pCamo,
            secondaryCamo = sCamo,
            perk = perk,
            audioEngine = audioEngine,
            lanManager = if (mode == GameMode.LAN_ARENA) lanManager else null,
            onMatchFinished = { finalHud ->
                viewModelScope.launch {
                    val delta = repository.recordMatchResult(
                        mode = mode,
                        mapId = mapId,
                        isVictory = finalHud.isVictory,
                        kills = finalHud.matchKills,
                        deaths = finalHud.matchDeaths,
                        headshots = finalHud.matchHeadshots,
                        shotsFired = finalHud.matchShotsFired,
                        shotsHit = finalHud.matchShotsHit,
                        alliedScore = finalHud.alliedScore,
                        enemyScore = finalHud.enemyScore,
                        weapon = primary
                    )
                    _hudState.value = finalHud.copy(lastMmrDelta = delta)
                }
            }
        )
        sim.settings = settings.value
        _activeSimulation.value = sim
        _hudState.value = sim.buildHudState()
    }

    fun updateHudFromRenderer(state: GameHudState) {
        if (!_hudState.value.isMatchOver) {
            _hudState.value = state
        }
    }

    fun togglePause() {
        val sim = _activeSimulation.value ?: return
        sim.isPaused = !sim.isPaused
        _hudState.value = sim.buildHudState()
    }

    fun endActiveMatch() {
        _activeSimulation.value = null
    }

    override fun onCleared() {
        super.onCleared()
        lanManager.stopSession()
    }
}
