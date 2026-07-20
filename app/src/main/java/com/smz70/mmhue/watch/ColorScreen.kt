package com.smz70.mmhue.watch

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Scaffold

/**
 * Hue picker for one light, on the crown.
 *
 * The swatch grid this replaced was quick but blunt: ten fixed colours and no
 * sense of the wheel between them. Here the crown scrubs the whole hue circle,
 * the ring shows the live colour, and it commits when you stop turning -- the
 * same gesture as brightness and warmth, so the three feel like one control.
 */
@Composable
fun ColorScreen(
    ui: UiState,
    lightId: String,
    onPreviewHue: (Float) -> Unit,
    onCommitHue: (Float) -> Unit,
) {
    val light = ui.home?.light(lightId)

    // Seed from the light's current hue if it has one; otherwise start at red.
    var hue by remember(lightId) { mutableFloatStateOf(light?.hue ?: 0f) }
    var pending by remember { mutableStateOf(false) }

    LaunchedEffect(hue, pending) {
        if (pending) {
            kotlinx.coroutines.delay(COMMIT_DELAY_MS)
            onCommitHue(hue)
            pending = false
        }
    }

    Scaffold {
        if (light == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        CrownDial(
            centerText = Hue.name(hue),
            subText = "${hue.toInt()}°",
            progress = Hue.wrap(hue) / 360f,
            ringColor = Color.hsv(Hue.wrap(hue), Hue.SATURATION, 1f),
            onStep = { steps ->
                hue = Hue.wrap(hue + steps * Hue.STEP)
                onPreviewHue(hue)
                pending = true
            },
        )
    }
}

private const val COMMIT_DELAY_MS = 250L
