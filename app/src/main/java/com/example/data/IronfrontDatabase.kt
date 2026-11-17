package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PlayerProfileEntity::class,
        WeaponLoadoutEntity::class,
        MatchHistoryEntity::class,
        LeaderboardEntryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class IronfrontDatabase : RoomDatabase() {
    abstract fun dao(): IronfrontDao

    companion object {
        @Volatile
        private var INSTANCE: IronfrontDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS weapon_loadouts (
                        slotIndex INTEGER NOT NULL PRIMARY KEY,
                        slotName TEXT NOT NULL,
                        primaryWeapon TEXT NOT NULL,
                        secondaryWeapon TEXT NOT NULL,
                        primaryOptic TEXT NOT NULL,
                        primaryMuzzle TEXT NOT NULL,
                        primaryCamo TEXT NOT NULL,
                        secondaryCamo TEXT NOT NULL,
                        perk TEXT NOT NULL,
                        isActive INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): IronfrontDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    IronfrontDatabase::class.java,
                    "ironfront_warfare.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
