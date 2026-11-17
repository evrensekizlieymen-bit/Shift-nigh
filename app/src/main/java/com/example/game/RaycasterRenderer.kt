package com.example.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private const val NUM_RAYS = 144
private const val HALF_FOV_TAN = 0.625f // ~64 degree FOV
private const val MAX_RAY_DIST = 24f

private sealed interface WorldSprite {
    val x: Float
    val y: Float
    val distSq: Float
}

private data class BrunoSprite(
    override val x: Float,
    override val y: Float,
    override val distSq: Float,
    val installedParts: Set<AnimatronicPartType>
) : WorldSprite

private data class PartSprite(
    val part: PartPickup,
    override val x: Float,
    override val y: Float,
    override val distSq: Float
) : WorldSprite

private data class BatterySprite(
    val battery: BatteryPickup,
    override val x: Float,
    override val y: Float,
    override val distSq: Float
) : WorldSprite

private data class StalkerSprite(
    val stalker: StalkerState,
    override val x: Float,
    override val y: Float,
    override val distSq: Float
) : WorldSprite

@Composable
fun FirstPersonViewport(
    state: GameUiState,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val bobY = sin(state.headBobPhase) * state.headBobAmount * (h * 0.016f)
        val bobX = cos(state.headBobPhase * 0.5f) * state.headBobAmount * (w * 0.008f)
        val horizonY = h * 0.5f + bobY

        val flashlightActive = state.flashlightOn && state.battery > 0.2f
        val beamIntensity = if (flashlightActive) {
            val battFactor = (0.42f + 0.58f * (state.battery / 100f).coerceIn(0f, 1f))
            battFactor * state.flashlightFlicker
        } else {
            0f
        }

        // 1. Pitch-black ceiling & floor background
        drawRect(color = Color(0xFF040306), size = size)

        // Subtle ceiling bounce inside flashlight cone
        if (beamIntensity > 0.02f) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF23180F).copy(alpha = 0.42f * beamIntensity),
                        Color(0xFF0A070D).copy(alpha = 0.15f * beamIntensity),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.5f + bobX, horizonY - h * 0.08f),
                    radius = w * 0.46f
                ),
                topLeft = Offset.Zero,
                size = Size(w, horizonY.coerceAtLeast(1f))
            )
        }

        // Perspective checkered pizzeria floor strips lit by flashlight
        drawLitPizzeriaFloor(
            w = w,
            h = h,
            horizonY = horizonY,
            bobX = bobX,
            playerX = state.playerX,
            playerY = state.playerY,
            playerAngle = state.playerAngle,
            beamIntensity = beamIntensity
        )

        // 2. DDA Raycasting for walls
        val zBuffer = FloatArray(NUM_RAYS) { MAX_RAY_DIST }
        val sliceWidth = w / NUM_RAYS.toFloat()

        val dirX = cos(state.playerAngle)
        val dirY = sin(state.playerAngle)
        val planeX = -dirY * HALF_FOV_TAN
        val planeY = dirX * HALF_FOV_TAN

        for (col in 0 until NUM_RAYS) {
            val cameraX = 2f * col / NUM_RAYS.toFloat() - 1f // -1..1
            val rayDirX = dirX + planeX * cameraX
            val rayDirY = dirY + planeY * cameraX

            var mapX = floor(state.playerX).toInt()
            var mapY = floor(state.playerY).toInt()

            val deltaDistX = if (abs(rayDirX) < 1e-6f) 1e6f else abs(1f / rayDirX)
            val deltaDistY = if (abs(rayDirY) < 1e-6f) 1e6f else abs(1f / rayDirY)

            val stepX: Int
            var sideDistX: Float
            if (rayDirX < 0f) {
                stepX = -1
                sideDistX = (state.playerX - mapX) * deltaDistX
            } else {
                stepX = 1
                sideDistX = (mapX + 1f - state.playerX) * deltaDistX
            }

            val stepY: Int
            var sideDistY: Float
            if (rayDirY < 0f) {
                stepY = -1
                sideDistY = (state.playerY - mapY) * deltaDistY
            } else {
                stepY = 1
                sideDistY = (mapY + 1f - state.playerY) * deltaDistY
            }

            var hit = false
            var side = 0
            var wallType = 1
            var steps = 0

            while (!hit && steps < 48) {
                steps++
                if (sideDistX < sideDistY) {
                    sideDistX += deltaDistX
                    mapX += stepX
                    side = 0
                } else {
                    sideDistY += deltaDistY
                    mapY += stepY
                    side = 1
                }
                val cell = PizzeriaMap.wallAt(mapX, mapY)
                if (cell > 0) {
                    hit = true
                    wallType = cell
                }
            }

            val rawDist = if (side == 0) {
                (mapX - state.playerX + (1 - stepX) * 0.5f) / (if (abs(rayDirX) < 1e-6f) 1e-6f else rayDirX)
            } else {
                (mapY - state.playerY + (1 - stepY) * 0.5f) / (if (abs(rayDirY) < 1e-6f) 1e-6f else rayDirY)
            }
            val perpDist = rawDist.coerceIn(0.18f, MAX_RAY_DIST)
            zBuffer[col] = perpDist

            val wallX = if (side == 0) {
                state.playerY + perpDist * rayDirY
            } else {
                state.playerX + perpDist * rayDirX
            }
            val wallU = (wallX - floor(wallX)).coerceIn(0f, 0.999f)

            val wallHeight = (h * 0.88f) / perpDist
            val wallTop = horizonY - wallHeight * 0.5f

            val light = computeLightAt(
                dist = perpDist,
                screenXNorm = cameraX,
                beamIntensity = beamIntensity,
                side = side,
                wallType = wallType
            )

            if (light > 0.01f) {
                drawPizzeriaWallSlice(
                    x = col * sliceWidth,
                    width = sliceWidth + 0.8f,
                    top = wallTop,
                    height = wallHeight,
                    wallType = wallType,
                    wallU = wallU,
                    light = light,
                    beamIntensity = beamIntensity,
                    screenXNorm = cameraX,
                    dist = perpDist
                )
            }
        }

        // 3. Collect and depth-sort 3D world sprites
        val sprites = buildList<WorldSprite> {
            val bdx = PizzeriaMap.BRUNO_X - state.playerX
            val bdy = PizzeriaMap.BRUNO_Y - state.playerY
            add(BrunoSprite(PizzeriaMap.BRUNO_X, PizzeriaMap.BRUNO_Y, bdx * bdx + bdy * bdy, state.installedParts))

            state.parts.forEach { part ->
                if (!part.collected) {
                    val dx = part.x - state.playerX
                    val dy = part.y - state.playerY
                    add(PartSprite(part, part.x, part.y, dx * dx + dy * dy))
                }
            }
            state.batteries.forEach { batt ->
                if (!batt.collected) {
                    val dx = batt.x - state.playerX
                    val dy = batt.y - state.playerY
                    add(BatterySprite(batt, batt.x, batt.y, dx * dx + dy * dy))
                }
            }
            if (state.horrorMode) {
                val dx = state.stalker.x - state.playerX
                val dy = state.stalker.y - state.playerY
                add(StalkerSprite(state.stalker, state.stalker.x, state.stalker.y, dx * dx + dy * dy))
            }
        }.sortedByDescending { it.distSq }

        val invDet = 1f / (planeX * dirY - dirX * planeY)

        for (sprite in sprites) {
            val sx = sprite.x - state.playerX
            val sy = sprite.y - state.playerY
            val transformX = invDet * (dirY * sx - dirX * sy)
            val transformY = invDet * (-planeY * sx + planeX * sy)
            if (transformY <= 0.28f || transformY > MAX_RAY_DIST) continue

            val screenXNorm = transformX / transformY
            if (abs(screenXNorm) > 1.35f) continue

            val centerCol = ((screenXNorm + 1f) * 0.5f * NUM_RAYS).toInt()
            // Occlusion check across sprite's central ray columns
            val checkRadiusCols = when (sprite) {
                is BrunoSprite -> 5
                is StalkerSprite -> 3
                else -> 2
            }
            var anyVisible = false
            for (c in (centerCol - checkRadiusCols)..(centerCol + checkRadiusCols)) {
                if (c in 0 until NUM_RAYS && transformY < zBuffer[c] + 0.12f) {
                    anyVisible = true
                    break
                }
            }
            if (!anyVisible) continue

            val screenX = (screenXNorm + 1f) * 0.5f * w
            val light = computeLightAt(
                dist = transformY,
                screenXNorm = screenXNorm.coerceIn(-1f, 1f),
                beamIntensity = beamIntensity,
                side = 0,
                wallType = 0
            )

            when (sprite) {
                is BrunoSprite -> drawBrunoSprite(
                    screenX = screenX,
                    horizonY = horizonY,
                    dist = transformY,
                    viewportHeight = h,
                    light = max(light, (1f - transformY / 5.5f).coerceAtLeast(0f) * 0.25f),
                    installedParts = sprite.installedParts,
                    time = state.elapsedSeconds
                )
                is PartSprite -> drawPartPickupSprite(
                    part = sprite.part,
                    screenX = screenX,
                    horizonY = horizonY,
                    dist = transformY,
                    viewportHeight = h,
                    light = light,
                    time = state.elapsedSeconds
                )
                is BatterySprite -> drawBatterySprite(
                    screenX = screenX,
                    horizonY = horizonY,
                    dist = transformY,
                    viewportHeight = h,
                    light = light,
                    time = state.elapsedSeconds
                )
                is StalkerSprite -> drawStalkerSprite(
                    stalker = sprite.stalker,
                    screenX = screenX,
                    horizonY = horizonY,
                    dist = transformY,
                    viewportHeight = h,
                    light = light,
                    time = state.elapsedSeconds
                )
            }
        }

        // 4. Flashlight volumetric cone & dark peripheral vignette
        if (beamIntensity > 0.02f) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFD89B).copy(alpha = 0.11f * beamIntensity),
                        Color(0xFFFFB347).copy(alpha = 0.05f * beamIntensity),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.5f + bobX, horizonY),
                    radius = min(w, h) * 0.44f
                ),
                radius = min(w, h) * 0.44f,
                center = Offset(w * 0.5f + bobX, horizonY)
            )
        }

        // Deep darkness vignette around edges
        val vignetteAlpha = if (flashlightActive) 0.78f else 0.94f
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFF040307).copy(alpha = vignetteAlpha * 0.65f),
                    Color(0xFF020104).copy(alpha = vignetteAlpha)
                ),
                center = Offset(w * 0.5f + bobX * 0.4f, horizonY),
                radius = max(w, h) * 0.64f
            ),
            size = size
        )

        // Subtle CRT scanlines for security/horror texture
        val scanStep = 6f
        var sy = 0f
        while (sy < h) {
            drawLine(
                color = Color.Black.copy(alpha = 0.16f),
                start = Offset(0f, sy),
                end = Offset(w, sy),
                strokeWidth = 1f
            )
            sy += scanStep
        }

        // 5. Stalker proximity crimson & static interference
        if (state.stalkerDanger > 0.05f) {
            val danger = state.stalkerDanger
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFFE53935).copy(alpha = danger * 0.38f)
                    ),
                    center = Offset(w * 0.5f, h * 0.5f),
                    radius = max(w, h) * 0.7f
                ),
                size = size
            )
            val glitchLines = (danger * 14).toInt()
            val seed = (state.elapsedSeconds * 30f).toInt()
            val rng = Random(seed)
            repeat(glitchLines) {
                val gy = rng.nextFloat() * h
                val gx = rng.nextFloat() * w * 0.6f
                val gw = rng.nextFloat() * w * 0.4f
                drawRect(
                    color = Color.White.copy(alpha = danger * 0.18f),
                    topLeft = Offset(gx, gy),
                    size = Size(gw, 2.5f)
                )
            }
        }
    }
}

private fun computeLightAt(
    dist: Float,
    screenXNorm: Float,
    beamIntensity: Float,
    side: Int,
    wallType: Int
): Float {
    val nearAmbient = ((1f - dist / 2.2f).coerceAtLeast(0f)) * 0.16f
    val sideShade = if (side == 1) 0.80f else 1.0f

    val cone = (1f - (abs(screenXNorm) / 0.58f)).coerceIn(0f, 1f)
    val smoothCone = cone * cone * (3f - 2f * cone)
    val beam = if (beamIntensity > 0f) {
        smoothCone * (1f / (1f + dist * dist * 0.038f)) * beamIntensity
    } else {
        0f
    }

    // Arcade neon & security monitors emit a whisper of self-glow at mid-range
    val emissive = when (wallType) {
        5 -> ((1f - dist / 6.5f).coerceAtLeast(0f)) * 0.12f
        6 -> ((1f - dist / 5.5f).coerceAtLeast(0f)) * 0.10f
        else -> 0f
    }

    return ((nearAmbient + beam * 1.15f + emissive) * sideShade).coerceIn(0f, 1f)
}

private fun DrawScope.drawLitPizzeriaFloor(
    w: Float,
    h: Float,
    horizonY: Float,
    bobX: Float,
    playerX: Float,
    playerY: Float,
    playerAngle: Float,
    beamIntensity: Float
) {
    val floorHeight = (h - horizonY).coerceAtLeast(1f)
    val bands = 22
    val bandH = floorHeight / bands.toFloat()
    val dirX = cos(playerAngle)
    val dirY = sin(playerAngle)

    for (b in 0 until bands) {
        val t = (b + 0.5f) / bands.toFloat() // 0 near horizon -> 1 near bottom
        val rowDist = (0.45f / t.coerceAtLeast(0.05f)).coerceAtMost(12f)
        val worldSampleX = playerX + dirX * rowDist
        val worldSampleY = playerY + dirY * rowDist
        val checker = ((floor(worldSampleX * 1.5f).toInt() + floor(worldSampleY * 1.5f).toInt()) and 1) == 0

        val baseShade = if (checker) Color(0xFF28212C) else Color(0xFF141018)
        val distAtten = (1f / (1f + rowDist * rowDist * 0.055f))
        val bandLight = (0.04f + beamIntensity * distAtten * 0.85f).coerceIn(0f, 1f)
        if (bandLight <= 0.01f) continue

        val topY = horizonY + b * bandH
        val beamHalfW = (w * (0.20f + t * 0.34f)).coerceAtLeast(16f)
        val center = w * 0.5f + bobX * (1f - t * 0.5f)

        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    shadeColor(baseShade, bandLight * 0.72f),
                    shadeColor(Color(0xFFFFD89B), bandLight * 0.14f).blendOver(shadeColor(baseShade, bandLight)),
                    shadeColor(baseShade, bandLight * 0.72f),
                    Color.Transparent
                ),
                startX = center - beamHalfW,
                endX = center + beamHalfW
            ),
            topLeft = Offset(center - beamHalfW, topY),
            size = Size(beamHalfW * 2f, bandH + 1f)
        )
    }
}

private fun DrawScope.drawPizzeriaWallSlice(
    x: Float,
    width: Float,
    top: Float,
    height: Float,
    wallType: Int,
    wallU: Float,
    light: Float,
    beamIntensity: Float,
    screenXNorm: Float,
    dist: Float
) {
    val baseColor = when (wallType) {
        1 -> Color(0xFF5A332A) // Dining Hall Brick
        2 -> Color(0xFF7D1924) // Show Stage Crimson Curtain
        3 -> Color(0xFF343B46) // Parts & Service Steel
        4 -> Color(0xFF475754) // Kitchen Tile
        5 -> Color(0xFF1E192E) // Arcade Dark Wall
        6 -> Color(0xFF282F2A) // Security Office Wall
        else -> Color(0xFF463B3A)
    }

    // Texture pattern modifier along U
    val patternMod = when (wallType) {
        1 -> if ((wallU * 6f).toInt() % 2 == 0) 0.94f else 1.06f
        2 -> 0.78f + 0.25f * sin(wallU * PI.toFloat() * 8f) // Curtain folds
        3 -> if (wallU < 0.06f || wallU > 0.94f) 0.68f else 1.0f // Steel panel seams
        4 -> if ((wallU * 5f) % 1f < 0.1f) 0.75f else 1.04f // Tile grout
        5 -> 0.92f
        else -> if (wallU < 0.08f || wallU > 0.92f) 0.72f else 1.0f
    }

    val warmTint = if (beamIntensity > 0.05f && abs(screenXNorm) < 0.55f) {
        val warmth = (1f - abs(screenXNorm) / 0.55f) * beamIntensity * 0.16f
        Color(0xFFFFC56D).copy(alpha = warmth)
    } else {
        Color.Transparent
    }

    val litMain = shadeColor(baseColor, (light * patternMod).coerceIn(0f, 1f))
    val litUpper = shadeColor(baseColor, (light * patternMod * 0.74f).coerceIn(0f, 1f))

    // Main wall slice
    drawRect(
        color = litMain,
        topLeft = Offset(x, top),
        size = Size(width, height)
    )
    // Upper shadow ceiling molding
    drawRect(
        color = litUpper,
        topLeft = Offset(x, top),
        size = Size(width, height * 0.22f)
    )

    // Pizzeria wall accents (checkered trim, hazard bands, arcade neon, security monitors)
    when (wallType) {
        1 -> {
            // Checkered pizzeria dado stripe
            val checkerCell = ((wallU * 8f).toInt() % 2 == 0)
            val stripeColor = if (checkerCell) Color(0xFFD6CEC2) else Color(0xFF151218)
            drawRect(
                color = shadeColor(stripeColor, light * 0.95f),
                topLeft = Offset(x, top + height * 0.48f),
                size = Size(width, height * 0.09f)
            )
        }
        2 -> {
            // Gold stage valance trim
            drawRect(
                color = shadeColor(Color(0xFFD4A034), light),
                topLeft = Offset(x, top + height * 0.06f),
                size = Size(width, height * 0.07f)
            )
        }
        3 -> {
            // Yellow/black industrial workshop hazard stripe
            val hazardYellow = ((wallU * 6f).toInt() % 2 == 0)
            val bandColor = if (hazardYellow) Color(0xFFE0A526) else Color(0xFF141418)
            drawRect(
                color = shadeColor(bandColor, light * 0.95f),
                topLeft = Offset(x, top + height * 0.62f),
                size = Size(width, height * 0.08f)
            )
        }
        5 -> {
            // Arcade neon cabinet vertical glow strip
            if (wallU in 0.40f..0.58f) {
                val neonColor = if ((wallU * 10f).toInt() % 2 == 0) Color(0xFF38BDF8) else Color(0xFFF43F5E)
                val glowAlpha = max(light, (1f - dist / 8f).coerceAtLeast(0.18f))
                drawRect(
                    color = neonColor.copy(alpha = glowAlpha.coerceIn(0.2f, 0.92f)),
                    topLeft = Offset(x, top + height * 0.24f),
                    size = Size(width, height * 0.45f)
                )
            }
        }
        6 -> {
            // Security CRT monitor glow window
            if (wallU in 0.24f..0.76f) {
                val monitorGlow = max(light * 0.8f, (1f - dist / 6.5f).coerceAtLeast(0.15f))
                drawRect(
                    color = Color(0xFF4ADE80).copy(alpha = (monitorGlow * 0.45f).coerceIn(0.08f, 0.6f)),
                    topLeft = Offset(x, top + height * 0.30f),
                    size = Size(width, height * 0.24f)
                )
            }
        }
    }

    if (warmTint.alpha > 0.01f) {
        drawRect(
            color = warmTint,
            topLeft = Offset(x, top),
            size = Size(width, height)
        )
    }
}

private fun DrawScope.drawBrunoSprite(
    screenX: Float,
    horizonY: Float,
    dist: Float,
    viewportHeight: Float,
    light: Float,
    installedParts: Set<AnimatronicPartType>,
    time: Float
) {
    val scale = (viewportHeight * 0.85f) / dist
    val top = horizonY - scale * 0.48f
    val cx = screenX

    // Pedestal base
    val pedW = scale * 0.56f
    val pedH = scale * 0.22f
    val pedTop = top + scale * 0.78f
    drawRect(
        color = shadeColor(Color(0xFF2B2D38), light),
        topLeft = Offset(cx - pedW * 0.5f, pedTop),
        size = Size(pedW, pedH)
    )
    drawRect(
        color = shadeColor(Color(0xFFD99A26), light),
        topLeft = Offset(cx - pedW * 0.5f, pedTop),
        size = Size(pedW, pedH * 0.22f)
    )

    // Legs / Lower frame
    val legW = scale * 0.10f
    drawRect(
        color = shadeColor(Color(0xFF4E342E), light),
        topLeft = Offset(cx - scale * 0.16f, top + scale * 0.54f),
        size = Size(legW, scale * 0.26f)
    )
    drawRect(
        color = shadeColor(Color(0xFF4E342E), light),
        topLeft = Offset(cx + scale * 0.06f, top + scale * 0.54f),
        size = Size(legW, scale * 0.26f)
    )

    // Torso chassis
    val torsoW = scale * 0.34f
    val torsoH = scale * 0.30f
    val torsoTop = top + scale * 0.26f
    drawRect(
        color = shadeColor(Color(0xFF6B4226), light),
        topLeft = Offset(cx - torsoW * 0.5f, torsoTop),
        size = Size(torsoW, torsoH)
    )

    // Left Arm (always attached structural frame)
    drawRect(
        color = shadeColor(Color(0xFF5D3A22), light),
        topLeft = Offset(cx - torsoW * 0.5f - scale * 0.10f, torsoTop + scale * 0.02f),
        size = Size(scale * 0.09f, scale * 0.27f)
    )

    // Right Arm (depends on SERVO_MOTOR)
    if (AnimatronicPartType.SERVO_MOTOR in installedParts) {
        drawRect(
            color = shadeColor(Color(0xFF6B4226), light),
            topLeft = Offset(cx + torsoW * 0.5f + scale * 0.01f, torsoTop + scale * 0.02f),
            size = Size(scale * 0.09f, scale * 0.27f)
        )
        drawCircle(
            color = shadeColor(Color(0xFF38BDF8), max(light, 0.45f)),
            radius = scale * 0.026f,
            center = Offset(cx + torsoW * 0.5f + scale * 0.05f, torsoTop + scale * 0.06f)
        )
    } else {
        // Exposed sparking shoulder socket
        drawRect(
            color = shadeColor(Color(0xFF23252E), light),
            topLeft = Offset(cx + torsoW * 0.5f, torsoTop + scale * 0.02f),
            size = Size(scale * 0.05f, scale * 0.10f)
        )
        if (sin(time * 9f) > 0.6f) {
            drawCircle(
                color = Color(0xFFFFB347),
                radius = scale * 0.02f,
                center = Offset(cx + torsoW * 0.54f, torsoTop + scale * 0.09f)
            )
        }
    }

    // Chest Cavity (depends on POWER_CORE)
    val coreCenter = Offset(cx, torsoTop + torsoH * 0.48f)
    if (AnimatronicPartType.POWER_CORE in installedParts) {
        drawCircle(
            color = Color(0xFF4ADE80).copy(alpha = 0.85f),
            radius = scale * 0.068f,
            center = coreCenter
        )
        drawCircle(
            color = Color(0xFFE0FFF0),
            radius = scale * 0.032f,
            center = coreCenter
        )
    } else {
        drawCircle(
            color = Color(0xFF0A080C),
            radius = scale * 0.065f,
            center = coreCenter
        )
        drawCircle(
            color = Color(0xFFE53935).copy(alpha = 0.45f + 0.25f * sin(time * 4f)),
            radius = scale * 0.065f,
            center = coreCenter,
            style = Stroke(width = (scale * 0.014f).coerceAtLeast(1.5f))
        )
    }

    // Bear Ears + Top Hat
    val headSize = scale * 0.23f
    val headTop = top + scale * 0.03f
    val earR = scale * 0.055f
    drawCircle(
        color = shadeColor(Color(0xFF5D3A22), light),
        radius = earR,
        center = Offset(cx - headSize * 0.46f, headTop + earR * 0.3f)
    )
    drawCircle(
        color = shadeColor(Color(0xFF5D3A22), light),
        radius = earR,
        center = Offset(cx + headSize * 0.46f, headTop + earR * 0.3f)
    )

    // Head
    drawRect(
        color = shadeColor(Color(0xFF7B4E2D), light),
        topLeft = Offset(cx - headSize * 0.5f, headTop),
        size = Size(headSize, headSize * 0.92f)
    )
    // Tiny top hat
    drawRect(
        color = shadeColor(Color(0xFF17131B), light),
        topLeft = Offset(cx - headSize * 0.26f, headTop - scale * 0.065f),
        size = Size(headSize * 0.52f, scale * 0.065f)
    )

    // Jaw / Voice Box
    val jawTop = headTop + headSize * 0.56f
    if (AnimatronicPartType.VOICE_BOX in installedParts) {
        drawRect(
            color = shadeColor(Color(0xFFD7B899), light),
            topLeft = Offset(cx - headSize * 0.32f, jawTop),
            size = Size(headSize * 0.64f, headSize * 0.28f)
        )
    } else {
        // Hollow crooked mouth cavity
        drawRect(
            color = Color(0xFF0A060A),
            topLeft = Offset(cx - headSize * 0.28f, jawTop + scale * 0.01f),
            size = Size(headSize * 0.56f, headSize * 0.24f)
        )
    }

    // Eyes / Optic Sensor
    val eyeY = headTop + headSize * 0.32f
    val eyeOffsetX = headSize * 0.22f
    val eyeR = scale * 0.028f
    if (AnimatronicPartType.OPTIC_SENSOR in installedParts) {
        drawCircle(color = Color(0xFF38BDF8), radius = eyeR, center = Offset(cx - eyeOffsetX, eyeY))
        drawCircle(color = Color(0xFF38BDF8), radius = eyeR, center = Offset(cx + eyeOffsetX, eyeY))
        drawCircle(color = Color.White, radius = eyeR * 0.45f, center = Offset(cx - eyeOffsetX, eyeY))
        drawCircle(color = Color.White, radius = eyeR * 0.45f, center = Offset(cx + eyeOffsetX, eyeY))
    } else {
        drawCircle(color = Color(0xFF07050A), radius = eyeR * 1.15f, center = Offset(cx - eyeOffsetX, eyeY))
        drawCircle(color = Color(0xFF07050A), radius = eyeR * 1.15f, center = Offset(cx + eyeOffsetX, eyeY))
        // Faint red diagnostic pinprick in dark socket
        drawCircle(
            color = Color(0xFFE53935).copy(alpha = 0.55f),
            radius = eyeR * 0.32f,
            center = Offset(cx + eyeOffsetX, eyeY)
        )
    }
}

private fun DrawScope.drawPartPickupSprite(
    part: PartPickup,
    screenX: Float,
    horizonY: Float,
    dist: Float,
    viewportHeight: Float,
    light: Float,
    time: Float
) {
    val scale = (viewportHeight * 0.42f) / dist
    val bob = sin(time * 3.2f + part.x) * scale * 0.07f
    val cy = horizonY + scale * 0.35f + bob
    val accent = when (part.type) {
        AnimatronicPartType.OPTIC_SENSOR -> Color(0xFF38BDF8)
        AnimatronicPartType.VOICE_BOX -> Color(0xFFFFB347)
        AnimatronicPartType.SERVO_MOTOR -> Color(0xFFE879F9)
        AnimatronicPartType.POWER_CORE -> Color(0xFF4ADE80)
    }

    val vis = max(light, ((1f - dist / 4.2f).coerceAtLeast(0f)) * 0.32f)
    if (vis < 0.04f) return

    // Soft floor beacon glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accent.copy(alpha = 0.48f * vis), Color.Transparent),
            center = Offset(screenX, cy),
            radius = scale * 0.48f
        ),
        radius = scale * 0.48f,
        center = Offset(screenX, cy)
    )

    // Metallic body
    drawRect(
        color = shadeColor(Color(0xFF3A3644), max(vis, 0.35f)),
        topLeft = Offset(screenX - scale * 0.16f, cy - scale * 0.14f),
        size = Size(scale * 0.32f, scale * 0.28f)
    )
    // Glowing part core
    drawCircle(
        color = accent.copy(alpha = (0.65f + 0.35f * vis).coerceIn(0f, 1f)),
        radius = scale * 0.095f,
        center = Offset(screenX, cy)
    )
}

private fun DrawScope.drawBatterySprite(
    screenX: Float,
    horizonY: Float,
    dist: Float,
    viewportHeight: Float,
    light: Float,
    time: Float
) {
    val scale = (viewportHeight * 0.30f) / dist
    val cy = horizonY + scale * 0.55f
    val vis = max(light, ((1f - dist / 4.5f).coerceAtLeast(0f)) * 0.28f)
    if (vis < 0.04f) return

    val pulse = 0.65f + 0.35f * sin(time * 5f)
    drawRect(
        color = shadeColor(Color(0xFFE0A526), max(vis, 0.35f)),
        topLeft = Offset(screenX - scale * 0.11f, cy - scale * 0.15f),
        size = Size(scale * 0.22f, scale * 0.30f)
    )
    drawRect(
        color = Color(0xFF4ADE80).copy(alpha = pulse * max(vis, 0.4f)),
        topLeft = Offset(screenX - scale * 0.06f, cy - scale * 0.08f),
        size = Size(scale * 0.12f, scale * 0.08f)
    )
}

private fun DrawScope.drawStalkerSprite(
    stalker: StalkerState,
    screenX: Float,
    horizonY: Float,
    dist: Float,
    viewportHeight: Float,
    light: Float,
    time: Float
) {
    val scale = (viewportHeight * 0.96f) / dist
    val top = horizonY - scale * 0.48f
    val jitter = if (stalker.behavior == StalkerBehavior.STUNNED) {
        sin(time * 48f) * scale * 0.04f
    } else {
        sin(time * 4f) * scale * 0.01f
    }
    val cx = screenX + jitter

    val bodyVis = max(light, ((1f - dist / 3.8f).coerceAtLeast(0f)) * 0.22f)

    // Gaunt dark endoskeleton body
    if (bodyVis > 0.03f) {
        val metalColor = shadeColor(Color(0xFF282530), bodyVis)
        // Torso & ribcage
        drawRect(
            color = metalColor,
            topLeft = Offset(cx - scale * 0.14f, top + scale * 0.25f),
            size = Size(scale * 0.28f, scale * 0.34f)
        )
        // Skeletal limbs
        drawRect(
            color = metalColor,
            topLeft = Offset(cx - scale * 0.22f, top + scale * 0.26f),
            size = Size(scale * 0.06f, scale * 0.38f)
        )
        drawRect(
            color = metalColor,
            topLeft = Offset(cx + scale * 0.16f, top + scale * 0.26f),
            size = Size(scale * 0.06f, scale * 0.38f)
        )
        drawRect(
            color = metalColor,
            topLeft = Offset(cx - scale * 0.11f, top + scale * 0.59f),
            size = Size(scale * 0.07f, scale * 0.34f)
        )
        drawRect(
            color = metalColor,
            topLeft = Offset(cx + scale * 0.04f, top + scale * 0.59f),
            size = Size(scale * 0.07f, scale * 0.34f)
        )
        // Skull head
        drawRect(
            color = shadeColor(Color(0xFF342E3E), bodyVis),
            topLeft = Offset(cx - scale * 0.10f, top + scale * 0.04f),
            size = Size(scale * 0.20f, scale * 0.20f)
        )
    }

    // Piercing crimson eyes that glow even in darkness!
    val eyeAlpha = if (stalker.behavior == StalkerBehavior.STUNNED) 0.3f else (1f - dist / 16f).coerceIn(0.35f, 1f)
    val eyeColor = if (stalker.behavior == StalkerBehavior.STUNNED) Color(0xFF38BDF8) else Color(0xFFFF2424)
    val eyeY = top + scale * 0.12f
    val eyeR = (scale * 0.024f).coerceAtLeast(2.2f)
    drawCircle(color = eyeColor.copy(alpha = eyeAlpha), radius = eyeR, center = Offset(cx - scale * 0.045f, eyeY))
    drawCircle(color = eyeColor.copy(alpha = eyeAlpha), radius = eyeR, center = Offset(cx + scale * 0.045f, eyeY))
    drawCircle(color = Color.White.copy(alpha = eyeAlpha * 0.7f), radius = eyeR * 0.45f, center = Offset(cx - scale * 0.045f, eyeY))
    drawCircle(color = Color.White.copy(alpha = eyeAlpha * 0.7f), radius = eyeR * 0.45f, center = Offset(cx + scale * 0.045f, eyeY))
}

private fun shadeColor(color: Color, brightness: Float): Color {
    val b = brightness.coerceIn(0f, 1f)
    return Color(
        red = color.red * b,
        green = color.green * b,
        blue = color.blue * b,
        alpha = color.alpha
    )
}

private fun Color.blendOver(base: Color): Color {
    val a = this.alpha
    return Color(
        red = this.red * a + base.red * (1f - a),
        green = this.green * a + base.green * (1f - a),
        blue = this.blue * a + base.blue * (1f - a),
        alpha = 1f
    )
}
