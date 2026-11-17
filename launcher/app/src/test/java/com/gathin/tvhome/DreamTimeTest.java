package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class DreamTimeTest {
    @Test
    public void dayRunsFromMidnight() {
        assertEquals(0f, DreamTime.day(0, 0, 0), 1e-6);
        assertEquals(0.5f, DreamTime.day(12, 0, 0), 1e-6);
        assertEquals((16 * 60 + 40) / 1440f, DreamTime.day(16, 40, 0), 1e-6);
    }

    @Test
    public void sunIsUpFromSixToEight() {
        assertEquals(-1f, DreamTime.sun(5 * 60 + 59), 0);
        assertEquals(0f, DreamTime.sun(6 * 60), 1e-6);
        assertEquals(0.5f, DreamTime.sun(13 * 60), 1e-6);
        assertEquals(-1f, DreamTime.sun(20 * 60), 0);
    }

    @Test
    public void moonsCrossTheNight() {
        assertEquals(-1f, DreamTime.moon(12 * 60), 0);
        assertEquals(0f, DreamTime.moon(20 * 60), 1e-6);
        assertEquals(0.4f, DreamTime.moon(0), 1e-6);   // 4 of the 10 night hours gone
        assertEquals(0.9f, DreamTime.moon(5 * 60), 1e-6);
    }

    @Test
    public void everyDigitHasAGlyph() {
        for (char c : "0123456789".toCharArray()) {
            String[] g = DreamTime.glyph(c);
            assertEquals(5, g.length);
            for (String row : g) assertEquals(3, row.length());
        }
        assertEquals(1, DreamTime.glyph(':')[0].length());
        assertNull(DreamTime.glyph('a'));
    }

    @Test
    public void uptimeReadsInHoursAndMinutes() {
        assertEquals("43m", DreamTime.uptime(43 * 60_000L));
        assertEquals("65h 43m", DreamTime.uptime((65 * 60 + 43) * 60_000L));
    }
}
