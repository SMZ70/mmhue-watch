package com.smz70.mmhue.watch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val home: HomeState? = null,
    val loading: Boolean = true,
    /** Non-null when the last network call failed. Cleared by the next success. */
    val error: String? = null,
)

/**
 * Holds the panel state and applies changes optimistically.
 *
 * The web panel re-polls every 2.5s and lets that be the source of truth. That is
 * fine on a phone but on a watch it reads as lag: you tap, nothing happens, then
 * a beat later the row flips. Here a tap mutates local state immediately, the
 * request goes out, and the authoritative refresh reconciles. If the request
 * fails we snap back to the last known-good state and surface the error.
 */
class HomeViewModel(
    private val client: MmhueClient = MmhueClient(),
) : ViewModel() {

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private var pollJob: Job? = null

    /** Poll while the app is on screen. Stopped in onPause so we do not burn battery. */
    fun startPolling() {
        if (pollJob?.isActive == true) return
        pollJob = viewModelScope.launch {
            while (true) {
                refresh()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    fun refresh() {
        viewModelScope.launch { fetch() }
    }

    private suspend fun fetch() {
        try {
            val home = client.state()
            _ui.update { it.copy(home = home, loading = false, error = null) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _ui.update { it.copy(loading = false, error = e.friendlyMessage()) }
        }
    }

    fun allOn() = optimistic(
        mutate = { home -> home.copy(onCount = home.total, lights = home.lights.map { it.copy(on = true) }, rooms = home.rooms.map { it.copy(onCount = it.total) }) },
        call = { client.allOn() },
    )

    fun allOff() = optimistic(
        mutate = { home -> home.copy(onCount = 0, lights = home.lights.map { it.copy(on = false) }, rooms = home.rooms.map { it.copy(onCount = 0) }) },
        call = { client.allOff() },
    )

    fun setRoom(roomId: String, on: Boolean) = optimistic(
        mutate = { home ->
            val lights = home.lights.map { if (it.roomId == roomId) it.copy(on = on) else it }
            home.copy(
                lights = lights,
                rooms = home.rooms.map { if (it.id == roomId) it.copy(onCount = if (on) it.total else 0) else it },
                onCount = lights.count { it.on },
            )
        },
        call = { client.setRoom(roomId, on) },
    )

    fun toggleLight(lightId: String) = optimistic(
        mutate = { home ->
            val target = home.light(lightId) ?: return@optimistic home
            val nowOn = !target.on
            val lights = home.lights.map { if (it.id == lightId) it.copy(on = nowOn) else it }
            home.copy(
                lights = lights,
                rooms = home.rooms.map { room ->
                    if (room.id == target.roomId) room.copy(onCount = lights.count { it.roomId == room.id && it.on })
                    else room
                },
                onCount = lights.count { it.on },
            )
        },
        call = { client.toggleLight(lightId) },
    )

    /**
     * Brightness is driven by the crown, which emits a burst of events per turn.
     * Firing one request per event would swamp the bridge (the Hue REST API takes
     * roughly ten commands a second across all lights), so the UI updates on every
     * event but only the settled value is sent -- see BrightnessDial.
     */
    fun setBrightness(lightId: String, pct: Int) = optimistic(
        mutate = { home ->
            home.copy(lights = home.lights.map { if (it.id == lightId) it.copy(brightness = Brightness.clamp(pct)) else it })
        },
        call = { client.setBrightness(lightId, pct) },
    )

    /**
     * Setting a colour turns the light on (you cannot see a colour that is off),
     * so reflect that optimistically too, matching what the bridge will do.
     */
    fun setColor(lightId: String, hue: Float, sat: Float) = optimistic(
        mutate = { home ->
            val lights = home.lights.map {
                if (it.id == lightId) it.copy(hue = hue, saturation = sat, on = true) else it
            }
            home.copy(
                lights = lights,
                rooms = home.rooms.map { room ->
                    val target = home.light(lightId)
                    if (room.id == target?.roomId) room.copy(onCount = lights.count { it.roomId == room.id && it.on })
                    else room
                },
                onCount = lights.count { it.on },
            )
        },
        call = { client.setColor(lightId, hue, sat) },
    )

    /** Local-only brightness update, for live crown feedback before the value settles. */
    fun previewBrightness(lightId: String, pct: Int) {
        _ui.update { state ->
            val home = state.home ?: return@update state
            state.copy(home = home.copy(lights = home.lights.map {
                if (it.id == lightId) it.copy(brightness = Brightness.clamp(pct)) else it
            }))
        }
    }

    private fun optimistic(mutate: (HomeState) -> HomeState, call: suspend () -> Unit) {
        val snapshot = _ui.value.home
        if (snapshot != null) {
            _ui.update { it.copy(home = mutate(snapshot), error = null) }
        }
        viewModelScope.launch {
            try {
                call()
                fetch()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Roll back to what we knew before the tap, then say why.
                _ui.update { it.copy(home = snapshot, error = e.friendlyMessage()) }
            }
        }
    }

    private companion object {
        const val POLL_INTERVAL_MS = 3_000L
    }
}

/** Watch screens are tiny. Keep failures to something that fits. */
private fun Exception.friendlyMessage(): String = when (this) {
    is java.net.UnknownHostException -> "Can't find mm.fritz.box"
    is java.net.SocketTimeoutException -> "Panel not responding"
    is java.net.ConnectException -> "Can't reach panel"
    else -> message?.take(60) ?: "Something went wrong"
}
