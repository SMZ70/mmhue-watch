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
