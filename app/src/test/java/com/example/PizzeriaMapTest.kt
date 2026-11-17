package com.example

import com.example.game.AnimatronicPartType
import com.example.game.PizzeriaMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.ArrayDeque
import kotlin.random.Random

class PizzeriaMapTest {

    @Test
    fun `all part spawns, batteries, and stalker waypoints are reachable from player start`() {
        val reachable = mutableSetOf<Pair<Int, Int>>()
        val queue = ArrayDeque<Pair<Int, Int>>()

        val startTile = PizzeriaMap.PLAYER_START_X.toInt() to PizzeriaMap.PLAYER_START_Y.toInt()
        assertTrue("Player start must be walkable", PizzeriaMap.isWalkable(startTile.first, startTile.second))
        reachable.add(startTile)
        queue.add(startTile)

        val dirs = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
        while (queue.isNotEmpty()) {
            val (cx, cy) = queue.removeFirst()
            for ((dx, dy) in dirs) {
                val nx = cx + dx
                val ny = cy + dy
                if (nx in 0 until PizzeriaMap.WIDTH && ny in 0 until PizzeriaMap.HEIGHT) {
                    if (PizzeriaMap.isWalkable(nx, ny) && reachable.add(nx to ny)) {
                        queue.add(nx to ny)
                    }
                }
            }
        }

        // Check Bruno's tile
        val brunoTile = PizzeriaMap.BRUNO_X.toInt() to PizzeriaMap.BRUNO_Y.toInt()
        assertTrue("Bruno must be in reachable area", brunoTile in reachable)

        // Check multiple random seeds for part spawns
        for (seed in 0..15) {
            val parts = PizzeriaMap.generatePartSpawns(Random(seed))
            assertEquals(AnimatronicPartType.entries.size, parts.size)
            for (part in parts) {
                val tile = part.x.toInt() to part.y.toInt()
                assertTrue("Part ${part.type} at $tile must be reachable", tile in reachable)
            }
        }

        // Check batteries
        for (batt in PizzeriaMap.generateBatteryPickups()) {
            val tile = batt.x.toInt() to batt.y.toInt()
            assertTrue("Battery ${batt.id} at $tile must be reachable", tile in reachable)
        }

        // Check stalker waypoints
        for (wp in PizzeriaMap.stalkerWaypoints) {
            val tile = wp.first.toInt() to wp.second.toInt()
            assertTrue("Stalker waypoint $tile must be reachable", tile in reachable)
        }
    }
}
