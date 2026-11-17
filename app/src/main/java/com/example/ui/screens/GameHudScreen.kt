package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.game.AnimatronicPartType
import com.example.game.FirstPersonViewport
import com.example.game.GameUiState
import com.example.game.InteractTargetType
import com.example.game.PizzeriaMap
import com.example.game.StalkerBehavior
import com.example.ui.theme.ArcadeCyan
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.BoneWhite
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ElevatedPlum
import com.example.ui.theme.FlashlightAmber
import com.example.ui.theme.MidnightCharcoal
import com.example.ui.theme.MutedAsh
import com.example.ui.theme.PhosphorGreen
import com.example.ui.theme.VoidObsidian
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun GameHudScreen(
    state: GameUiState,
    onMoveInput: (forward: Float, strafe: Float) -> Unit,
    onLookDelta: (Float) -> Unit,
    onToggleFlashlight: () -> Unit,
    onInteract: () -> Unit,
    onToggleRadar: () -> Unit,
    onPause: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidObsidian)
    ) {
        // 1. Fullscreen First-Person 3D Raycaster Canvas
        FirstPersonViewport(state = state)

        // 2. Split Touch Controls Layer (Left = Move Pad, Right = Look Drag Surface)
        Row(modifier = Modifier.fillMaxSize()) {
            // Left half spacer (thumbstick sits on top in bottom-left)
            Box(modifier = Modifier.weight(1f).fillMaxHeight())
            // Right half dedicated smooth look-drag area
            Box(
                modifier = Modifier
                    .weight(1.15f)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onLookDelta(dragAmount.x)
                        }
                    }
            )
        }

        // 3. Center Reticle + Stalker Flashlight Stun Ring
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(38.dp)) {
                val r = size.minDimension * 0.42f
                val center = Offset(size.width * 0.5f, size.height * 0.5f)
                drawCircle(
                    color = BoneWhite.copy(alpha = 0.45f),
                    radius = 2.2.dp.toPx(),
                    center = center
                )
                if (state.interactPrompt.type != InteractTargetType.NONE) {
                    drawCircle(
                        color = FlashlightAmber.copy(alpha = 0.8f),
                        radius = r,
                        center = center,
                        style = Stroke(width = 1.6.dp.toPx())
                    )
                }
                val blindProg = (state.stalker.blindExposure / 0.62f).coerceIn(0f, 1f)
                if (blindProg > 0.04f && state.horrorMode) {
                    drawArc(
                        color = ArcadeCyan,
                        startAngle = -90f,
                        sweepAngle = 360f * blindProg,
                        useCenter = false,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }

        // 4. Safe-area HUD Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // TOP HUD
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Room & Timer Pill
                    Surface(
                        color = MidnightCharcoal.copy(alpha = 0.80f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            1.dp,
                            if (state.stalkerDanger > 0.45f) CrimsonAlert else BorderSlate
                        )
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Canvas(modifier = Modifier.size(7.dp)) {
                                    drawCircle(
                                        color = if (state.stalkerDanger > 0.45f) CrimsonAlert else PhosphorGreen
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = state.currentRoomLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BoneWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "SHIFT ${formatShiftSeconds(state.elapsedSeconds.toInt())}",
                                style = MaterialTheme.typography.labelSmall,
                                color = FlashlightAmber
                            )
                        }
                    }

                    // Right top utility buttons (Radar + Pause)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            color = if (state.radarVisible) ElevatedPlum else MidnightCharcoal.copy(alpha = 0.80f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                1.dp,
                                if (state.radarVisible) ArcadeCyan else BorderSlate
                            )
                        ) {
                            IconButton(
                                onClick = onToggleRadar,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("radar_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = stringResource(R.string.action_map),
                                    tint = if (state.radarVisible) ArcadeCyan else BoneWhite
                                )
                            }
                        }

                        Surface(
                            color = MidnightCharcoal.copy(alpha = 0.80f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, BorderSlate)
                        ) {
                            IconButton(
                                onClick = onPause,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("pause_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Pause,
                                    contentDescription = stringResource(R.string.action_pause),
                                    tint = BoneWhite
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 4-Part Inventory Satchel Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AnimatronicPartType.entries.forEach { partType ->
                        val isInstalled = partType in state.installedParts
                        val isCollected = partType in state.collectedParts
                        val borderColor = when {
                            isInstalled -> PhosphorGreen
                            isCollected -> FlashlightAmber
                            else -> BorderSlate.copy(alpha = 0.7f)
                        }
                        val labelColor = when {
                            isInstalled -> PhosphorGreen
                            isCollected -> FlashlightAmber
                            else -> MutedAsh
                        }
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = MidnightCharcoal.copy(alpha = 0.78f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, borderColor)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isInstalled) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = PhosphorGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                }
                                Text(
                                    text = partType.shortLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = labelColor,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Subtle Radio / Status Banner + Optional Blueprint Radar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        color = MidnightCharcoal.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = state.statusBanner,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (state.stalkerDanger > 0.5f) CrimsonAlert else BoneWhite.copy(alpha = 0.9f),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    AnimatedVisibility(
                        visible = state.radarVisible,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Spacer(modifier = Modifier.width(8.dp))
                        BlueprintRadarCard(state = state)
                    }
                }
            }

            // BOTTOM CONTROLS
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Virtual Thumbstick (Bottom Left)
                VirtualThumbstick(
                    onMove = onMoveInput,
                    modifier = Modifier.testTag("movement_thumbstick")
                )

                // Contextual Interact + Flashlight Button Cluster (Bottom Right)
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AnimatedVisibility(
                        visible = state.interactPrompt.type != InteractTargetType.NONE,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        val isRepair = state.interactPrompt.type == InteractTargetType.ANIMATRONIC
                        Button(
                            onClick = onInteract,
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("interact_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRepair) PhosphorGreen else FlashlightAmber,
                                contentColor = VoidObsidian
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = state.interactPrompt.label,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    // Flashlight Toggle with circular battery ring
                    FlashlightControlButton(
                        battery = state.battery,
                        isOn = state.flashlightOn,
                        onClick = onToggleFlashlight,
                        modifier = Modifier.testTag("flashlight_button")
                    )
                }
            }
        }
    }
}

@Composable
private fun VirtualThumbstick(
    onMove: (forward: Float, strafe: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .size(132.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val center = Offset(size.width * 0.5f, size.height * 0.5f)
                    val maxRadius = size.width * 0.36f

                    fun updateFromPos(pos: Offset) {
                        val dx = pos.x - center.x
                        val dy = pos.y - center.y
                        val dist = hypot(dx, dy)
                        val clampedDist = dist.coerceAtMost(maxRadius)
                        val nx = if (dist > 1f) (dx / dist) * (clampedDist / maxRadius) else 0f
                        val ny = if (dist > 1f) (dy / dist) * (clampedDist / maxRadius) else 0f
                        knobOffset = Offset(nx * maxRadius, ny * maxRadius)
                        onMove(-ny, nx)
                    }

                    updateFromPos(down.position)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        change.consume()
                        updateFromPos(change.position)
                    }
                    knobOffset = Offset.Zero
                    onMove(0f, 0f)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width * 0.5f, size.height * 0.5f)
            val outerR = size.minDimension * 0.43f
            drawCircle(
                color = MidnightCharcoal.copy(alpha = 0.62f),
                radius = outerR,
                center = center
            )
            drawCircle(
                color = BoneWhite.copy(alpha = 0.28f),
                radius = outerR,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
            drawCircle(
                color = FlashlightAmber.copy(alpha = 0.85f),
                radius = 22.dp.toPx(),
                center = center + knobOffset
            )
        }
    }
}

@Composable
private fun FlashlightControlButton(
    battery: Float,
    isOn: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val battRatio = (battery / 100f).coerceIn(0f, 1f)
    val ringColor = when {
        battery < 20f -> CrimsonAlert
        isOn -> FlashlightAmber
        else -> MutedAsh
    }

    Surface(
        onClick = onClick,
        modifier = modifier.size(74.dp),
        shape = CircleShape,
        color = MidnightCharcoal.copy(alpha = 0.88f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                drawCircle(
                    color = BorderSlate,
                    style = Stroke(width = 4.dp.toPx())
                )
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f * battRatio,
                    useCenter = false,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = if (isOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                    contentDescription = stringResource(R.string.action_flashlight),
                    tint = if (isOn) FlashlightAmber else MutedAsh,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "${battery.toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (battery < 20f) CrimsonAlert else BoneWhite
                )
            }
        }
    }
}

@Composable
private fun BlueprintRadarCard(state: GameUiState) {
    Surface(
        color = MidnightCharcoal.copy(alpha = 0.88f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, ArcadeCyan.copy(alpha = 0.6f)),
        modifier = Modifier.size(134.dp)
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Text(
                text = "BLUEPRINT",
                style = MaterialTheme.typography.labelSmall,
                color = ArcadeCyan
            )
            Spacer(modifier = Modifier.height(3.dp))
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cellW = size.width / PizzeriaMap.WIDTH
                val cellH = size.height / PizzeriaMap.HEIGHT
                for (y in 0 until PizzeriaMap.HEIGHT) {
                    for (x in 0 until PizzeriaMap.WIDTH) {
                        val w = PizzeriaMap.wallAt(x, y)
                        if (w > 0) {
                            drawRect(
                                color = Color(0xFF334155),
                                topLeft = Offset(x * cellW, y * cellH),
                                size = Size(cellW + 0.5f, cellH + 0.5f)
                            )
                        }
                    }
                }
                // Bruno marker (Parts & Service)
                drawCircle(
                    color = PhosphorGreen,
                    radius = 3.5.dp.toPx(),
                    center = Offset(PizzeriaMap.BRUNO_X * cellW, PizzeriaMap.BRUNO_Y * cellH)
                )
                // Uncollected parts pulse if within 8 tiles
                state.parts.filter { !it.collected }.forEach { part ->
                    val d = PizzeriaMap.distance(state.playerX, state.playerY, part.x, part.y)
                    if (d < 8.5f) {
                        drawCircle(
                            color = FlashlightAmber,
                            radius = 2.6.dp.toPx(),
                            center = Offset(part.x * cellW, part.y * cellH)
                        )
                    }
                }
                // Stalker ping if close or stunned
                if (state.horrorMode && (state.stalker.distanceToPlayer < 6.5f || state.stalker.behavior == StalkerBehavior.STUNNED)) {
                    drawCircle(
                        color = CrimsonAlert,
                        radius = 3.dp.toPx(),
                        center = Offset(state.stalker.x * cellW, state.stalker.y * cellH)
                    )
                }
                // Player position & heading vector
                val px = state.playerX * cellW
                val py = state.playerY * cellH
                val dirEnd = Offset(
                    px + cos(state.playerAngle) * 10.dp.toPx(),
                    py + sin(state.playerAngle) * 10.dp.toPx()
                )
                drawLine(
                    color = BoneWhite,
                    start = Offset(px, py),
                    end = dirEnd,
                    strokeWidth = 2.dp.toPx()
                )
                drawCircle(
                    color = ArcadeCyan,
                    radius = 3.dp.toPx(),
                    center = Offset(px, py)
                )
            }
        }
    }
}
