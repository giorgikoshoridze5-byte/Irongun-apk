package com.example.model

import androidx.compose.ui.graphics.Color

enum class ArenaMapId(
    val displayName: String,
    val codeName: String,
    val themeTag: String,
    val description: String,
    val engagementProfile: String,
    val skyTopRgb: Triple<Float, Float, Float>,
    val skyHorizonRgb: Triple<Float, Float, Float>,
    val fogRgb: Triple<Float, Float, Float>,
    val sunDir: Triple<Float, Float, Float>,
    val sunColor: Triple<Float, Float, Float>,
    val groundPrimaryRgb: Triple<Float, Float, Float>,
    val groundSecondaryRgb: Triple<Float, Float, Float>
) {
    URBAN_DISTRICT(
        displayName = "District 09: Metro Crossfire",
        codeName = "URBAN COMBAT",
        themeTag = "Nightfall City Grid • Tight Alleys & Avenue",
        description = "Dense urban intersection with mid-rise concrete blocks, central plaza sentry turret, breakable street barricades, and explosive fuel drums.",
        engagementProfile = "Close-to-Mid Range • Vertical Flanking",
        skyTopRgb = Triple(0.03f, 0.05f, 0.11f),
        skyHorizonRgb = Triple(0.12f, 0.19f, 0.30f),
        fogRgb = Triple(0.09f, 0.14f, 0.22f),
        sunDir = Triple(0.45f, 0.78f, 0.42f),
        sunColor = Triple(0.95f, 0.90f, 0.85f),
        groundPrimaryRgb = Triple(0.14f, 0.16f, 0.19f),
        groundSecondaryRgb = Triple(0.19f, 0.21f, 0.25f)
    ),
    DESERT_OUTPOST(
        displayName = "Kandahar Ridge: Solar Citadel",
        codeName = "DESERT OUTPOST",
        themeTag = "Arid Canyon • Long Sightlines & Bunkers",
        description = "Sun-scorched sandstone outpost with fortified bunkers, sniper ridge, destructible crate barriers, munitions drums, and a hackable bunker turret.",
        engagementProfile = "Mid-to-Long Range • Bunker Sieges",
        skyTopRgb = Triple(0.15f, 0.31f, 0.52f),
        skyHorizonRgb = Triple(0.84f, 0.62f, 0.38f),
        fogRgb = Triple(0.62f, 0.48f, 0.34f),
        sunDir = Triple(0.60f, 0.72f, -0.32f),
        sunColor = Triple(1.05f, 0.94f, 0.78f),
        groundPrimaryRgb = Triple(0.56f, 0.44f, 0.29f),
        groundSecondaryRgb = Triple(0.46f, 0.35f, 0.22f)
    )
}

enum class WeaponSlot {
    PRIMARY,
    SECONDARY
}

enum class WeaponType(
    val slot: WeaponSlot,
    val displayName: String,
    val caliber: String,
    val role: String,
    val damage: Int,
    val fireRateRpm: Int,
    val magSize: Int,
    val reloadSeconds: Float,
    val baseSpreadDeg: Float,
    val adsSpreadDeg: Float,
    val rangeMeters: Float,
    val adsFovDeg: Float,
    val pellets: Int,
    val recoilKickDeg: Float,
    val isAutomatic: Boolean,
    val moveSpeedMult: Float
) {
    // Primary Firearms
    M4A1(
        slot = WeaponSlot.PRIMARY,
        displayName = "M4A1 Tactical",
        caliber = "5.56x45mm NATO",
        role = "Assault Rifle • Balanced All-Range",
        damage = 28,
        fireRateRpm = 680,
        magSize = 30,
        reloadSeconds = 1.65f,
        baseSpreadDeg = 2.0f,
        adsSpreadDeg = 0.45f,
        rangeMeters = 90f,
        adsFovDeg = 48f,
        pellets = 1,
        recoilKickDeg = 1.25f,
        isAutomatic = true,
        moveSpeedMult = 1.0f
    ),
    MP9(
        slot = WeaponSlot.PRIMARY,
        displayName = "MP9 Vector",
        caliber = "9x19mm Parabellum",
        role = "SMG • Ultra-High Fire Rate",
        damage = 20,
        fireRateRpm = 960,
        magSize = 36,
        reloadSeconds = 1.35f,
        baseSpreadDeg = 2.8f,
        adsSpreadDeg = 0.85f,
        rangeMeters = 58f,
        adsFovDeg = 54f,
        pellets = 1,
        recoilKickDeg = 0.9f,
        isAutomatic = true,
        moveSpeedMult = 1.12f
    ),
    AX50(
        slot = WeaponSlot.PRIMARY,
        displayName = "AX-50 Marksman",
        caliber = ".50 BMG",
        role = "Sniper • High-Precision Lethality",
        damage = 88,
        fireRateRpm = 56,
        magSize = 5,
        reloadSeconds = 2.3f,
        baseSpreadDeg = 4.2f,
        adsSpreadDeg = 0.08f,
        rangeMeters = 150f,
        adsFovDeg = 24f,
        pellets = 1,
        recoilKickDeg = 4.6f,
        isAutomatic = false,
        moveSpeedMult = 0.90f
    ),
    M870(
        slot = WeaponSlot.PRIMARY,
        displayName = "M870 Breacher",
        caliber = "12-Gauge Buckshot",
        role = "Combat Shotgun • CQB Devastator",
        damage = 22,
        fireRateRpm = 88,
        magSize = 8,
        reloadSeconds = 1.95f,
        baseSpreadDeg = 6.2f,
        adsSpreadDeg = 3.8f,
        rangeMeters = 32f,
        adsFovDeg = 58f,
        pellets = 6,
        recoilKickDeg = 3.8f,
        isAutomatic = false,
        moveSpeedMult = 1.05f
    ),

    // Secondary Sidearms
    P226(
        slot = WeaponSlot.SECONDARY,
        displayName = "P226 Legion",
        caliber = "9x19mm Sidearm",
        role = "Tactical Pistol • Fast Swap & Precision",
        damage = 26,
        fireRateRpm = 420,
        magSize = 15,
        reloadSeconds = 1.10f,
        baseSpreadDeg = 1.5f,
        adsSpreadDeg = 0.35f,
        rangeMeters = 55f,
        adsFovDeg = 56f,
        pellets = 1,
        recoilKickDeg = 0.95f,
        isAutomatic = false,
        moveSpeedMult = 1.16f
    ),
    DEAGLE(
        slot = WeaponSlot.SECONDARY,
        displayName = "Desert Hawk .50",
        caliber = ".50 Action Express",
        role = "Heavy Magnum • High Stopping Power",
        damage = 54,
        fireRateRpm = 170,
        magSize = 7,
        reloadSeconds = 1.45f,
        baseSpreadDeg = 2.1f,
        adsSpreadDeg = 0.28f,
        rangeMeters = 75f,
        adsFovDeg = 50f,
        pellets = 1,
        recoilKickDeg = 3.2f,
        isAutomatic = false,
        moveSpeedMult = 1.10f
    ),
    G18(
        slot = WeaponSlot.SECONDARY,
        displayName = "G18C Auto-Pistol",
        caliber = "9x19mm Machine Pistol",
        role = "Full-Auto Sidearm • CQB Backup",
        damage = 17,
        fireRateRpm = 900,
        magSize = 20,
        reloadSeconds = 1.25f,
        baseSpreadDeg = 3.3f,
        adsSpreadDeg = 1.25f,
        rangeMeters = 42f,
        adsFovDeg = 58f,
        pellets = 1,
        recoilKickDeg = 1.1f,
        isAutomatic = true,
        moveSpeedMult = 1.15f
    );

    companion object {
        val primaries: List<WeaponType> get() = entries.filter { it.slot == WeaponSlot.PRIMARY }
        val secondaries: List<WeaponType> get() = entries.filter { it.slot == WeaponSlot.SECONDARY }
    }
}

enum class OpticAttachment(
    val displayName: String,
    val description: String,
    val spreadMult: Float,
    val adsFovDelta: Float
) {
    IRON_SIGHTS("Iron Sights", "Fastest ADS speed • Clean sight picture", 1.0f, 0f),
    HOLO_CYAN("Cyan Holographic", "-25% ADS spread • Illuminated reticle", 0.75f, -4f),
    MARKSMAN_OPTIC("2x Tactical Optic", "-40% ADS spread • Enhanced zoom", 0.60f, -10f)
}

enum class MuzzleAttachment(
    val displayName: String,
    val description: String,
    val recoilMult: Float,
    val rangeMult: Float
) {
    STANDARD("Standard Flash Hider", "Balanced factory muzzle", 1.0f, 1.0f),
    SUPPRESSOR("Tactical Suppressor", "+15% effective range • -15% recoil", 0.85f, 1.15f),
    COMPENSATOR("Heavy Compensator", "-30% vertical recoil kick", 0.70f, 1.0f)
}

enum class WeaponCamo(
    val displayName: String,
    val rgb: Triple<Float, Float, Float>,
    val accentRgb: Triple<Float, Float, Float>,
    val swatchColor: Color
) {
    OBSIDIAN_STEEL("Obsidian Steel", Triple(0.18f, 0.20f, 0.24f), Triple(0.08f, 0.75f, 0.90f), Color(0xFF334155)),
    DESERT_TAN("Kandahar Coyote", Triple(0.56f, 0.46f, 0.31f), Triple(0.24f, 0.22f, 0.18f), Color(0xFFA68B5B)),
    ARCTIC_WHITE("Arctic Spec-Ops", Triple(0.76f, 0.80f, 0.84f), Triple(0.16f, 0.20f, 0.26f), Color(0xFFCBD5E1)),
    GOLD_ELITE("Vanguard Gold", Triple(0.84f, 0.64f, 0.18f), Triple(0.14f, 0.14f, 0.16f), Color(0xFFF59E0B))
}

enum class TacticalPerk(
    val displayName: String,
    val description: String
) {
    KEVLAR_PLATING("Kevlar Plating", "+35 Max Tactical Armor on deploy"),
    SLEIGHT_OF_HAND("Sleight of Hand", "30% faster weapon reload speed"),
    LIGHTWEIGHT("Lightweight Rig", "+14% sprint & strafe mobility"),
    DEAD_EYE("Dead-Eye Optics", "35% tighter ADS & hipfire spread")
}

enum class GameMode(
    val displayName: String,
    val subtitle: String,
    val targetScore: Int,
    val durationSec: Int
) {
    TEAM_DEATHMATCH(
        displayName = "Team Deathmatch",
        subtitle = "4v4 Tactical Skirmish • First squad to 20 eliminations",
        targetScore = 20,
        durationSec = 180
    ),
    ZONE_DOMINATION(
        displayName = "Zone Domination",
        subtitle = "Capture & hold Sectors A, B, C to reach 100 points",
        targetScore = 100,
        durationSec = 210
    ),
    WAVE_SURVIVAL(
        displayName = "Wave Survival",
        subtitle = "Hold your outpost against escalating hostile waves",
        targetScore = 5,
        durationSec = 300
    ),
    LAN_ARENA(
        displayName = "Wi-Fi LAN Arena",
        subtitle = "Real-time UDP PvP duel on your local network",
        targetScore = 10,
        durationSec = 180
    )
}

enum class RankTier(
    val title: String,
    val minMmr: Int,
    val maxMmr: Int,
    val color: Color
) {
    BRONZE("Bronze Striker", 0, 1099, Color(0xFFCD7F32)),
    SILVER("Silver Recon", 1100, 1299, Color(0xFFCBD5E1)),
    GOLD("Gold Sentinel", 1300, 1549, Color(0xFFF59E0B)),
    PLATINUM("Platinum Spec-Ops", 1550, 1849, Color(0xFF06B6D4)),
    DIAMOND("Diamond Ghost", 1850, 2199, Color(0xFF818CF8)),
    VANGUARD("Vanguard Elite", 2200, 9999, Color(0xFFEF4444));

    companion object {
        fun fromMmr(mmr: Int): RankTier {
            return entries.lastOrNull { mmr >= it.minMmr } ?: BRONZE
        }
    }
}

data class PlayerSettings(
    val lookSensitivity: Float = 1.0f,
    val invertY: Boolean = false,
    val aimAssist: Boolean = true,
    val highQualityGraphics: Boolean = true,
    val audioEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true
)

enum class HitMarkerType {
    NONE,
    BODY,
    HEADSHOT,
    KILL
}

data class KillFeedItem(
    val id: Long,
    val killer: String,
    val victim: String,
    val weapon: String,
    val isHeadshot: Boolean,
    val killerIsAllied: Boolean,
    val timestampMs: Long
)

data class MinimapBlip(
    val x: Float,
    val z: Float,
    val isAllied: Boolean,
    val isObjective: Boolean = false,
    val isInteractive: Boolean = false,
    val label: String = "",
    val ownerTeam: Int = 0
)

data class ZoneHudState(
    val label: String,
    val ownerTeam: Int,
    val captureProgress: Float
)

data class GameHudState(
    val gameMode: GameMode = GameMode.TEAM_DEATHMATCH,
    val mapId: ArenaMapId = ArenaMapId.URBAN_DISTRICT,
    val hp: Int = 100,
    val maxHp: Int = 100,
    val armor: Int = 50,
    val maxArmor: Int = 50,
    val ammo: Int = 30,
    val maxAmmo: Int = 30,
    val primaryAmmo: Int = 30,
    val secondaryAmmo: Int = 15,
    val activeSlot: WeaponSlot = WeaponSlot.PRIMARY,
    val primaryWeapon: WeaponType = WeaponType.M4A1,
    val secondaryWeapon: WeaponType = WeaponType.P226,
    val isReloading: Boolean = false,
    val reloadProgress: Float = 0f,
    val isAds: Boolean = false,
    val isCrouching: Boolean = false,
    val isSprinting: Boolean = false,
    val activeWeapon: WeaponType = WeaponType.M4A1,
    val alliedScore: Int = 0,
    val enemyScore: Int = 0,
    val targetScore: Int = 20,
    val timeRemainingSec: Int = 180,
    val waveNumber: Int = 1,
    val zones: List<ZoneHudState> = emptyList(),
    val killFeed: List<KillFeedItem> = emptyList(),
    val hitMarkerType: HitMarkerType = HitMarkerType.NONE,
    val hitMarkerId: Long = 0L,
    val killBannerText: String? = null,
    val killBannerId: Long = 0L,
    val interactPrompt: String? = null,
    val turretOwnerTeam: Int = 0,
    val damageFlashAlpha: Float = 0f,
    val crosshairSpreadPx: Float = 14f,
    val minimapBlips: List<MinimapBlip> = emptyList(),
    val playerX: Float = 0f,
    val playerZ: Float = 0f,
    val playerYawDeg: Float = 0f,
    val fps: Int = 60,
    val pingMs: Int = 0,
    val isMatchOver: Boolean = false,
    val isVictory: Boolean = false,
    val isPaused: Boolean = false,
    val isRespawning: Boolean = false,
    val respawnSecondsLeft: Int = 0,
    val matchKills: Int = 0,
    val matchDeaths: Int = 0,
    val matchHeadshots: Int = 0,
    val matchShotsFired: Int = 0,
    val matchShotsHit: Int = 0,
    val lastMmrDelta: Int = 0,
    val isLanMatch: Boolean = false,
    val lanConnected: Boolean = false
)
