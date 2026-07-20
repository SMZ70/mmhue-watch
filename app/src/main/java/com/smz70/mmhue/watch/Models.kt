package com.smz70.mmhue.watch

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The subset of GET /api/state this app cares about.
 *
 * The panel also returns scenes, dances and dance_running; the watch deliberately
 * ignores them. A wrist is for "turn the kitchen off", not for running a show.
 */
@Serializable
data class HomeState(
    val rooms: List<Room> = emptyList(),
    val lights: List<Light> = emptyList(),
    @SerialName("on_count") val onCount: Int = 0,
    val total: Int = 0,
) {
    fun lightsIn(roomId: String): List<Light> = lights.filter { it.roomId == roomId }

    fun light(lightId: String): Light? = lights.firstOrNull { it.id == lightId }

    fun room(roomId: String): Room? = rooms.firstOrNull { it.id == roomId }

    /** True when every light the bridge knows about is off. */
    val allOff: Boolean get() = onCount == 0
}

@Serializable
data class Room(
    val id: String,
    val name: String,
    val archetype: String? = null,
    @SerialName("on_count") val onCount: Int = 0,
    val total: Int = 0,
) {
    val anyOn: Boolean get() = onCount > 0
}

@Serializable
data class Light(
    val id: String,
    val name: String,
    val room: String? = null,
    @SerialName("room_id") val roomId: String? = null,
    val on: Boolean = false,
    /** Percent, 0-100. The bridge reports the last set value even while off. */
    val brightness: Int = 0,
    /** Degrees, 0-360. Null when the light has never been given a colour. */
    val hue: Float? = null,
    /** 0-1. */
    val saturation: Float? = null,
    @SerialName("supports_color") val supportsColor: Boolean = false,
)

/**
 * mmhue's web panel clamps brightness to 20-100. Below roughly 20% the bulbs
 * behave inconsistently -- some cut out entirely -- so the watch keeps the same
 * floor rather than inventing a different one.
 */
object Brightness {
    const val MIN = 20
    const val MAX = 100
    const val STEP = 5

    fun clamp(pct: Int): Int = pct.coerceIn(MIN, MAX)

    /** Snap to the nearest step so crown turns land on round numbers. */
    fun snap(pct: Int): Int = clamp((Math.round(pct / STEP.toFloat()) * STEP))
}

/**
 * A named colour the watch can send: a hue in degrees and a saturation.
 *
 * mmhue's set_color takes hue 0-360 and saturation 0-1 and converts to CIE xy on
 * the Pi, so the watch only has to name a point in HSV. A picker wheel is a poor
 * fit for a fingertip on a round screen, so this is a fixed palette of tappable
 * swatches instead -- the same shape the web panel uses.
 */
data class Swatch(val label: String, val hue: Float, val saturation: Float)

object Palette {
    /** Vivid ring, saturation 1.0. */
    val vivid: List<Swatch> = listOf(
        Swatch("Red", 0f, 1f),
        Swatch("Orange", 30f, 1f),
        Swatch("Yellow", 55f, 1f),
        Swatch("Green", 120f, 1f),
        Swatch("Teal", 175f, 1f),
        Swatch("Cyan", 195f, 1f),
        Swatch("Blue", 225f, 1f),
        Swatch("Violet", 265f, 1f),
        Swatch("Magenta", 300f, 1f),
        Swatch("Pink", 330f, 1f),
    )

    /** Soft ring, same hues at lower saturation -- the pastels the panel author
     *  added saturation control specifically to reach. */
    val soft: List<Swatch> = listOf(
        Swatch("Peach", 25f, 0.45f),
        Swatch("Lime", 90f, 0.45f),
        Swatch("Mint", 150f, 0.4f),
        Swatch("Sky", 205f, 0.45f),
        Swatch("Lavender", 260f, 0.4f),
        Swatch("Rose", 335f, 0.4f),
    )

    /** True when this light is roughly showing the given swatch, so the picker
     *  can mark the current one. Hue is circular, hence the wrap-around distance. */
    fun Light.matches(swatch: Swatch): Boolean {
        val h = hue ?: return false
        val s = saturation ?: return false
        val dh = kotlin.math.abs(h - swatch.hue).let { minOf(it, 360f - it) }
        return dh <= 8f && kotlin.math.abs(s - swatch.saturation) <= 0.12f
    }
}
