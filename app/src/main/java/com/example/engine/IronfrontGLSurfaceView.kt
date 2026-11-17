package com.example.engine

import android.content.Context
import android.opengl.GLSurfaceView
import com.example.model.GameHudState

class IronfrontGLSurfaceView(
    context: Context,
    simulation: GameSimulation,
    highQuality: Boolean,
    onHudUpdate: (GameHudState) -> Unit
) : GLSurfaceView(context) {

    init {
        setEGLContextClientVersion(3)
        preserveEGLContextOnPause = true
        setRenderer(WarfareRenderer(simulation, highQuality, onHudUpdate))
        renderMode = RENDERMODE_CONTINUOUSLY
    }
}
