package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.game.GameScreen
import com.example.game.GameViewModel
import com.example.ui.screens.GameHudScreen
import com.example.ui.screens.GameOverScreen
import com.example.ui.screens.PauseOverlay
import com.example.ui.screens.RepairBenchOverlay
import com.example.ui.screens.TitleScreen
import com.example.ui.screens.VictoryScreen
import com.example.ui.theme.NightShiftTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            NightShiftTheme {
                NightShiftApp()
            }
        }
    }
}

@Composable
fun NightShiftApp(
    viewModel: GameViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when (state.screen) {
        GameScreen.TITLE -> {
            TitleScreen(
                state = state,
                onStartShift = viewModel::startNewShift,
                onSelectHorrorMode = viewModel::setHorrorMode,
                onToggleAudio = viewModel::toggleAudio
            )
        }

        GameScreen.PLAYING -> {
            BackHandler { viewModel.pauseGame() }
            GameHudScreen(
                state = state,
                onMoveInput = viewModel::updateMoveInput,
                onLookDelta = viewModel::applyLookDelta,
                onToggleFlashlight = viewModel::toggleFlashlight,
                onInteract = viewModel::performInteract,
                onToggleRadar = viewModel::toggleRadar,
                onPause = viewModel::pauseGame
            )
        }

        GameScreen.REPAIR_BENCH -> {
            // Render HUD softly behind workbench so transition feels grounded
            GameHudScreen(
                state = state,
                onMoveInput = { _, _ -> },
                onLookDelta = {},
                onToggleFlashlight = {},
                onInteract = {},
                onToggleRadar = {},
                onPause = viewModel::pauseGame
            )
            RepairBenchOverlay(
                state = state,
                onSelectPart = viewModel::selectRepairPart,
                onCalibrate = viewModel::triggerCalibrationPulse,
                onInitiateReboot = viewModel::initiateBrunoReboot,
                onClose = viewModel::closeRepairBench
            )
        }

        GameScreen.PAUSED -> {
            GameHudScreen(
                state = state,
                onMoveInput = { _, _ -> },
                onLookDelta = {},
                onToggleFlashlight = {},
                onInteract = {},
                onToggleRadar = {},
                onPause = {}
            )
            PauseOverlay(
                state = state,
                onResume = viewModel::resumeGame,
                onRestart = viewModel::startNewShift,
                onToggleAudio = viewModel::toggleAudio,
                onSensitivityChange = viewModel::setLookSensitivity,
                onQuit = viewModel::returnToTitle
            )
        }

        GameScreen.VICTORY -> {
            BackHandler { viewModel.returnToTitle() }
            VictoryScreen(
                state = state,
                onPlayAgain = viewModel::startNewShift,
                onReturnToTitle = viewModel::returnToTitle
            )
        }

        GameScreen.GAME_OVER -> {
            BackHandler { viewModel.returnToTitle() }
            GameOverScreen(
                state = state,
                onRetry = viewModel::startNewShift,
                onReturnToTitle = viewModel::returnToTitle
            )
        }
    }
}
