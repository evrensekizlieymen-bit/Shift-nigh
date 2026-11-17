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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.game.GameUiState
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.BoneWhite
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ElevatedPlum
import com.example.ui.theme.FlashlightAmber
import com.example.ui.theme.MidnightCharcoal
import com.example.ui.theme.MutedAsh
import com.example.ui.theme.PhosphorGreen
import com.example.ui.theme.VoidObsidian

@Composable
fun PauseOverlay(
    state: GameUiState,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onToggleAudio: () -> Unit,
    onSensitivityChange: (Float) -> Unit,
    onQuit: () -> Unit
) {
    BackHandler { onResume() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidObsidian.copy(alpha = 0.88f))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp),
            shape = RoundedCornerShape(16.dp),
            color = MidnightCharcoal,
            border = BorderStroke(1.dp, BorderSlate)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "SHIFT PAUSED",
                    style = MaterialTheme.typography.headlineMedium,
                    color = BoneWhite
                )
                Text(
                    text = "${state.currentRoomLabel} · Parts mounted ${state.installedParts.size}/4",
                    style = MaterialTheme.typography.labelSmall,
                    color = FlashlightAmber
                )

                // Look sensitivity slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "LOOK SENSITIVITY",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedAsh
                        )
                        Text(
                            text = "%.1fx".format(state.lookSensitivity),
                            style = MaterialTheme.typography.labelSmall,
                            color = BoneWhite
                        )
                    }
                    Slider(
                        value = state.lookSensitivity,
                        onValueChange = onSensitivityChange,
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = FlashlightAmber,
                            activeTrackColor = FlashlightAmber
                        )
                    )
                }

                OutlinedButton(
                    onClick = onToggleAudio,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("pause_audio_button"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderSlate)
                ) {
                    Icon(
                        imageVector = if (state.audioEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = null,
                        tint = BoneWhite
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (state.audioEnabled) "AUDIO: ON" else "AUDIO: MUTED",
                        style = MaterialTheme.typography.labelLarge,
                        color = BoneWhite
                    )
                }

                Button(
                    onClick = onResume,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("resume_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FlashlightAmber,
                        contentColor = VoidObsidian
                    )
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.action_resume), style = MaterialTheme.typography.labelLarge)
                }

                OutlinedButton(
                    onClick = onRestart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("pause_restart_button"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderSlate)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = BoneWhite)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.action_restart),
                        style = MaterialTheme.typography.labelLarge,
                        color = BoneWhite
                    )
                }

                OutlinedButton(
                    onClick = onQuit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("pause_quit_button"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = stringResource(R.string.action_quit),
                        style = MaterialTheme.typography.labelLarge,
                        color = CrimsonAlert
                    )
                }
            }
        }
    }
}

@Composable
fun VictoryScreen(
    state: GameUiState,
    onPlayAgain: () -> Unit,
    onReturnToTitle: () -> Unit
) {
    val clearSec = state.elapsedSeconds.toInt()
    val rank = when {
        clearSec <= 95 && state.battery >= 35f -> "S"
        clearSec <= 165 -> "A"
        else -> "B"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidObsidian)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp),
            shape = RoundedCornerShape(16.dp),
            color = MidnightCharcoal,
            border = BorderStroke(1.5.dp, PhosphorGreen.copy(alpha = 0.7f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = PhosphorGreen,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = stringResource(R.string.victory_subtitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = PhosphorGreen
                )
                Text(
                    text = stringResource(R.string.victory_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = BoneWhite
                )
                Text(
                    text = "Bruno MK-II rebooted smoothly. All 4 service modules passed diagnostics.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedAsh
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutcomeMetric(
                        label = "CLEAR TIME",
                        value = formatShiftSeconds(clearSec),
                        highlight = if (state.isNewBestTime) PhosphorGreen else FlashlightAmber,
                        modifier = Modifier.weight(1f)
                    )
                    OutcomeMetric(
                        label = "BATTERY",
                        value = "${state.battery.toInt()}%",
                        highlight = BoneWhite,
                        modifier = Modifier.weight(1f)
                    )
                    OutcomeMetric(
                        label = "RANK",
                        value = rank,
                        highlight = PhosphorGreen,
                        modifier = Modifier.weight(0.75f)
                    )
                }

                if (state.isNewBestTime) {
                    Surface(
                        color = ElevatedPlum,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, PhosphorGreen)
                    ) {
                        Text(
                            text = "NEW RECORD LOGGED",
                            style = MaterialTheme.typography.labelSmall,
                            color = PhosphorGreen,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onPlayAgain,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("victory_replay_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FlashlightAmber,
                        contentColor = VoidObsidian
                    )
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.action_restart), style = MaterialTheme.typography.labelLarge)
                }

                OutlinedButton(
                    onClick = onReturnToTitle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("victory_lobby_button"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderSlate)
                ) {
                    Text(
                        text = stringResource(R.string.action_quit),
                        style = MaterialTheme.typography.labelLarge,
                        color = BoneWhite
                    )
                }
            }
        }
    }
}

@Composable
fun GameOverScreen(
    state: GameUiState,
    onRetry: () -> Unit,
    onReturnToTitle: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidObsidian)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        // Eerie jumpscare endoskeleton eyes silhouette in background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width * 0.5f
            val cy = size.height * 0.25f
            drawRect(color = Color(0xFF1A070A), size = size)
            drawCircle(color = CrimsonAlert.copy(alpha = 0.16f), radius = 140.dp.toPx(), center = Offset(cx, cy))
            drawCircle(color = CrimsonAlert, radius = 12.dp.toPx(), center = Offset(cx - 42.dp.toPx(), cy))
            drawCircle(color = CrimsonAlert, radius = 12.dp.toPx(), center = Offset(cx + 42.dp.toPx(), cy))
            drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(cx - 42.dp.toPx(), cy))
            drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(cx + 42.dp.toPx(), cy))
            var y = 0f
            while (y < size.height) {
                drawRect(
                    color = Color.Black.copy(alpha = 0.28f),
                    topLeft = Offset(0f, y),
                    size = Size(size.width, 3f)
                )
                y += 8f
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp),
            shape = RoundedCornerShape(16.dp),
            color = MidnightCharcoal.copy(alpha = 0.94f),
            border = BorderStroke(1.5.dp, CrimsonAlert)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = CrimsonAlert,
                    modifier = Modifier.size(44.dp)
                )
                Text(
                    text = stringResource(R.string.gameover_subtitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = CrimsonAlert
                )
                Text(
                    text = stringResource(R.string.gameover_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = BoneWhite
                )
                Text(
                    text = "The stray endoskeleton caught you in ${state.currentRoomLabel}. Recovered ${state.collectedParts.size}/4 parts (${state.installedParts.size}/4 mounted).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedAsh
                )

                Button(
                    onClick = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("gameover_retry_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CrimsonAlert,
                        contentColor = BoneWhite
                    )
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.action_restart), style = MaterialTheme.typography.labelLarge)
                }

                OutlinedButton(
                    onClick = onReturnToTitle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("gameover_lobby_button"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderSlate)
                ) {
                    Text(
                        text = stringResource(R.string.action_quit),
                        style = MaterialTheme.typography.labelLarge,
                        color = BoneWhite
                    )
                }
            }
        }
    }
}

@Composable
private fun OutcomeMetric(
    label: String,
    value: String,
    highlight: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = ElevatedPlum,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, BorderSlate)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MutedAsh
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = highlight
            )
        }
    }
}
