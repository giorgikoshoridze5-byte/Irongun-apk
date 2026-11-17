package com.example.engine

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.model.GameHudState
import com.example.model.MuzzleAttachment
import com.example.model.OpticAttachment
import com.example.model.WeaponCamo
import com.example.model.WeaponSlot
import com.example.model.WeaponType
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

class WarfareRenderer(
    private val simulation: GameSimulation,
    private val highQuality: Boolean,
    private val onHudUpdate: (GameHudState) -> Unit
) : GLSurfaceView.Renderer {

    private lateinit var litShader: LitShaderProgram
    private lateinit var unlitShader: UnlitShaderProgram

    private lateinit var skyDomeMesh: GlMesh
    private lateinit var sunDiscMesh: GlMesh
    private lateinit var staticMapMesh: GlMesh
    private lateinit var barrelMesh: GlMesh
    private lateinit var unitCubeMesh: GlMesh
    private lateinit var turretBaseMesh: GlMesh
    private lateinit var turretHeadMesh: GlMesh
    private lateinit var supplyCrystalMesh: GlMesh
    private lateinit var soldierTorsoMesh: GlMesh
    private lateinit var soldierHeadMesh: GlMesh
    private lateinit var soldierLegMesh: GlMesh
    private lateinit var soldierRifleMesh: GlMesh
    private lateinit var primaryViewModelMesh: GlMesh
    private lateinit var secondaryViewModelMesh: GlMesh
    private lateinit var handsViewModelMesh: GlMesh
    private lateinit var muzzleFlashMesh: GlMesh
    private lateinit var zonePillarMesh: GlMesh

    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val viewProjMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val fwdTmp = FloatArray(3)

    private var lastFrameNs = 0L
    private var viewportWidth = 1
    private var viewportHeight = 1
    private var lastFovDeg = 68f
    private var elapsedSec = 0f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        val fog = simulation.mapId.fogRgb
        GLES30.glClearColor(fog.first, fog.second, fog.third, 1f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        litShader = LitShaderProgram()
        unlitShader = UnlitShaderProgram()

        skyDomeMesh = MeshBuilder(4096).apply {
            addSkyDome(140f, simulation.mapId.skyTopRgb, simulation.mapId.skyHorizonRgb)
        }.build()
        sunDiscMesh = MeshBuilder(128).apply {
            addCenteredBox(0f, 0f, 0f, 9.5f, 9.5f, 9.5f, 1.0f, 0.94f, 0.78f)
        }.build()
        staticMapMesh = simulation.blueprint.staticVertexMeshBuilder.build()

        barrelMesh = MeshBuilder(1024).apply {
            addCylinder(0f, 0f, 0f, 0.44f, 1.32f, 10, 0.78f, 0.14f, 0.12f, 0.92f, 0.22f, 0.16f)
            addCylinder(0f, 0.52f, 0f, 0.46f, 0.24f, 10, 0.95f, 0.65f, 0.10f)
        }.build()

        unitCubeMesh = MeshBuilder(256).apply {
            addBox(-0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f, 1f, 1f, 1f)
        }.build()

        turretBaseMesh = MeshBuilder(512).apply {
            addCylinder(0f, 0f, 0f, 0.72f, 0.45f, 8, 0.26f, 0.30f, 0.36f)
            addCylinder(0f, 0.45f, 0f, 0.32f, 0.75f, 8, 0.20f, 0.23f, 0.28f)
        }.build()
        turretHeadMesh = MeshBuilder(512).apply {
            addBox(-0.36f, -0.22f, -0.45f, 0.36f, 0.26f, 0.45f, 0.30f, 0.34f, 0.40f)
            addBox(-0.22f, -0.05f, -1.15f, -0.10f, 0.08f, -0.40f, 0.14f, 0.16f, 0.20f)
            addBox(0.10f, -0.05f, -1.15f, 0.22f, 0.08f, -0.40f, 0.14f, 0.16f, 0.20f)
            addBox(-0.14f, 0.26f, -0.14f, 0.14f, 0.42f, 0.14f, 1f, 1f, 1f)
        }.build()

        supplyCrystalMesh = MeshBuilder(256).apply {
            addBox(-0.32f, -0.32f, -0.32f, 0.32f, 0.32f, 0.32f, 0.10f, 0.85f, 0.55f)
            addBox(-0.12f, -0.44f, -0.12f, 0.12f, 0.44f, 0.12f, 0.95f, 0.98f, 1.0f)
        }.build()

        // Detailed articulated soldier armor & gear
        soldierTorsoMesh = MeshBuilder(512).apply {
            addBox(-0.30f, 0.68f, -0.17f, 0.30f, 1.38f, 0.17f, 0.85f, 0.88f, 0.92f)
            addBox(-0.35f, 0.98f, -0.21f, 0.35f, 1.36f, 0.21f, 0.24f, 0.27f, 0.32f) // Plate carrier
            addBox(-0.44f, 1.16f, -0.16f, -0.31f, 1.38f, 0.16f, 0.32f, 0.36f, 0.42f) // Left shoulder pad
            addBox(0.31f, 1.16f, -0.16f, 0.44f, 1.38f, 0.16f, 0.32f, 0.36f, 0.42f) // Right shoulder pad
            addBox(-0.22f, 0.82f, 0.17f, 0.22f, 1.30f, 0.30f, 0.20f, 0.22f, 0.26f) // Tactical backpack
        }.build()
        soldierHeadMesh = MeshBuilder(256).apply {
            addBox(-0.18f, 1.42f, -0.18f, 0.18f, 1.78f, 0.18f, 0.22f, 0.25f, 0.30f)
            addBox(-0.15f, 1.52f, -0.20f, 0.15f, 1.63f, -0.16f, 1f, 1f, 1f)
        }.build()
        soldierLegMesh = MeshBuilder(128).apply {
            addBox(-0.11f, -0.68f, -0.11f, 0.11f, 0f, 0.11f, 0.20f, 0.22f, 0.26f)
            addBox(-0.13f, -0.36f, -0.14f, 0.13f, -0.20f, 0.12f, 0.30f, 0.33f, 0.38f) // Knee pad
        }.build()
        soldierRifleMesh = MeshBuilder(128).apply {
            addBox(0.22f, 1.06f, -0.64f, 0.30f, 1.16f, 0.12f, 0.14f, 0.15f, 0.18f)
        }.build()

        primaryViewModelMesh = buildWeaponViewModel(
            weapon = simulation.primaryWeapon,
            optic = simulation.primaryOptic,
            muzzle = simulation.primaryMuzzle,
            camo = simulation.primaryCamo
        )
        secondaryViewModelMesh = buildWeaponViewModel(
            weapon = simulation.secondaryWeapon,
            optic = OpticAttachment.IRON_SIGHTS,
            muzzle = MuzzleAttachment.STANDARD,
            camo = simulation.secondaryCamo
        )
        handsViewModelMesh = MeshBuilder(256).apply {
            // Gloved tactical right hand & forearm
            addBox(-0.042f, -0.18f, -0.02f, 0.042f, -0.05f, 0.22f, 0.16f, 0.18f, 0.21f)
            // Tactical left support forearm
            addBox(-0.16f, -0.16f, -0.28f, -0.045f, -0.045f, 0.08f, 0.18f, 0.21f, 0.24f)
        }.build()

        muzzleFlashMesh = MeshBuilder(128).apply {
            addBox(-0.10f, -0.10f, -0.10f, 0.10f, 0.10f, 0.10f, 1.0f, 0.88f, 0.35f)
            addBox(-0.18f, -0.03f, -0.03f, 0.18f, 0.03f, 0.03f, 1.0f, 0.68f, 0.18f)
            addBox(-0.03f, -0.18f, -0.03f, 0.03f, 0.18f, 0.03f, 1.0f, 0.68f, 0.18f)
        }.build()

        zonePillarMesh = MeshBuilder(256).apply {
            addCylinder(0f, 0f, 0f, 4.3f, 0.14f, 16, 1f, 1f, 1f)
            addCylinder(0f, 0.14f, 0f, 0.16f, 3.8f, 8, 1f, 1f, 1f)
        }.build()

        lastFrameNs = System.nanoTime()
    }

    private fun buildWeaponViewModel(
        weapon: WeaponType,
        optic: OpticAttachment,
        muzzle: MuzzleAttachment,
        camo: WeaponCamo
    ): GlMesh {
        val mb = MeshBuilder(1024)
        val (cr, cg, cb) = camo.rgb
        val (ar, ag, ab) = camo.accentRgb
        var muzzleZ = -0.68f

        when (weapon) {
            WeaponType.M4A1 -> {
                mb.addBox(-0.045f, -0.055f, -0.35f, 0.045f, 0.04f, 0.22f, cr, cg, cb)
                mb.addBox(-0.038f, 0.038f, -0.33f, 0.038f, 0.048f, 0.18f, ar, ag, ab) // Top picatinny rail
                mb.addBox(-0.018f, -0.01f, -0.68f, 0.018f, 0.025f, -0.35f, 0.12f, 0.13f, 0.15f)
                mb.addBox(-0.022f, -0.16f, -0.14f, 0.022f, -0.055f, -0.05f, ar, ag, ab)
                muzzleZ = -0.68f
            }
            WeaponType.MP9 -> {
                mb.addBox(-0.042f, -0.05f, -0.26f, 0.042f, 0.035f, 0.18f, cr, cg, cb)
                mb.addBox(-0.024f, -0.01f, -0.44f, 0.024f, 0.025f, -0.26f, 0.18f, 0.20f, 0.24f)
                mb.addBox(-0.018f, -0.19f, -0.04f, 0.018f, -0.05f, 0.03f, ar, ag, ab)
                muzzleZ = -0.44f
            }
            WeaponType.AX50 -> {
                mb.addBox(-0.048f, -0.06f, -0.38f, 0.048f, 0.04f, 0.25f, cr, cg, cb)
                mb.addBox(-0.018f, -0.005f, -0.92f, 0.018f, 0.03f, -0.38f, 0.14f, 0.15f, 0.17f)
                mb.addBox(-0.035f, 0.045f, -0.20f, 0.035f, 0.105f, 0.12f, 0.11f, 0.12f, 0.14f)
                mb.addBox(-0.025f, 0.055f, 0.12f, 0.025f, 0.095f, 0.13f, ar, ag, ab)
                muzzleZ = -0.92f
            }
            WeaponType.M870 -> {
                mb.addBox(-0.05f, -0.055f, -0.32f, 0.05f, 0.042f, 0.24f, cr, cg, cb)
                mb.addBox(-0.022f, 0.005f, -0.68f, 0.022f, 0.038f, -0.32f, 0.15f, 0.16f, 0.18f)
                mb.addBox(-0.034f, -0.048f, -0.58f, 0.034f, -0.005f, -0.34f, ar, ag, ab)
                muzzleZ = -0.68f
            }
            WeaponType.P226 -> {
                mb.addBox(-0.028f, -0.01f, -0.26f, 0.028f, 0.042f, 0.14f, cr, cg, cb) // Slide
                mb.addBox(-0.026f, -0.048f, -0.24f, 0.026f, -0.01f, 0.12f, 0.14f, 0.15f, 0.18f) // Frame
                mb.addBox(-0.012f, 0.042f, 0.08f, 0.012f, 0.054f, 0.12f, ar, ag, ab) // Tritium rear sight
                muzzleZ = -0.26f
            }
            WeaponType.DEAGLE -> {
                mb.addBox(-0.034f, -0.015f, -0.32f, 0.034f, 0.055f, 0.16f, cr, cg, cb) // Heavy slide
                mb.addBox(-0.030f, -0.055f, -0.28f, 0.030f, -0.015f, 0.14f, 0.20f, 0.22f, 0.26f)
                mb.addBox(-0.014f, 0.055f, 0.09f, 0.014f, 0.068f, 0.14f, 0.95f, 0.65f, 0.12f)
                muzzleZ = -0.32f
            }
            WeaponType.G18 -> {
                mb.addBox(-0.026f, -0.01f, -0.24f, 0.026f, 0.038f, 0.13f, cr, cg, cb)
                mb.addBox(-0.024f, -0.15f, 0.02f, 0.024f, -0.01f, 0.10f, ar, ag, ab) // Extended mag
                muzzleZ = -0.24f
            }
        }

        // Optic geometry for primary weapons
        if (weapon != WeaponType.AX50 && weapon.slot == WeaponSlot.PRIMARY) {
            when (optic) {
                OpticAttachment.HOLO_CYAN -> {
                    mb.addBox(-0.028f, 0.046f, -0.08f, 0.028f, 0.092f, 0.06f, 0.16f, 0.18f, 0.22f)
                    mb.addBox(-0.015f, 0.058f, -0.04f, 0.015f, 0.078f, 0.03f, 0.08f, 0.90f, 1.0f)
                }
                OpticAttachment.MARKSMAN_OPTIC -> {
                    mb.addBox(-0.026f, 0.048f, -0.14f, 0.026f, 0.098f, 0.09f, 0.14f, 0.16f, 0.20f)
                    mb.addBox(-0.018f, 0.056f, 0.09f, 0.018f, 0.090f, 0.10f, 0.96f, 0.65f, 0.15f)
                }
                OpticAttachment.IRON_SIGHTS -> {
                    mb.addBox(-0.010f, 0.044f, 0.08f, 0.010f, 0.062f, 0.12f, 0.22f, 0.24f, 0.28f)
                }
            }
        }

        // Muzzle attachment geometry
        if (weapon.slot == WeaponSlot.PRIMARY) {
            when (muzzle) {
                MuzzleAttachment.SUPPRESSOR -> {
                    mb.addBox(-0.026f, -0.016f, muzzleZ - 0.18f, 0.026f, 0.032f, muzzleZ, 0.10f, 0.11f, 0.13f)
                }
                MuzzleAttachment.COMPENSATOR -> {
                    mb.addBox(-0.024f, -0.014f, muzzleZ - 0.09f, 0.024f, 0.030f, muzzleZ, 0.34f, 0.36f, 0.40f)
                }
                MuzzleAttachment.STANDARD -> {}
            }
        }

        return mb.build()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportWidth = width.coerceAtLeast(1)
        viewportHeight = height.coerceAtLeast(1)
        GLES30.glViewport(0, 0, viewportWidth, viewportHeight)
        updateProjection(simulation.currentFovDeg)
    }

    private fun updateProjection(fovDeg: Float) {
        lastFovDeg = fovDeg
        val ratio = viewportWidth.toFloat() / viewportHeight.toFloat()
        Matrix.perspectiveM(projMatrix, 0, fovDeg, ratio, 0.12f, 200f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val nowNs = System.nanoTime()
        val dt = ((nowNs - lastFrameNs) / 1_000_000_000.0).toFloat().coerceIn(0.001f, 0.05f)
        lastFrameNs = nowNs
        elapsedSec += dt

        simulation.step(dt, onHudUpdate)

        if (kotlin.math.abs(simulation.currentFovDeg - lastFovDeg) > 0.05f) {
            updateProjection(simulation.currentFovDeg)
        }

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        val eyeX = simulation.playerX
        val eyeY = simulation.getPlayerEyeHeight()
        val eyeZ = simulation.playerZ
        Math3D.forwardVector(simulation.playerYawDeg, simulation.playerPitchDeg, fwdTmp)
        Matrix.setLookAtM(
            viewMatrix, 0,
            eyeX, eyeY, eyeZ,
            eyeX + fwdTmp[0], eyeY + fwdTmp[1], eyeZ + fwdTmp[2],
            0f, 1f, 0f
        )
        Matrix.multiplyMM(viewProjMatrix, 0, projMatrix, 0, viewMatrix, 0)

        val map = simulation.mapId

        // 1. Sky Dome + Glowing Sun Disc
        GLES30.glDisable(GLES30.GL_CULL_FACE)
        GLES30.glDepthMask(false)
        unlitShader.use()
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, eyeX, 0f, eyeZ)
        Matrix.multiplyMM(mvpMatrix, 0, viewProjMatrix, 0, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(unlitShader.uMvpMatrix, 1, false, mvpMatrix, 0)
        GLES30.glUniform4f(unlitShader.uColorAlpha, 1f, 1f, 1f, 1f)
        skyDomeMesh.draw()

        // Sun / Moon disc aligned to sunDir
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(
            modelMatrix, 0,
            eyeX + map.sunDir.first * 105f,
            map.sunDir.second * 105f,
            eyeZ + map.sunDir.third * 105f
        )
        Matrix.multiplyMM(mvpMatrix, 0, viewProjMatrix, 0, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(unlitShader.uMvpMatrix, 1, false, mvpMatrix, 0)
        GLES30.glUniform4f(unlitShader.uColorAlpha, map.sunColor.first, map.sunColor.second, map.sunColor.third, 0.9f)
        sunDiscMesh.draw()
        GLES30.glDepthMask(true)
        GLES30.glEnable(GLES30.GL_CULL_FACE)

        // 2. Lit World Geometry & Actors
        litShader.use()
        GLES30.glUniform3f(litShader.uSunDir, map.sunDir.first, map.sunDir.second, map.sunDir.third)
        GLES30.glUniform3f(litShader.uSunColor, map.sunColor.first, map.sunColor.second, map.sunColor.third)
        GLES30.glUniform3f(litShader.uSkyAmbient, map.skyHorizonRgb.first * 0.52f + 0.18f, map.skyHorizonRgb.second * 0.52f + 0.20f, map.skyHorizonRgb.third * 0.52f + 0.24f)
        GLES30.glUniform3f(litShader.uGroundAmbient, map.groundPrimaryRgb.first * 0.45f, map.groundPrimaryRgb.second * 0.45f, map.groundPrimaryRgb.third * 0.45f)
        GLES30.glUniform3f(litShader.uFlashPos, simulation.flashPosX, simulation.flashPosY, simulation.flashPosZ)
        GLES30.glUniform3f(litShader.uFlashColor, simulation.flashR, simulation.flashG, simulation.flashB)
        GLES30.glUniform1f(litShader.uFlashIntensity, if (highQuality) simulation.flashIntensity else 0f)
        GLES30.glUniform3f(litShader.uCameraPos, eyeX, eyeY, eyeZ)
        GLES30.glUniform3f(litShader.uFogColor, map.fogRgb.first, map.fogRgb.second, map.fogRgb.third)
        GLES30.glUniform1f(litShader.uFogDensity, if (highQuality) 0.011f else 0.008f)

        // Static Arena Map (single batched draw call with procedural noise + contact AO)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, viewProjMatrix, 0, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(litShader.uModelMatrix, 1, false, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(litShader.uMvpMatrix, 1, false, mvpMatrix, 0)
        GLES30.glUniform3f(litShader.uColorTint, 1f, 1f, 1f)
        GLES30.glUniform1f(litShader.uDetailScale, 0.42f)
        GLES30.glUniform1f(litShader.uContactAo, 1f)
        staticMapMesh.draw()

        GLES30.glUniform1f(litShader.uDetailScale, 0f)
        GLES30.glUniform1f(litShader.uContactAo, 0f)

        // Explosive Barrels
        for (barrel in simulation.barrels) {
            if (barrel.isDestroyed) continue
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, barrel.x, 0f, barrel.z)
            val dmgRatio = barrel.hp.toFloat() / barrel.maxHp
            drawLitMesh(barrelMesh, 1f, 0.4f + 0.6f * dmgRatio, 0.4f + 0.6f * dmgRatio)
        }

        // Breakable Barriers
        for (barrier in simulation.barriers) {
            if (barrier.isDestroyed) continue
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, barrier.x, barrier.height * 0.5f, barrier.z)
            Matrix.scaleM(modelMatrix, 0, barrier.halfW * 2f, barrier.height, barrier.halfD * 2f)
            val hpFrac = (barrier.hp.toFloat() / barrier.maxHp).coerceIn(0.25f, 1f)
            val baseR = if (map == com.example.model.ArenaMapId.URBAN_DISTRICT) 0.62f else 0.72f
            val baseG = if (map == com.example.model.ArenaMapId.URBAN_DISTRICT) 0.44f else 0.56f
            val baseB = if (map == com.example.model.ArenaMapId.URBAN_DISTRICT) 0.22f else 0.32f
            drawLitMesh(unitCubeMesh, baseR * hpFrac, baseG * hpFrac, baseB * hpFrac)
        }

        // Controllable Sentry Turrets
        for (turret in simulation.turrets) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, turret.x, turret.baseY, turret.z)
            drawLitMesh(turretBaseMesh, 1f, 1f, 1f)

            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, turret.x, turret.baseY + 1.32f, turret.z)
            Matrix.rotateM(modelMatrix, 0, -turret.yawDeg, 0f, 1f, 0f)
            when (turret.ownerTeam) {
                1 -> drawLitMesh(turretHeadMesh, 0.20f, 0.85f, 1.0f)
                -1 -> drawLitMesh(turretHeadMesh, 1.0f, 0.28f, 0.28f)
                else -> drawLitMesh(turretHeadMesh, 0.96f, 0.68f, 0.16f)
            }
        }

        // Supply Crates
        for (crate in simulation.supplyCrates) {
            if (crate.cooldownSec > 0f) continue
            Matrix.setIdentityM(modelMatrix, 0)
            val bob = sin(elapsedSec * 3.2f + crate.id) * 0.14f
            Matrix.translateM(modelMatrix, 0, crate.x, 0.85f + bob, crate.z)
            Matrix.rotateM(modelMatrix, 0, elapsedSec * 75f, 0f, 1f, 0f)
            drawLitMesh(supplyCrystalMesh, 1f, 1f, 1f)
        }

        // Articulated 3D Soldiers
        for (s in simulation.soldiers) {
            if (!s.isAlive) continue
            val scale = if (s.isHeavy) 1.24f else 1.0f
            val teamR = when {
                s.hitFlashTimer > 0f -> 1.8f
                s.isAllied -> 0.18f
                s.isHeavy -> 0.95f
                else -> 0.92f
            }
            val teamG = when {
                s.hitFlashTimer > 0f -> 1.8f
                s.isAllied -> 0.78f
                s.isHeavy -> 0.58f
                else -> 0.22f
            }
            val teamB = when {
                s.hitFlashTimer > 0f -> 1.8f
                s.isAllied -> 0.96f
                s.isHeavy -> 0.14f
                else -> 0.20f
            }

            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, s.x, s.y, s.z)
            Matrix.rotateM(modelMatrix, 0, -s.yawDeg, 0f, 1f, 0f)
            Matrix.scaleM(modelMatrix, 0, scale, scale, scale)
            drawLitMesh(soldierTorsoMesh, teamR * 0.75f, teamG * 0.75f, teamB * 0.75f)
            drawLitMesh(soldierHeadMesh, teamR, teamG, teamB)
            drawLitMesh(soldierRifleMesh, 0.8f, 0.8f, 0.8f)

            val swing = sin(s.walkPhase) * 28f
            for (side in -1..1 step 2) {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, s.x, s.y + 0.68f * scale, s.z)
                Matrix.rotateM(modelMatrix, 0, -s.yawDeg, 0f, 1f, 0f)
                Matrix.translateM(modelMatrix, 0, side * 0.16f * scale, 0f, 0f)
                Matrix.rotateM(modelMatrix, 0, swing * side, 1f, 0f, 0f)
                Matrix.scaleM(modelMatrix, 0, scale, scale, scale)
                drawLitMesh(soldierLegMesh, 0.8f, 0.8f, 0.85f)
            }
        }

        // 3. Unlit Translucent Zones, Enemy 3D Health Bars, Tracers & Sparks
        unlitShader.use()
        GLES30.glDisable(GLES30.GL_CULL_FACE)

        // Floating 3D health bars above wounded enemies
        for (s in simulation.soldiers) {
            if (!s.isAlive || s.isAllied || s.hp >= s.maxHp) continue
            val hpPct = (s.hp.toFloat() / s.maxHp).coerceIn(0.05f, 1f)
            val barY = s.y + (if (s.isHeavy) 2.45f else 2.05f)
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, s.x, barY, s.z)
            Matrix.rotateM(modelMatrix, 0, -simulation.playerYawDeg, 0f, 1f, 0f)
            Matrix.scaleM(modelMatrix, 0, 0.9f * hpPct, 0.08f, 0.04f)
            Matrix.multiplyMM(mvpMatrix, 0, viewProjMatrix, 0, modelMatrix, 0)
            GLES30.glUniformMatrix4fv(unlitShader.uMvpMatrix, 1, false, mvpMatrix, 0)
            GLES30.glUniform4f(unlitShader.uColorAlpha, 0.96f, 0.25f, 0.25f, 0.92f)
            unitCubeMesh.draw()
        }

        if (simulation.gameMode == com.example.model.GameMode.ZONE_DOMINATION) {
            for (zone in simulation.zones) {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, zone.x, 0.02f, zone.z)
                Matrix.multiplyMM(mvpMatrix, 0, viewProjMatrix, 0, modelMatrix, 0)
                GLES30.glUniformMatrix4fv(unlitShader.uMvpMatrix, 1, false, mvpMatrix, 0)
                when (zone.ownerTeam) {
                    1 -> GLES30.glUniform4f(unlitShader.uColorAlpha, 0.04f, 0.72f, 0.84f, 0.45f)
                    -1 -> GLES30.glUniform4f(unlitShader.uColorAlpha, 0.94f, 0.27f, 0.27f, 0.45f)
                    else -> GLES30.glUniform4f(unlitShader.uColorAlpha, 0.96f, 0.62f, 0.04f, 0.38f)
                }
                zonePillarMesh.draw()
            }
        }

        for (tracer in simulation.tracers) {
            val mx = (tracer.x0 + tracer.x1) * 0.5f
            val my = (tracer.y0 + tracer.y1) * 0.5f
            val mz = (tracer.z0 + tracer.z1) * 0.5f
            val dx = tracer.x1 - tracer.x0; val dy = tracer.y1 - tracer.y0; val dz = tracer.z1 - tracer.z0
            val len = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(0.1f)
            val yaw = Math3D.angleBetweenDeg(dx, dz)
            val pitch = (kotlin.math.asin((dy / len).coerceIn(-1f, 1f)) * 180.0 / Math.PI).toFloat()

            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, mx, my, mz)
            Matrix.rotateM(modelMatrix, 0, -yaw, 0f, 1f, 0f)
            Matrix.rotateM(modelMatrix, 0, pitch, 1f, 0f, 0f)
            Matrix.scaleM(modelMatrix, 0, 0.055f, 0.055f, len)
            Matrix.multiplyMM(mvpMatrix, 0, viewProjMatrix, 0, modelMatrix, 0)
            GLES30.glUniformMatrix4fv(unlitShader.uMvpMatrix, 1, false, mvpMatrix, 0)
            val alpha = (tracer.life / tracer.maxLife).coerceIn(0f, 1f)
            GLES30.glUniform4f(unlitShader.uColorAlpha, tracer.r, tracer.g, tracer.b, alpha)
            unitCubeMesh.draw()
        }

        for (p in simulation.particles) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, p.x, p.y, p.z)
            Matrix.scaleM(modelMatrix, 0, p.size, p.size, p.size)
            Matrix.multiplyMM(mvpMatrix, 0, viewProjMatrix, 0, modelMatrix, 0)
            GLES30.glUniformMatrix4fv(unlitShader.uMvpMatrix, 1, false, mvpMatrix, 0)
            val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
            GLES30.glUniform4f(unlitShader.uColorAlpha, p.r, p.g, p.b, alpha)
            unitCubeMesh.draw()
        }
        GLES30.glEnable(GLES30.GL_CULL_FACE)

        // 4. First-Person 3D Weapon Viewmodel + Tactical Hands
        if (simulation.isPlayerAlive && !(simulation.isAds && simulation.weapon == WeaponType.AX50)) {
            GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT)
            litShader.use()
            GLES30.glUniform3f(litShader.uCameraPos, 0f, 0f, 0f)
            GLES30.glUniform1f(litShader.uFogDensity, 0f)
            GLES30.glUniform3f(litShader.uFlashPos, 0f, 0.05f, -0.7f)

            val adsFactor = simulation.adsBlend
            val bobX = cos(simulation.weaponBobPhase * 0.5f) * 0.012f * (1f - adsFactor * 0.7f)
            val bobY = kotlin.math.abs(sin(simulation.weaponBobPhase)) * 0.014f * (1f - adsFactor * 0.7f)
            val reloadTilt = if (simulation.isReloading) -32f else 0f
            val swapTilt = -simulation.swapDip * 28f
            val recoilZ = simulation.recoilOffset * 0.08f
            val recoilPitch = simulation.recoilOffset * 6.5f

            Matrix.setIdentityM(modelMatrix, 0)
            val baseX = 0.20f * (1f - adsFactor) + 0.0f * adsFactor + bobX
            val baseY = -0.19f * (1f - adsFactor) - 0.115f * adsFactor - bobY - simulation.swapDip * 0.16f
            val baseZ = -0.41f + recoilZ
            Matrix.translateM(modelMatrix, 0, baseX, baseY, baseZ)
            Matrix.rotateM(modelMatrix, 0, recoilPitch + reloadTilt + swapTilt, 1f, 0f, 0f)

            Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, modelMatrix, 0)
            GLES30.glUniformMatrix4fv(litShader.uModelMatrix, 1, false, modelMatrix, 0)
            GLES30.glUniformMatrix4fv(litShader.uMvpMatrix, 1, false, mvpMatrix, 0)
            GLES30.glUniform3f(litShader.uColorTint, 1f, 1f, 1f)

            val activeMesh = if (simulation.activeSlot == WeaponSlot.PRIMARY) primaryViewModelMesh else secondaryViewModelMesh
            activeMesh.draw()
            handsViewModelMesh.draw()

            if (simulation.muzzleFlashTimer > 0f) {
                unlitShader.use()
                Matrix.translateM(modelMatrix, 0, 0f, 0.01f, -0.78f)
                Matrix.rotateM(modelMatrix, 0, elapsedSec * 900f, 0f, 0f, 1f)
                Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, modelMatrix, 0)
                GLES30.glUniformMatrix4fv(unlitShader.uMvpMatrix, 1, false, mvpMatrix, 0)
                GLES30.glUniform4f(unlitShader.uColorAlpha, 1f, 0.85f, 0.3f, 0.92f)
                muzzleFlashMesh.draw()
            }
        }
    }

    private fun drawLitMesh(mesh: GlMesh, r: Float, g: Float, b: Float) {
        Matrix.multiplyMM(mvpMatrix, 0, viewProjMatrix, 0, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(litShader.uModelMatrix, 1, false, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(litShader.uMvpMatrix, 1, false, mvpMatrix, 0)
        GLES30.glUniform3f(litShader.uColorTint, r, g, b)
        mesh.draw()
    }
}
