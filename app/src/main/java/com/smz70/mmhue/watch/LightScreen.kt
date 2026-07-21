package com.smz70.mmhue.watch

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.InlineSlider
import androidx.wear.compose.material.InlineSliderDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults

/**
 * One light, as a control panel: power, then brightness, warmth and colour as
 * in-place sliders you adjust without leaving the screen.
 *
 * Earlier versions buried each control behind its own full-screen dial, so
 * changing brightness was four taps deep. Wear's InlineSlider puts the minus/plus
 * right on the row, so the common actions -- dim a light, warm it up -- happen
 * where you land. The crown scrolls this list; the sliders are touch, so nothing
 * depends on the crown working.
 */
@Composable
fun LightScreen(
    ui: UiState,
    lightId: String,
    onToggle: () -> Unit,
    onBrightness: (Int) -> Unit,
    onWarmth: (Int) -> Unit,
    onHue: (Int) -> Unit,
) {
    val light = ui.home?.light(lightId)
    val listState = rememberScalingLazyListState()

    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
    ) {
        if (light == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("…")
            }
            return@Scaffold
        }

        ScalingLazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
            item {
                Text(
                    text = light.name,
                    style = MaterialTheme.typography.title3,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                )
            }

            item {
                ToggleChip(
                    checked = light.on,
                    onCheckedChange = { onToggle() },
                    label = { Text(if (light.on) "On" else "Off") },
                    toggleControl = { androidx.wear.compose.material.Switch(checked = light.on) },
                    colors = ToggleChipDefaults.toggleChipColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // The sliders below only make sense on a lit bulb; when it is off they
            // read the last-known values and turning any of them nudges it back on.
            item {
                ControlLabel("Brightness", if (light.on) "${light.brightness}%" else "off")
            }
            item {
                InlineSlider(
                    value = light.brightness.coerceIn(Brightness.MIN, Brightness.MAX),
                    onValueChange = onBrightness,
                    valueProgression = Brightness.MIN..Brightness.MAX step Brightness.STEP,
                    decreaseIcon = { Icon(InlineSliderDefaults.Decrease, "dimmer") },
                    increaseIcon = { Icon(InlineSliderDefaults.Increase, "brighter") },
                    segmented = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (light.supportsColorTemp) {
                item {
                    ControlLabel("Warmth", light.colorTemp?.let { "${ColorTemp.kelvin(it)}K" } ?: "—")
                }
                item {
                    InlineSlider(
                        value = (light.colorTemp ?: ColorTemp.DEFAULT).coerceIn(ColorTemp.MIN, ColorTemp.MAX),
                        onValueChange = onWarmth,
                        valueProgression = ColorTemp.MIN..ColorTemp.MAX step ColorTemp.STEP,
                        decreaseIcon = { Icon(InlineSliderDefaults.Decrease, "cooler") },
                        increaseIcon = { Icon(InlineSliderDefaults.Increase, "warmer") },
                        segmented = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (light.supportsColor) {
                item {
                    ControlLabel("Colour", light.hue?.let { Hue.name(it) } ?: "—")
                }
                item {
                    InlineSlider(
                        value = (light.hue?.toInt() ?: 0).coerceIn(0, HUE_MAX),
                        onValueChange = onHue,
                        valueProgression = 0..HUE_MAX step 10,
                        decreaseIcon = { Icon(InlineSliderDefaults.Decrease, "hue down") },
                        increaseIcon = { Icon(InlineSliderDefaults.Increase, "hue up") },
                        segmented = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
internal fun ControlLabel(name: String, value: String) {
    Text(
        text = "$name · $value",
        style = MaterialTheme.typography.caption1,
        color = MaterialTheme.colors.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
    )
}

// Hue slider stops one step short of 360 because 0 and 360 are the same red.
private const val HUE_MAX = 350
