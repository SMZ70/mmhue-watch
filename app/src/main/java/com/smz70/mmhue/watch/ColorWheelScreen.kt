package com.smz70.mmhue.watch

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Scaffold
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * A proper colour picker: an HSV wheel you tap or drag. Angle is hue, distance
 * from the centre is saturation, so the middle is white and the rim is vivid --
 * pastels live in between. This replaces the hue "+/-" slider, which could only
 * walk the rim one notch at a time and had no way to reach a soft colour.
 *
 * The chosen point is committed a beat after you stop moving, so dragging round
 * the wheel does not flood the bridge; the marker tracks your finger live.
 */
@Composable
fun ColorWheelScreen(
    initialHue: Float,
    initialSat: Float,
    onPick: (hue: Float, sat: Float) -> Unit,
) {
    var hue by remember { mutableFloatStateOf(initialHue) }
    var sat by remember { mutableFloatStateOf(initialSat) }
    var dirty by remember { mutableStateOf(false) }

    LaunchedEffect(hue, sat, dirty) {
        if (dirty) {
            kotlinx.coroutines.delay(180)
            onPick(hue, sat)
            dirty = false
        }
    }

    // Turn a touch point into hue/saturation relative to the wheel centre.
    fun pick(pos: Offset, radiusPx: Float, centre: Offset) {
        val dx = pos.x - centre.x
        val dy = pos.y - centre.y
        val r = hypot(dx, dy)
        sat = (r / radiusPx).coerceIn(0f, 1f)
        val deg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        hue = ((deg % 360f) + 360f) % 360f
        dirty = true
    }

    Scaffold(timeText = { androidx.wear.compose.material.TimeText() }) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .pointerInput(Unit) {
                        val centre = Offset(size.width / 2f, size.height / 2f)
                        val radius = minOf(size.width, size.height) / 2f
                        // Only claim touches that start inside the wheel. Anything
                        // outside -- the margin ring at the screen edges -- is left
                        // unconsumed so the swipe-from-edge back gesture still works.
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val d0 = hypot(down.position.x - centre.x, down.position.y - centre.y)
                            if (d0 > radius) return@awaitEachGesture
                            pick(down.position, radius, centre)
                            down.consume()
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (!change.pressed) break
                                pick(change.position, radius, centre)
                                change.consume()
                            }
                        }
                    },
            ) {
                val radius = size.minDimension / 2f
                val centre = Offset(size.width / 2f, size.height / 2f)

                // Hue around the rim (sweep gradient starts at 3 o'clock, matching atan2).
                val hueColors = (0..360 step 30).map { Color.hsv(it.toFloat(), 1f, 1f) }
                drawCircle(brush = Brush.sweepGradient(hueColors, center = centre), radius = radius, center = centre)
                // White centre fading out gives the saturation axis.
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color.White, Color.Transparent),
                        center = centre,
                        radius = radius,
                    ),
                    radius = radius,
                    center = centre,
                )

                // Marker at the current selection.
                val a = Math.toRadians(hue.toDouble())
                val mx = centre.x + (cos(a) * sat * radius).toFloat()
                val my = centre.y + (sin(a) * sat * radius).toFloat()
                drawCircle(Color.hsv(hue, sat, 1f), radius = 14f, center = Offset(mx, my))
                drawCircle(Color.White, radius = 14f, center = Offset(mx, my), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
            }
        }
    }
}
