package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IronfrontDao {
    @Query("SELECT * FROM player_profile WHERE id = 1")
    fun observeProfile(): Flow<PlayerProfileEntity?>

    @Query("SELECT * FROM player_profile WHERE id = 1")
    suspend fun getProfileOnce(): PlayerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: PlayerProfileEntity)

    @Query("SELECT * FROM weapon_loadouts ORDER BY slotIndex ASC")
    fun observeLoadouts(): Flow<List<WeaponLoadoutEntity>>

    @Query("SELECT * FROM weapon_loadouts WHERE isActive = 1 LIMIT 1")
    fun observeActiveLoadout(): Flow<WeaponLoadoutEntity?>

    @Query("SELECT * FROM weapon_loadouts ORDER BY slotIndex ASC")
    suspend fun getAllLoadoutsOnce(): List<WeaponLoadoutEntity>

    @Query("SELECT * FROM weapon_loadouts WHERE slotIndex = :slotIndex LIMIT 1")
    suspend fun getLoadoutBySlot(slotIndex: Int): WeaponLoadoutEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLoadout(loadout: WeaponLoadoutEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaultLoadouts(loadouts: List<WeaponLoadoutEntity>)

    @Query("UPDATE weapon_loadouts SET isActive = (slotIndex = :slotIndex)")
    suspend fun setActiveLoadoutSlot(slotIndex: Int)

    @Query("SELECT * FROM match_history ORDER BY playedAtMs DESC LIMIT 25")
    fun observeMatchHistory(): Flow<List<MatchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchHistoryEntity)

    @Query("SELECT * FROM leaderboard_entries ORDER BY mmr DESC")
    fun observeLeaderboard(): Flow<List<LeaderboardEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLeaderboardEntry(entry: LeaderboardEntryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLeaderboardSeeds(entries: List<LeaderboardEntryEntity>)

    @Query("DELETE FROM leaderboard_entries WHERE isLocalPlayer = 1")
    suspend fun clearLocalPlayerLeaderboardEntries()
}
