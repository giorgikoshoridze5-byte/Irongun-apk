package com.example.engine

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

data class Aabb(
    val minX: Float,
    val minY: Float,
    val minZ: Float,
    val maxX: Float,
    val maxY: Float,
    val maxZ: Float
) {
    fun intersectsCylinder(cx: Float, cz: Float, radius: Float, bottomY: Float, topY: Float): Boolean {
        if (topY <= minY || bottomY >= maxY) return false
        val closestX = cx.coerceIn(minX, maxX)
        val closestZ = cz.coerceIn(minZ, maxZ)
        val dx = cx - closestX
        val dz = cz - closestZ
        return (dx * dx + dz * dz) < (radius * radius)
    }

    fun rayIntersect(
        ox: Float, oy: Float, oz: Float,
        dx: Float, dy: Float, dz: Float,
        maxDist: Float
    ): Float? {
        var tMin = 0f
        var tMax = maxDist

        // X slab
        if (kotlin.math.abs(dx) < 1e-6f) {
            if (ox < minX || ox > maxX) return null
        } else {
            val invD = 1f / dx
            var t1 = (minX - ox) * invD
            var t2 = (maxX - ox) * invD
            if (t1 > t2) { val tmp = t1; t1 = t2; t2 = tmp }
            tMin = max(tMin, t1)
            tMax = min(tMax, t2)
            if (tMin > tMax) return null
        }

        // Y slab
        if (kotlin.math.abs(dy) < 1e-6f) {
            if (oy < minY || oy > maxY) return null
        } else {
            val invD = 1f / dy
            var t1 = (minY - oy) * invD
            var t2 = (maxY - oy) * invD
            if (t1 > t2) { val tmp = t1; t1 = t2; t2 = tmp }
            tMin = max(tMin, t1)
            tMax = min(tMax, t2)
            if (tMin > tMax) return null
        }

        // Z slab
        if (kotlin.math.abs(dz) < 1e-6f) {
            if (oz < minZ || oz > maxZ) return null
        } else {
            val invD = 1f / dz
            var t1 = (minZ - oz) * invD
            var t2 = (maxZ - oz) * invD
            if (t1 > t2) { val tmp = t1; t1 = t2; t2 = tmp }
            tMin = max(tMin, t1)
            tMax = min(tMax, t2)
            if (tMin > tMax) return null
        }

        return if (tMin in 0f..maxDist) tMin else null
    }
}

object Math3D {
    const val DEG_TO_RAD = (PI / 180.0).toFloat()

    fun forwardVector(yawDeg: Float, pitchDeg: Float, out: FloatArray) {
        val yawRad = yawDeg * DEG_TO_RAD
        val pitchRad = pitchDeg * DEG_TO_RAD
        val cosPitch = cos(pitchRad)
        out[0] = sin(yawRad) * cosPitch
        out[1] = sin(pitchRad)
        out[2] = -cos(yawRad) * cosPitch
    }

    fun distSq2D(x1: Float, z1: Float, x2: Float, z2: Float): Float {
        val dx = x2 - x1
        val dz = z2 - z1
        return dx * dx + dz * dz
    }

    fun dist2D(x1: Float, z1: Float, x2: Float, z2: Float): Float {
        return sqrt(distSq2D(x1, z1, x2, z2))
    }

    fun angleBetweenDeg(dx: Float, dz: Float): Float {
        return (kotlin.math.atan2(dx.toDouble(), (-dz).toDouble()) * 180.0 / PI).toFloat()
    }

    fun wrapAngleDeg(deg: Float): Float {
        var a = deg % 360f
        if (a > 180f) a -= 360f
        if (a < -180f) a += 360f
        return a
    }
}
