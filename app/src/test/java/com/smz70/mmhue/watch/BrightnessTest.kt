package com.smz70.mmhue.watch

import org.junit.Assert.assertEquals
import org.junit.Test

class BrightnessTest {

    @Test
    fun `clamps to the 20-100 range the panel uses`() {
        assertEquals(20, Brightness.clamp(0))
        assertEquals(20, Brightness.clamp(19))
        assertEquals(20, Brightness.clamp(20))
        assertEquals(57, Brightness.clamp(57))
        assertEquals(100, Brightness.clamp(100))
        assertEquals(100, Brightness.clamp(101))
    }

    @Test
    fun `snaps crown values to round steps`() {
        assertEquals(50, Brightness.snap(51))
        assertEquals(55, Brightness.snap(53))
        assertEquals(20, Brightness.snap(4))
        assertEquals(100, Brightness.snap(99))
    }

    @Test
    fun `stepping never escapes the range`() {
        // Walking the crown hard in either direction must settle at a bound,
        // not overshoot into a value the bridge would reject.
        var value = 50
        repeat(100) { value = Brightness.clamp(value + Brightness.STEP) }
        assertEquals(100, value)

        repeat(100) { value = Brightness.clamp(value - Brightness.STEP) }
        assertEquals(20, value)
    }
}
