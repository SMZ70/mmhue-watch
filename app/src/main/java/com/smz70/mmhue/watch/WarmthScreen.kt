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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Scaffold

/**
 * White colour-temperature ("warmth") for one light, on the crown.
 *
 * This is the control the light most often needs -- shades of white, not colour
 * -- so it gets the same first-class crown dial. Turning up goes warmer (higher
 * mirek, lower Kelvin); the ring tints from cool blue-white to warm amber so the
 * end you are heading for is obvious without reading the number.
 */
@Composable
fun WarmthScreen(
    ui: UiState,
    lightId: String,
    onPreviewTemp: (Int) -> Unit,
    onCommitTemp: (Int) -> Unit,
) {
    val light = ui.home?.light(lightId)

    var mirek by remember(lightId) { mutableIntStateOf(light?.colorTemp ?: ColorTemp.DEFAULT) }
    var pending by remember { mutableStateOf(false) }

    LaunchedEffect(mirek, pending) {
        if (pending) {
            kotlinx.coroutines.delay(COMMIT_DELAY_MS)
            onCommitTemp(mirek)
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

        val warmth = ColorTemp.warmth(mirek)
        // Cool blue-white -> warm amber, matching the direction of the dial.
        val tint = lerp(Color(0xFFCFE0FF), Color(0xFFFFB25A), warmth)

        CrownDial(
            centerText = "${ColorTemp.kelvin(mirek)}K",
            subText = ColorTemp.label(mirek),
            // Full ring at the warm end so "more" always reads as "more warmth".
            progress = warmth,
            ringColor = tint,
            onStep = { steps ->
                // Crown up (positive) should warm the light: higher mirek.
                mirek = ColorTemp.clamp(mirek + steps * ColorTemp.STEP)
                onPreviewTemp(mirek)
                pending = true
            },
        )
    }
}

private const val COMMIT_DELAY_MS = 250L
