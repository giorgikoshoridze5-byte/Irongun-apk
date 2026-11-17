package com.example.engine

import com.example.audio.TacticalAudioEngine
import com.example.model.ArenaMapId
import com.example.model.GameHudState
import com.example.model.GameMode
import com.example.model.HitMarkerType
import com.example.model.KillFeedItem
import com.example.model.MinimapBlip
import com.example.model.MuzzleAttachment
import com.example.model.OpticAttachment
import com.example.model.PlayerSettings
import com.example.model.TacticalPerk
import com.example.model.WeaponCamo
import com.example.model.WeaponSlot
import com.example.model.WeaponType
import com.example.model.ZoneHudState
import com.example.network.LanMultiplayerManager
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class SoldierUnit(
    val id: Int,
    val callsign: String,
    val isAllied: Boolean,
    val isHeavy: Boolean = false,
    var x: Float,
    var y: Float = 0f,
    var z: Float,
    var yawDeg: Float = 0f,
    var hp: Int = 100,
    val maxHp: Int = 100,
    var isAlive: Boolean = true,
    var respawnTimer: Float = 0f,
    var fireCooldown: Float = 0f,
    var walkPhase: Float = 0f,
    var hitFlashTimer: Float = 0f,
    var deathTilt: Float = 0f,
    var strafeDir: Float = 1f,
    var strafeTimer: Float = 1.5f
)

data class TracerBolt(
    var x0: Float, var y0: Float, var z0: Float,
    var x1: Float, var y1: Float, var z1: Float,
    var life: Float,
    val maxLife: Float,
    val r: Float, val g: Float, val b: Float
)

data class SparkParticle(
    var x: Float, var y: Float, var z: Float,
    var vx: Float, var vy: Float, var vz: Float,
    var life: Float,
    val maxLife: Float,
    var size: Float,
    val r: Float, val g: Float, val b: Float
)

class GameSimulation(
    val mapId: ArenaMapId,
    val gameMode: GameMode,
    val playerCallsign: String,
    val primaryWeapon: WeaponType,
    val secondaryWeapon: WeaponType,
    val primaryOptic: OpticAttachment,
    val primaryMuzzle: MuzzleAttachment,
    val primaryCamo: WeaponCamo,
    val secondaryCamo: WeaponCamo,
    val perk: TacticalPerk,
    private val audioEngine: TacticalAudioEngine,
    private val lanManager: LanMultiplayerManager?,
    private val onMatchFinished: (GameHudState) -> Unit
) {
    val blueprint: ArenaBlueprint = ArenaFactory.buildBlueprint(mapId)
    val barrels: List<ExplosiveBarrel> = blueprint.barrels
    val barriers: List<BreakableBarrier> = blueprint.barriers
    val turrets: List<ControllableTurret> = blueprint.turrets
    val supplyCrates: List<SupplyCrate> = blueprint.supplyCrates
    val zones: List<CaptureZone> = blueprint.zones

    // Player state
    var playerX = blueprint.alliedSpawns.first().first
    var playerY = 0f
    var playerZ = blueprint.alliedSpawns.first().second
    var playerVelY = 0f
    var playerYawDeg = 0f
    var playerPitchDeg = 0f
    var playerHp = 100
    val playerMaxHp = 100
    var playerArmor = if (perk == TacticalPerk.KEVLAR_PLATING) 85 else 50
    val playerMaxArmor = playerArmor
    var isPlayerAlive = true
    var playerRespawnTimer = 0f

    // Dual-weapon Loadout & Animation State
    var activeSlot = WeaponSlot.PRIMARY
    val weapon: WeaponType
        get() = if (activeSlot == WeaponSlot.PRIMARY) primaryWeapon else secondaryWeapon
    val activeCamo: WeaponCamo
        get() = if (activeSlot == WeaponSlot.PRIMARY) primaryCamo else secondaryCamo

    var primaryAmmo = primaryWeapon.magSize
    var secondaryAmmo = secondaryWeapon.magSize
    var ammo: Int
        get() = if (activeSlot == WeaponSlot.PRIMARY) primaryAmmo else secondaryAmmo
        set(value) {
            if (activeSlot == WeaponSlot.PRIMARY) primaryAmmo = value else secondaryAmmo = value
        }

    var isReloading = false
    var reloadTimer = 0f
    var fireCooldown = 0f
    var recoilOffset = 0f
    var weaponBobPhase = 0f
    var swapDip = 0f
    var isAds = false
    var adsBlend = 0f
    var isCrouching = false
    var isFiringInput = false
    var currentFovDeg = 68f
    var muzzleFlashTimer = 0f
    var flashPosX = 0f
    var flashPosY = 0f
    var flashPosZ = 0f
    var flashR = 1f
    var flashG = 0.7f
    var flashB = 0.25f
    var flashIntensity = 0f

    @Volatile var moveStickX = 0f
    @Volatile var moveStickY = 0f
    @Volatile var isPaused = false
    @Volatile var settings = PlayerSettings()

    var alliedScore = 0
    var enemyScore = 0
    var timeRemainingSec = gameMode.durationSec.toFloat()
    var waveNumber = 1
    var isMatchOver = false
    var isVictory = false
    private var matchFinishNotified = false

    var matchKills = 0
    var matchDeaths = 0
    var matchHeadshots = 0
    var matchShotsFired = 0
    var matchShotsHit = 0
    var damageFlashAlpha = 0f

    private var hitMarkerType = HitMarkerType.NONE
    private var hitMarkerTimer = 0f
    private var hitMarkerCounter = 0L
    private var killBannerText: String? = null
    private var killBannerTimer = 0f
    private var killBannerCounter = 0L
    private var dominationTickTimer = 0f
    private var lanSyncTimer = 0f
    private var lastRemoteKills = 0
    private var lastRemoteHitsClaimed = 0

    val soldiers = ArrayList<SoldierUnit>()
    val tracers = ArrayList<TracerBolt>(64)
    val particles = ArrayList<SparkParticle>(128)
    private val killFeed = ArrayList<KillFeedItem>()
    private var killFeedId = 0L
    private val fwdTmp = FloatArray(3)
    private var semiAutoShotConsumed = false
    private var interactPrompt: String? = null
    private var hudEmitAccum = 0f
    private var fpsAccumFrames = 0
    private var fpsAccumTime = 0f
    private var measuredFps = 60

    init {
        spawnInitialSquads()
    }

    private fun spawnInitialSquads() {
        soldiers.clear()
        when (gameMode) {
            GameMode.TEAM_DEATHMATCH, GameMode.ZONE_DOMINATION -> {
                val allyNames = listOf("ALPHA-2", "BRAVO-3", "DELTA-4")
                for (i in 0 until 3) {
                    val sp = blueprint.alliedSpawns[(i + 1) % blueprint.alliedSpawns.size]
                    soldiers.add(SoldierUnit(10 + i, allyNames[i], isAllied = true, x = sp.first, z = sp.second))
                }
                val enemyNames = listOf("VIPER-1", "KOBRA-2", "REAPER-3", "JACKAL-4")
                for (i in 0 until 4) {
                    val sp = blueprint.enemySpawns[i % blueprint.enemySpawns.size]
                    soldiers.add(SoldierUnit(20 + i, enemyNames[i], isAllied = false, x = sp.first, z = sp.second, yawDeg = 180f))
                }
            }
            GameMode.WAVE_SURVIVAL -> {
                val allySp = blueprint.alliedSpawns[1]
                soldiers.add(SoldierUnit(10, "WINGMAN-1", isAllied = true, x = allySp.first, z = allySp.second))
                spawnSurvivalWave(1)
            }
            GameMode.LAN_ARENA -> {
                val enemySp = blueprint.enemySpawns[1]
                soldiers.add(
                    SoldierUnit(
                        id = 99,
                        callsign = lanManager?.lobbyState?.value?.peerCallsign?.ifBlank { "LAN-RIVAL" } ?: "LAN-RIVAL",
                        isAllied = false,
                        x = enemySp.first,
                        z = enemySp.second,
                        yawDeg = 180f
                    )
                )
            }
        }
    }

    private fun spawnSurvivalWave(wave: Int) {
        soldiers.removeAll { !it.isAllied }
        val count = (3 + wave).coerceAtMost(8)
        for (i in 0 until count) {
            val sp = blueprint.enemySpawns[i % blueprint.enemySpawns.size]
            val isHeavy = wave >= 2 && i == count - 1
            val hp = if (isHeavy) 180 + wave * 20 else 90 + wave * 12
            soldiers.add(
                SoldierUnit(
                    id = 30 + wave * 10 + i,
                    callsign = if (isHeavy) "JUGGERNAUT" else "RAIDER-${i + 1}",
                    isAllied = false,
                    isHeavy = isHeavy,
                    x = sp.first + (i % 3 - 1) * 1.6f,
                    z = sp.second,
                    yawDeg = 180f,
                    hp = hp,
                    maxHp = hp
                )
            )
        }
    }

    fun switchWeaponSlot(targetSlot: WeaponSlot? = null) {
        if (isPaused || isMatchOver || !isPlayerAlive) return
        val next = targetSlot ?: if (activeSlot == WeaponSlot.PRIMARY) WeaponSlot.SECONDARY else WeaponSlot.PRIMARY
        if (next == activeSlot) return
        activeSlot = next
        isReloading = false
        reloadTimer = 0f
        isAds = false
        swapDip = 1.0f
        fireCooldown = 0.22f
        audioEngine.playReload()
    }

    fun addLookDelta(dxPx: Float, dyPx: Float) {
        if (isPaused || isMatchOver) return
        val targetFov = effectiveAdsFov()
        val adsScale = if (isAds) (targetFov / 68f).coerceIn(0.30f, 0.85f) else 1f
        val sens = 0.22f * settings.lookSensitivity * adsScale
        playerYawDeg = (playerYawDeg + dxPx * sens) % 360f
        val ySign = if (settings.invertY) 1f else -1f
        playerPitchDeg = (playerPitchDeg + dyPx * sens * ySign).coerceIn(-75f, 75f)
    }

    fun setFiring(firing: Boolean) {
        isFiringInput = firing
        if (!firing) semiAutoShotConsumed = false
    }

    fun toggleAds() {
        if (!isMatchOver && !isPaused) {
            isAds = !isAds
        }
    }

    fun toggleCrouch() {
        if (!isMatchOver && !isPaused) {
            isCrouching = !isCrouching
        }
    }

    fun triggerJump() {
        if (!isMatchOver && !isPaused && isPlayerAlive && playerY <= 0.01f) {
            playerVelY = 6.2f
            isCrouching = false
        }
    }

    fun triggerReload() {
        if (!isMatchOver && !isPaused && !isReloading && ammo < weapon.magSize) {
            isReloading = true
            isAds = false
            reloadTimer = 0f
            audioEngine.playReload()
        }
    }

    fun triggerInteract() {
        if (isMatchOver || isPaused || !isPlayerAlive) return
        val nearbyTurret = turrets.minByOrNull { Math3D.dist2D(playerX, playerZ, it.x, it.z) }
        if (nearbyTurret != null && Math3D.dist2D(playerX, playerZ, nearbyTurret.x, nearbyTurret.z) <= 4.5f) {
            if (nearbyTurret.ownerTeam != 1) {
                nearbyTurret.ownerTeam = 1
                audioEngine.playTurretHack()
                killBannerText = "${nearbyTurret.label} HACKED"
                killBannerTimer = 2.0f
                killBannerCounter++
            }
        }
    }

    private fun effectiveAdsFov(): Float {
        return if (activeSlot == WeaponSlot.PRIMARY) {
            (weapon.adsFovDeg + primaryOptic.adsFovDelta).coerceIn(18f, 62f)
        } else {
            weapon.adsFovDeg
        }
    }

    fun step(dtRaw: Float, onHudUpdate: (GameHudState) -> Unit) {
        val dt = dtRaw.coerceIn(0.001f, 0.05f)
        fpsAccumFrames++
        fpsAccumTime += dtRaw
        if (fpsAccumTime >= 0.5f) {
            measuredFps = (fpsAccumFrames / fpsAccumTime).toInt().coerceIn(1, 120)
            fpsAccumFrames = 0
            fpsAccumTime = 0f
        }

        if (isPaused) {
            hudEmitAccum += dt
            if (hudEmitAccum >= 0.1f) {
                hudEmitAccum = 0f
                onHudUpdate(buildHudState())
            }
            return
        }

        updateVisualEffects(dt)

        if (!isMatchOver) {
            timeRemainingSec = max(0f, timeRemainingSec - dt)
            if (timeRemainingSec <= 0f) {
                finishMatch(isVictory = alliedScore >= enemyScore)
            }

            if (isPlayerAlive) {
                stepPlayerMovement(dt)
                stepPlayerWeapon(dt)
                stepSupplyPickups(dt)
                updateInteractPrompt()
            } else {
                playerRespawnTimer -= dt
                if (playerRespawnTimer <= 0f) {
                    respawnPlayer()
                }
            }

            stepSoldiers(dt)
            stepTurrets(dt)
            stepBarrelsRespawn(dt)
            if (gameMode == GameMode.ZONE_DOMINATION) {
                stepDominationZones(dt)
            }
            if (gameMode == GameMode.LAN_ARENA) {
                stepLanSync(dt)
            }
        }

        hudEmitAccum += dt
        if (hudEmitAccum >= 0.04f) {
            hudEmitAccum = 0f
            onHudUpdate(buildHudState())
        }
    }

    private fun updateVisualEffects(dt: Float) {
        damageFlashAlpha = max(0f, damageFlashAlpha - dt * 1.8f)
        recoilOffset = max(0f, recoilOffset - dt * 10f)
        swapDip = max(0f, swapDip - dt * 4.5f)
        val targetAds = if (isAds) 1f else 0f
        adsBlend += (targetAds - adsBlend) * min(1f, dt * 15f)

        if (hitMarkerTimer > 0f) {
            hitMarkerTimer -= dt
            if (hitMarkerTimer <= 0f) hitMarkerType = HitMarkerType.NONE
        }
        if (killBannerTimer > 0f) {
            killBannerTimer -= dt
            if (killBannerTimer <= 0f) killBannerText = null
        }
        if (muzzleFlashTimer > 0f) {
            muzzleFlashTimer -= dt
            flashIntensity = (muzzleFlashTimer / 0.08f).coerceIn(0f, 1f) * 3.8f
        } else {
            flashIntensity = 0f
        }

        val targetFov = 68f + (effectiveAdsFov() - 68f) * adsBlend
        currentFovDeg += (targetFov - currentFovDeg) * min(1f, dt * 15f)

        val tIter = tracers.iterator()
        while (tIter.hasNext()) {
            val t = tIter.next()
            t.life -= dt
            if (t.life <= 0f) tIter.remove()
        }

        val pIter = particles.iterator()
        while (pIter.hasNext()) {
            val p = pIter.next()
            p.life -= dt
            if (p.life <= 0f) {
                pIter.remove()
            } else {
                p.vy -= 14f * dt
                p.x += p.vx * dt
                p.y = max(0.05f, p.y + p.vy * dt)
                p.z += p.vz * dt
            }
        }
    }

    private fun stepPlayerMovement(dt: Float) {
        val perkMult = if (perk == TacticalPerk.LIGHTWEIGHT) 1.14f else 1.0f
        val stanceMult = when {
            isAds -> 0.62f
            isCrouching -> 0.58f
            else -> 1.0f
        }
        val baseSpeed = 7.4f * weapon.moveSpeedMult * perkMult * stanceMult
        val yawRad = playerYawDeg * Math3D.DEG_TO_RAD
        val sinY = sin(yawRad)
        val cosY = cos(yawRad)

        val forwardAmt = -moveStickY
        val strafeAmt = moveStickX
        val vx = (sinY * forwardAmt + cosY * strafeAmt) * baseSpeed
        val vz = (-cosY * forwardAmt + sinY * strafeAmt) * baseSpeed

        val speedMag = sqrt(vx * vx + vz * vz)
        if (speedMag > 0.25f) {
            weaponBobPhase += dt * speedMag * 1.45f
        }

        val radius = 0.55f
        val nextX = (playerX + vx * dt).coerceIn(-blueprint.boundsHalfSize + 1f, blueprint.boundsHalfSize - 1f)
        if (!collidesWorld(nextX, playerZ, radius)) {
            playerX = nextX
        }
        val nextZ = (playerZ + vz * dt).coerceIn(-blueprint.boundsHalfSize + 1f, blueprint.boundsHalfSize - 1f)
        if (!collidesWorld(playerX, nextZ, radius)) {
            playerZ = nextZ
        }

        if (playerY > 0f || playerVelY > 0f) {
            playerY += playerVelY * dt
            playerVelY -= 18f * dt
            if (playerY <= 0f) {
                playerY = 0f
                playerVelY = 0f
            }
        }
    }

    private fun stepPlayerWeapon(dt: Float) {
        if (fireCooldown > 0f) fireCooldown -= dt

        if (isReloading) {
            val reloadMult = if (perk == TacticalPerk.SLEIGHT_OF_HAND) 0.70f else 1.0f
            reloadTimer += dt
            val totalReload = weapon.reloadSeconds * reloadMult
            if (reloadTimer >= totalReload) {
                isReloading = false
                reloadTimer = 0f
                ammo = weapon.magSize
            }
            return
        }

        if (isFiringInput && fireCooldown <= 0f) {
            if (!weapon.isAutomatic && semiAutoShotConsumed) return
            if (ammo <= 0) {
                triggerReload()
                return
            }
            firePlayerWeapon()
            semiAutoShotConsumed = true
        }
    }

    private fun firePlayerWeapon() {
        ammo--
        matchShotsFired++
        fireCooldown = 60f / weapon.fireRateRpm
        recoilOffset = min(1f, recoilOffset + 0.55f)
        val muzzleRecoilMult = if (activeSlot == WeaponSlot.PRIMARY) primaryMuzzle.recoilMult else 1.0f
        playerPitchDeg = (playerPitchDeg + weapon.recoilKickDeg * muzzleRecoilMult * (if (isAds) 0.6f else 1.0f)).coerceIn(-75f, 75f)
        audioEngine.playWeaponFire(weapon)

        val eyeY = getPlayerEyeHeight()
        Math3D.forwardVector(playerYawDeg, playerPitchDeg, fwdTmp)
        flashPosX = playerX + fwdTmp[0] * 1.0f
        flashPosY = eyeY + fwdTmp[1] * 1.0f - 0.12f
        flashPosZ = playerZ + fwdTmp[2] * 1.0f
        flashR = 1.0f; flashG = 0.72f; flashB = 0.25f
        muzzleFlashTimer = if (activeSlot == WeaponSlot.PRIMARY && primaryMuzzle == MuzzleAttachment.SUPPRESSOR) 0.04f else 0.08f

        val spreadPerk = if (perk == TacticalPerk.DEAD_EYE) 0.65f else 1.0f
        val opticSpreadMult = if (isAds && activeSlot == WeaponSlot.PRIMARY) primaryOptic.spreadMult else 1.0f
        val crouchSpread = if (isCrouching) 0.75f else 1.0f
        val spreadDeg = (if (isAds) weapon.adsSpreadDeg else weapon.baseSpreadDeg) * spreadPerk * opticSpreadMult * crouchSpread
        val effectiveRange = weapon.rangeMeters * (if (activeSlot == WeaponSlot.PRIMARY) primaryMuzzle.rangeMult else 1.0f)

        var anyHit = false
        var anyHeadshot = false

        for (p in 0 until weapon.pellets) {
            val jitterYaw = playerYawDeg + (Random.nextFloat() - 0.5f) * 2f * spreadDeg
            val jitterPitch = playerPitchDeg + (Random.nextFloat() - 0.5f) * 2f * spreadDeg
            Math3D.forwardVector(jitterYaw, jitterPitch, fwdTmp)

            if (settings.aimAssist && weapon.pellets == 1) {
                applyAimAssist(eyeY, fwdTmp, effectiveRange)
            }

            val hit = castRay(
                ox = playerX, oy = eyeY, oz = playerZ,
                dx = fwdTmp[0], dy = fwdTmp[1], dz = fwdTmp[2],
                maxDist = effectiveRange,
                targetAllies = false
            )

            val endX = playerX + fwdTmp[0] * hit.dist
            val endY = eyeY + fwdTmp[1] * hit.dist
            val endZ = playerZ + fwdTmp[2] * hit.dist

            tracers.add(
                TracerBolt(
                    x0 = flashPosX, y0 = flashPosY, z0 = flashPosZ,
                    x1 = endX, y1 = endY, z1 = endZ,
                    life = 0.09f, maxLife = 0.09f,
                    r = 1.0f, g = 0.85f, b = 0.35f
                )
            )

            when (hit.type) {
                RayHitType.SOLDIER -> {
                    val victim = hit.soldier!!
                    anyHit = true
                    if (hit.isHeadshot) anyHeadshot = true
                    val dmg = if (hit.isHeadshot) (weapon.damage * 1.85f).toInt() else weapon.damage
                    damageSoldier(victim, dmg, killerName = playerCallsign, killerIsAllied = true, weaponName = weapon.displayName, isHeadshot = hit.isHeadshot, isPlayerKill = true)
                    spawnImpactSparks(endX, endY, endZ, 1.0f, 0.25f, 0.2f, 5)
                }
                RayHitType.BARREL -> {
                    val barrel = hit.barrel!!
                    anyHit = true
                    damageBarrel(barrel, weapon.damage, killerName = playerCallsign, killerIsAllied = true)
                    spawnImpactSparks(endX, endY, endZ, 1.0f, 0.6f, 0.1f, 6)
                }
                RayHitType.BARRIER -> {
                    val barrier = hit.barrier!!
                    anyHit = true
                    damageBarrier(barrier, weapon.damage)
                    spawnImpactSparks(endX, endY, endZ, 0.85f, 0.72f, 0.50f, 5)
                }
                RayHitType.TURRET -> {
                    val turret = hit.turret!!
                    if (turret.ownerTeam == -1) {
                        anyHit = true
                        turret.ownerTeam = 0
                        killBannerText = "${turret.label} DISABLED"
                        killBannerTimer = 1.8f
                        killBannerCounter++
                    }
                    spawnImpactSparks(endX, endY, endZ, 0.3f, 0.85f, 1.0f, 6)
                }
                RayHitType.WORLD -> {
                    spawnImpactSparks(endX, endY, endZ, 0.9f, 0.85f, 0.7f, 3)
                }
                RayHitType.NONE -> {}
            }
        }

        if (anyHit) {
            matchShotsHit++
            hitMarkerType = if (anyHeadshot) HitMarkerType.HEADSHOT else HitMarkerType.BODY
            hitMarkerTimer = 0.22f
            hitMarkerCounter++
            audioEngine.playHitMarker(anyHeadshot)
        }
        if (ammo == 0) {
            triggerReload()
        }
    }

    private fun applyAimAssist(eyeY: Float, dirOut: FloatArray, effectiveRange: Float) {
        var bestDot = 0.9965f
        var bestDx = 0f; var bestDy = 0f; var bestDz = 0f
        for (s in soldiers) {
            if (!s.isAlive || s.isAllied) continue
            val tx = s.x - playerX
            val ty = (s.y + 1.25f) - eyeY
            val tz = s.z - playerZ
            val len = sqrt(tx * tx + ty * ty + tz * tz)
            if (len < 0.5f || len > effectiveRange) continue
            val nx = tx / len; val ny = ty / len; val nz = tz / len
            val dot = nx * dirOut[0] + ny * dirOut[1] + nz * dirOut[2]
            if (dot > bestDot) {
                bestDot = dot
                bestDx = nx; bestDy = ny; bestDz = nz
            }
        }
        if (bestDot > 0.9965f) {
            dirOut[0] = dirOut[0] * 0.45f + bestDx * 0.55f
            dirOut[1] = dirOut[1] * 0.45f + bestDy * 0.55f
            dirOut[2] = dirOut[2] * 0.45f + bestDz * 0.55f
            val n = sqrt(dirOut[0] * dirOut[0] + dirOut[1] * dirOut[1] + dirOut[2] * dirOut[2])
            if (n > 0f) { dirOut[0] /= n; dirOut[1] /= n; dirOut[2] /= n }
        }
    }

    private fun damageBarrel(barrel: ExplosiveBarrel, dmg: Int, killerName: String, killerIsAllied: Boolean) {
        if (barrel.isDestroyed) return
        barrel.hp -= dmg
        if (barrel.hp <= 0) {
            barrel.isDestroyed = true
            barrel.respawnTimer = if (gameMode == GameMode.WAVE_SURVIVAL) 30f else 9999f
            audioEngine.playExplosion()
            flashPosX = barrel.x; flashPosY = 1.2f; flashPosZ = barrel.z
            flashR = 1.0f; flashG = 0.45f; flashB = 0.08f
            muzzleFlashTimer = 0.14f
            spawnImpactSparks(barrel.x, 1.0f, barrel.z, 1.0f, 0.45f, 0.08f, 26, speed = 11f)

            val radius = 6.8f
            for (s in soldiers) {
                if (!s.isAlive) continue
                val d = Math3D.dist2D(barrel.x, barrel.z, s.x, s.z)
                if (d < radius) {
                    val falloff = 1f - (d / radius)
                    val blastDmg = (115f * falloff).toInt().coerceAtLeast(25)
                    damageSoldier(s, blastDmg, killerName, killerIsAllied, "Fuel Barrel", false, killerName == playerCallsign)
                }
            }
            if (isPlayerAlive) {
                val dp = Math3D.dist2D(barrel.x, barrel.z, playerX, playerZ)
                if (dp < radius) {
                    val blastDmg = (75f * (1f - dp / radius)).toInt().coerceAtLeast(15)
                    damagePlayer(blastDmg, killerName = "Fuel Barrel")
                }
            }
        }
    }

    private fun damageBarrier(barrier: BreakableBarrier, dmg: Int) {
        if (barrier.isDestroyed) return
        barrier.hp -= dmg
        if (barrier.hp <= 0) {
            barrier.isDestroyed = true
            audioEngine.playExplosion()
            spawnImpactSparks(barrier.x, barrier.height * 0.5f, barrier.z, 0.78f, 0.65f, 0.45f, 18, speed = 8f)
        }
    }

    private fun damageSoldier(
        victim: SoldierUnit,
        dmg: Int,
        killerName: String,
        killerIsAllied: Boolean,
        weaponName: String,
        isHeadshot: Boolean,
        isPlayerKill: Boolean
    ) {
        if (!victim.isAlive) return
        victim.hp -= dmg
        victim.hitFlashTimer = 0.14f
        if (victim.hp <= 0) {
            victim.hp = 0
            victim.isAlive = false
            victim.respawnTimer = 4.5f
            victim.deathTilt = 1f

            pushKillFeed(killerName, victim.callsign, weaponName, isHeadshot, killerIsAllied)

            if (killerIsAllied && !victim.isAllied) {
                if (gameMode != GameMode.ZONE_DOMINATION) alliedScore++
                if (isPlayerKill) {
                    matchKills++
                    if (isHeadshot) matchHeadshots++
                    hitMarkerType = HitMarkerType.KILL
                    hitMarkerTimer = 0.3f
                    hitMarkerCounter++
                    killBannerText = if (isHeadshot) "HEADSHOT ELIMINATION" else "ELIMINATED ${victim.callsign}"
                    killBannerTimer = 2.0f
                    killBannerCounter++
                }
                checkScoreVictory()
            } else if (!killerIsAllied && victim.isAllied) {
                if (gameMode != GameMode.ZONE_DOMINATION) enemyScore++
                checkScoreVictory()
            }
        }
    }

    private fun damagePlayer(dmg: Int, killerName: String) {
        if (!isPlayerAlive || isMatchOver) return
        damageFlashAlpha = 0.75f
        var remaining = dmg
        if (playerArmor > 0) {
            val absorbed = min(playerArmor, (dmg * 0.65f).toInt().coerceAtLeast(1))
            playerArmor -= absorbed
            remaining -= absorbed
        }
        playerHp = max(0, playerHp - remaining)
        if (playerHp <= 0) {
            isPlayerAlive = false
            isAds = false
            isReloading = false
            playerRespawnTimer = 4.0f
            matchDeaths++
            if (gameMode != GameMode.ZONE_DOMINATION) enemyScore++
            pushKillFeed(killerName, playerCallsign, "Rifle", false, false)
            if (gameMode == GameMode.WAVE_SURVIVAL) {
                finishMatch(isVictory = false)
            } else {
                checkScoreVictory()
            }
        }
    }

    private fun checkScoreVictory() {
        if (isMatchOver) return
        when (gameMode) {
            GameMode.TEAM_DEATHMATCH, GameMode.ZONE_DOMINATION, GameMode.LAN_ARENA -> {
                if (alliedScore >= gameMode.targetScore) finishMatch(isVictory = true)
                else if (enemyScore >= gameMode.targetScore) finishMatch(isVictory = false)
            }
            GameMode.WAVE_SURVIVAL -> {
                val anyHostileAlive = soldiers.any { !it.isAllied && it.isAlive }
                if (!anyHostileAlive) {
                    if (waveNumber >= gameMode.targetScore) {
                        finishMatch(isVictory = true)
                    } else {
                        waveNumber++
                        alliedScore = waveNumber
                        killBannerText = "WAVE $waveNumber INCOMING"
                        killBannerTimer = 2.5f
                        killBannerCounter++
                        spawnSurvivalWave(waveNumber)
                    }
                }
            }
        }
    }

    private fun finishMatch(isVictory: Boolean) {
        if (isMatchOver) return
        this.isMatchOver = true
        this.isVictory = isVictory
        this.isFiringInput = false
        this.isAds = false
        if (!matchFinishNotified) {
            matchFinishNotified = true
            onMatchFinished(buildHudState())
        }
    }

    private fun respawnPlayer() {
        val sp = blueprint.alliedSpawns[Random.nextInt(blueprint.alliedSpawns.size)]
        playerX = sp.first
        playerZ = sp.second
        playerY = 0f
        playerVelY = 0f
        playerHp = playerMaxHp
        playerArmor = playerMaxArmor
        primaryAmmo = primaryWeapon.magSize
        secondaryAmmo = secondaryWeapon.magSize
        isReloading = false
        isPlayerAlive = true
    }

    private fun stepSupplyPickups(dt: Float) {
        for (crate in supplyCrates) {
            if (crate.cooldownSec > 0f) {
                crate.cooldownSec -= dt
                continue
            }
            if (Math3D.dist2D(playerX, playerZ, crate.x, crate.z) < 2.0f) {
                if (playerHp < playerMaxHp || playerArmor < playerMaxArmor || primaryAmmo < primaryWeapon.magSize || secondaryAmmo < secondaryWeapon.magSize) {
                    playerHp = min(playerMaxHp, playerHp + 40)
                    playerArmor = min(playerMaxArmor, playerArmor + 30)
                    primaryAmmo = primaryWeapon.magSize
                    secondaryAmmo = secondaryWeapon.magSize
                    isReloading = false
                    crate.cooldownSec = 18f
                    audioEngine.playReload()
                    killBannerText = "RESUPPLY ACQUIRED"
                    killBannerTimer = 1.6f
                    killBannerCounter++
                }
            }
        }
    }

    private fun updateInteractPrompt() {
        val nearbyTurret = turrets.minByOrNull { Math3D.dist2D(playerX, playerZ, it.x, it.z) }
        interactPrompt = if (nearbyTurret != null &&
            Math3D.dist2D(playerX, playerZ, nearbyTurret.x, nearbyTurret.z) <= 4.5f &&
            nearbyTurret.ownerTeam != 1
        ) {
            "HACK ${nearbyTurret.label}"
        } else {
            null
        }
    }

    private fun stepTurrets(dt: Float) {
        for (turret in turrets) {
            if (turret.fireCooldown > 0f) turret.fireCooldown -= dt
            if (turret.ownerTeam == 0) {
                turret.yawDeg = (turret.yawDeg + dt * 38f) % 360f
                continue
            }

            val shootAtAllies = turret.ownerTeam == -1
            var bestDist = 26f
            var targetX = 0f; var targetY = 1.25f; var targetZ = 0f
            var targetSoldier: SoldierUnit? = null
            var targetIsPlayer = false

            for (s in soldiers) {
                if (!s.isAlive || (s.isAllied != shootAtAllies)) continue
                val d = Math3D.dist2D(turret.x, turret.z, s.x, s.z)
                if (d < bestDist && hasLineOfSight(turret.x, turret.baseY + 1.4f, turret.z, s.x, 1.2f, s.z)) {
                    bestDist = d
                    targetX = s.x; targetY = 1.2f; targetZ = s.z
                    targetSoldier = s
                    targetIsPlayer = false
                }
            }
            if (shootAtAllies && isPlayerAlive) {
                val dp = Math3D.dist2D(turret.x, turret.z, playerX, playerZ)
                if (dp < bestDist && hasLineOfSight(turret.x, turret.baseY + 1.4f, turret.z, playerX, getPlayerEyeHeight() - 0.2f, playerZ)) {
                    bestDist = dp
                    targetX = playerX; targetY = 1.25f; targetZ = playerZ
                    targetSoldier = null
                    targetIsPlayer = true
                }
            }

            if (targetSoldier != null || targetIsPlayer) {
                val desiredYaw = Math3D.angleBetweenDeg(targetX - turret.x, targetZ - turret.z)
                val diff = Math3D.wrapAngleDeg(desiredYaw - turret.yawDeg)
                turret.yawDeg += diff.coerceIn(-160f * dt, 160f * dt)

                if (abs(diff) < 18f && turret.fireCooldown <= 0f) {
                    turret.fireCooldown = 0.24f
                    val colR = if (turret.ownerTeam == 1) 0.15f else 1.0f
                    val colG = if (turret.ownerTeam == 1) 0.90f else 0.25f
                    val colB = if (turret.ownerTeam == 1) 1.0f else 0.25f
                    tracers.add(
                        TracerBolt(
                            turret.x, turret.baseY + 1.35f, turret.z,
                            targetX, targetY, targetZ,
                            0.10f, 0.10f, colR, colG, colB
                        )
                    )
                    if (targetSoldier != null) {
                        damageSoldier(targetSoldier, 14, turret.label, turret.ownerTeam == 1, "Sentry Turret", false, false)
                    } else if (targetIsPlayer) {
                        damagePlayer(10, turret.label)
                    }
                }
            } else {
                turret.yawDeg = (turret.yawDeg + dt * 28f) % 360f
            }
        }
    }

    private fun stepBarrelsRespawn(dt: Float) {
        if (gameMode != GameMode.WAVE_SURVIVAL) return
        for (b in barrels) {
            if (b.isDestroyed) {
                b.respawnTimer -= dt
                if (b.respawnTimer <= 0f) {
                    b.isDestroyed = false
                    b.hp = b.maxHp
                }
            }
        }
    }

    private fun stepSoldiers(dt: Float) {
        if (gameMode == GameMode.LAN_ARENA) return

        for (s in soldiers) {
            if (s.hitFlashTimer > 0f) s.hitFlashTimer -= dt
            if (!s.isAlive) {
                if (gameMode != GameMode.WAVE_SURVIVAL || s.isAllied) {
                    s.respawnTimer -= dt
                    if (s.respawnTimer <= 0f) {
                        val spawns = if (s.isAllied) blueprint.alliedSpawns else blueprint.enemySpawns
                        val sp = spawns[Random.nextInt(spawns.size)]
                        s.x = sp.first; s.z = sp.second
                        s.hp = s.maxHp; s.isAlive = true; s.deathTilt = 0f
                    }
                }
                continue
            }

            if (s.fireCooldown > 0f) s.fireCooldown -= dt
            s.strafeTimer -= dt
            if (s.strafeTimer <= 0f) {
                s.strafeDir = if (Random.nextBoolean()) 1f else -1f
                s.strafeTimer = 1.2f + Random.nextFloat() * 1.4f
            }

            var bestEnemyDist = 999f
            var enemyX = 0f; var enemyZ = 0f; var enemyY = 1.25f
            var targetUnit: SoldierUnit? = null
            var targetIsPlayer = false

            for (other in soldiers) {
                if (!other.isAlive || other.isAllied == s.isAllied) continue
                val d = Math3D.dist2D(s.x, s.z, other.x, other.z)
                if (d < bestEnemyDist) {
                    bestEnemyDist = d
                    enemyX = other.x; enemyZ = other.z; enemyY = 1.25f
                    targetUnit = other; targetIsPlayer = false
                }
            }
            if (!s.isAllied && isPlayerAlive) {
                val dp = Math3D.dist2D(s.x, s.z, playerX, playerZ)
                if (dp < bestEnemyDist) {
                    bestEnemyDist = dp
                    enemyX = playerX; enemyZ = playerZ; enemyY = getPlayerEyeHeight() - 0.15f
                    targetUnit = null; targetIsPlayer = true
                }
            }

            val hasTarget = (targetUnit != null || targetIsPlayer)
            val canSee = hasTarget && bestEnemyDist < 40f && hasLineOfSight(s.x, 1.35f, s.z, enemyX, enemyY, enemyZ)

            var moveX = 0f; var moveZ = 0f
            val moveSpeed = if (s.isHeavy) 3.1f else 4.5f

            if (canSee) {
                val desiredYaw = Math3D.angleBetweenDeg(enemyX - s.x, enemyZ - s.z)
                s.yawDeg = desiredYaw
                val yawRad = desiredYaw * Math3D.DEG_TO_RAD
                val rightX = cos(yawRad); val rightZ = sin(yawRad)
                val fwdX = sin(yawRad); val fwdZ = -cos(yawRad)

                moveX += rightX * s.strafeDir * 0.75f
                moveZ += rightZ * s.strafeDir * 0.75f
                if (bestEnemyDist > 13f) {
                    moveX += fwdX * 0.5f; moveZ += fwdZ * 0.5f
                } else if (bestEnemyDist < 6f) {
                    moveX -= fwdX * 0.5f; moveZ -= fwdZ * 0.5f
                }

                if (s.fireCooldown <= 0f) {
                    s.fireCooldown = if (s.isHeavy) 0.35f else (0.48f + Random.nextFloat() * 0.35f)
                    val spread = 0.08f
                    val tx = enemyX + (Random.nextFloat() - 0.5f) * bestEnemyDist * spread
                    val tz = enemyZ + (Random.nextFloat() - 0.5f) * bestEnemyDist * spread
                    tracers.add(
                        TracerBolt(
                            s.x, 1.25f, s.z,
                            tx, enemyY, tz,
                            0.09f, 0.09f,
                            if (s.isAllied) 0.2f else 1.0f,
                            if (s.isAllied) 0.9f else 0.3f,
                            if (s.isAllied) 1.0f else 0.2f
                        )
                    )
                    val hitChance = (0.55f - bestEnemyDist * 0.01f).coerceIn(0.18f, 0.55f)
                    if (Random.nextFloat() < hitChance) {
                        val aiDmg = if (s.isHeavy) 12 else 8
                        if (targetIsPlayer) {
                            damagePlayer(aiDmg, s.callsign)
                        } else if (targetUnit != null) {
                            damageSoldier(targetUnit, aiDmg, s.callsign, s.isAllied, "Carbine", false, false)
                        }
                    }
                }
            } else {
                val goalX = if (gameMode == GameMode.ZONE_DOMINATION) {
                    zones[(s.id) % zones.size].x
                } else if (hasTarget) enemyX else 0f
                val goalZ = if (gameMode == GameMode.ZONE_DOMINATION) {
                    zones[(s.id) % zones.size].z
                } else if (hasTarget) enemyZ else 0f

                val dx = goalX - s.x; val dz = goalZ - s.z
                val len = sqrt(dx * dx + dz * dz)
                if (len > 1f) {
                    moveX = dx / len; moveZ = dz / len
                    s.yawDeg = Math3D.angleBetweenDeg(moveX, moveZ)
                }
            }

            val mLen = sqrt(moveX * moveX + moveZ * moveZ)
            if (mLen > 0.01f) {
                val nx = (moveX / mLen) * moveSpeed * dt
                val nz = (moveZ / mLen) * moveSpeed * dt
                val candX = (s.x + nx).coerceIn(-blueprint.boundsHalfSize + 1f, blueprint.boundsHalfSize - 1f)
                if (!collidesWorld(candX, s.z, 0.5f)) s.x = candX
                else s.strafeDir = -s.strafeDir
                val candZ = (s.z + nz).coerceIn(-blueprint.boundsHalfSize + 1f, blueprint.boundsHalfSize - 1f)
                if (!collidesWorld(s.x, candZ, 0.5f)) s.z = candZ
                else s.strafeDir = -s.strafeDir
                s.walkPhase += dt * 7f
            }
        }
    }

    private fun stepDominationZones(dt: Float) {
        for (zone in zones) {
            var allyCount = 0
            var enemyCount = 0
            if (isPlayerAlive && Math3D.dist2D(playerX, playerZ, zone.x, zone.z) <= zone.radius) {
                allyCount++
            }
            for (s in soldiers) {
                if (!s.isAlive) continue
                if (Math3D.dist2D(s.x, s.z, zone.x, zone.z) <= zone.radius) {
                    if (s.isAllied) allyCount++ else enemyCount++
                }
            }
            val net = (allyCount - enemyCount).coerceIn(-2, 2)
            if (net != 0) {
                zone.progress = (zone.progress + net * 0.22f * dt).coerceIn(-1f, 1f)
                if (zone.progress >= 1f) zone.ownerTeam = 1
                else if (zone.progress <= -1f) zone.ownerTeam = -1
            }
        }

        dominationTickTimer += dt
        if (dominationTickTimer >= 1.0f) {
            dominationTickTimer -= 1.0f
            val alliedZones = zones.count { it.ownerTeam == 1 }
            val enemyZones = zones.count { it.ownerTeam == -1 }
            alliedScore += alliedZones * 2
            enemyScore += enemyZones * 2
            checkScoreVictory()
        }
    }

    private fun stepLanSync(dt: Float) {
        val mgr = lanManager ?: return
        lanSyncTimer += dt
        if (lanSyncTimer >= 0.033f) {
            lanSyncTimer = 0f
            mgr.sendGameState(
                callsign = playerCallsign,
                x = playerX, y = playerY, z = playerZ,
                yawDeg = playerYawDeg, pitchDeg = playerPitchDeg,
                hp = playerHp, isFiring = isFiringInput,
                hitsClaimed = matchKills, kills = alliedScore,
                mapId = mapId
            )
        }
        val remote = mgr.latestRemotePacket.get() ?: return
        val rival = soldiers.firstOrNull { it.id == 99 } ?: return
        rival.x += (remote.x - rival.x) * 0.4f
        rival.z += (remote.z - rival.z) * 0.4f
        rival.yawDeg = remote.yawDeg
        rival.hp = remote.hp
        rival.isAlive = remote.hp > 0
        if (remote.hitsClaimed > lastRemoteHitsClaimed) {
            lastRemoteHitsClaimed = remote.hitsClaimed
            damagePlayer(25, remote.callsign)
        }
        if (remote.kills > lastRemoteKills) {
            lastRemoteKills = remote.kills
            enemyScore = remote.kills
            checkScoreVictory()
        }
    }

    private fun collidesWorld(cx: Float, cz: Float, radius: Float): Boolean {
        for (box in blueprint.staticColliders) {
            if (box.intersectsCylinder(cx, cz, radius, 0f, 1.8f)) return true
        }
        for (barrier in barriers) {
            if (!barrier.isDestroyed && barrier.aabb.intersectsCylinder(cx, cz, radius, 0f, 1.8f)) return true
        }
        for (barrel in barrels) {
            if (!barrel.isDestroyed && barrel.aabb.intersectsCylinder(cx, cz, radius, 0f, 1.5f)) return true
        }
        for (turret in turrets) {
            if (turret.aabb.intersectsCylinder(cx, cz, radius, 0f, 1.8f)) return true
        }
        return false
    }

    private fun hasLineOfSight(x0: Float, y0: Float, z0: Float, x1: Float, y1: Float, z1: Float): Boolean {
        val dx = x1 - x0; val dy = y1 - y0; val dz = z1 - z0
        val dist = sqrt(dx * dx + dy * dy + dz * dz)
        if (dist < 0.05f) return true
        val nx = dx / dist; val ny = dy / dist; val nz = dz / dist
        for (box in blueprint.staticColliders) {
            val t = box.rayIntersect(x0, y0, z0, nx, ny, nz, dist)
            if (t != null && t < dist - 0.1f) return false
        }
        for (barrier in barriers) {
            if (!barrier.isDestroyed) {
                val t = barrier.aabb.rayIntersect(x0, y0, z0, nx, ny, nz, dist)
                if (t != null && t < dist - 0.1f) return false
            }
        }
        return true
    }

    private enum class RayHitType { NONE, WORLD, BARRIER, BARREL, TURRET, SOLDIER }
    private data class RayHit(
        val type: RayHitType,
        val dist: Float,
        val soldier: SoldierUnit? = null,
        val isHeadshot: Boolean = false,
        val barrel: ExplosiveBarrel? = null,
        val barrier: BreakableBarrier? = null,
        val turret: ControllableTurret? = null
    )

    private fun castRay(
        ox: Float, oy: Float, oz: Float,
        dx: Float, dy: Float, dz: Float,
        maxDist: Float,
        targetAllies: Boolean
    ): RayHit {
        var bestDist = maxDist
        var bestType = RayHitType.NONE
        var hitSoldier: SoldierUnit? = null
        var isHeadshot = false
        var hitBarrel: ExplosiveBarrel? = null
        var hitBarrier: BreakableBarrier? = null
        var hitTurret: ControllableTurret? = null

        for (box in blueprint.staticColliders) {
            val t = box.rayIntersect(ox, oy, oz, dx, dy, dz, bestDist)
            if (t != null && t < bestDist) {
                bestDist = t; bestType = RayHitType.WORLD
            }
        }
        if (dy < -0.001f) {
            val tGround = -oy / dy
            if (tGround in 0f..bestDist) {
                bestDist = tGround; bestType = RayHitType.WORLD
            }
        }
        for (barrier in barriers) {
            if (barrier.isDestroyed) continue
            val t = barrier.aabb.rayIntersect(ox, oy, oz, dx, dy, dz, bestDist)
            if (t != null && t < bestDist) {
                bestDist = t; bestType = RayHitType.BARRIER; hitBarrier = barrier
            }
        }
        for (barrel in barrels) {
            if (barrel.isDestroyed) continue
            val t = barrel.aabb.rayIntersect(ox, oy, oz, dx, dy, dz, bestDist)
            if (t != null && t < bestDist) {
                bestDist = t; bestType = RayHitType.BARREL; hitBarrel = barrel
            }
        }
        for (turret in turrets) {
            val t = turret.aabb.rayIntersect(ox, oy, oz, dx, dy, dz, bestDist)
            if (t != null && t < bestDist) {
                bestDist = t; bestType = RayHitType.TURRET; hitTurret = turret
            }
        }
        for (s in soldiers) {
            if (!s.isAlive || (s.isAllied != targetAllies)) continue
            val rad = if (s.isHeavy) 0.55f else 0.42f
            val headBox = Aabb(s.x - 0.24f, s.y + 1.42f, s.z - 0.24f, s.x + 0.24f, s.y + 1.88f, s.z + 0.24f)
            val tHead = headBox.rayIntersect(ox, oy, oz, dx, dy, dz, bestDist)
            if (tHead != null && tHead < bestDist) {
                bestDist = tHead; bestType = RayHitType.SOLDIER; hitSoldier = s; isHeadshot = true
            }
            val bodyBox = Aabb(s.x - rad, s.y, s.z - rad, s.x + rad, s.y + 1.42f, s.z + rad)
            val tBody = bodyBox.rayIntersect(ox, oy, oz, dx, dy, dz, bestDist)
            if (tBody != null && tBody < bestDist) {
                bestDist = tBody; bestType = RayHitType.SOLDIER; hitSoldier = s; isHeadshot = false
            }
        }

        return RayHit(bestType, bestDist, hitSoldier, isHeadshot, hitBarrel, hitBarrier, hitTurret)
    }

    private fun spawnImpactSparks(
        x: Float, y: Float, z: Float,
        r: Float, g: Float, b: Float,
        count: Int,
        speed: Float = 5.5f
    ) {
        if (particles.size > 110) return
        for (i in 0 until count) {
            particles.add(
                SparkParticle(
                    x = x, y = y, z = z,
                    vx = (Random.nextFloat() - 0.5f) * speed * 2f,
                    vy = Random.nextFloat() * speed + 1.5f,
                    vz = (Random.nextFloat() - 0.5f) * speed * 2f,
                    life = 0.45f + Random.nextFloat() * 0.3f,
                    maxLife = 0.75f,
                    size = if (speed > 8f) 0.26f else 0.14f,
                    r = r, g = g, b = b
                )
            )
        }
    }

    private fun pushKillFeed(killer: String, victim: String, weaponName: String, hs: Boolean, killerIsAllied: Boolean) {
        killFeed.add(
            0,
            KillFeedItem(
                id = ++killFeedId,
                killer = killer,
                victim = victim,
                weapon = weaponName,
                isHeadshot = hs,
                killerIsAllied = killerIsAllied,
                timestampMs = System.currentTimeMillis()
            )
        )
        if (killFeed.size > 4) killFeed.removeAt(killFeed.lastIndex)
    }

    fun getPlayerEyeHeight(): Float = playerY + (if (isCrouching) 1.05f else 1.62f)

    fun buildHudState(): GameHudState {
        val reloadMult = if (perk == TacticalPerk.SLEIGHT_OF_HAND) 0.70f else 1.0f
        val reloadPct = if (isReloading) (reloadTimer / (weapon.reloadSeconds * reloadMult)).coerceIn(0f, 1f) else 0f
        val blips = ArrayList<MinimapBlip>(soldiers.size + zones.size + turrets.size)
        for (s in soldiers) {
            if (s.isAlive) blips.add(MinimapBlip(s.x, s.z, isAllied = s.isAllied))
        }
        for (t in turrets) {
            blips.add(MinimapBlip(t.x, t.z, isAllied = t.ownerTeam == 1, isInteractive = true, ownerTeam = t.ownerTeam))
        }
        if (gameMode == GameMode.ZONE_DOMINATION) {
            for (z in zones) {
                blips.add(MinimapBlip(z.x, z.z, isAllied = z.ownerTeam == 1, isObjective = true, label = z.label, ownerTeam = z.ownerTeam))
            }
        }
        val spreadPx = (if (isAds) 6f else 14f) + recoilOffset * 18f
        return GameHudState(
            gameMode = gameMode,
            mapId = mapId,
            hp = playerHp,
            maxHp = playerMaxHp,
            armor = playerArmor,
            maxArmor = playerMaxArmor,
            ammo = ammo,
            maxAmmo = weapon.magSize,
            primaryAmmo = primaryAmmo,
            secondaryAmmo = secondaryAmmo,
            activeSlot = activeSlot,
            primaryWeapon = primaryWeapon,
            secondaryWeapon = secondaryWeapon,
            isReloading = isReloading,
            reloadProgress = reloadPct,
            isAds = isAds,
            isCrouching = isCrouching,
            activeWeapon = weapon,
            alliedScore = alliedScore,
            enemyScore = enemyScore,
            targetScore = gameMode.targetScore,
            timeRemainingSec = timeRemainingSec.toInt(),
            waveNumber = waveNumber,
            zones = zones.map { ZoneHudState(it.label, it.ownerTeam, it.progress) },
            killFeed = killFeed.toList(),
            hitMarkerType = hitMarkerType,
            hitMarkerId = hitMarkerCounter,
            killBannerText = killBannerText,
            killBannerId = killBannerCounter,
            interactPrompt = interactPrompt,
            turretOwnerTeam = turrets.firstOrNull()?.ownerTeam ?: 0,
            damageFlashAlpha = damageFlashAlpha,
            crosshairSpreadPx = spreadPx,
            minimapBlips = blips,
            playerX = playerX,
            playerZ = playerZ,
            playerYawDeg = playerYawDeg,
            fps = measuredFps,
            pingMs = lanManager?.lobbyState?.value?.pingMs ?: 0,
            isMatchOver = isMatchOver,
            isVictory = isVictory,
            isPaused = isPaused,
            isRespawning = !isPlayerAlive,
            respawnSecondsLeft = max(1, playerRespawnTimer.toInt() + 1),
            matchKills = matchKills,
            matchDeaths = matchDeaths,
            matchHeadshots = matchHeadshots,
            matchShotsFired = matchShotsFired,
            matchShotsHit = matchShotsHit,
            isLanMatch = gameMode == GameMode.LAN_ARENA,
            lanConnected = lanManager?.lobbyState?.value?.isConnected == true
        )
    }
}
