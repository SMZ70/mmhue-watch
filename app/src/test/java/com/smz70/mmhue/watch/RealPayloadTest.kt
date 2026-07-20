package com.smz70.mmhue.watch

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Parses a payload captured verbatim from the Pi (see resources/real_state.json).
 *
 * The hand-written fixtures in MmhueClientTest encode what the API is *supposed*
 * to return. This one encodes what it actually returned, including the fields
 * the watch ignores. Re-capture it with:
 *
 *     curl -s http://mm.fritz.box/api/state > app/src/test/resources/real_state.json
 */
class RealPayloadTest {

    private lateinit var server: MockWebServer
    private lateinit var client: MmhueClient

    private val payload: String by lazy {
        checkNotNull(javaClass.classLoader?.getResourceAsStream("real_state.json")) {
            "real_state.json fixture missing"
        }.bufferedReader().readText()
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = MmhueClient(baseUrl = server.url("/").toString().trimEnd('/'))
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `parses the live payload`() = runTest {
        server.enqueue(MockResponse().setBody(payload))

        val state = client.state()

        assertTrue("expected rooms in the live payload", state.rooms.isNotEmpty())
        assertTrue("expected lights in the live payload", state.lights.isNotEmpty())
        assertEquals(state.lights.size, state.total)
    }

    @Test
    fun `every light maps to a room that exists`() = runTest {
        // The room drill-down silently shows an empty list if this ever breaks,
        // which is exactly the kind of bug that is invisible until you are
        // standing in a dark kitchen.
        server.enqueue(MockResponse().setBody(payload))

        val state = client.state()

        state.lights.forEach { light ->
            assertNotNull("light ${light.name} has no room_id", light.roomId)
            assertNotNull(
                "light ${light.name} points at unknown room ${light.roomId}",
                state.room(light.roomId!!),
            )
        }
    }

    @Test
    fun `room counts agree with the lights in them`() = runTest {
        server.enqueue(MockResponse().setBody(payload))

        val state = client.state()

        state.rooms.forEach { room ->
            val lights = state.lightsIn(room.id)
            assertEquals("total mismatch for ${room.name}", room.total, lights.size)
            assertEquals("on_count mismatch for ${room.name}", room.onCount, lights.count { it.on })
        }
        assertEquals(state.onCount, state.lights.count { it.on })
    }
}
