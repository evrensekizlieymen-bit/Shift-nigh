package com.example.game

import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

object PizzeriaMap {
    const val WIDTH = 22
    const val HEIGHT = 22

    const val BRUNO_X = 2.4f
    const val BRUNO_Y = 2.4f

    const val PLAYER_START_X = 4.5f
    const val PLAYER_START_Y = 4.5f
    const val PLAYER_START_ANGLE = -2.3561945f // Facing Bruno in Parts & Service

    // Wall types:
    // 0 = Open Floor
    // 1 = Dining Hall Brick & Checker Trim
    // 2 = Show Stage Crimson Curtain
    // 3 = Parts & Service Riveted Steel
    // 4 = Kitchen & Freezer Tile
    // 5 = Arcade Neon Wall
    // 6 = Security Office Monitor Wall
    private val RAW_ROWS = arrayOf(
        "3333333111111111555555",
        "3000003100000001500005",
        "3000003102222001500005",
        "3000000002222000000005",
        "3000000000000000005005",
        "3000003100000001500005",
        "3330033101100111550055",
        "1110011100000001000005",
        "1000000000000000000005",
        "1001100001000100005505",
        "1001100000000000000005",
        "1000000001000100000005",
        "1110011111001111100111",
        "4000004000000000600006",
        "4000000000000000000006",
        "4000000006600660000006",
        "4004004000000000600606",
        "4000004000000000600006",
        "4000004600000006600006",
        "4000004600000006600006",
        "4000000000000000000006",
        "4444444666666666666666"
    )

    val grid: Array<IntArray> = Array(HEIGHT) { y ->
        IntArray(WIDTH) { x ->
            RAW_ROWS[y][x].digitToInt()
        }
    }

    fun wallAt(tx: Int, ty: Int): Int {
        if (tx < 0 || ty < 0 || tx >= WIDTH || ty >= HEIGHT) return 1
        return grid[ty][tx]
    }

    fun isWalkable(tx: Int, ty: Int): Boolean = wallAt(tx, ty) == 0

    fun isCircleClear(cx: Float, cy: Float, radius: Float): Boolean {
        val minTx = (cx - radius).toInt()
        val maxTx = (cx + radius).toInt()
        val minTy = (cy - radius).toInt()
        val maxTy = (cy + radius).toInt()
        for (ty in minTy..maxTy) {
            for (tx in minTx..maxTx) {
                if (!isWalkable(tx, ty)) {
                    val nearestX = cx.coerceIn(tx.toFloat(), (tx + 1).toFloat())
                    val nearestY = cy.coerceIn(ty.toFloat(), (ty + 1).toFloat())
                    val dx = cx - nearestX
                    val dy = cy - nearestY
                    if (dx * dx + dy * dy < radius * radius) return false
                }
            }
        }
        // Also prevent walking directly through Bruno's workbench pedestal
        val bdx = cx - BRUNO_X
        val bdy = cy - BRUNO_Y
        if (bdx * bdx + bdy * bdy < (radius + 0.42f) * (radius + 0.42f)) return false
        return true
    }

    fun hasLineOfSight(x1: Float, y1: Float, x2: Float, y2: Float): Boolean {
        val dx = x2 - x1
        val dy = y2 - y1
        val dist = sqrt(dx * dx + dy * dy)
        if (dist < 0.05f) return true
        val steps = (dist * 6f).toInt().coerceAtLeast(1)
        val stepX = dx / steps
        val stepY = dy / steps
        var cx = x1
        var cy = y1
        for (i in 1 until steps) {
            cx += stepX
            cy += stepY
            if (!isWalkable(cx.toInt(), cy.toInt())) return false
        }
        return true
    }

    fun roomLabelFor(x: Float, y: Float): String {
        val tx = x.toInt()
        val ty = y.toInt()
        return when {
            tx <= 5 && ty <= 5 -> "CAM-03 // PARTS & SERVICE"
            tx in 7..15 && ty <= 5 -> "CAM-01 // SHOW STAGE"
            tx >= 16 && ty <= 11 -> "CAM-02 // ARCADE CORNER"
            tx <= 5 && ty in 7..11 -> "CAM-04 // WEST HALLWAY"
            tx in 6..15 && ty in 6..11 -> "CAM-01 // MAIN DINING HALL"
            tx <= 5 && ty >= 13 -> "CAM-05 // KITCHEN & FREEZER"
            tx >= 16 && ty >= 13 -> "CAM-06 // SECURITY OFFICE"
            tx in 7..15 && ty >= 16 -> "CAM-07 // SUPPLY VAULT"
            else -> "CAM-04 // BACK CORRIDOR"
        }
    }

    fun generatePartSpawns(random: Random = Random.Default): List<PartPickup> {
        val opticSpots = listOf(19.5f to 2.5f, 17.5f to 5.2f, 19.5f to 7.8f)
        val voiceSpots = listOf(8.5f to 1.6f, 14.5f to 1.6f, 11.5f to 10.5f)
        val servoSpots = listOf(1.6f to 19.5f, 4.5f to 14.5f, 1.6f to 14.2f)
        val coreSpots = listOf(19.5f to 19.5f, 11.5f to 17.5f, 17.5f to 14.2f)

        val o = opticSpots[random.nextInt(opticSpots.size)]
        val v = voiceSpots[random.nextInt(voiceSpots.size)]
        val s = servoSpots[random.nextInt(servoSpots.size)]
        val c = coreSpots[random.nextInt(coreSpots.size)]

        return listOf(
            PartPickup(AnimatronicPartType.OPTIC_SENSOR, o.first, o.second),
            PartPickup(AnimatronicPartType.VOICE_BOX, v.first, v.second),
            PartPickup(AnimatronicPartType.SERVO_MOTOR, s.first, s.second),
            PartPickup(AnimatronicPartType.POWER_CORE, c.first, c.second)
        )
    }

    fun generateBatteryPickups(): List<BatteryPickup> = listOf(
        BatteryPickup(1, 11.5f, 4.8f),
        BatteryPickup(2, 2.2f, 10.5f),
        BatteryPickup(3, 18.2f, 11.2f),
        BatteryPickup(4, 11.5f, 19.8f)
    )

    val stalkerWaypoints: List<Pair<Float, Float>> = listOf(
        19.5f to 19.5f,
        13.5f to 20.5f,
        7.5f to 14.5f,
        11.5f to 10.5f,
        14.5f to 5.0f,
        19.5f to 3.5f,
        17.5f to 8.5f,
        8.5f to 8.5f,
        2.5f to 8.5f,
        4.5f to 14.5f,
        2.5f to 19.5f,
        11.5f to 17.5f
    )

    fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx * dx + dy * dy)
    }

    fun normalizeAngle(angle: Float): Float {
        var a = angle
        while (a > Math.PI) a -= (2.0 * Math.PI).toFloat()
        while (a < -Math.PI) a += (2.0 * Math.PI).toFloat()
        return a
    }

    fun angleDiff(a: Float, b: Float): Float = abs(normalizeAngle(a - b))
}
