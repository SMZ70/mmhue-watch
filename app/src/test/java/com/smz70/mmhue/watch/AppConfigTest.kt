package com.smz70.mmhue.watch

import org.junit.Assert.assertEquals
import org.junit.Test

class AppConfigTest {

    @Test
    fun `adds http scheme when none is given`() {
        assertEquals("http://mm.fritz.box", AppConfig.normalizeUrl("mm.fritz.box"))
        assertEquals("http://192.168.1.5", AppConfig.normalizeUrl("192.168.1.5"))
    }

    @Test
    fun `keeps an explicit scheme`() {
        assertEquals("http://host", AppConfig.normalizeUrl("http://host"))
        assertEquals("https://host", AppConfig.normalizeUrl("https://host"))
    }

    @Test
    fun `trims whitespace and trailing slashes`() {
        assertEquals("http://host", AppConfig.normalizeUrl("  http://host/  "))
        assertEquals("http://host:8000", AppConfig.normalizeUrl("host:8000/"))
    }

    @Test
    fun `blank falls back to the build default`() {
        assertEquals(BuildConfig.MMHUE_BASE_URL, AppConfig.normalizeUrl("   "))
    }
}
