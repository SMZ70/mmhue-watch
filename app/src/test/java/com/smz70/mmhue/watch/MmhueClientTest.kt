package com.smz70.mmhue.watch

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class MmhueClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: MmhueClient

    /** Trimmed from a real GET /api/state against the Pi. */
    private val sampleState = """
        {
          "rooms": [
            {"id": "room-kitchen", "name": "Kitchen", "archetype": "kitchen", "on_count": 2, "total": 3},
            {"id": "room-bed", "name": "Bedroom", "archetype": "bedroom", "on_count": 0, "total": 1}
          ],
          "lights": [
            {"id": "l1", "name": "Kitchen 1", "room": "Kitchen", "room_id": "room-kitchen",
             "on": true, "brightness": 80, "color_temp": 366, "hue": 21.2, "saturation": 0.9,
             "supports_color": true, "supports_color_temp": true},
            {"id": "l2", "name": "Kitchen 2", "room": "Kitchen", "room_id": "room-kitchen",
             "on": true, "brightness": 55, "color_temp": null, "hue": 198.2, "saturation": 1.0,
             "supports_color": true, "supports_color_temp": true},
            {"id": "l3", "name": "Kitchen 3", "room": "Kitchen", "room_id": "room-kitchen",
             "on": false, "brightness": 100, "color_temp": null, "hue": 0.0, "saturation": 0.0,
             "supports_color": true, "supports_color_temp": true},
            {"id": "l4", "name": "Bedroom light", "room": "Bedroom", "room_id": "room-bed",
             "on": false, "brightness": 91, "color_temp": null, "hue": 198.2, "saturation": 1.0,
             "supports_color": true, "supports_color_temp": true}
          ],
          "scenes": [{"id": "s1", "name": "Relax", "room": "Kitchen"}],
          "dances": ["birthday", "rave"],
          "dance_running": null,
          "on_count": 2,
          "total": 4
        }
    """.trimIndent()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = MmhueClient(baseUrl = server.url("/").toString().trimEnd('/'))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `parses state and ignores scenes and dances`() = runTest {
        server.enqueue(MockResponse().setBody(sampleState))

        val state = client.state()

        assertEquals(2, state.onCount)
        assertEquals(4, state.total)
        assertEquals(2, state.rooms.size)
        assertEquals(4, state.lights.size)
        assertEquals("/api/state", server.takeRequest().path)
    }

    @Test
    fun `groups lights by room`() = runTest {
        server.enqueue(MockResponse().setBody(sampleState))

        val state = client.state()

        assertEquals(3, state.lightsIn("room-kitchen").size)
        assertEquals(1, state.lightsIn("room-bed").size)
        assertEquals("Kitchen 2", state.light("l2")?.name)
        assertTrue(state.room("room-kitchen")!!.anyOn)
        assertFalse(state.room("room-bed")!!.anyOn)
    }

    @Test
    fun `unknown fields do not break parsing`() = runTest {
        // The panel is a separate project and will grow fields. It must not take
        // the watch down when it does.
        server.enqueue(
            MockResponse().setBody(
                """{"rooms": [], "lights": [], "on_count": 0, "total": 0, "brand_new_field": {"a": 1}}"""
            )
        )

        val state = client.state()

        assertEquals(0, state.total)
        assertTrue(state.allOff)
    }

    @Test
    fun `builds the documented endpoint paths`() = runTest {
        repeat(5) { server.enqueue(MockResponse().setBody("{}")) }

        client.allOff()
        client.allOn()
        client.setRoom("room-kitchen", on = true)
        client.toggleLight("l1")
        client.setBrightness("l1", 60)

        assertEquals("/api/all/off", server.takeRequest().path)
        assertEquals("/api/all/on", server.takeRequest().path)
        assertEquals("/api/rooms/room-kitchen/on", server.takeRequest().path)
        assertEquals("/api/lights/l1/toggle", server.takeRequest().path)
        assertEquals("/api/lights/l1/brightness/60", server.takeRequest().path)
    }

    @Test
    fun `color sends hue and saturation, wrapping and clamping`() = runTest {
        repeat(3) { server.enqueue(MockResponse().setBody("{}")) }

        client.setColor("l1", hue = 120f, sat = 1f)
        client.setColor("l1", hue = 400f, sat = 1f)   // 400 wraps to 40
        client.setColor("l1", hue = -30f, sat = 2f)   // -30 wraps to 330, sat clamps to 1

        assertEquals("/api/lights/l1/color/120.0/1.0", server.takeRequest().path)
        assertEquals("/api/lights/l1/color/40.0/1.0", server.takeRequest().path)
        assertEquals("/api/lights/l1/color/330.0/1.0", server.takeRequest().path)
    }

    @Test
    fun `parses color fields when present`() = runTest {
        server.enqueue(MockResponse().setBody(sampleState))

        val state = client.state()
        val l1 = state.light("l1")!!

        assertTrue(l1.supportsColor)
        assertEquals(21.2f, l1.hue!!, 0.01f)
        assertEquals(0.9f, l1.saturation!!, 0.01f)
        assertTrue(l1.supportsColorTemp)
        assertEquals(366, l1.colorTemp)
    }

    @Test
    fun `color temp is clamped to the panel range before it is sent`() = runTest {
        repeat(3) { server.enqueue(MockResponse().setBody("{}")) }

        client.setColorTemp("l1", 300)
        client.setColorTemp("l1", 50)    // below cool bound -> 153
        client.setColorTemp("l1", 999)   // above warm bound -> 500

        assertEquals("/api/lights/l1/ct/300", server.takeRequest().path)
        assertEquals("/api/lights/l1/ct/153", server.takeRequest().path)
        assertEquals("/api/lights/l1/ct/500", server.takeRequest().path)
    }

    @Test
    fun `room off uses the off verb`() = runTest {
        server.enqueue(MockResponse().setBody("{}"))

        client.setRoom("room-bed", on = false)

        assertEquals("/api/rooms/room-bed/off", server.takeRequest().path)
    }

    @Test
    fun `brightness is clamped to the panel's usable range before it is sent`() = runTest {
        repeat(2) { server.enqueue(MockResponse().setBody("{}")) }

        client.setBrightness("l1", 3)
        client.setBrightness("l1", 400)

        assertEquals("/api/lights/l1/brightness/20", server.takeRequest().path)
        assertEquals("/api/lights/l1/brightness/100", server.takeRequest().path)
    }

    @Test(expected = IOException::class)
    fun `server errors surface as IOException`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))

        client.state()
    }
}
