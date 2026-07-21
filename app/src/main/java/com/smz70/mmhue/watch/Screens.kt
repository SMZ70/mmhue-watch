package com.smz70.mmhue.watch

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.InlineSlider
import androidx.wear.compose.material.InlineSliderDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.SplitToggleChip
import androidx.wear.compose.material.Switch
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition

/**
 * The root screen. Three things earn the top of the list: how many lights are on,
 * All off, and All on. Everything else is below the fold.
 */
@Composable
fun HomeScreen(
    ui: UiState,
    onAllOn: () -> Unit,
    onAllOff: () -> Unit,
    onRoomToggle: (String, Boolean) -> Unit,
    onRoomOpen: (String) -> Unit,
    onRetry: () -> Unit,
) {
    val listState = rememberScalingLazyListState()

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { androidx.wear.compose.material.PositionIndicator(scalingLazyListState = listState) },
    ) {
        val home = ui.home
        if (home == null) {
            LoadingOrError(ui = ui, onRetry = onRetry)
            return@Scaffold
        }

        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
        ) {
            item {
                Text(
                    text = "${home.onCount} of ${home.total} on",
                    style = MaterialTheme.typography.title3,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
            }

            // All off first: it is the button you reach for in a hurry, and putting
            // it above All on means you never have to aim past a destructive
            // neighbour to hit it.
            item {
                Chip(
                    label = { Text("All off") },
                    onClick = onAllOff,
                    colors = ChipDefaults.primaryChipColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                Chip(
                    label = { Text("All on") },
                    onClick = onAllOn,
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                Text(
                    text = "Rooms",
                    style = MaterialTheme.typography.caption1,
                    color = MaterialTheme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            items(home.rooms, key = { it.id }) { room ->
                SplitToggleChip(
                    checked = room.anyOn,
                    onCheckedChange = { on -> onRoomToggle(room.id, on) },
                    label = { Text(room.name, maxLines = 1) },
                    secondaryLabel = { Text("${room.onCount} of ${room.total} on") },
                    onClick = { onRoomOpen(room.id) },
                    toggleControl = { androidx.wear.compose.material.Switch(checked = room.anyOn) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (ui.error != null) {
                item { ErrorLine(ui.error) }
            }
        }
    }
}

/**
 * A room as one grouped light: a single power toggle and brightness/warmth/colour
 * that drive every bulb at once, with the individual bulbs tucked behind an
 * expander. A three-bulb kitchen reads and controls as one, until you want a
 * single bulb.
 */
@Composable
fun RoomScreen(
    ui: UiState,
    roomId: String,
    onRoomToggle: (Boolean) -> Unit,
    onRoomBrightness: (Int) -> Unit,
    onRoomWarmth: (Int) -> Unit,
    onRoomHue: (Int) -> Unit,
    onLightToggle: (String) -> Unit,
    onLightOpen: (String) -> Unit,
) {
    val home = ui.home
    val room = home?.room(roomId)
    val listState = rememberScalingLazyListState()
    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { androidx.wear.compose.material.PositionIndicator(scalingLazyListState = listState) },
    ) {
        if (home == null || room == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val lights = home.lightsIn(roomId)
        val group = RoomAggregate(lights)

        ScalingLazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
            item {
                Text(
                    text = room.name,
                    style = MaterialTheme.typography.title3,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                )
            }

            item {
                ToggleChip(
                    checked = group.anyOn,
                    onCheckedChange = { on -> onRoomToggle(on) },
                    label = { Text(if (group.anyOn) "On" else "Off") },
                    secondaryLabel = { Text("${room.onCount} of ${room.total} on") },
                    toggleControl = { Switch(checked = group.anyOn) },
                    colors = ToggleChipDefaults.toggleChipColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Group sliders: one value, fanned out to every bulb in the room.
            item { ControlLabel("Brightness", if (group.anyOn) "${group.brightness}%" else "off") }
            item {
                InlineSlider(
                    value = group.brightness,
                    onValueChange = onRoomBrightness,
                    valueProgression = Brightness.MIN..Brightness.MAX step Brightness.STEP,
                    decreaseIcon = { Icon(InlineSliderDefaults.Decrease, "dimmer") },
                    increaseIcon = { Icon(InlineSliderDefaults.Increase, "brighter") },
                    segmented = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (group.supportsColorTemp) {
                item { ControlLabel("Warmth", group.colorTemp?.let { "${ColorTemp.kelvin(it)}K" } ?: "—") }
                item {
                    InlineSlider(
                        value = (group.colorTemp ?: ColorTemp.DEFAULT).coerceIn(ColorTemp.MIN, ColorTemp.MAX),
                        onValueChange = onRoomWarmth,
                        valueProgression = ColorTemp.MIN..ColorTemp.MAX step ColorTemp.STEP,
                        decreaseIcon = { Icon(InlineSliderDefaults.Decrease, "cooler") },
                        increaseIcon = { Icon(InlineSliderDefaults.Increase, "warmer") },
                        segmented = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (group.supportsColor) {
                item { ControlLabel("Colour", group.hue?.let { Hue.name(it) } ?: "—") }
                item {
                    InlineSlider(
                        value = (group.hue?.toInt() ?: 0).coerceIn(0, 350),
                        onValueChange = onRoomHue,
                        valueProgression = 0..350 step 10,
                        decreaseIcon = { Icon(InlineSliderDefaults.Decrease, "hue down") },
                        increaseIcon = { Icon(InlineSliderDefaults.Increase, "hue up") },
                        segmented = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Expander: reveal the individual bulbs only when wanted.
            if (lights.size > 1) {
                item {
                    Chip(
                        label = { Text(if (expanded) "Hide lights" else "Lights (${lights.size})") },
                        onClick = { expanded = !expanded },
                        colors = ChipDefaults.secondaryChipColors(),
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    )
                }
            }

            if (expanded || lights.size == 1) {
                items(lights, key = { it.id }) { light ->
                    SplitToggleChip(
                        checked = light.on,
                        onCheckedChange = { onLightToggle(light.id) },
                        label = { Text(light.name, maxLines = 1) },
                        secondaryLabel = { Text(if (light.on) "${light.brightness}%" else "Off") },
                        onClick = { onLightOpen(light.id) },
                        toggleControl = { Switch(checked = light.on) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (ui.error != null) {
                item { ErrorLine(ui.error) }
            }
        }
    }
}

@Composable
private fun LoadingOrError(ui: UiState, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (ui.loading) {
            CircularProgressIndicator()
        } else {
            ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
                item { Text("mmhue", style = MaterialTheme.typography.title3) }
                item { ErrorLine(ui.error ?: "No data") }
                item {
                    Chip(
                        label = { Text("Retry") },
                        onClick = onRetry,
                        colors = ChipDefaults.primaryChipColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorLine(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.caption2,
        color = MaterialTheme.colors.error,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
    )
}
