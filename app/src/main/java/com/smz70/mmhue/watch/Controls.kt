package com.smz70.mmhue.watch

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.InlineSlider
import androidx.wear.compose.material.InlineSliderDefaults

/**
 * An InlineSlider that stays smooth under the finger.
 *
 * The problem it solves: driving the slider straight from the panel value made it
 * jump. A tap fired a network write, the optimistic value showed, then the next
 * poll (or a lagging bridge) briefly reported the old value and the thumb snapped
 * back and forth. Here the slider is driven by a local value that the user's taps
 * own outright; the network write is debounced until they stop, and the panel
 * value is only allowed to re-seed the slider while it is idle.
 */
@Composable
fun SmoothControl(
    title: String,
    value: Int,
    range: IntProgression,
    format: (Int) -> String,
    decreaseDesc: String,
    increaseDesc: String,
    onCommit: (Int) -> Unit,
) {
    var local by remember { mutableIntStateOf(value) }
    var editing by remember { mutableStateOf(false) }

    // Accept panel updates only when the user is not mid-adjustment.
    if (!editing && local != value) local = value

    // Commit once the crown/taps settle, then release control back to the panel.
    androidx.compose.runtime.LaunchedEffect(local, editing) {
        if (editing) {
            kotlinx.coroutines.delay(COMMIT_DELAY_MS)
            onCommit(local)
            editing = false
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        ControlLabel(title, format(local))
        InlineSlider(
            value = local.coerceIn(range.first, range.last),
            onValueChange = { local = it; editing = true },
            valueProgression = range,
            decreaseIcon = { Icon(InlineSliderDefaults.Decrease, decreaseDesc) },
            increaseIcon = { Icon(InlineSliderDefaults.Increase, increaseDesc) },
            segmented = false,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private const val COMMIT_DELAY_MS = 350L
