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
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults

/**
 * One light, as a short menu: on/off, then brightness, warmth and colour, each
 * opening its own crown dial.
 *
 * This used to be a single brightness ring with the other controls crammed on
 * top. Two things were wrong with that: the buttons fought the rotary handler
 * for focus so the crown did nothing, and four controls stacked in a circle was
 * cramped. A plain scrollable list fixes both -- the crown scrolls the list, and
 * each control gets a full screen to itself.
 */
@Composable
fun LightScreen(
    ui: UiState,
    lightId: String,
    onToggle: () -> Unit,
    onOpenBrightness: () -> Unit,
    onOpenWarmth: () -> Unit,
    onOpenColor: () -> Unit,
) {
    val light = ui.home?.light(lightId)
    val listState = rememberScalingLazyListState()

    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
    ) {
        if (light == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
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
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
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

            item {
                Chip(
                    label = { Text("Brightness") },
                    secondaryLabel = { Text(if (light.on) "${light.brightness}%" else "Off") },
                    onClick = onOpenBrightness,
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Warmth before Colour: shades of white are the common need.
            if (light.supportsColorTemp) {
                item {
                    Chip(
                        label = { Text("Warmth") },
                        secondaryLabel = light.colorTemp?.let { { Text("${ColorTemp.kelvin(it)}K") } },
                        onClick = onOpenWarmth,
                        colors = ChipDefaults.secondaryChipColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (light.supportsColor) {
                item {
                    Chip(
                        label = { Text("Colour") },
                        secondaryLabel = light.hue?.let { { Text(Hue.name(it)) } },
                        onClick = onOpenColor,
                        colors = ChipDefaults.secondaryChipColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
