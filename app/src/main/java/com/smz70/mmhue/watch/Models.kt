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

    /**
     * Apply [edit] to one light, force it on, and recompute the room and total
     * on-counts. Used for optimistic colour/warmth changes, which implicitly turn
     * a light on because you cannot show a colour on a dark bulb.
     */
    fun withLightOn(lightId: String, edit: (Light) -> Light): HomeState {
        val target = light(lightId) ?: return this
        val newLights = lights.map { if (it.id == lightId) edit(it).copy(on = true) else it }
        return copy(
            lights = newLights,
            rooms = rooms.map { room ->
                if (room.id == target.roomId) room.copy(onCount = newLights.count { it.roomId == room.id && it.on })
                else room
            },
            onCount = newLights.count { it.on },
        )
    }

    /** Apply [edit] to every light in a room (optionally forcing them on) and
     *  recompute counts. Used for the room-level group controls. */
    fun withRoomLights(roomId: String, forceOn: Boolean, edit: (Light) -> Light): HomeState {
        val newLights = lights.map {
            if (it.roomId == roomId) edit(it).let { e -> if (forceOn) e.copy(on = true) else e } else it
        }
        return copy(
            lights = newLights,
            rooms = rooms.map { room ->
                if (room.id == roomId) room.copy(onCount = newLights.count { it.roomId == room.id && it.on })
                else room
            },
            onCount = newLights.count { it.on },
        )
    }
}

/**
 * A room presented as if it were one light: aggregate values for the group
 * sliders. A three-bulb kitchen reads as a single brightness/warmth/colour, and
 * setting it fans the value out to each bulb.
 */
class RoomAggregate(val lights: List<Light>) {
    val anyOn: Boolean get() = lights.any { it.on }
    val supportsColorTemp: Boolean get() = lights.any { it.supportsColorTemp }
    val supportsColor: Boolean get() = lights.any { it.supportsColor }

    /** Average brightness of the bulbs that are on, or of all if none are. */
    val brightness: Int
        get() {
            val lit = lights.filter { it.on }.ifEmpty { lights }
            if (lit.isEmpty()) return Brightness.MIN
            return (lit.sumOf { it.brightness } / lit.size).coerceIn(Brightness.MIN, Brightness.MAX)
        }

    val colorTemp: Int?
        get() = lights.mapNotNull { it.colorTemp }.ifEmpty { null }?.average()?.toInt()

    /** Hue of the first colour-capable bulb; averaging hues around the wheel is
     *  meaningless, so pick a representative rather than blend. */
    val hue: Float? get() = lights.firstOrNull { it.hue != null }?.hue
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
    /** Mirek (153 cool - 500 warm). Null when the light is showing a colour, not white. */
    @SerialName("color_temp") val colorTemp: Int? = null,
    @SerialName("supports_color_temp") val supportsColorTemp: Boolean = false,
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
 * White colour temperature, in mirek.
 *
 * mmhue clamps to 153 (cool daylight) - 500 (warm candle); the watch keeps the
 * same range. Lower mirek is cooler/bluer, higher is warmer/oranger -- the
 * opposite direction to Kelvin, which is why the dial converts for display.
 */
object ColorTemp {
    const val MIN = 153   // coolest, ~6500K
    const val MAX = 500   // warmest, ~2000K
    const val STEP = 10
    const val DEFAULT = 320

    fun clamp(mirek: Int): Int = mirek.coerceIn(MIN, MAX)

    /** Mirek -> Kelvin, rounded to the nearest 100 for a stable readout. */
    fun kelvin(mirek: Int): Int = (Math.round(1_000_000.0 / clamp(mirek) / 100.0) * 100).toInt()

    /** 0 at the coolest end, 1 at the warmest -- for the dial's progress ring. */
    fun warmth(mirek: Int): Float = (clamp(mirek) - MIN).toFloat() / (MAX - MIN)

    fun label(mirek: Int): String = when {
        mirek <= 200 -> "Daylight"
        mirek <= 270 -> "Cool white"
        mirek <= 350 -> "Neutral"
        mirek <= 430 -> "Warm white"
        else -> "Candle"
    }
}

/** Hue wheel, in degrees. Saturation is fixed at full for the crown dial. */
object Hue {
    const val STEP = 8f
    const val SATURATION = 1f

    fun wrap(deg: Float): Float = ((deg % 360f) + 360f) % 360f

    fun name(deg: Float): String = when (wrap(deg).toInt()) {
        in 0..14, in 345..360 -> "Red"
        in 15..44 -> "Orange"
        in 45..69 -> "Yellow"
        in 70..159 -> "Green"
        in 160..199 -> "Teal"
        in 200..254 -> "Blue"
        in 255..289 -> "Violet"
        in 290..324 -> "Magenta"
        else -> "Pink"
    }
}
