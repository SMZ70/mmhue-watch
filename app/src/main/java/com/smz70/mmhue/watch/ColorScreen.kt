package com.smz70.mmhue.watch

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import com.smz70.mmhue.watch.Palette.matches

/**
 * A colour picker for one light: two rings of tappable swatches, vivid and soft.
 *
 * A hue wheel is the obvious idea and the wrong one on a round 480px screen --
 * the useful targets end up smaller than a fingertip and half of them sit under
 * your thumb as you reach. Fixed swatches are bigger, eyes-free, and cover the
 * colours anyone actually asks a lamp for. Tap one and it applies immediately,
 * then the screen pops back to the light.
 */
@Composable
fun ColorScreen(
    ui: UiState,
    lightId: String,
    onPick: (Swatch) -> Unit,
    onPicked: () -> Unit,
) {
    val light = ui.home?.light(lightId)

    Scaffold {
        if (light == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Text(
                    text = light.name,
                    style = MaterialTheme.typography.title3,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                )
            }
            item {
                Text(
                    text = "Colour",
                    style = MaterialTheme.typography.caption1,
                    color = MaterialTheme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }

            items(Palette.vivid.chunked(3)) { row ->
                SwatchRow(row, light) { swatch -> onPick(swatch); onPicked() }
            }

            item {
                Text(
                    text = "Soft",
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                )
            }

            items(Palette.soft.chunked(3)) { row ->
                SwatchRow(row, light) { swatch -> onPick(swatch); onPicked() }
            }
        }
    }
}

@Composable
private fun SwatchRow(row: List<Swatch>, light: Light, onPick: (Swatch) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        row.forEach { swatch ->
            val current = light.matches(swatch)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.hsv(swatch.hue, swatch.saturation, 1f))
                    .then(
                        if (current) Modifier.border(3.dp, MaterialTheme.colors.onSurface, CircleShape)
                        else Modifier
                    )
                    .clickable { onPick(swatch) },
                contentAlignment = Alignment.Center,
            ) {
                // The current colour is marked by the ring border from the
                // modifier above; the dot itself carries the hue.
            }
        }
    }
}
