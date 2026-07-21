package com.smz70.mmhue.watch

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText

/**
 * Brightness for one light, on the crown -- the same dial as warmth and colour.
 *
 * Split out of the light screen so the crown has a screen to itself; sharing it
 * with tappable buttons stopped the rotary events reaching this control at all.
 */
@Composable
fun BrightnessScreen(
    ui: UiState,
    lightId: String,
    onPreview: (Int) -> Unit,
    onCommit: (Int) -> Unit,
) {
    val light = ui.home?.light(lightId)

    var pct by remember(lightId) { mutableIntStateOf(light?.brightness ?: Brightness.MIN) }
    var pending by remember { mutableStateOf(false) }

    LaunchedEffect(pct, pending) {
        if (pending) {
            kotlinx.coroutines.delay(COMMIT_DELAY_MS)
            onCommit(pct)
            pending = false
        }
    }

    Scaffold(timeText = { TimeText() }) {
        if (light == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        CrownDial(
            centerText = "$pct%",
            subText = light.name,
            progress = pct / 100f,
            ringColor = MaterialTheme.colors.primary,
            onStep = { steps ->
                pct = Brightness.clamp(pct + steps * Brightness.STEP)
                onPreview(pct)
                pending = true
            },
        )
    }
}

private const val COMMIT_DELAY_MS = 250L
