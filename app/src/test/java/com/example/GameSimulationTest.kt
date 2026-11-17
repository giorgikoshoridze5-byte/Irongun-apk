package com.example

import com.example.audio.TacticalAudioEngine
import com.example.engine.ArenaFactory
import com.example.engine.GameSimulation
import com.example.model.ArenaMapId
import com.example.model.GameMode
import com.example.model.MuzzleAttachment
import com.example.model.OpticAttachment
import com.example.model.TacticalPerk
import com.example.model.WeaponCamo
import com.example.model.WeaponSlot
import com.example.model.WeaponType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameSimulationTest {

    private fun createSim(
        mapId: ArenaMapId = ArenaMapId.URBAN_DISTRICT,
        mode: GameMode = GameMode.TEAM_DEATHMATCH,
        primary: WeaponType = WeaponType.M4A1,
        secondary: WeaponType = WeaponType.P226
    ) = GameSimulation(
        mapId = mapId,
        gameMode = mode,
        playerCallsign = "TEST-01",
        primaryWeapon = primary,
        secondaryWeapon = secondary,
        primaryOptic = OpticAttachment.HOLO_CYAN,
        primaryMuzzle = MuzzleAttachment.SUPPRESSOR,
        primaryCamo = WeaponCamo.OBSIDIAN_STEEL,
        secondaryCamo = WeaponCamo.GOLD_ELITE,
        perk = TacticalPerk.KEVLAR_PLATING,
        audioEngine = TacticalAudioEngine(),
        lanManager = null,
        onMatchFinished = {}
    )

    @Test
    fun bothMapsHaveDistinctLayoutsAndInteractiveElements() {
        val urban = ArenaFactory.buildBlueprint(ArenaMapId.URBAN_DISTRICT)
        val desert = ArenaFactory.buildBlueprint(ArenaMapId.DESERT_OUTPOST)

        assertTrue(urban.barrels.size >= 3)
        assertTrue(urban.barriers.size >= 3)
        assertTrue(urban.turrets.size >= 2)
        assertTrue(urban.supplyCrates.size >= 2)

        assertTrue(desert.barrels.size >= 3)
        assertTrue(desert.barriers.size >= 3)
        assertTrue(desert.turrets.size >= 2)
        assertTrue(desert.supplyCrates.size >= 2)

        assertTrue(urban.boundsHalfSize != desert.boundsHalfSize)
    }

    @Test
    fun turretHackFlipsOwnershipWhenPlayerIsNear() {
        val sim = createSim()
        val turret = sim.turrets.first()
        sim.playerX = turret.x + 1.5f
        sim.playerZ = turret.z + 1.5f
        sim.triggerInteract()
        assertEquals(1, turret.ownerTeam)
    }

    @Test
    fun primaryAndSecondaryWeaponsMaintainIndependentMagazinesOnSwap() {
        val sim = createSim(
            mapId = ArenaMapId.DESERT_OUTPOST,
            mode = GameMode.ZONE_DOMINATION,
            primary = WeaponType.AX50,
            secondary = WeaponType.DEAGLE
        )
        assertEquals(WeaponSlot.PRIMARY, sim.activeSlot)
        assertEquals(WeaponType.AX50.magSize, sim.ammo)

        sim.setFiring(true)
        sim.step(0.016f) {}
        sim.setFiring(false)
        assertEquals(WeaponType.AX50.magSize - 1, sim.primaryAmmo)

        sim.switchWeaponSlot(WeaponSlot.SECONDARY)
        assertEquals(WeaponSlot.SECONDARY, sim.activeSlot)
        assertEquals(WeaponType.DEAGLE.magSize, sim.ammo)
        assertTrue(sim.tracers.isNotEmpty())
    }
}
