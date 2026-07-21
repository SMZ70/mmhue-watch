package com.smz70.mmhue.watch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText

/**
 * App settings. For now: the mmhue address, so the app is not tied to one home.
 *
 * Text entry on a watch is never lovely, but this is a set-once field and the
 * system input method offers voice and handwriting as well as the keyboard. The
 * field is pre-filled with the current address so most edits are a couple of
 * characters, and the value is normalised on save (adds http://, trims slashes).
 */
@Composable
fun SettingsScreen(
    currentUrl: String,
    onSave: (String) -> Unit,
) {
    var text by remember { mutableStateOf(currentUrl) }
    val listState = rememberScalingLazyListState()

    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
    ) {
        ScalingLazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
            item {
                Text(
                    text = "mmhue address",
                    style = MaterialTheme.typography.title3,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                )
            }
            item {
                Text(
                    text = "The panel's address on your Wi-Fi",
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colors.surface, RoundedCornerShape(24.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = MaterialTheme.colors.onSurface,
                            textAlign = TextAlign.Center,
                            fontSize = MaterialTheme.typography.body1.fontSize,
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colors.primary),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { onSave(text) }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item {
                Chip(
                    label = { Text("Save") },
                    onClick = { onSave(text) },
                    colors = ChipDefaults.primaryChipColors(),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
            item {
                Chip(
                    label = { Text("Reset to default") },
                    onClick = { text = BuildConfig.MMHUE_BASE_URL; onSave(BuildConfig.MMHUE_BASE_URL) },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
        }
    }
}
