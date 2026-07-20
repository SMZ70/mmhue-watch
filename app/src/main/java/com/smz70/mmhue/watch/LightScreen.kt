package com.smz70.mmhue.watch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text

/**
 * One light: on/off and brightness.
 *
 * Brightness is bound to the rotating crown rather than a slider. A slider on a
 * 1.4" screen means covering the value with your thumb and aiming at a target a
 * few millimetres wide; the crown is a physical detented dial you can work
 * without looking, which is the entire reason to put this on a wrist at all.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LightScreen(
    ui: UiState,
    lightId: String,
    onToggle: () -> Unit,
    onPreviewBrightness: (Int) -> Unit,
    onCommitBrightness: (Int) -> Unit,
) {
    val light = ui.home?.light(lightId)
    val focusRequester = remember { FocusRequester() }

    // Pixels accumulated since the last whole step. The crown reports fine-grained
    // scroll deltas; one detent is nowhere near one brightness step.
    var scrollAccumulator by remember { mutableFloatStateOf(0f) }

    // Set while the user is actively turning. Null means "nothing pending".
    var pendingBrightness by remember { mutableIntStateOf(NO_PENDING) }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    // The Hue bridge accepts roughly ten commands a second across all lights, so
    // firing one request per rotary event would smear or drop them. Update the UI
    // live, but only send once the crown has been still for a moment.
    LaunchedEffect(pendingBrightness) {
        if (pendingBrightness != NO_PENDING) {
            kotlinx.coroutines.delay(COMMIT_DELAY_MS)
            onCommitBrightness(pendingBrightness)
            pendingBrightness = NO_PENDING
        }
    }

    Scaffold {
        if (light == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onRotaryScrollEvent { event ->
                    scrollAccumulator += event.verticalScrollPixels
                    val steps = (scrollAccumulator / PIXELS_PER_STEP).toInt()
                    if (steps != 0) {
                        scrollAccumulator -= steps * PIXELS_PER_STEP
                        val next = Brightness.clamp(light.brightness + steps * Brightness.STEP)
                        if (next != light.brightness) {
                            onPreviewBrightness(next)
                            pendingBrightness = next
                        }
                    }
                    true
                }
                .focusRequester(focusRequester)
                .focusable(),
            contentAlignment = Alignment.Center,
        ) {
            // The ring doubles as the brightness readout, so the number never has
            // to compete with a slider track for space.
            CircularProgressIndicator(
                progress = light.brightness / 100f,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                strokeWidth = 6.dp,
                indicatorColor = if (light.on) MaterialTheme.colors.primary else MaterialTheme.colors.onSurfaceVariant,
                trackColor = MaterialTheme.colors.surface,
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 28.dp),
            ) {
                Text(
                    text = light.name,
                    style = MaterialTheme.typography.caption1,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )

                Text(
                    text = if (light.on) "${light.brightness}%" else "Off",
                    style = MaterialTheme.typography.display3,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 2.dp),
                )

                Chip(
                    label = { Text(if (light.on) "Turn off" else "Turn on") },
                    onClick = onToggle,
                    colors = if (light.on) ChipDefaults.secondaryChipColors() else ChipDefaults.primaryChipColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * Roughly one brightness step per crown detent on a Pixel Watch. Tuned by feel:
 * too small and a flick slams the light to 100, too large and it takes a full
 * revolution to cross the useful range.
 */
private const val PIXELS_PER_STEP = 60f
private const val COMMIT_DELAY_MS = 250L
private const val NO_PENDING = -1
