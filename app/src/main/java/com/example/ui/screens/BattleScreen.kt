package com.example.ui.screens

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.R
import com.example.engine.GameSimulation
import com.example.engine.IronfrontGLSurfaceView
import com.example.model.GameHudState
import com.example.model.GameMode
import com.example.model.HitMarkerType
import com.example.model.WeaponType
import com.example.ui.theme.AmberMuted
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.CrimsonThreat
import com.example.ui.theme.CyanAllied
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.GunmetalSurface
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SteelBorder
import com.example.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun BattleScreen(
    simulation: GameSimulation,
    hud: GameHudState,
    highQualityGraphics: Boolean,
    hapticsEnabled: Boolean,
    onHudUpdate: (GameHudState) -> Unit,
    onTogglePause: () -> Unit,
    onRematch: () -> Unit,
    onExitBattle: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val glView = remember(simulation) {
        IronfrontGLSurfaceView(context, simulation, highQualityGraphics, onHudUpdate)
    }

    DisposableEffect(lifecycleOwner, glView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> glView.onResume()
                Lifecycle.Event.ON_PAUSE -> glView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            glView.onPause()
        }
    }

    BackHandler {
        if (hud.isMatchOver) onExitBattle()
        else onTogglePause()
    }

    Box(modifier = Modifier.fillMaxSize().background(ObsidianBg)) {
        // 1. OpenGL ES 3.0 3D Viewport
        AndroidView(
            factory = { glView },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Full-screen Aim Look Touch Surface (below HUD controls)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(simulation) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        simulation.addLookDelta(dragAmount.x, dragAmount.y)
                    }
                }
        )

        // 3. Damage Vignette Flash
        if (hud.damageFlashAlpha > 0.02f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CrimsonThreat.copy(alpha = (hud.damageFlashAlpha * 0.38f).coerceIn(0f, 0.45f)))
            )
        }

        // 4. Sniper Scope Overlay when ADS with AX-50
        if (hud.isAds && hud.activeWeapon == WeaponType.AX50 && !hud.isRespawning && !hud.isMatchOver) {
            SniperScopeOverlay()
        }

        // 5. Center Dynamic Crosshair & Hitmarkers
        if (!hud.isRespawning && !hud.isMatchOver && !hud.isPaused) {
            CenterReticle(
                spreadPx = hud.crosshairSpreadPx,
                isSniperAds = hud.isAds && hud.activeWeapon == WeaponType.AX50,
                hitMarkerType = hud.hitMarkerType,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // 6. Safe-Area Tactical HUD
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // TOP HUD ROW: Minimap (left), Scoreboard (center), Kill Feed + Pause (right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                RadarMinimap(
                    hud = hud,
                    modifier = Modifier.size(92.dp)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MatchScoreboardPill(hud)
                    if (hud.gameMode == GameMode.ZONE_DOMINATION && hud.zones.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            hud.zones.forEach { z ->
                                val col = when (z.ownerTeam) {
                                    1 -> CyanAllied
                                    -1 -> CrimsonThreat
                                    else -> AmberPrimary
                                }
                                Surface(
                                    color = Color(0xBB0A0E14),
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(1.dp, col)
                                ) {
                                    Text(
                                        text = z.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = col,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    IconButton(
                        onClick = onTogglePause,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xAA121821))
                            .border(1.dp, SteelBorder, RoundedCornerShape(10.dp))
                            .testTag("pause_button")
                    ) {
                        Icon(Icons.Filled.Pause, contentDescription = stringResource(R.string.cd_pause), tint = Color.White)
                    }
                    Spacer(Modifier.height(4.dp))
                    hud.killFeed.take(3).forEach { kf ->
                        Surface(
                            color = Color(0x990A0E14),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(vertical = 1.dp)
                        ) {
                            Text(
                                text = "${kf.killer} ▸ ${kf.victim}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (kf.killerIsAllied) CyanAllied else CrimsonThreat,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Kill / Hack Notification Banner
            AnimatedVisibility(
                visible = hud.killBannerText != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 74.dp)
            ) {
                Surface(
                    color = Color(0xCC121821),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, AmberPrimary)
                ) {
                    Text(
                        text = hud.killBannerText.orEmpty(),
                        style = MaterialTheme.typography.labelLarge,
                        color = AmberPrimary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            // Interactive Turret Hack Button when nearby
            if (hud.interactPrompt != null && !hud.isRespawning && !hud.isMatchOver) {
                Button(
                    onClick = {
                        if (hapticsEnabled) view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        simulation.triggerInteract()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAllied, contentColor = ObsidianBg),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 148.dp)
                        .height(48.dp)
                        .testTag("interact_turret_button")
                ) {
                    Icon(Icons.Filled.Memory, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(hud.interactPrompt, style = MaterialTheme.typography.labelLarge)
                }
            }

            // BOTTOM HUD: Virtual Thumbstick (left) + Vitals (center) + Combat Buttons (right)
            if (!hud.isMatchOver) {
                VirtualThumbstick(
                    onMove = { x, y ->
                        simulation.moveStickX = x
                        simulation.moveStickY = y
                    },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 12.dp)
                )

                // Vitals & Dual-Weapon Loadout Strip
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .width(178.dp)
                ) {
                    if (hud.isReloading) {
                        Text("RELOADING...", style = MaterialTheme.typography.labelSmall, color = AmberPrimary)
                        LinearProgressIndicator(
                            progress = { hud.reloadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = AmberPrimary,
                            trackColor = Color(0x88121821)
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val priActive = hud.activeSlot == com.example.model.WeaponSlot.PRIMARY
                        Surface(
                            color = if (priActive) Color(0xCC06B6D4) else Color(0xAA0A0E14),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, if (priActive) CyanAllied else SteelBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { simulation.switchWeaponSlot(com.example.model.WeaponSlot.PRIMARY) }
                                .testTag("swap_primary_button")
                        ) {
                            Text(
                                text = "${hud.primaryWeapon.displayName.substringBefore(" ")} ${hud.primaryAmmo}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (priActive) ObsidianBg else Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                        val secActive = hud.activeSlot == com.example.model.WeaponSlot.SECONDARY
                        Surface(
                            color = if (secActive) Color(0xCCF59E0B) else Color(0xAA0A0E14),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, if (secActive) AmberPrimary else SteelBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { simulation.switchWeaponSlot(com.example.model.WeaponSlot.SECONDARY) }
                                .testTag("swap_secondary_button")
                        ) {
                            Text(
                                text = "${hud.secondaryWeapon.displayName.substringBefore(" ")} ${hud.secondaryAmmo}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (secActive) ObsidianBg else Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = Color(0xBB0A0E14),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, SteelBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("HP ${hud.hp}", style = MaterialTheme.typography.labelMedium, color = if (hud.hp > 35) EmeraldReady else CrimsonThreat)
                                Text("AR ${hud.armor}", style = MaterialTheme.typography.labelMedium, color = CyanAllied)
                            }
                            Spacer(Modifier.height(3.dp))
                            LinearProgressIndicator(
                                progress = { (hud.hp.toFloat() / hud.maxHp).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp),
                                color = if (hud.hp > 35) EmeraldReady else CrimsonThreat,
                                trackColor = Color(0xFF1E293B)
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(hud.activeWeapon.displayName.substringBefore(" "), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text("${hud.ammo} / ${hud.maxAmmo}", style = MaterialTheme.typography.labelMedium, color = AmberPrimary)
                            }
                        }
                    }
                }

                // Right Combat Cluster: Fire, ADS, Reload, Crouch, Jump
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TacticalActionButton(
                            icon = Icons.Filled.ArrowUpward,
                            contentDesc = stringResource(R.string.cd_jump),
                            isActive = false,
                            onClick = { simulation.triggerJump() },
                            modifier = Modifier.testTag("jump_button")
                        )
                        TacticalActionButton(
                            icon = Icons.Filled.Refresh,
                            contentDesc = stringResource(R.string.cd_reload),
                            isActive = hud.isReloading,
                            onClick = { simulation.triggerReload() },
                            modifier = Modifier.testTag("reload_button")
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            TacticalActionButton(
                                icon = Icons.Filled.CenterFocusStrong,
                                contentDesc = stringResource(R.string.cd_ads),
                                isActive = hud.isAds,
                                onClick = { simulation.toggleAds() },
                                modifier = Modifier.testTag("ads_button")
                            )
                            Surface(
                                color = if (hud.isCrouching) AmberMuted else Color(0xAA121821),
                                shape = CircleShape,
                                border = BorderStroke(1.dp, if (hud.isCrouching) AmberPrimary else SteelBorder),
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .clickable { simulation.toggleCrouch() }
                                    .testTag("crouch_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (hud.isCrouching) "LOW" else "STD",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (hud.isCrouching) AmberPrimary else Color.White
                                    )
                                }
                            }
                        }

                        // Primary Hold-to-Fire Trigger
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(78.dp)
                                .clip(CircleShape)
                                .background(Color(0xCCEF4444))
                                .border(2.dp, Color(0xFFFCA5A5), CircleShape)
                                .testTag("fire_button")
                                .pointerInput(simulation, hapticsEnabled) {
                                    awaitEachGesture {
                                        awaitFirstDown(requireUnconsumed = false)
                                        if (hapticsEnabled) {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        }
                                        simulation.setFiring(true)
                                        do {
                                            val event = awaitPointerEvent()
                                        } while (event.changes.any { it.pressed })
                                        simulation.setFiring(false)
                                    }
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.GpsFixed,
                                contentDescription = stringResource(R.string.cd_fire),
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }
            }
        }

        // Respawn Overlay
        if (hud.isRespawning && !hud.isMatchOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x990A0E14)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("OPERATOR DOWN", style = MaterialTheme.typography.headlineMedium, color = CrimsonThreat)
                    Spacer(Modifier.height(6.dp))
                    Text("Redeploying in ${hud.respawnSecondsLeft}s...", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
            }
        }

        // Pause Modal
        if (hud.isPaused && !hud.isMatchOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xBB0A0E14)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GunmetalSurface),
                    border = BorderStroke(1.dp, SteelBorder),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("COMBAT PAUSED", style = MaterialTheme.typography.headlineMedium)
                        Text(hud.mapId.displayName, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Button(
                            onClick = onTogglePause,
                            colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary, contentColor = ObsidianBg),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("RESUME MATCH")
                        }
                        OutlinedButton(
                            onClick = onExitBattle,
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("RETREAT TO BASE")
                        }
                    }
                }
            }
        }

        // Post-Match Competitive Debrief
        if (hud.isMatchOver) {
            PostMatchDebriefOverlay(
                hud = hud,
                onRematch = onRematch,
                onExit = onExitBattle
            )
        }
    }
}

@Composable
private fun MatchScoreboardPill(hud: GameHudState) {
    val mins = hud.timeRemainingSec / 60
    val secs = hud.timeRemainingSec % 60
    val timeStr = String.format("%d:%02d", mins, secs)
    Surface(
        color = Color(0xCC0A0E14),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, SteelBorder)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${hud.alliedScore}", style = MaterialTheme.typography.titleLarge, color = CyanAllied)
                Text(timeStr, style = MaterialTheme.typography.labelMedium, color = AmberPrimary)
                Text("${hud.enemyScore}", style = MaterialTheme.typography.titleLarge, color = CrimsonThreat)
            }
            val sub = buildString {
                append(hud.mapId.codeName)
                append(" • ${hud.fps} FPS")
                if (hud.isLanMatch && hud.pingMs > 0) append(" • ${hud.pingMs}ms")
            }
            Text(sub, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

@Composable
private fun RadarMinimap(hud: GameHudState, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color(0xBB0A0E14),
        shape = CircleShape,
        border = BorderStroke(1.5.dp, SteelBorder)
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val rad = size.minDimension / 2f
            drawCircle(color = Color(0x332B384B), radius = rad, style = Stroke(1.2f))
            drawCircle(color = Color(0x222B384B), radius = rad * 0.5f, style = Stroke(1f))

            val yawRad = -hud.playerYawDeg * (PI / 180.0).toFloat()
            val cosY = cos(yawRad)
            val sinY = sin(yawRad)
            val mapRange = 36f

            for (blip in hud.minimapBlips) {
                val dx = blip.x - hud.playerX
                val dz = blip.z - hud.playerZ
                // Rotate into player-facing coordinates
                val rx = dx * cosY - dz * sinY
                val rz = dx * sinY + dz * cosY
                val nx = (rx / mapRange).coerceIn(-0.92f, 0.92f)
                val ny = (rz / mapRange).coerceIn(-0.92f, 0.92f)
                val distNorm = hypot(nx, ny)
                val scale = if (distNorm > 0.92f) 0.92f / distNorm else 1f
                val px = center.x + nx * scale * rad
                val py = center.y + ny * scale * rad

                val color = when {
                    blip.isInteractive -> when (blip.ownerTeam) {
                        1 -> CyanAllied
                        -1 -> CrimsonThreat
                        else -> AmberPrimary
                    }
                    blip.isObjective -> when (blip.ownerTeam) {
                        1 -> CyanAllied
                        -1 -> CrimsonThreat
                        else -> AmberPrimary
                    }
                    blip.isAllied -> CyanAllied
                    else -> CrimsonThreat
                }
                drawCircle(color = color, radius = if (blip.isObjective || blip.isInteractive) 5f else 3.8f, center = Offset(px, py))
            }

            // Player center directional triangle
            val arrow = Path().apply {
                moveTo(center.x, center.y - 6f)
                lineTo(center.x - 4.5f, center.y + 5f)
                lineTo(center.x + 4.5f, center.y + 5f)
                close()
            }
            drawPath(arrow, color = Color.White)
        }
    }
}

@Composable
private fun CenterReticle(
    spreadPx: Float,
    isSniperAds: Boolean,
    hitMarkerType: HitMarkerType,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(84.dp)) {
        val c = Offset(size.width / 2f, size.height / 2f)
        if (!isSniperAds) {
            val g = spreadPx
            val len = 10f
            drawLine(Color.White.copy(alpha = 0.85f), Offset(c.x - g - len, c.y), Offset(c.x - g, c.y), strokeWidth = 2.2f)
            drawLine(Color.White.copy(alpha = 0.85f), Offset(c.x + g, c.y), Offset(c.x + g + len, c.y), strokeWidth = 2.2f)
            drawLine(Color.White.copy(alpha = 0.85f), Offset(c.x, c.y - g - len), Offset(c.x, c.y - g), strokeWidth = 2.2f)
            drawLine(Color.White.copy(alpha = 0.85f), Offset(c.x, c.y + g), Offset(c.x, c.y + g + len), strokeWidth = 2.2f)
            drawCircle(AmberPrimary, radius = 2.2f, center = c)
        }

        if (hitMarkerType != HitMarkerType.NONE) {
            val col = when (hitMarkerType) {
                HitMarkerType.KILL -> CrimsonThreat
                HitMarkerType.HEADSHOT -> AmberPrimary
                else -> Color.White
            }
            val inner = 8f
            val outer = 18f
            drawLine(col, Offset(c.x - outer, c.y - outer), Offset(c.x - inner, c.y - inner), strokeWidth = 3f)
            drawLine(col, Offset(c.x + outer, c.y - outer), Offset(c.x + inner, c.y - inner), strokeWidth = 3f)
            drawLine(col, Offset(c.x - outer, c.y + outer), Offset(c.x - inner, c.y + inner), strokeWidth = 3f)
            drawLine(col, Offset(c.x + outer, c.y + outer), Offset(c.x + inner, c.y + inner), strokeWidth = 3f)
        }
    }
}

@Composable
private fun SniperScopeOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val scopeRadius = size.minDimension * 0.36f
        // Outer dark optic ring
        drawCircle(
            color = Color(0xEE05070B),
            radius = size.maxDimension,
            center = c,
            style = Stroke(width = (size.maxDimension - scopeRadius) * 2f)
        )
        drawCircle(color = AmberPrimary.copy(alpha = 0.7f), radius = scopeRadius, center = c, style = Stroke(3f))
        drawLine(Color.Black.copy(alpha = 0.8f), Offset(c.x - scopeRadius, c.y), Offset(c.x + scopeRadius, c.y), strokeWidth = 2f)
        drawLine(Color.Black.copy(alpha = 0.8f), Offset(c.x, c.y - scopeRadius), Offset(c.x, c.y + scopeRadius), strokeWidth = 2f)
        drawCircle(CrimsonThreat, radius = 3.5f, center = c)
    }
}

@Composable
private fun VirtualThumbstick(
    onMove: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var knobX by remember { mutableFloatStateOf(0f) }
    var knobY by remember { mutableFloatStateOf(0f) }
    val maxRadiusPx = 95f

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(124.dp)
            .clip(CircleShape)
            .background(Color(0x55121821))
            .border(1.5.dp, SteelBorder, CircleShape)
            .testTag("movement_joystick")
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    val center = Offset(size.width / 2f, size.height / 2f)
                    while (true) {
                        val event = awaitPointerEvent()
                        val ptr = event.changes.firstOrNull() ?: break
                        if (!ptr.pressed) {
                            knobX = 0f; knobY = 0f
                            onMove(0f, 0f)
                            break
                        }
                        ptr.consume()
                        val dx = ptr.position.x - center.x
                        val dy = ptr.position.y - center.y
                        val dist = hypot(dx, dy).coerceAtLeast(1f)
                        val clamped = dist.coerceAtMost(maxRadiusPx)
                        knobX = (dx / dist) * clamped
                        knobY = (dy / dist) * clamped
                        onMove((knobX / maxRadiusPx).coerceIn(-1f, 1f), (knobY / maxRadiusPx).coerceIn(-1f, 1f))
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(knobX.roundToInt(), knobY.roundToInt()) }
                .size(48.dp)
                .clip(CircleShape)
                .background(CyanAllied.copy(alpha = 0.72f))
                .border(1.5.dp, Color.White, CircleShape)
        )
    }
}

@Composable
private fun TacticalActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDesc: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isActive) AmberMuted else Color(0xAA121821),
        shape = CircleShape,
        border = BorderStroke(1.dp, if (isActive) AmberPrimary else SteelBorder),
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDesc,
                tint = if (isActive) AmberPrimary else Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun PostMatchDebriefOverlay(
    hud: GameHudState,
    onRematch: () -> Unit,
    onExit: () -> Unit
) {
    val acc = if (hud.matchShotsFired > 0) ((hud.matchShotsHit.toFloat() / hud.matchShotsFired) * 100f).roundToInt() else 0
    val deltaStr = if (hud.lastMmrDelta >= 0) "+${hud.lastMmrDelta}" else "${hud.lastMmrDelta}"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xD90A0E14)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = GunmetalSurface),
            border = BorderStroke(1.5.dp, if (hud.isVictory) EmeraldReady else CrimsonThreat),
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (hud.isVictory) "VICTORY" else "DEFEAT",
                    style = MaterialTheme.typography.displaySmall,
                    color = if (hud.isVictory) EmeraldReady else CrimsonThreat
                )
                Text(
                    text = "${hud.mapId.displayName} • ${hud.gameMode.displayName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DebriefMetric("KILLS", "${hud.matchKills}")
                    DebriefMetric("DEATHS", "${hud.matchDeaths}")
                    DebriefMetric("HEADSHOTS", "${hud.matchHeadshots}")
                    DebriefMetric("ACCURACY", "$acc%")
                }
                Text(
                    text = "COMPETITIVE RATING: $deltaStr MMR",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (hud.lastMmrDelta >= 0) EmeraldReady else CrimsonThreat
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onExit,
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("BASE")
                    }
                    Button(
                        onClick = onRematch,
                        colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary, contentColor = ObsidianBg),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Filled.Replay, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("REMATCH")
                    }
                }
            }
        }
    }
}

@Composable
private fun DebriefMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.titleLarge, color = Color.White)
    }
}
