package com.example.engine

import com.example.model.ArenaMapId

data class ExplosiveBarrel(
    val id: Int,
    val x: Float,
    val z: Float,
    var hp: Int = 45,
    val maxHp: Int = 45,
    var isDestroyed: Boolean = false,
    var respawnTimer: Float = 0f,
    val aabb: Aabb = Aabb(x - 0.55f, 0f, z - 0.55f, x + 0.55f, 1.45f, z + 0.55f)
)

data class BreakableBarrier(
    val id: Int,
    val x: Float,
    val z: Float,
    val halfW: Float,
    val height: Float,
    val halfD: Float,
    var hp: Int = 95,
    val maxHp: Int = 95,
    var isDestroyed: Boolean = false,
    val aabb: Aabb = Aabb(x - halfW, 0f, z - halfD, x + halfW, height, z + halfD)
)

data class ControllableTurret(
    val id: Int,
    val label: String,
    val x: Float,
    val baseY: Float,
    val z: Float,
    var ownerTeam: Int = 0,
    var yawDeg: Float = 0f,
    var fireCooldown: Float = 0f,
    var hackProgress: Float = 0f,
    val aabb: Aabb = Aabb(x - 0.8f, 0f, z - 0.8f, x + 0.8f, baseY + 1.8f, z + 0.8f)
)

data class SupplyCrate(
    val id: Int,
    val x: Float,
    val z: Float,
    var cooldownSec: Float = 0f
)

data class CaptureZone(
    val label: String,
    val x: Float,
    val z: Float,
    val radius: Float = 4.5f,
    var ownerTeam: Int = 0,
    var progress: Float = 0f
)

data class ArenaBlueprint(
    val mapId: ArenaMapId,
    val boundsHalfSize: Float,
    val staticColliders: List<Aabb>,
    val alliedSpawns: List<Pair<Float, Float>>,
    val enemySpawns: List<Pair<Float, Float>>,
    val barrels: List<ExplosiveBarrel>,
    val barriers: List<BreakableBarrier>,
    val turrets: List<ControllableTurret>,
    val supplyCrates: List<SupplyCrate>,
    val zones: List<CaptureZone>,
    val staticVertexMeshBuilder: MeshBuilder
)

object ArenaFactory {
    fun buildBlueprint(mapId: ArenaMapId): ArenaBlueprint {
        return when (mapId) {
            ArenaMapId.URBAN_DISTRICT -> buildUrbanDistrict()
            ArenaMapId.DESERT_OUTPOST -> buildDesertOutpost()
        }
    }

    private fun buildUrbanDistrict(): ArenaBlueprint {
        val half = 36f
        val mb = MeshBuilder(24576)
        val colliders = ArrayList<Aabb>()

        fun addSolidBlock(
            minX: Float, minY: Float, minZ: Float,
            maxX: Float, maxY: Float, maxZ: Float,
            r: Float, g: Float, b: Float
        ) {
            mb.addBox(minX, minY, minZ, maxX, maxY, maxZ, r, g, b)
            colliders.add(Aabb(minX, minY, minZ, maxX, maxY, maxZ))
        }

        // Asphalt cross-avenues + raised concrete plaza sidewalks
        mb.addBox(-half, -0.3f, -half, half, 0f, half, 0.12f, 0.14f, 0.17f, 0.13f, 0.15f, 0.18f)
        mb.addBox(-half, 0f, -half, -10f, 0.16f, -10f, 0.22f, 0.24f, 0.28f)
        mb.addBox(10f, 0f, -half, half, 0.16f, -10f, 0.22f, 0.24f, 0.28f)
        mb.addBox(-half, 0f, 10f, -10f, 0.16f, half, 0.22f, 0.24f, 0.28f)
        mb.addBox(10f, 0f, 10f, half, 0.16f, half, 0.22f, 0.24f, 0.28f)
        // Crosswalk stripes & lane amber markings
        for (i in -30..30 step 6) {
            mb.addBox(-0.22f, 0.01f, i.toFloat(), 0.22f, 0.03f, i + 2.6f, 0.90f, 0.65f, 0.14f)
            mb.addBox(i.toFloat(), 0.01f, -0.22f, i + 2.6f, 0.03f, 0.22f, 0.90f, 0.65f, 0.14f)
        }
        for (x in -8..8 step 2) {
            mb.addBox(x - 0.45f, 0.01f, 9.2f, x + 0.45f, 0.03f, 10.6f, 0.72f, 0.75f, 0.78f)
            mb.addBox(x - 0.45f, 0.01f, -10.6f, x + 0.45f, 0.03f, -9.2f, 0.72f, 0.75f, 0.78f)
        }

        // Perimeter city walls + distant skyline silhouettes outside the map
        addSolidBlock(-half - 1f, 0f, -half - 1f, half + 1f, 7.5f, -half, 0.16f, 0.19f, 0.24f)
        addSolidBlock(-half - 1f, 0f, half, half + 1f, 7.5f, half + 1f, 0.16f, 0.19f, 0.24f)
        addSolidBlock(-half - 1f, 0f, -half, -half, 7.5f, half, 0.16f, 0.19f, 0.24f)
        addSolidBlock(half, 0f, -half, half + 1f, 7.5f, half, 0.16f, 0.19f, 0.24f)

        // Distant skyline towers outside perimeter for rich depth
        val skylineOffsets = listOf(
            Triple(-52f, -54f, 26f), Triple(-24f, -58f, 34f), Triple(8f, -56f, 29f), Triple(38f, -55f, 38f),
            Triple(-55f, 12f, 31f), Triple(52f, -10f, 35f), Triple(-46f, 54f, 28f), Triple(26f, 56f, 32f)
        )
        for ((sx, sz, h) in skylineOffsets) {
            mb.addBox(sx - 8f, 0f, sz - 8f, sx + 8f, h, sz + 8f, 0.10f, 0.13f, 0.19f)
            mb.addBox(sx - 8.2f, h - 2.5f, sz - 8.2f, sx + 8.2f, h - 1.9f, sz + 8.2f, 0.12f, 0.75f, 0.90f)
        }

        // 4 Urban Mid-Rise Corner Buildings with window bands & rooftop HVAC units
        fun addUrbanTower(minX: Float, minZ: Float, maxX: Float, maxZ: Float, h: Float, r: Float, g: Float, b: Float, accentCyan: Boolean) {
            addSolidBlock(minX, 0f, minZ, maxX, h, maxZ, r, g, b)
            // Rooftop parapet & HVAC
            mb.addBox(minX - 0.2f, h, minZ - 0.2f, maxX + 0.2f, h + 0.55f, maxZ + 0.2f, r * 0.75f, g * 0.75f, b * 0.75f)
            mb.addBox(minX + 2.5f, h + 0.55f, minZ + 2.5f, minX + 6.2f, h + 2.0f, minZ + 6.2f, 0.28f, 0.30f, 0.34f)
            // Illuminated window bands
            val ar = if (accentCyan) 0.10f else 0.96f
            val ag = if (accentCyan) 0.78f else 0.62f
            val ab = if (accentCyan) 0.95f else 0.12f
            var wy = 3.0f
            while (wy < h - 1.5f) {
                mb.addBox(minX - 0.08f, wy, minZ + 1.4f, maxX + 0.08f, wy + 0.55f, maxZ - 1.4f, ar, ag, ab)
                wy += 2.6f
            }
        }
        addUrbanTower(-28f, -28f, -14f, -14f, 11.0f, 0.20f, 0.24f, 0.30f, true)
        addUrbanTower(14f, -28f, 28f, -14f, 9.5f, 0.24f, 0.23f, 0.28f, false)
        addUrbanTower(-28f, 14f, -14f, 28f, 8.8f, 0.22f, 0.25f, 0.29f, false)
        addUrbanTower(14f, 14f, 28f, 28f, 10.5f, 0.19f, 0.24f, 0.31f, true)

        // Streetlamps along plaza corners
        val lampSpots = listOf(-9.2f to -9.2f, 9.2f to -9.2f, -9.2f to 9.2f, 9.2f to 9.2f)
        for ((lx, lz) in lampSpots) {
            mb.addCylinder(lx, 0.16f, lz, 0.12f, 4.4f, 6, 0.22f, 0.25f, 0.30f)
            mb.addBox(lx - 0.35f, 4.45f, lz - 0.35f, lx + 0.35f, 4.75f, lz + 0.35f, 0.98f, 0.82f, 0.42f)
        }

        // Central Elevated Metro Overpass Pillars + Skybridge
        addSolidBlock(-7.5f, 0f, -7.5f, -5.2f, 5.2f, -5.2f, 0.28f, 0.31f, 0.36f)
        addSolidBlock(5.2f, 0f, -7.5f, 7.5f, 5.2f, -5.2f, 0.28f, 0.31f, 0.36f)
        addSolidBlock(-7.5f, 0f, 5.2f, -5.2f, 5.2f, 7.5f, 0.28f, 0.31f, 0.36f)
        addSolidBlock(5.2f, 0f, 5.2f, 7.5f, 5.2f, 7.5f, 0.28f, 0.31f, 0.36f)
        mb.addBox(-8.2f, 5.2f, -8.2f, 8.2f, 6.3f, -4.6f, 0.22f, 0.26f, 0.32f)
        mb.addBox(-8.2f, 5.2f, 4.6f, 8.2f, 6.3f, 8.2f, 0.22f, 0.26f, 0.32f)
        mb.addBox(-8.3f, 5.4f, -8.3f, 8.3f, 5.65f, -8.15f, 0.08f, 0.72f, 0.88f)
        mb.addBox(-8.3f, 5.4f, 8.15f, 8.3f, 5.65f, 8.3f, 0.95f, 0.62f, 0.12f)

        // Detailed Armored Street APCs
        fun addApcVehicle(minX: Float, minZ: Float, maxX: Float, maxZ: Float, r: Float, g: Float, b: Float) {
            addSolidBlock(minX, 0.35f, minZ, maxX, 1.85f, maxZ, r, g, b)
            mb.addBox(minX + 0.6f, 1.85f, minZ + 0.7f, maxX - 0.6f, 2.35f, maxZ - 0.7f, r * 0.85f, g * 0.85f, b * 0.85f)
            mb.addCylinder(minX + 0.35f, 0f, minZ + 0.6f, 0.42f, 0.4f, 8, 0.10f, 0.11f, 0.13f)
            mb.addCylinder(maxX - 0.35f, 0f, minZ + 0.6f, 0.42f, 0.4f, 8, 0.10f, 0.11f, 0.13f)
            mb.addCylinder(minX + 0.35f, 0f, maxZ - 0.6f, 0.42f, 0.4f, 8, 0.10f, 0.11f, 0.13f)
            mb.addCylinder(maxX - 0.35f, 0f, maxZ - 0.6f, 0.42f, 0.4f, 8, 0.10f, 0.11f, 0.13f)
        }
        addApcVehicle(-3.2f, 12.5f, 1.8f, 16.0f, 0.18f, 0.31f, 0.25f)
        addApcVehicle(-1.8f, -16.0f, 3.2f, -12.5f, 0.36f, 0.20f, 0.20f)

        // West & East Street Shipping Containers with ribbed trim
        addSolidBlock(-23f, 0f, -3.5f, -17f, 2.7f, 1.5f, 0.15f, 0.38f, 0.50f)
        addSolidBlock(17f, 0f, -1.5f, 23f, 2.7f, 3.5f, 0.58f, 0.24f, 0.16f)

        // Low Jersey Concrete Barriers in Central Intersection
        addSolidBlock(-4.0f, 0f, -0.7f, -1.5f, 1.15f, 0.7f, 0.44f, 0.47f, 0.50f)
        addSolidBlock(1.5f, 0f, -0.7f, 4.0f, 1.15f, 0.7f, 0.44f, 0.47f, 0.50f)

        val barrels = listOf(
            ExplosiveBarrel(1, -11.5f, -4.0f),
            ExplosiveBarrel(2, 11.5f, 4.0f),
            ExplosiveBarrel(3, -3.8f, 10.5f),
            ExplosiveBarrel(4, 3.8f, -10.5f)
        )
        val barriers = listOf(
            BreakableBarrier(1, -11.0f, 9.5f, 2.2f, 1.75f, 0.45f),
            BreakableBarrier(2, 11.0f, -9.5f, 2.2f, 1.75f, 0.45f),
            BreakableBarrier(3, -15.5f, 6.5f, 0.5f, 1.8f, 2.3f),
            BreakableBarrier(4, 15.5f, -6.5f, 0.5f, 1.8f, 2.3f)
        )
        val turrets = listOf(
            ControllableTurret(1, "PLAZA SENTRY", 0f, 0f, 0f, ownerTeam = 0),
            ControllableTurret(2, "OVERPASS TURRET", 12.0f, 0f, 0f, ownerTeam = 0)
        )
        val crates = listOf(
            SupplyCrate(1, -20f, 8.5f),
            SupplyCrate(2, 20f, -8.5f)
        )
        val zones = listOf(
            CaptureZone("A", -18f, 0f),
            CaptureZone("B", 0f, 0f),
            CaptureZone("C", 18f, 0f)
        )

        return ArenaBlueprint(
            mapId = ArenaMapId.URBAN_DISTRICT,
            boundsHalfSize = half,
            staticColliders = colliders,
            alliedSpawns = listOf(-8f to 27f, 0f to 28f, 8f to 27f, -24f to 22f),
            enemySpawns = listOf(-8f to -27f, 0f to -28f, 8f to -27f, 24f to -22f),
            barrels = barrels,
            barriers = barriers,
            turrets = turrets,
            supplyCrates = crates,
            zones = zones,
            staticVertexMeshBuilder = mb
        )
    }

    private fun buildDesertOutpost(): ArenaBlueprint {
        val half = 40f
        val mb = MeshBuilder(24576)
        val colliders = ArrayList<Aabb>()

        fun addSolidBlock(
            minX: Float, minY: Float, minZ: Float,
            maxX: Float, maxY: Float, maxZ: Float,
            r: Float, g: Float, b: Float
        ) {
            mb.addBox(minX, minY, minZ, maxX, maxY, maxZ, r, g, b)
            colliders.add(Aabb(minX, minY, minZ, maxX, maxY, maxZ))
        }

        // Sun-baked sandstone canyon floor + helipad deck
        mb.addBox(-half, -0.3f, -half, half, 0f, half, 0.56f, 0.44f, 0.29f, 0.58f, 0.46f, 0.30f)
        mb.addBox(-9f, 0.01f, -9f, 9f, 0.09f, 9f, 0.34f, 0.33f, 0.31f)
        mb.addBox(-3.8f, 0.09f, -0.45f, 3.8f, 0.12f, 0.45f, 0.90f, 0.68f, 0.16f)
        mb.addBox(-0.45f, 0.09f, -3.8f, 0.45f, 0.12f, 3.8f, 0.90f, 0.68f, 0.16f)

        // Canyon Perimeter Sandstone Cliffs
        addSolidBlock(-half - 1.5f, 0f, -half - 1.5f, half + 1.5f, 8.5f, -half, 0.52f, 0.38f, 0.24f)
        addSolidBlock(-half - 1.5f, 0f, half, half + 1.5f, 8.5f, half + 1.5f, 0.52f, 0.38f, 0.24f)
        addSolidBlock(-half - 1.5f, 0f, -half, -half, 8.5f, half, 0.48f, 0.35f, 0.22f)
        addSolidBlock(half, 0f, -half, half + 1.5f, 8.5f, half, 0.48f, 0.35f, 0.22f)

        // Distant Stepped Sandstone Mesas outside canyon bounds
        val mesaCoords = listOf(
            Triple(-58f, -56f, 22f), Triple(10f, -62f, 26f), Triple(58f, -50f, 20f),
            Triple(-60f, 18f, 24f), Triple(60f, 20f, 25f), Triple(-20f, 62f, 21f)
        )
        for ((mx, mz, mh) in mesaCoords) {
            mb.addBox(mx - 14f, 0f, mz - 12f, mx + 14f, mh * 0.6f, mz + 12f, 0.50f, 0.36f, 0.22f)
            mb.addBox(mx - 9f, mh * 0.6f, mz - 8f, mx + 9f, mh, mz + 8f, 0.58f, 0.42f, 0.26f)
        }

        // West Sniper Ridge & Radar Watchtower + Dish Assembly
        addSolidBlock(-33f, 0f, -14f, -22f, 4.2f, 14f, 0.49f, 0.38f, 0.26f)
        addSolidBlock(-31f, 4.2f, -4f, -25f, 9.8f, 4f, 0.32f, 0.34f, 0.36f)
        mb.addBox(-32.5f, 9.8f, -5.5f, -23.5f, 10.8f, 5.5f, 0.24f, 0.28f, 0.32f)
        mb.addCylinder(-28f, 10.8f, 0f, 0.5f, 2.2f, 8, 0.36f, 0.38f, 0.42f)
        mb.addBox(-29.5f, 12.6f, -2.5f, -26.5f, 14.5f, 2.5f, 0.78f, 0.82f, 0.86f) // Radar dish reflector

        // East Solar Array & Communications Bunker
        addSolidBlock(21f, 0f, -18f, 33f, 3.4f, -6f, 0.42f, 0.39f, 0.34f)
        mb.addBox(22f, 3.4f, -17f, 32f, 3.85f, -7f, 0.10f, 0.26f, 0.50f)
        mb.addBox(22f, 3.85f, -12.2f, 32f, 3.92f, -11.8f, 0.78f, 0.82f, 0.86f)
        addSolidBlock(21f, 0f, 6f, 33f, 3.4f, 18f, 0.42f, 0.39f, 0.34f)
        mb.addBox(22f, 3.4f, 7f, 32f, 3.85f, 17f, 0.10f, 0.26f, 0.50f)
        mb.addBox(22f, 3.85f, 11.8f, 32f, 3.92f, 12.2f, 0.78f, 0.82f, 0.86f)

        // North & South Fortified Outpost Bunkers
        addSolidBlock(-12f, 0f, -28f, -4f, 3.1f, -23f, 0.46f, 0.41f, 0.32f)
        addSolidBlock(4f, 0f, -28f, 12f, 3.1f, -23f, 0.46f, 0.41f, 0.32f)
        addSolidBlock(-12f, 0f, 23f, -4f, 3.1f, 28f, 0.46f, 0.41f, 0.32f)
        addSolidBlock(4f, 0f, 23f, 12f, 3.1f, 28f, 0.46f, 0.41f, 0.32f)

        // Desert Olive & Tan Military Shipping Containers + Hesco Bastion Walls
        addSolidBlock(-16f, 0f, -6f, -11.5f, 2.6f, 0f, 0.28f, 0.35f, 0.22f)
        addSolidBlock(11.5f, 0f, 0f, 16f, 2.6f, 6f, 0.56f, 0.48f, 0.30f)
        addSolidBlock(-6.5f, 0f, 11f, -2.5f, 1.35f, 13.5f, 0.58f, 0.50f, 0.34f)
        addSolidBlock(2.5f, 0f, -13.5f, 6.5f, 1.35f, -11f, 0.58f, 0.50f, 0.34f)

        val barrels = listOf(
            ExplosiveBarrel(1, -10.5f, -14.0f),
            ExplosiveBarrel(2, 10.5f, 14.0f),
            ExplosiveBarrel(3, -10.5f, 2.0f),
            ExplosiveBarrel(4, 10.5f, -2.0f)
        )
        val barriers = listOf(
            BreakableBarrier(1, 0f, -18.5f, 3.2f, 1.65f, 0.6f),
            BreakableBarrier(2, 0f, 18.5f, 3.2f, 1.65f, 0.6f),
            BreakableBarrier(3, -18.5f, -9.0f, 0.6f, 1.75f, 2.6f),
            BreakableBarrier(4, 18.5f, 0f, 0.6f, 1.75f, 2.8f)
        )
        val turrets = listOf(
            ControllableTurret(1, "HELIPAD CANNON", 0f, 0f, 0f, ownerTeam = 0),
            ControllableTurret(2, "SOLAR SENTRY", 16.5f, 0f, -10.0f, ownerTeam = 0)
        )
        val crates = listOf(
            SupplyCrate(1, -18.5f, 15.0f),
            SupplyCrate(2, 18.5f, -15.0f)
        )
        val zones = listOf(
            CaptureZone("A", -15f, -12f),
            CaptureZone("B", 0f, 0f),
            CaptureZone("C", 15f, 12f)
        )

        return ArenaBlueprint(
            mapId = ArenaMapId.DESERT_OUTPOST,
            boundsHalfSize = half,
            staticColliders = colliders,
            alliedSpawns = listOf(-10f to 33f, 0f to 34f, 10f to 33f, 26f to 28f),
            enemySpawns = listOf(-10f to -33f, 0f to -34f, 10f to -33f, -26f to -28f),
            barrels = barrels,
            barriers = barriers,
            turrets = turrets,
            supplyCrates = crates,
            zones = zones,
            staticVertexMeshBuilder = mb
        )
    }
}
