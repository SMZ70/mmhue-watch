package com.smz70.mmhue.watch

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

/**
 * A full-screen circular control driven by the rotating crown.
 *
 * This is the shape the brightness screen established and the one the light
 * detail screens now share: a coloured progress ring that doubles as the readout,
 * a value in the middle, and the crown as the input. No sliders -- a physical
 * dial you can work without looking is the whole point of doing this on a wrist.
 *
 * The caller owns the value. Each crown detent calls [onStep] with +1/-1 (times
 * however many steps the turn covered); the caller clamps and applies. Rotary
 * events arrive in bursts, so callers debounce the network write themselves.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CrownDial(
    centerText: String,
    subText: String,
    progress: Float,
    ringColor: Color,
    onStep: (Int) -> Unit,
    bottom: @Composable (() -> Unit)? = null,
) {
    val focusRequester = remember { FocusRequester() }
    val accumulator = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onRotaryScrollEvent { event ->
                accumulator.floatValue += event.verticalScrollPixels
                val steps = (accumulator.floatValue / PIXELS_PER_STEP).toInt()
                if (steps != 0) {
                    accumulator.floatValue -= steps * PIXELS_PER_STEP
                    onStep(steps)
                }
                true
            }
            .focusRequester(focusRequester)
            .focusable(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = progress.coerceIn(0f, 1f),
            modifier = Modifier.fillMaxSize().padding(4.dp),
            strokeWidth = 8.dp,
            indicatorColor = ringColor,
            trackColor = MaterialTheme.colors.surface,
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 30.dp),
        ) {
            Text(
                text = centerText,
                style = MaterialTheme.typography.title2,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Text(
                text = subText,
                style = MaterialTheme.typography.caption1,
                color = MaterialTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            if (bottom != null) {
                Box(Modifier.padding(top = 6.dp)) { bottom() }
            }
        }
    }
}

/**
 * Crown sensitivity: pixels of scroll per value step. Tuned by feel on a Pixel
 * Watch -- smaller slams the value across in one flick, larger makes a full turn
 * barely move it. Shared by every dial so they all feel the same under the finger.
 */
const val PIXELS_PER_STEP = 55f
