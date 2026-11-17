package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun TitleScreen(
    state: GameUiState,
    onStartShift: () -> Unit,
    onSelectHorrorMode: (Boolean) -> Unit,
    onToggleAudio: () -> Unit
) {
    val bestTime = if (state.horrorMode) state.bestHorrorSeconds else state.bestExploreSeconds

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidObsidian)
    ) {
        // Atmospheric generated pizzeria backdrop
        Image(
            painter = painterResource(id = R.drawable.img_pizzeria_hero),
            contentDescription = "Dark abandoned pizzeria hallway lit by flashlight",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.62f
        )

        // Moody gradient scrim for crisp typography contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            VoidObsidian.copy(alpha = 0.78f),
                            VoidObsidian.copy(alpha = 0.45f),
                            VoidObsidian.copy(alpha = 0.90f),
                            VoidObsidian
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top status bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MidnightCharcoal.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, FlashlightAmber.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = stringResource(R.string.title_shift_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = FlashlightAmber,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                IconButton(
                    onClick = onToggleAudio,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("title_audio_button")
                ) {
                    Icon(
                        imageVector = if (state.audioEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = stringResource(R.string.action_toggle_audio),
                        tint = BoneWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
            ) {
                Text(
                    text = stringResource(R.string.title_pizzeria_sub),
                    style = MaterialTheme.typography.labelLarge,
                    color = CrimsonAlert,
                    letterSpacing = 2.4.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "NIGHT SHIFT",
                    style = MaterialTheme.typography.displayLarge,
                    color = BoneWhite
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.title_briefing),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MutedAsh
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 3-step objective strip
                Surface(
                    color = MidnightCharcoal.copy(alpha = 0.86f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderSlate),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ObjectiveStep(
                            icon = Icons.Default.FlashlightOn,
                            step = "01",
                            label = "Sweep Dark",
                            tint = FlashlightAmber,
                            modifier = Modifier.weight(1f)
                        )
                        ObjectiveStep(
                            icon = Icons.Default.Visibility,
                            step = "02",
                            label = "Find 4 Parts",
                            tint = BoneWhite,
                            modifier = Modifier.weight(1f)
                        )
                        ObjectiveStep(
                            icon = Icons.Default.Build,
                            step = "03",
                            label = "Repair Bruno",
                            tint = PhosphorGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mode selector
                Text(
                    text = "SHIFT DIFFICULTY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedAsh
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ModeCard(
                        title = "HORROR",
                        subtitle = "Stalker active",
                        selected = state.horrorMode,
                        accentColor = CrimsonAlert,
                        onClick = { onSelectHorrorMode(true) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("mode_horror_button")
                    )
                    ModeCard(
                        title = "EXPLORE",
                        subtitle = "No stalker",
                        selected = !state.horrorMode,
                        accentColor = PhosphorGreen,
                        onClick = { onSelectHorrorMode(false) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("mode_explore_button")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Records row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatChip(
                        label = "BEST TIME",
                        value = bestTime?.let { formatShiftSeconds(it) } ?: "--:--",
                        modifier = Modifier.weight(1f)
                    )
                    StatChip(
                        label = "REPAIRS LOGGED",
                        value = "${state.totalRepairsCompleted}",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = onStartShift,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("start_shift_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FlashlightAmber,
                        contentColor = VoidObsidian
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.action_start_shift),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (state.horrorMode) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = CrimsonAlert,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Tip: Shine your beam in the Stalker's eyes to blind it, or kill your light.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedAsh
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ObjectiveStep(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    step: String,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$step · $label",
            style = MaterialTheme.typography.labelSmall,
            color = BoneWhite
        )
    }
}

@Composable
private fun ModeCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(68.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) ElevatedPlum else MidnightCharcoal.copy(alpha = 0.78f),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) accentColor else BorderSlate
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) accentColor else BoneWhite
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MutedAsh
            )
        }
    }
}

@Composable
private fun StatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(8.dp)),
        color = MidnightCharcoal.copy(alpha = 0.8f),
        border = BorderStroke(1.dp, BorderSlate)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MutedAsh
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = FlashlightAmber
            )
        }
    }
}

fun formatShiftSeconds(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%02d:%02d".format(m, s)
}
