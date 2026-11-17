package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.MuzzleAttachment
import com.example.model.OpticAttachment
import com.example.model.TacticalPerk
import com.example.model.WeaponCamo
import com.example.model.WeaponType

@Entity(tableName = "player_profile")
data class PlayerProfileEntity(
    @PrimaryKey val id: Int = 1,
    val callsign: String = "VANGUARD-01",
    val mmr: Int = 1150,
    val totalKills: Int = 0,
    val totalDeaths: Int = 0,
    val totalHeadshots: Int = 0,
    val totalShotsFired: Int = 0,
    val totalShotsHit: Int = 0,
    val matchesPlayed: Int = 0,
    val matchesWon: Int = 0,
    val equippedWeapon: String = WeaponType.M4A1.name,
    val equippedPerk: String = TacticalPerk.KEVLAR_PLATING.name
)

@Entity(tableName = "weapon_loadouts")
data class WeaponLoadoutEntity(
    @PrimaryKey val slotIndex: Int, // 1, 2, 3
    val slotName: String,
    val primaryWeapon: String = WeaponType.M4A1.name,
    val secondaryWeapon: String = WeaponType.P226.name,
    val primaryOptic: String = OpticAttachment.HOLO_CYAN.name,
    val primaryMuzzle: String = MuzzleAttachment.STANDARD.name,
    val primaryCamo: String = WeaponCamo.OBSIDIAN_STEEL.name,
    val secondaryCamo: String = WeaponCamo.OBSIDIAN_STEEL.name,
    val perk: String = TacticalPerk.KEVLAR_PLATING.name,
    val isActive: Boolean = false
)

@Entity(tableName = "match_history")
data class MatchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameMode: String,
    val mapName: String = "District 09: Metro Crossfire",
    val isVictory: Boolean,
    val kills: Int,
    val deaths: Int,
    val headshots: Int,
    val accuracyPercent: Int,
    val alliedScore: Int,
    val enemyScore: Int,
    val mmrDelta: Int,
    val weaponUsed: String,
    val playedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "leaderboard_entries")
data class LeaderboardEntryEntity(
    @PrimaryKey val callsign: String,
    val mmr: Int,
    val kills: Int,
    val deaths: Int,
    val wins: Int,
    val isLocalPlayer: Boolean = false
)
