package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.game.AnimatronicPartType
import com.example.game.GameUiState
import com.example.ui.theme.ArcadeCyan
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.BoneWhite
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.DeepVelvetRed
import com.example.ui.theme.ElevatedPlum
import com.example.ui.theme.FlashlightAmber
import com.example.ui.theme.MidnightCharcoal
import com.example.ui.theme.MutedAsh
import com.example.ui.theme.PhosphorGreen
import com.example.ui.theme.VoidObsidian

@Composable
fun RepairBenchOverlay(
    state: GameUiState,
    onSelectPart: (AnimatronicPartType) -> Unit,
    onCalibrate: () -> Unit,
    onInitiateReboot: () -> Unit,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    val repair = state.repairState
    val dangerHigh = state.horrorMode && state.stalkerDanger > 0.25f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidObsidian.copy(alpha = 0.94f))
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 580.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("repair_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.repair_close),
                            tint = BoneWhite
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.repair_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = BoneWhite
                        )
                        Text(
                            text = stringResource(R.string.repair_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = FlashlightAmber
                        )
                    }
                }

                Surface(
                    color = MidnightCharcoal,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, PhosphorGreen.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "${state.installedParts.size}/4 MOUNTED",
                        style = MaterialTheme.typography.labelLarge,
                        color = PhosphorGreen,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Proximity Warning Strip
            if (dangerHigh) {
                Surface(
                    color = DeepVelvetRed.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, CrimsonAlert),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = BoneWhite,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ALERT: Movement outside workshop door! Step back if needed.",
                            style = MaterialTheme.typography.labelSmall,
                            color = BoneWhite
                        )
                    }
                }
            }

            // Bruno Diagnostic Chassis Schematic
            Surface(
                color = MidnightCharcoal,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BorderSlate),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                BrunoSchematicCanvas(
                    installedParts = state.installedParts,
                    activePart = repair.activePart,
                    modifier = Modifier.fillMaxSize().padding(12.dp)
                )
            }

            // Module Slots Grid
            Text(
                text = "ANIMATRONIC MODULES",
                style = MaterialTheme.typography.labelSmall,
                color = MutedAsh
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AnimatronicPartType.entries.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { part ->
                            val isInstalled = part in state.installedParts
                            val isCollected = part in state.collectedParts
                            val isActive = repair.activePart == part

                            val borderColor = when {
                                isInstalled -> PhosphorGreen
                                isActive -> FlashlightAmber
                                isCollected -> ArcadeCyan
                                else -> BorderSlate
                            }
                            Surface(
                                onClick = { if (isCollected && !isInstalled) onSelectPart(part) },
                                enabled = isCollected && !isInstalled,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp)
                                    .testTag("repair_part_${part.name}"),
                                shape = RoundedCornerShape(10.dp),
                                color = if (isActive) ElevatedPlum else MidnightCharcoal,
                                border = BorderStroke(if (isActive) 1.5.dp else 1.dp, borderColor)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = part.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCollected || isInstalled) BoneWhite else MutedAsh
                                        )
                                        Text(
                                            text = when {
                                                isInstalled -> "SECURED"
                                                isActive -> "CALIBRATING"
                                                isCollected -> "READY · TAP"
                                                else -> "MISSING (${part.hintRoom})"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = when {
                                                isInstalled -> PhosphorGreen
                                                isActive -> FlashlightAmber
                                                isCollected -> ArcadeCyan
                                                else -> MutedAsh
                                            }
                                        )
                                    }
                                    if (isInstalled) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Installed",
                                            tint = PhosphorGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Calibration Oscilloscope or Final Reboot Lever
            if (state.allPartsInstalled) {
                Button(
                    onClick = onInitiateReboot,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag("reboot_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PhosphorGreen,
                        contentColor = VoidObsidian
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.repair_reboot),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            } else {
                Surface(
                    color = MidnightCharcoal,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (repair.isErrorFlash) CrimsonAlert else BorderSlate
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = repair.activePart?.let { "MOUNTING: ${it.displayName.uppercase()}" }
                                    ?: "NO MODULE LOADED",
                                style = MaterialTheme.typography.labelLarge,
                                color = FlashlightAmber
                            )
                            Text(
                                text = "${repair.progressSteps}/${repair.requiredSteps} LOCKS",
                                style = MaterialTheme.typography.labelSmall,
                                color = PhosphorGreen
                            )
                        }

                        Text(
                            text = repair.feedbackText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = BoneWhite
                        )

                        // Oscilloscope frequency lock bar
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                        ) {
                            val trackH = 18.dp.toPx()
                            val trackTop = (size.height - trackH) * 0.5f
                            drawRoundRect(
                                color = Color(0xFF0B090E),
                                topLeft = Offset(0f, trackTop),
                                size = Size(size.width, trackH),
                                cornerRadius = CornerRadius(6.dp.toPx())
                            )

                            if (repair.activePart != null) {
                                val zoneLeft = ((repair.targetCenter - repair.targetHalfWidth) * size.width)
                                    .coerceIn(0f, size.width)
                                val zoneWidth = (repair.targetHalfWidth * 2f * size.width)
                                    .coerceAtMost(size.width - zoneLeft)
                                drawRoundRect(
                                    color = FlashlightAmber.copy(alpha = 0.42f),
                                    topLeft = Offset(zoneLeft, trackTop),
                                    size = Size(zoneWidth, trackH),
                                    cornerRadius = CornerRadius(4.dp.toPx())
                                )
                                drawRoundRect(
                                    color = FlashlightAmber,
                                    topLeft = Offset(zoneLeft, trackTop),
                                    size = Size(zoneWidth, trackH),
                                    cornerRadius = CornerRadius(4.dp.toPx()),
                                    style = Stroke(width = 1.5.dp.toPx())
                                )

                                // Moving needle
                                val needleX = (repair.cursorPosition * size.width).coerceIn(4f, size.width - 4f)
                                drawLine(
                                    color = BoneWhite,
                                    start = Offset(needleX, 2f),
                                    end = Offset(needleX, size.height - 2f),
                                    strokeWidth = 3.5.dp.toPx()
                                )
                            }
                        }

                        Button(
                            onClick = onCalibrate,
                            enabled = repair.activePart != null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("calibrate_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FlashlightAmber,
                                contentColor = VoidObsidian,
                                disabledContainerColor = BorderSlate,
                                disabledContentColor = MutedAsh
                            )
                        ) {
                            Icon(
                                imageVector = if (repair.activePart != null) Icons.Default.Bolt else Icons.Default.Build,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CALIBRATE & LOCK",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrunoSchematicCanvas(
    installedParts: Set<AnimatronicPartType>,
    activePart: AnimatronicPartType?,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width * 0.5f
        val cy = size.height * 0.52f
        val unit = size.minDimension * 0.44f

        // Subtle diagnostic grid
        val step = 22.dp.toPx()
        var gx = 0f
        while (gx < size.width) {
            drawLine(BorderSlate.copy(alpha = 0.25f), Offset(gx, 0f), Offset(gx, size.height), 1f)
            gx += step
        }
        var gy = 0f
        while (gy < size.height) {
            drawLine(BorderSlate.copy(alpha = 0.25f), Offset(0f, gy), Offset(size.width, gy), 1f)
            gy += step
        }

        // Torso frame
        val torsoW = unit * 1.1f
        val torsoH = unit * 1.0f
        val torsoTop = cy - unit * 0.06f
        drawRoundRect(
            color = Color(0xFF282130),
            topLeft = Offset(cx - torsoW * 0.5f, torsoTop),
            size = Size(torsoW, torsoH),
            cornerRadius = CornerRadius(10.dp.toPx())
        )
        drawRoundRect(
            color = BorderSlate,
            topLeft = Offset(cx - torsoW * 0.5f, torsoTop),
            size = Size(torsoW, torsoH),
            cornerRadius = CornerRadius(10.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )

        // Left arm
        drawRoundRect(
            color = Color(0xFF282130),
            topLeft = Offset(cx - torsoW * 0.5f - unit * 0.34f, torsoTop + unit * 0.06f),
            size = Size(unit * 0.26f, unit * 0.85f),
            cornerRadius = CornerRadius(6.dp.toPx())
        )

        // Right arm (SERVO_MOTOR)
        val servoColor = socketColor(AnimatronicPartType.SERVO_MOTOR, installedParts, activePart)
        drawRoundRect(
            color = if (AnimatronicPartType.SERVO_MOTOR in installedParts) Color(0xFF3E2B22) else Color(0xFF15121B),
            topLeft = Offset(cx + torsoW * 0.5f + unit * 0.08f, torsoTop + unit * 0.06f),
            size = Size(unit * 0.26f, unit * 0.85f),
            cornerRadius = CornerRadius(6.dp.toPx())
        )
        drawCircle(
            color = servoColor,
            radius = unit * 0.12f,
            center = Offset(cx + torsoW * 0.5f + unit * 0.21f, torsoTop + unit * 0.18f),
            style = if (AnimatronicPartType.SERVO_MOTOR in installedParts) {
                androidx.compose.ui.graphics.drawscope.Fill
            } else {
                Stroke(2.dp.toPx())
            }
        )

        // Chest Core (POWER_CORE)
        val coreColor = socketColor(AnimatronicPartType.POWER_CORE, installedParts, activePart)
        drawCircle(
            color = coreColor.copy(alpha = 0.22f),
            radius = unit * 0.28f,
            center = Offset(cx, torsoTop + torsoH * 0.48f)
        )
        drawCircle(
            color = coreColor,
            radius = unit * 0.18f,
            center = Offset(cx, torsoTop + torsoH * 0.48f),
            style = if (AnimatronicPartType.POWER_CORE in installedParts) {
                androidx.compose.ui.graphics.drawscope.Fill
            } else {
                Stroke(2.2.dp.toPx())
            }
        )

        // Head & Ears
        val headW = unit * 0.88f
        val headH = unit * 0.74f
        val headTop = torsoTop - headH - unit * 0.08f
        drawCircle(color = Color(0xFF282130), radius = unit * 0.18f, center = Offset(cx - headW * 0.45f, headTop + unit * 0.12f))
        drawCircle(color = Color(0xFF282130), radius = unit * 0.18f, center = Offset(cx + headW * 0.45f, headTop + unit * 0.12f))
        drawRoundRect(
            color = Color(0xFF33293F),
            topLeft = Offset(cx - headW * 0.5f, headTop),
            size = Size(headW, headH),
            cornerRadius = CornerRadius(12.dp.toPx())
        )

        // Optic Sensor (Eyes)
        val opticColor = socketColor(AnimatronicPartType.OPTIC_SENSOR, installedParts, activePart)
        val eyeY = headTop + headH * 0.36f
        drawCircle(color = opticColor, radius = unit * 0.09f, center = Offset(cx - headW * 0.22f, eyeY))
        drawCircle(color = opticColor, radius = unit * 0.09f, center = Offset(cx + headW * 0.22f, eyeY))

        // Voice Box (Jaw)
        val voiceColor = socketColor(AnimatronicPartType.VOICE_BOX, installedParts, activePart)
        drawRoundRect(
            color = voiceColor.copy(alpha = 0.35f),
            topLeft = Offset(cx - headW * 0.32f, headTop + headH * 0.62f),
            size = Size(headW * 0.64f, headH * 0.24f),
            cornerRadius = CornerRadius(4.dp.toPx())
        )
        drawRoundRect(
            color = voiceColor,
            topLeft = Offset(cx - headW * 0.32f, headTop + headH * 0.62f),
            size = Size(headW * 0.64f, headH * 0.24f),
            cornerRadius = CornerRadius(4.dp.toPx()),
            style = Stroke(1.6.dp.toPx())
        )
    }
}

private fun socketColor(
    part: AnimatronicPartType,
    installedParts: Set<AnimatronicPartType>,
    activePart: AnimatronicPartType?
): Color = when {
    part in installedParts -> PhosphorGreen
    part == activePart -> FlashlightAmber
    else -> CrimsonAlert
}
