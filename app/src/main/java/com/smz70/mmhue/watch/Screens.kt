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
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.SplitToggleChip
import androidx.wear.compose.material.Text
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

    Scaffold(vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }) {
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

/** The lights in one room. Tap the switch to toggle, tap the name for brightness. */
@Composable
fun RoomScreen(
    ui: UiState,
    roomId: String,
    onLightToggle: (String) -> Unit,
    onLightOpen: (String) -> Unit,
) {
    val home = ui.home
    val room = home?.room(roomId)

    Scaffold(vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }) {
        if (home == null || room == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Text(
                    text = room.name,
                    style = MaterialTheme.typography.title3,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
            }

            items(home.lightsIn(roomId), key = { it.id }) { light ->
                SplitToggleChip(
                    checked = light.on,
                    onCheckedChange = { onLightToggle(light.id) },
                    label = { Text(light.name, maxLines = 1) },
                    secondaryLabel = { Text(if (light.on) "${light.brightness}%" else "Off") },
                    onClick = { onLightOpen(light.id) },
                    toggleControl = { androidx.wear.compose.material.Switch(checked = light.on) },
                    modifier = Modifier.fillMaxWidth(),
                )
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
