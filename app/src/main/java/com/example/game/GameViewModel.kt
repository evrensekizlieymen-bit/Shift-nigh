package com.example.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.HorrorAudioEngine
import com.example.data.ShiftPreferencesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = ShiftPreferencesRepository(application.applicationContext)
    private val audioEngine = HorrorAudioEngine()

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    @Volatile
    private var moveForwardInput = 0f // -1..1
    @Volatile
    private var moveStrafeInput = 0f  // -1..1

    private var gameLoopJob: Job? = null
    private var stepDistanceAccumulator = 0f
    private var heartbeatCooldown = 0f

    init {
        viewModelScope.launch {
            prefsRepo.statsFlow.collect { stats ->
                audioEngine.enabled = stats.audioEnabled
                _uiState.update {
                    it.copy(
                        bestHorrorSeconds = stats.bestHorrorSeconds,
                        bestExploreSeconds = stats.bestExploreSeconds,
                        totalRepairsCompleted = stats.totalRepairsCompleted,
                        audioEnabled = stats.audioEnabled,
                        horrorMode = if (it.screen == GameScreen.TITLE) stats.horrorModeEnabled else it.horrorMode,
                        lookSensitivity = stats.lookSensitivity
                    )
                }
            }
        }
    }

    fun setHorrorMode(enabled: Boolean) {
        _uiState.update { it.copy(horrorMode = enabled) }
        viewModelScope.launch { prefsRepo.setHorrorModeEnabled(enabled) }
    }

    fun toggleAudio() {
        val next = !_uiState.value.audioEnabled
        audioEngine.enabled = next
        _uiState.update { it.copy(audioEnabled = next) }
        viewModelScope.launch { prefsRepo.setAudioEnabled(next) }
    }

    fun setLookSensitivity(value: Float) {
        val clamped = value.coerceIn(0.5f, 2.0f)
        _uiState.update { it.copy(lookSensitivity = clamped) }
        viewModelScope.launch { prefsRepo.setLookSensitivity(clamped) }
    }

    fun startNewShift() {
        moveForwardInput = 0f
        moveStrafeInput = 0f
        stepDistanceAccumulator = 0f
        heartbeatCooldown = 0f

        val parts = PizzeriaMap.generatePartSpawns()
        val batteries = PizzeriaMap.generateBatteryPickups()
        val currentHorror = _uiState.value.horrorMode

        _uiState.update {
            it.copy(
                screen = GameScreen.PLAYING,
                playerX = PizzeriaMap.PLAYER_START_X,
                playerY = PizzeriaMap.PLAYER_START_Y,
                playerAngle = PizzeriaMap.PLAYER_START_ANGLE,
                headBobPhase = 0f,
                headBobAmount = 0f,
                flashlightOn = true,
                flashlightFlicker = 1f,
                battery = 100f,
                elapsedSeconds = 0f,
                currentRoomLabel = PizzeriaMap.roomLabelFor(
                    PizzeriaMap.PLAYER_START_X,
                    PizzeriaMap.PLAYER_START_Y
                ),
                parts = parts,
                batteries = batteries,
                installedParts = emptySet(),
                stalker = StalkerState(
                    x = 18.5f,
                    y = 18.5f,
                    behavior = StalkerBehavior.PATROL,
                    waypointIndex = 1
                ),
                stalkerDanger = 0f,
                interactPrompt = InteractPrompt(),
                repairState = RepairMiniGameState(),
                radarVisible = false,
                statusBanner = "Search the dark pizzeria for 4 parts, then return to Bruno.",
                isNewBestTime = false
            )
        }
        audioEngine.playFlashlightClick(true)
        ensureLoopRunning()
    }

    fun toggleFlashlight() {
        val state = _uiState.value
        if (state.screen != GameScreen.PLAYING) return
        if (!state.flashlightOn && state.battery <= 1f) {
            _uiState.update { it.copy(statusBanner = "Battery depleted — stay still to trickle-charge or find a cell.") }
            return
        }
        val nextOn = !state.flashlightOn
        audioEngine.playFlashlightClick(nextOn)
        _uiState.update { it.copy(flashlightOn = nextOn) }
    }

    fun toggleRadar() {
        _uiState.update { it.copy(radarVisible = !it.radarVisible) }
    }

    fun pauseGame() {
        if (_uiState.value.screen == GameScreen.PLAYING || _uiState.value.screen == GameScreen.REPAIR_BENCH) {
            moveForwardInput = 0f
            moveStrafeInput = 0f
            _uiState.update { it.copy(screen = GameScreen.PAUSED) }
        }
    }

    fun resumeGame() {
        if (_uiState.value.screen == GameScreen.PAUSED) {
            _uiState.update { it.copy(screen = GameScreen.PLAYING) }
            ensureLoopRunning()
        }
    }

    fun returnToTitle() {
        gameLoopJob?.cancel()
        gameLoopJob = null
        moveForwardInput = 0f
        moveStrafeInput = 0f
        _uiState.update { it.copy(screen = GameScreen.TITLE) }
    }

    fun updateMoveInput(forward: Float, strafe: Float) {
        moveForwardInput = forward.coerceIn(-1f, 1f)
        moveStrafeInput = strafe.coerceIn(-1f, 1f)
    }

    fun applyLookDelta(deltaPixelsX: Float) {
        val state = _uiState.value
        if (state.screen != GameScreen.PLAYING) return
        val radDelta = deltaPixelsX * 0.0042f * state.lookSensitivity
        _uiState.update {
            it.copy(playerAngle = PizzeriaMap.normalizeAngle(it.playerAngle + radDelta))
        }
    }

    fun performInteract() {
        val state = _uiState.value
        if (state.screen != GameScreen.PLAYING) return
        val prompt = state.interactPrompt
        when (prompt.type) {
            InteractTargetType.PART -> {
                val targetType = prompt.partType ?: return
                audioEngine.playPartPickup()
                val updatedParts = state.parts.map {
                    if (it.type == targetType) it.copy(collected = true) else it
                }
                val count = updatedParts.count { it.collected }
                val banner = if (count == AnimatronicPartType.entries.size) {
                    "All 4 parts recovered! Return to Parts & Service and repair Bruno."
                } else {
                    "Recovered ${targetType.displayName} ($count/4). Bring parts to Bruno."
                }
                _uiState.update {
                    it.copy(
                        parts = updatedParts,
                        statusBanner = banner
                    )
                }
            }

            InteractTargetType.BATTERY -> {
                val battId = prompt.batteryId ?: return
                audioEngine.playBatteryPickup()
                val updatedBatteries = state.batteries.map {
                    if (it.id == battId) it.copy(collected = true) else it
                }
                val newBatt = (state.battery + 40f).coerceAtMost(100f)
                _uiState.update {
                    it.copy(
                        batteries = updatedBatteries,
                        battery = newBatt,
                        statusBanner = "Power cell loaded (+40% flashlight battery)."
                    )
                }
            }

            InteractTargetType.ANIMATRONIC -> {
                moveForwardInput = 0f
                moveStrafeInput = 0f
                val firstReady = state.readyToInstallParts.firstOrNull()
                _uiState.update {
                    it.copy(
                        screen = GameScreen.REPAIR_BENCH,
                        repairState = if (firstReady != null) {
                            RepairMiniGameState(
                                activePart = firstReady,
                                targetCenter = firstReady.calibrationTarget,
                                feedbackText = "Lock frequency twice to mount ${firstReady.displayName}."
                            )
                        } else {
                            RepairMiniGameState(
                                activePart = null,
                                feedbackText = if (it.allPartsInstalled) {
                                    "All modules secured. Pull Reboot to power up Bruno!"
                                } else {
                                    "Recover animatronic parts from the pizzeria first."
                                }
                            )
                        }
                    )
                }
            }

            InteractTargetType.NONE -> Unit
        }
    }

    fun closeRepairBench() {
        if (_uiState.value.screen == GameScreen.REPAIR_BENCH) {
            _uiState.update {
                it.copy(
                    screen = GameScreen.PLAYING,
                    repairState = RepairMiniGameState()
                )
            }
        }
    }

    fun selectRepairPart(partType: AnimatronicPartType) {
        val state = _uiState.value
        if (partType !in state.collectedParts || partType in state.installedParts) return
        _uiState.update {
            it.copy(
                repairState = RepairMiniGameState(
                    activePart = partType,
                    targetCenter = partType.calibrationTarget,
                    progressSteps = 0,
                    feedbackText = "Tap CALIBRATE inside the amber zone (0/2)."
                )
            )
        }
    }

    fun triggerCalibrationPulse() {
        val state = _uiState.value
        if (state.screen != GameScreen.REPAIR_BENCH) return
        val repair = state.repairState
        val activePart = repair.activePart ?: return

        val diff = kotlin.math.abs(repair.cursorPosition - repair.targetCenter)
        if (diff <= repair.targetHalfWidth) {
            val nextSteps = repair.progressSteps + 1
            audioEngine.playRepairCalibration(true)
            if (nextSteps >= repair.requiredSteps) {
                val newInstalled = state.installedParts + activePart
                val remaining = state.collectedParts.filter { it !in newInstalled }
                val nextPart = remaining.firstOrNull()
                val msg = if (newInstalled.size == AnimatronicPartType.entries.size) {
                    "All 4 parts installed! Pull INITIATE REBOOT!"
                } else {
                    "${activePart.displayName} mounted! Select next module."
                }
                _uiState.update {
                    it.copy(
                        installedParts = newInstalled,
                        repairState = RepairMiniGameState(
                            activePart = nextPart,
                            targetCenter = nextPart?.calibrationTarget ?: 0.5f,
                            progressSteps = 0,
                            feedbackText = msg
                        )
                    )
                }
            } else {
                // Shift target slightly for step 2
                val newTarget = ((activePart.calibrationTarget + 0.35f) % 0.65f + 0.18f).coerceIn(0.18f, 0.82f)
                _uiState.update {
                    it.copy(
                        repairState = repair.copy(
                            progressSteps = nextSteps,
                            targetCenter = newTarget,
                            cursorVelocity = repair.cursorVelocity * 1.18f,
                            feedbackText = "Torque locked (1/2) — calibrate final pin!",
                            isErrorFlash = false
                        )
                    )
                }
            }
        } else {
            audioEngine.playRepairCalibration(false)
            _uiState.update {
                it.copy(
                    repairState = repair.copy(
                        progressSteps = 0,
                        isErrorFlash = true,
                        feedbackText = "Slip! Align inside the highlighted zone (reset to 0/2)."
                    )
                )
            }
        }
    }

    fun initiateBrunoReboot() {
        val state = _uiState.value
        if (!state.allPartsInstalled) return
        gameLoopJob?.cancel()
        gameLoopJob = null
        audioEngine.playVictoryChime()

        val clearSec = state.elapsedSeconds.toInt().coerceAtLeast(1)
        val prevBest = if (state.horrorMode) state.bestHorrorSeconds else state.bestExploreSeconds
        val isBest = prevBest == null || clearSec < prevBest

        _uiState.update {
            it.copy(
                screen = GameScreen.VICTORY,
                isNewBestTime = isBest
            )
        }
        viewModelScope.launch {
            prefsRepo.recordShiftVictory(clearSec, state.horrorMode)
        }
    }

    private fun ensureLoopRunning() {
        if (gameLoopJob?.isActive == true) return
        gameLoopJob = viewModelScope.launch {
            var lastTimeNs = System.nanoTime()
            while (isActive) {
                delay(16L)
                val nowNs = System.nanoTime()
                val dt = ((nowNs - lastTimeNs) / 1_000_000_000f).coerceIn(0.004f, 0.05f)
                lastTimeNs = nowNs

                val current = _uiState.value
                when (current.screen) {
                    GameScreen.PLAYING -> tickPlaying(current, dt)
                    GameScreen.REPAIR_BENCH -> tickRepairBench(current, dt)
                    else -> Unit
                }
            }
        }
    }

    private fun tickPlaying(state: GameUiState, dt: Float) {
        // 1. Player movement with circle-vs-grid wall sliding
        val inputMag = sqrt(moveForwardInput * moveForwardInput + moveStrafeInput * moveStrafeInput)
        val clampedMag = inputMag.coerceAtMost(1f)
        val dirX = cos(state.playerAngle)
        val dirY = sin(state.playerAngle)
        val rightX = -dirY
        val rightY = dirX

        val speed = if (clampedMag > 0.78f) 3.55f else 2.55f
        val moveDx = (dirX * moveForwardInput + rightX * moveStrafeInput) * speed * dt
        val moveDy = (dirY * moveForwardInput + rightY * moveStrafeInput) * speed * dt

        var nextX = state.playerX
        var nextY = state.playerY
        val playerRadius = 0.27f

        if (PizzeriaMap.isCircleClear(nextX + moveDx, nextY, playerRadius)) {
            nextX += moveDx
        }
        if (PizzeriaMap.isCircleClear(nextX, nextY + moveDy, playerRadius)) {
            nextY += moveDy
        }

        val movedDist = PizzeriaMap.distance(state.playerX, state.playerY, nextX, nextY)
        val isMoving = movedDist > 0.002f
        val nextBobPhase = if (isMoving) state.headBobPhase + dt * (if (clampedMag > 0.78f) 13f else 9.5f) else state.headBobPhase
        val targetBobAmount = if (isMoving) clampedMag else 0f
        val nextBobAmount = state.headBobAmount + (targetBobAmount - state.headBobAmount) * (dt * 10f).coerceAtMost(1f)

        if (isMoving) {
            stepDistanceAccumulator += movedDist
            if (stepDistanceAccumulator >= 1.15f) {
                stepDistanceAccumulator = 0f
                audioEngine.playFootstep()
            }
        }

        // 2. Battery drain & emergency trickle recharge when off
        var nextBattery = state.battery
        var nextFlashlightOn = state.flashlightOn
        if (nextFlashlightOn) {
            nextBattery = (nextBattery - dt * 1.45f).coerceAtLeast(0f)
            if (nextBattery <= 0.1f) {
                nextBattery = 0f
                nextFlashlightOn = false
                audioEngine.playFlashlightClick(false)
            }
        } else if (nextBattery < 25f) {
            nextBattery = (nextBattery + dt * 2.8f).coerceAtMost(25f)
        }

        val flicker = if (nextFlashlightOn && nextBattery < 22f) {
            if (Random.nextFloat() < 0.12f) Random.nextFloat() * 0.45f + 0.35f else 1f
        } else {
            1f
        }

        // 3. Stalker update
        val (nextStalker, captured, stunTriggered) = if (state.horrorMode) {
            updateStalker(
                stalker = state.stalker,
                playerX = nextX,
                playerY = nextY,
                playerAngle = state.playerAngle,
                flashlightOn = nextFlashlightOn && nextBattery > 0.5f,
                dt = dt,
                speedMultiplier = 1f
            )
        } else {
            Triple(state.stalker, false, false)
        }

        if (stunTriggered) {
            audioEngine.playStalkerStun()
            nextBattery = (nextBattery - 6f).coerceAtLeast(0f)
        }

        if (captured) {
            audioEngine.playJumpscare()
            moveForwardInput = 0f
            moveStrafeInput = 0f
            _uiState.update {
                it.copy(
                    screen = GameScreen.GAME_OVER,
                    stalker = nextStalker,
                    stalkerDanger = 1f
                )
            }
            return
        }

        val danger = if (state.horrorMode && nextStalker.behavior != StalkerBehavior.STUNNED) {
            (1f - (nextStalker.distanceToPlayer - 1.2f) / 6.8f).coerceIn(0f, 1f)
        } else {
            0f
        }

        if (danger > 0.18f) {
            heartbeatCooldown -= dt
            if (heartbeatCooldown <= 0f) {
                audioEngine.playHeartbeat(danger)
                heartbeatCooldown = (1.15f - danger * 0.72f).coerceAtLeast(0.38f)
            }
        } else {
            heartbeatCooldown = 0f
        }

        // 4. Contextual interaction detection
        val prompt = findInteractPrompt(
            playerX = nextX,
            playerY = nextY,
            playerAngle = state.playerAngle,
            parts = state.parts,
            batteries = state.batteries,
            installedCount = state.installedParts.size
        )

        // 5. Status banner guidance
        val banner = when {
            stunTriggered -> "Stalker blinded! Move while it reboots!"
            state.horrorMode && nextStalker.behavior == StalkerBehavior.CHASE && danger > 0.45f ->
                "Endoskeleton closing in — blind its eyes or cut your flashlight!"
            state.collectedParts.size == AnimatronicPartType.entries.size && !state.allPartsInstalled ->
                "All 4 parts collected! Return to Bruno in Parts & Service."
            else -> state.statusBanner
        }

        _uiState.update {
            it.copy(
                playerX = nextX,
                playerY = nextY,
                headBobPhase = nextBobPhase,
                headBobAmount = nextBobAmount,
                flashlightOn = nextFlashlightOn,
                flashlightFlicker = flicker,
                battery = nextBattery,
                elapsedSeconds = it.elapsedSeconds + dt,
                currentRoomLabel = PizzeriaMap.roomLabelFor(nextX, nextY),
                stalker = nextStalker,
                stalkerDanger = danger,
                interactPrompt = prompt,
                statusBanner = banner
            )
        }
    }

    private fun tickRepairBench(state: GameUiState, dt: Float) {
        val repair = state.repairState
        var nextPos = repair.cursorPosition + repair.cursorVelocity * dt
        var nextVel = repair.cursorVelocity
        if (nextPos >= 0.94f) {
            nextPos = 0.94f
            nextVel = -kotlin.math.abs(nextVel)
        } else if (nextPos <= 0.06f) {
            nextPos = 0.06f
            nextVel = kotlin.math.abs(nextVel)
        }

        // Stalker creeps at half speed while you're distracted at the workbench
        val (nextStalker, captured, _) = if (state.horrorMode) {
            updateStalker(
                stalker = state.stalker,
                playerX = state.playerX,
                playerY = state.playerY,
                playerAngle = state.playerAngle,
                flashlightOn = false,
                dt = dt,
                speedMultiplier = 0.55f
            )
        } else {
            Triple(state.stalker, false, false)
        }

        if (captured) {
            audioEngine.playJumpscare()
            _uiState.update {
                it.copy(
                    screen = GameScreen.GAME_OVER,
                    stalker = nextStalker,
                    stalkerDanger = 1f
                )
            }
            return
        }

        val danger = if (state.horrorMode && nextStalker.behavior != StalkerBehavior.STUNNED) {
            (1f - (nextStalker.distanceToPlayer - 1.2f) / 6.8f).coerceIn(0f, 1f)
        } else {
            0f
        }

        if (danger > 0.25f) {
            heartbeatCooldown -= dt
            if (heartbeatCooldown <= 0f) {
                audioEngine.playHeartbeat(danger)
                heartbeatCooldown = (1.1f - danger * 0.65f).coerceAtLeast(0.42f)
            }
        }

        _uiState.update {
            it.copy(
                elapsedSeconds = it.elapsedSeconds + dt,
                stalker = nextStalker,
                stalkerDanger = danger,
                repairState = repair.copy(
                    cursorPosition = nextPos,
                    cursorVelocity = nextVel,
                    isErrorFlash = false
                )
            )
        }
    }

    private fun updateStalker(
        stalker: StalkerState,
        playerX: Float,
        playerY: Float,
        playerAngle: Float,
        flashlightOn: Boolean,
        dt: Float,
        speedMultiplier: Float
    ): Triple<StalkerState, Boolean, Boolean> {
        val distToPlayer = PizzeriaMap.distance(stalker.x, stalker.y, playerX, playerY)
        val hasLos = distToPlayer < 9.5f && PizzeriaMap.hasLineOfSight(stalker.x, stalker.y, playerX, playerY)

        if (stalker.behavior == StalkerBehavior.STUNNED) {
            val remaining = stalker.stunTimer - dt
            return if (remaining <= 0f) {
                Triple(
                    stalker.copy(
                        behavior = StalkerBehavior.PATROL,
                        stunTimer = 0f,
                        blindExposure = 0f,
                        distanceToPlayer = distToPlayer
                    ),
                    false,
                    false
                )
            } else {
                Triple(
                    stalker.copy(stunTimer = remaining, distanceToPlayer = distToPlayer),
                    false,
                    false
                )
            }
        }

        // Check if player's flashlight is blinding the Stalker
        var blindExposure = stalker.blindExposure
        if (flashlightOn && hasLos && distToPlayer < 6.8f) {
            val angleToStalker = atan2(stalker.y - playerY, stalker.x - playerX)
            val diff = PizzeriaMap.angleDiff(playerAngle, angleToStalker)
            if (diff < 0.28f) {
                blindExposure += dt
                if (blindExposure >= 0.62f) {
                    return Triple(
                        stalker.copy(
                            behavior = StalkerBehavior.STUNNED,
                            stunTimer = 3.8f,
                            blindExposure = 0f,
                            distanceToPlayer = distToPlayer
                        ),
                        false,
                        true
                    )
                }
            } else {
                blindExposure = (blindExposure - dt * 1.4f).coerceAtLeast(0f)
            }
        } else {
            blindExposure = (blindExposure - dt * 1.4f).coerceAtLeast(0f)
        }

        val detectRange = if (flashlightOn) 8.2f else 3.0f
        val nextBehavior = when {
            hasLos && distToPlayer <= detectRange -> StalkerBehavior.CHASE
            stalker.behavior == StalkerBehavior.CHASE && distToPlayer < 6.5f -> StalkerBehavior.INVESTIGATE
            else -> StalkerBehavior.PATROL
        }

        val waypoints = PizzeriaMap.stalkerWaypoints
        var targetWpIndex = stalker.waypointIndex
        val (targetX, targetY) = when (nextBehavior) {
            StalkerBehavior.CHASE, StalkerBehavior.INVESTIGATE -> playerX to playerY
            else -> {
                val wp = waypoints[targetWpIndex % waypoints.size]
                if (PizzeriaMap.distance(stalker.x, stalker.y, wp.first, wp.second) < 0.7f) {
                    targetWpIndex = (targetWpIndex + 1) % waypoints.size
                }
                waypoints[targetWpIndex % waypoints.size]
            }
        }

        val moveSpeed = when (nextBehavior) {
            StalkerBehavior.CHASE -> 2.12f
            StalkerBehavior.INVESTIGATE -> 1.75f
            else -> 1.55f
        } * speedMultiplier

        val toTargetX = targetX - stalker.x
        val toTargetY = targetY - stalker.y
        val targetDist = sqrt(toTargetX * toTargetX + toTargetY * toTargetY).coerceAtLeast(0.001f)
        val stepDx = (toTargetX / targetDist) * moveSpeed * dt
        val stepDy = (toTargetY / targetDist) * moveSpeed * dt

        var nx = stalker.x
        var ny = stalker.y
        val r = 0.28f
        if (isStalkerWalkable(nx + stepDx, ny, r)) nx += stepDx
        if (isStalkerWalkable(nx, ny + stepDy, r)) ny += stepDy

        // If blocked while patrolling, advance waypoint so it never gets wedged
        if (nextBehavior == StalkerBehavior.PATROL &&
            PizzeriaMap.distance(stalker.x, stalker.y, nx, ny) < 0.003f
        ) {
            targetWpIndex = (targetWpIndex + 1) % waypoints.size
        }

        val newDist = PizzeriaMap.distance(nx, ny, playerX, playerY)
        val caught = newDist < 0.68f
        return Triple(
            stalker.copy(
                x = nx,
                y = ny,
                angle = atan2(toTargetY, toTargetX),
                behavior = nextBehavior,
                waypointIndex = targetWpIndex,
                blindExposure = blindExposure,
                distanceToPlayer = newDist
            ),
            caught,
            false
        )
    }

    private fun isStalkerWalkable(cx: Float, cy: Float, radius: Float): Boolean {
        val minTx = (cx - radius).toInt()
        val maxTx = (cx + radius).toInt()
        val minTy = (cy - radius).toInt()
        val maxTy = (cy + radius).toInt()
        for (ty in minTy..maxTy) {
            for (tx in minTx..maxTx) {
                if (!PizzeriaMap.isWalkable(tx, ty)) {
                    val nearestX = cx.coerceIn(tx.toFloat(), (tx + 1).toFloat())
                    val nearestY = cy.coerceIn(ty.toFloat(), (ty + 1).toFloat())
                    val dx = cx - nearestX
                    val dy = cy - nearestY
                    if (dx * dx + dy * dy < radius * radius) return false
                }
            }
        }
        return true
    }

    private fun findInteractPrompt(
        playerX: Float,
        playerY: Float,
        playerAngle: Float,
        parts: List<PartPickup>,
        batteries: List<BatteryPickup>,
        installedCount: Int
    ): InteractPrompt {
        // Check uncollected parts first
        val nearPart = parts
            .filter { !it.collected }
            .mapNotNull { part ->
                val d = PizzeriaMap.distance(playerX, playerY, part.x, part.y)
                val angleTo = atan2(part.y - playerY, part.x - playerX)
                val diff = PizzeriaMap.angleDiff(playerAngle, angleTo)
                if (d < 1.85f && (diff < 0.95f || d < 0.95f)) part to d else null
            }
            .minByOrNull { it.second }

        if (nearPart != null) {
            return InteractPrompt(
                type = InteractTargetType.PART,
                label = "TAKE ${nearPart.first.type.displayName.uppercase()}",
                partType = nearPart.first.type
            )
        }

        // Check batteries
        val nearBatt = batteries
            .filter { !it.collected }
            .mapNotNull { batt ->
                val d = PizzeriaMap.distance(playerX, playerY, batt.x, batt.y)
                val angleTo = atan2(batt.y - playerY, batt.x - playerX)
                val diff = PizzeriaMap.angleDiff(playerAngle, angleTo)
                if (d < 1.65f && (diff < 0.95f || d < 0.9f)) batt to d else null
            }
            .minByOrNull { it.second }

        if (nearBatt != null) {
            return InteractPrompt(
                type = InteractTargetType.BATTERY,
                label = "LOAD BATTERY CELL",
                batteryId = nearBatt.first.id
            )
        }

        // Check Bruno workbench
        val distBruno = PizzeriaMap.distance(playerX, playerY, PizzeriaMap.BRUNO_X, PizzeriaMap.BRUNO_Y)
        val angleBruno = atan2(PizzeriaMap.BRUNO_Y - playerY, PizzeriaMap.BRUNO_X - playerX)
        val diffBruno = PizzeriaMap.angleDiff(playerAngle, angleBruno)
        if (distBruno < 2.25f && (diffBruno < 1.05f || distBruno < 1.35f)) {
            val label = if (installedCount == AnimatronicPartType.entries.size) {
                "REBOOT BRUNO"
            } else {
                "REPAIR BRUNO ($installedCount/4)"
            }
            return InteractPrompt(
                type = InteractTargetType.ANIMATRONIC,
                label = label
            )
        }

        return InteractPrompt()
    }

    override fun onCleared() {
        super.onCleared()
        gameLoopJob?.cancel()
    }
}
