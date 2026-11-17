package com.example.game

enum class AnimatronicPartType(
    val displayName: String,
    val shortLabel: String,
    val socketName: String,
    val hintRoom: String,
    val calibrationTarget: Float
) {
    OPTIC_SENSOR(
        displayName = "Optic Sensor",
        shortLabel = "OPTIC",
        socketName = "Head Socket",
        hintRoom = "Arcade & Prize Corner",
        calibrationTarget = 0.28f
    ),
    VOICE_BOX(
        displayName = "Voice Box",
        shortLabel = "VOICE",
        socketName = "Jaw Chassis",
        hintRoom = "Main Dining / Show Stage",
        calibrationTarget = 0.52f
    ),
    SERVO_MOTOR(
        displayName = "Servo Motor",
        shortLabel = "SERVO",
        socketName = "Right Shoulder",
        hintRoom = "Kitchen & Freezer",
        calibrationTarget = 0.72f
    ),
    POWER_CORE(
        displayName = "Power Core",
        shortLabel = "CORE",
        socketName = "Chest Cavity",
        hintRoom = "Security Office / Backstage",
        calibrationTarget = 0.44f
    )
}

data class PartPickup(
    val type: AnimatronicPartType,
    val x: Float,
    val y: Float,
    val collected: Boolean = false
)

data class BatteryPickup(
    val id: Int,
    val x: Float,
    val y: Float,
    val collected: Boolean = false
)

enum class StalkerBehavior {
    PATROL,
    INVESTIGATE,
    CHASE,
    STUNNED
}

data class StalkerState(
    val x: Float = 18.5f,
    val y: Float = 18.5f,
    val angle: Float = 0f,
    val behavior: StalkerBehavior = StalkerBehavior.PATROL,
    val waypointIndex: Int = 0,
    val stunTimer: Float = 0f,
    val blindExposure: Float = 0f,
    val distanceToPlayer: Float = 20f
)

enum class GameScreen {
    TITLE,
    PLAYING,
    REPAIR_BENCH,
    PAUSED,
    VICTORY,
    GAME_OVER
}

enum class InteractTargetType {
    NONE,
    PART,
    BATTERY,
    ANIMATRONIC
}

data class InteractPrompt(
    val type: InteractTargetType = InteractTargetType.NONE,
    val label: String = "",
    val partType: AnimatronicPartType? = null,
    val batteryId: Int? = null
)

data class RepairMiniGameState(
    val activePart: AnimatronicPartType? = null,
    val cursorPosition: Float = 0.1f,
    val cursorVelocity: Float = 0.95f,
    val targetCenter: Float = 0.5f,
    val targetHalfWidth: Float = 0.11f,
    val progressSteps: Int = 0,
    val requiredSteps: Int = 2,
    val feedbackText: String = "Select a recovered part to mount on Bruno.",
    val isErrorFlash: Boolean = false
)

data class GameUiState(
    val screen: GameScreen = GameScreen.TITLE,
    val horrorMode: Boolean = true,
    val audioEnabled: Boolean = true,
    val lookSensitivity: Float = 1.0f,
    val playerX: Float = 4.5f,
    val playerY: Float = 4.5f,
    val playerAngle: Float = -2.356f,
    val headBobPhase: Float = 0f,
    val headBobAmount: Float = 0f,
    val flashlightOn: Boolean = true,
    val flashlightFlicker: Float = 1f,
    val battery: Float = 100f,
    val elapsedSeconds: Float = 0f,
    val currentRoomLabel: String = "CAM-03 // PARTS & SERVICE",
    val parts: List<PartPickup> = emptyList(),
    val batteries: List<BatteryPickup> = emptyList(),
    val installedParts: Set<AnimatronicPartType> = emptySet(),
    val stalker: StalkerState = StalkerState(),
    val stalkerDanger: Float = 0f, // 0..1 proximity tension
    val interactPrompt: InteractPrompt = InteractPrompt(),
    val repairState: RepairMiniGameState = RepairMiniGameState(),
    val radarVisible: Boolean = false,
    val statusBanner: String = "Find all 4 parts and repair Bruno in Parts & Service.",
    val bestHorrorSeconds: Int? = null,
    val bestExploreSeconds: Int? = null,
    val totalRepairsCompleted: Int = 0,
    val isNewBestTime: Boolean = false
) {
    val collectedParts: Set<AnimatronicPartType>
        get() = parts.filter { it.collected }.map { it.type }.toSet()

    val readyToInstallParts: List<AnimatronicPartType>
        get() = collectedParts.filter { it !in installedParts }

    val allPartsInstalled: Boolean
        get() = installedParts.size == AnimatronicPartType.entries.size
}
