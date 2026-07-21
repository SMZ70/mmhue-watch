package com.smz70.mmhue.watch

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

/**
 * A full-screen value control: a progress ring, the value in the middle, and a
 * minus/plus pair to change it. The rotating crown adjusts it too, but only as a
 * bonus -- the buttons are the real control, so the screen works even on a unit
 * where the crown does nothing.
 *
 * The caller owns the value. [onStep] is called with +1/-1 (times the steps a
 * crown turn covered, or exactly 1 from a button); the caller clamps and applies.
 * Rotary events arrive in bursts, so callers debounce the network write.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalWearFoundationApi::class)
@Composable
fun CrownDial(
    centerText: String,
    subText: String,
    progress: Float,
    ringColor: Color,
    onStep: (Int) -> Unit,
) {
    // Wear-specific: binds rotary focus to this screen being the active one. A
    // plain FocusRequester + requestFocus() silently fails to grab the crown,
    // which reads on-device as "the crown does nothing".
    val focusRequester = rememberActiveFocusRequester()
    val accumulator = remember { mutableFloatStateOf(0f) }

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
            modifier = Modifier.fillMaxSize().padding(3.dp),
            strokeWidth = 8.dp,
            indicatorColor = ringColor,
            trackColor = MaterialTheme.colors.surface,
        )

        // Stacked, not flanked: on a round screen a value plus two side buttons
        // runs wider than the glass and clips the "+". Value on top, the minus
        // and plus beneath it, always fits.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 40.dp),
        ) {
            Text(
                text = centerText,
                style = MaterialTheme.typography.display3,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subText,
                style = MaterialTheme.typography.caption2,
                color = MaterialTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(top = 2.dp),
            ) {
                StepButton("−", onClick = { onStep(-1) })   // minus sign
                StepButton("+", onClick = { onStep(1) })
            }
        }
    }
}

@Composable
private fun StepButton(symbol: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        colors = ButtonDefaults.secondaryButtonColors(),
    ) {
        Text(symbol, style = MaterialTheme.typography.title2)
    }
}

/**
 * Crown sensitivity: pixels of scroll per value step. Tuned by feel on a Pixel
 * Watch, and shared by every dial so they feel the same. Only matters when the
 * crown works; the buttons step by exactly one regardless.
 */
const val PIXELS_PER_STEP = 55f
