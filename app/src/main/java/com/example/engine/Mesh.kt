package com.example.engine

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class GlMesh(vertices: FloatArray) {
    val vertexCount: Int = vertices.size / FLOATS_PER_VERTEX
    private val vboId: Int

    init {
        val buffers = IntArray(1)
        GLES30.glGenBuffers(1, buffers, 0)
        vboId = buffers[0]
        val fb = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
        fb.position(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.size * 4, fb, GLES30.GL_STATIC_DRAW)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    fun draw() {
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, STRIDE_BYTES, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, STRIDE_BYTES, 12)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 3, GLES30.GL_FLOAT, false, STRIDE_BYTES, 24)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, vertexCount)

        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
        GLES30.glDisableVertexAttribArray(2)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    fun release() {
        GLES30.glDeleteBuffers(1, intArrayOf(vboId), 0)
    }

    companion object {
        const val FLOATS_PER_VERTEX = 9 // x, y, z, nx, ny, nz, r, g, b
        const val STRIDE_BYTES = FLOATS_PER_VERTEX * 4
    }
}

class MeshBuilder(initialCapacity: Int = 2048) {
    private val data = ArrayList<Float>(initialCapacity)

    private fun pushVertex(
        x: Float, y: Float, z: Float,
        nx: Float, ny: Float, nz: Float,
        r: Float, g: Float, b: Float
    ) {
        data.add(x); data.add(y); data.add(z)
        data.add(nx); data.add(ny); data.add(nz)
        data.add(r); data.add(g); data.add(b)
    }

    private fun pushQuad(
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        nx: Float, ny: Float, nz: Float,
        r: Float, g: Float, b: Float
    ) {
        pushVertex(x0, y0, z0, nx, ny, nz, r, g, b)
        pushVertex(x1, y1, z1, nx, ny, nz, r, g, b)
        pushVertex(x2, y2, z2, nx, ny, nz, r, g, b)
        pushVertex(x0, y0, z0, nx, ny, nz, r, g, b)
        pushVertex(x2, y2, z2, nx, ny, nz, r, g, b)
        pushVertex(x3, y3, z3, nx, ny, nz, r, g, b)
    }

    fun addBox(
        minX: Float, minY: Float, minZ: Float,
        maxX: Float, maxY: Float, maxZ: Float,
        r: Float, g: Float, b: Float,
        topR: Float = (r * 1.12f).coerceAtMost(1f),
        topG: Float = (g * 1.12f).coerceAtMost(1f),
        topB: Float = (b * 1.12f).coerceAtMost(1f)
    ) {
        // Front (+Z)
        pushQuad(minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, 0f, 0f, 1f, r, g, b)
        // Back (-Z)
        pushQuad(maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, 0f, 0f, -1f, r * 0.92f, g * 0.92f, b * 0.92f)
        // Left (-X)
        pushQuad(minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, -1f, 0f, 0f, r * 0.88f, g * 0.88f, b * 0.88f)
        // Right (+X)
        pushQuad(maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, 1f, 0f, 0f, r * 0.95f, g * 0.95f, b * 0.95f)
        // Top (+Y)
        pushQuad(minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ, 0f, 1f, 0f, topR, topG, topB)
        // Bottom (-Y)
        pushQuad(minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, 0f, -1f, 0f, r * 0.65f, g * 0.65f, b * 0.65f)
    }

    fun addCenteredBox(cx: Float, cy: Float, cz: Float, sx: Float, sy: Float, sz: Float, r: Float, g: Float, b: Float) {
        val hx = sx * 0.5f
        val hy = sy * 0.5f
        val hz = sz * 0.5f
        addBox(cx - hx, cy - hy, cz - hz, cx + hx, cy + hy, cz + hz, r, g, b)
    }

    fun addCylinder(
        cx: Float, bottomY: Float, cz: Float,
        radius: Float, height: Float,
        segments: Int,
        r: Float, g: Float, b: Float,
        bandR: Float = r, bandG: Float = g, bandB: Float = b
    ) {
        val topY = bottomY + height
        for (i in 0 until segments) {
            val a0 = (i.toFloat() / segments) * (2.0 * PI)
            val a1 = ((i + 1).toFloat() / segments) * (2.0 * PI)
            val x0 = cx + radius * cos(a0).toFloat()
            val z0 = cz + radius * sin(a0).toFloat()
            val x1 = cx + radius * cos(a1).toFloat()
            val z1 = cz + radius * sin(a1).toFloat()
            val nx = cos((a0 + a1) * 0.5).toFloat()
            val nz = sin((a0 + a1) * 0.5).toFloat()

            // Side
            val sr = if (i % 2 == 0) r else bandR
            val sg = if (i % 2 == 0) g else bandG
            val sb = if (i % 2 == 0) b else bandB
            pushQuad(x0, bottomY, z0, x1, bottomY, z1, x1, topY, z1, x0, topY, z0, nx, 0f, nz, sr, sg, sb)

            // Top cap
            pushVertex(cx, topY, cz, 0f, 1f, 0f, r * 1.1f, g * 1.1f, b * 1.1f)
            pushVertex(x0, topY, z0, 0f, 1f, 0f, r * 1.1f, g * 1.1f, b * 1.1f)
            pushVertex(x1, topY, z1, 0f, 1f, 0f, r * 1.1f, g * 1.1f, b * 1.1f)
        }
    }

    fun addSkyDome(
        radius: Float,
        topRgb: Triple<Float, Float, Float>,
        horizonRgb: Triple<Float, Float, Float>
    ) {
        val rings = 8
        val segs = 16
        for (ring in 0 until rings) {
            val lat0 = (ring.toFloat() / rings) * (PI * 0.55)
            val lat1 = ((ring + 1).toFloat() / rings) * (PI * 0.55)
            val y0 = radius * sin(lat0).toFloat() - 6f
            val y1 = radius * sin(lat1).toFloat() - 6f
            val r0 = radius * cos(lat0).toFloat()
            val r1 = radius * cos(lat1).toFloat()
            val t0 = (ring.toFloat() / rings)
            val t1 = ((ring + 1).toFloat() / rings)
            val c0r = horizonRgb.first + (topRgb.first - horizonRgb.first) * t0
            val c0g = horizonRgb.second + (topRgb.second - horizonRgb.second) * t0
            val c0b = horizonRgb.third + (topRgb.third - horizonRgb.third) * t0
            val c1r = horizonRgb.first + (topRgb.first - horizonRgb.first) * t1
            val c1g = horizonRgb.second + (topRgb.second - horizonRgb.second) * t1
            val c1b = horizonRgb.third + (topRgb.third - horizonRgb.third) * t1

            for (s in 0 until segs) {
                val lon0 = (s.toFloat() / segs) * (2.0 * PI)
                val lon1 = ((s + 1).toFloat() / segs) * (2.0 * PI)
                val x00 = r0 * cos(lon0).toFloat(); val z00 = r0 * sin(lon0).toFloat()
                val x01 = r0 * cos(lon1).toFloat(); val z01 = r0 * sin(lon1).toFloat()
                val x10 = r1 * cos(lon0).toFloat(); val z10 = r1 * sin(lon0).toFloat()
                val x11 = r1 * cos(lon1).toFloat(); val z11 = r1 * sin(lon1).toFloat()

                pushVertex(x00, y0, z00, 0f, -1f, 0f, c0r, c0g, c0b)
                pushVertex(x10, y1, z10, 0f, -1f, 0f, c1r, c1g, c1b)
                pushVertex(x11, y1, z11, 0f, -1f, 0f, c1r, c1g, c1b)

                pushVertex(x00, y0, z00, 0f, -1f, 0f, c0r, c0g, c0b)
                pushVertex(x11, y1, z11, 0f, -1f, 0f, c1r, c1g, c1b)
                pushVertex(x01, y0, z01, 0f, -1f, 0f, c0r, c0g, c0b)
            }
        }
    }

    fun build(): GlMesh = GlMesh(data.toFloatArray())
}
