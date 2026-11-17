package com.example.data

import com.example.model.ArenaMapId
import com.example.model.GameMode
import com.example.model.MuzzleAttachment
import com.example.model.OpticAttachment
import com.example.model.PlayerSettings
import com.example.model.TacticalPerk
import com.example.model.WeaponCamo
import com.example.model.WeaponType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.max
import kotlin.math.roundToInt

class GameRepository(
    private val dao: IronfrontDao,
    private val settingsStore: SettingsStore
) {
    val profileFlow: Flow<PlayerProfileEntity> = dao.observeProfile().map { it ?: PlayerProfileEntity() }
    val loadoutsFlow: Flow<List<WeaponLoadoutEntity>> = dao.observeLoadouts()
    val activeLoadoutFlow: Flow<WeaponLoadoutEntity> = dao.observeActiveLoadout().map {
        it ?: defaultLoadouts().first()
    }
    val matchHistoryFlow: Flow<List<MatchHistoryEntity>> = dao.observeMatchHistory()
    val leaderboardFlow: Flow<List<LeaderboardEntryEntity>> = dao.observeLeaderboard()
    val settingsFlow: Flow<PlayerSettings> = settingsStore.settingsFlow

    private fun defaultLoadouts(): List<WeaponLoadoutEntity> = listOf(
        WeaponLoadoutEntity(
            slotIndex = 1,
            slotName = "ASSAULT",
            primaryWeapon = WeaponType.M4A1.name,
            secondaryWeapon = WeaponType.P226.name,
            primaryOptic = OpticAttachment.HOLO_CYAN.name,
            primaryMuzzle = MuzzleAttachment.COMPENSATOR.name,
            primaryCamo = WeaponCamo.OBSIDIAN_STEEL.name,
            secondaryCamo = WeaponCamo.OBSIDIAN_STEEL.name,
            perk = TacticalPerk.KEVLAR_PLATING.name,
            isActive = true
        ),
        WeaponLoadoutEntity(
            slotIndex = 2,
            slotName = "RECON",
            primaryWeapon = WeaponType.AX50.name,
            secondaryWeapon = WeaponType.G18.name,
            primaryOptic = OpticAttachment.MARKSMAN_OPTIC.name,
            primaryMuzzle = MuzzleAttachment.SUPPRESSOR.name,
            primaryCamo = WeaponCamo.DESERT_TAN.name,
            secondaryCamo = WeaponCamo.DESERT_TAN.name,
            perk = TacticalPerk.DEAD_EYE.name,
            isActive = false
        ),
        WeaponLoadoutEntity(
            slotIndex = 3,
            slotName = "BREACHER",
            primaryWeapon = WeaponType.M870.name,
            secondaryWeapon = WeaponType.DEAGLE.name,
            primaryOptic = OpticAttachment.IRON_SIGHTS.name,
            primaryMuzzle = MuzzleAttachment.COMPENSATOR.name,
            primaryCamo = WeaponCamo.GOLD_ELITE.name,
            secondaryCamo = WeaponCamo.GOLD_ELITE.name,
            perk = TacticalPerk.LIGHTWEIGHT.name,
            isActive = false
        )
    )

    suspend fun ensureInitialized() {
        val existing = dao.getProfileOnce()
        val profile = existing ?: PlayerProfileEntity().also { dao.upsertProfile(it) }

        if (dao.getAllLoadoutsOnce().isEmpty()) {
            dao.insertDefaultLoadouts(defaultLoadouts())
        }

        val rivalSeeds = listOf(
            LeaderboardEntryEntity("SPECTRE-7", 2240, 412, 189, 34, false),
            LeaderboardEntryEntity("VALKYRIE-X", 1910, 318, 174, 27, false),
            LeaderboardEntryEntity("KESTREL-9", 1640, 245, 160, 21, false),
            LeaderboardEntryEntity("IRONCLAD", 1410, 198, 152, 16, false),
            LeaderboardEntryEntity("ROOK-04", 1185, 124, 118, 10, false),
            LeaderboardEntryEntity("SENTINEL-2", 990, 74, 96, 5, false)
        )
        dao.insertLeaderboardSeeds(rivalSeeds)
        dao.upsertLeaderboardEntry(
            LeaderboardEntryEntity(
                callsign = profile.callsign,
                mmr = profile.mmr,
                kills = profile.totalKills,
                deaths = profile.totalDeaths,
                wins = profile.matchesWon,
                isLocalPlayer = true
            )
        )
    }

    suspend fun selectActiveLoadoutSlot(slotIndex: Int) {
        dao.setActiveLoadoutSlot(slotIndex)
        val loadout = dao.getLoadoutBySlot(slotIndex) ?: return
        val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
        dao.upsertProfile(
            profile.copy(
                equippedWeapon = loadout.primaryWeapon,
                equippedPerk = loadout.perk
            )
        )
    }

    suspend fun updateLoadout(slotIndex: Int, transform: (WeaponLoadoutEntity) -> WeaponLoadoutEntity) {
        val current = dao.getLoadoutBySlot(slotIndex)
            ?: defaultLoadouts().firstOrNull { it.slotIndex == slotIndex }
            ?: defaultLoadouts().first()
        val updated = transform(current)
        dao.upsertLoadout(updated)
        if (updated.isActive) {
            val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
            dao.upsertProfile(
                profile.copy(
                    equippedWeapon = updated.primaryWeapon,
                    equippedPerk = updated.perk
                )
            )
        }
    }

    suspend fun updateCallsign(rawCallsign: String) {
        val cleaned = rawCallsign.trim().uppercase().take(14).ifEmpty { "VANGUARD-01" }
        val current = dao.getProfileOnce() ?: PlayerProfileEntity()
        val updated = current.copy(callsign = cleaned)
        dao.upsertProfile(updated)
        dao.clearLocalPlayerLeaderboardEntries()
        dao.upsertLeaderboardEntry(
            LeaderboardEntryEntity(
                callsign = updated.callsign,
                mmr = updated.mmr,
                kills = updated.totalKills,
                deaths = updated.totalDeaths,
                wins = updated.matchesWon,
                isLocalPlayer = true
            )
        )
    }

    suspend fun updateSettings(transform: (PlayerSettings) -> PlayerSettings) {
        settingsStore.updateSettings(transform)
    }

    suspend fun recordMatchResult(
        mode: GameMode,
        mapId: ArenaMapId,
        isVictory: Boolean,
        kills: Int,
        deaths: Int,
        headshots: Int,
        shotsFired: Int,
        shotsHit: Int,
        alliedScore: Int,
        enemyScore: Int,
        weapon: WeaponType
    ): Int {
        val current = dao.getProfileOnce() ?: PlayerProfileEntity()
        val accuracy = if (shotsFired > 0) ((shotsHit.toFloat() / shotsFired) * 100f).roundToInt().coerceIn(0, 100) else 0

        val baseWinDelta = if (isVictory) 24 else -14
        val kdBonus = ((kills - deaths) * 2).coerceIn(-10, 16)
        val hsBonus = (headshots * 1).coerceAtMost(8)
        val delta = (baseWinDelta + kdBonus + hsBonus).coerceIn(-25, 45)
        val nextMmr = max(400, current.mmr + delta)

        val updatedProfile = current.copy(
            mmr = nextMmr,
            totalKills = current.totalKills + kills,
            totalDeaths = current.totalDeaths + deaths,
            totalHeadshots = current.totalHeadshots + headshots,
            totalShotsFired = current.totalShotsFired + shotsFired,
            totalShotsHit = current.totalShotsHit + shotsHit,
            matchesPlayed = current.matchesPlayed + 1,
            matchesWon = current.matchesWon + if (isVictory) 1 else 0
        )
        dao.upsertProfile(updatedProfile)
        dao.insertMatch(
            MatchHistoryEntity(
                gameMode = mode.displayName,
                mapName = mapId.displayName,
                isVictory = isVictory,
                kills = kills,
                deaths = deaths,
                headshots = headshots,
                accuracyPercent = accuracy,
                alliedScore = alliedScore,
                enemyScore = enemyScore,
                mmrDelta = delta,
                weaponUsed = weapon.displayName
            )
        )
        dao.clearLocalPlayerLeaderboardEntries()
        dao.upsertLeaderboardEntry(
            LeaderboardEntryEntity(
                callsign = updatedProfile.callsign,
                mmr = updatedProfile.mmr,
                kills = updatedProfile.totalKills,
                deaths = updatedProfile.totalDeaths,
                wins = updatedProfile.matchesWon,
                isLocalPlayer = true
            )
        )
        return delta
    }
}
