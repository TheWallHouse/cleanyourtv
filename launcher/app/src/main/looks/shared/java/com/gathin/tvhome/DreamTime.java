package com.gathin.tvhome;

/**
 * The arithmetic behind the looks' screensavers (LookDream): how far the day has gone, where Dune's sun or moons sit,
 * the console clock's block digits. Kept free of Android so it is unit-tested.
 */
final class DreamTime {
    private DreamTime() {
    }

    /** The day gone by at h:m:s, 0 at midnight to just under 1. */
    static float day(int h, int m, int s) {
        return (h * 3600 + m * 60 + s) / 86400f;
    }

    /** Dune's sun is up from 06:00 to 20:00. */
    static final int SUNRISE = 6 * 60, SUNSET = 20 * 60;

    /** Dune: how far the sun has gone across the sky at this minute of the day (0 rising, 1 setting), or -1 at night. */
    static float sun(int minute) {
        if (minute < SUNRISE || minute >= SUNSET) return -1f;
        return (minute - SUNRISE) / (float) (SUNSET - SUNRISE);
    }

    /** Dune: how far the moons have gone across the night sky (0 at sunset, 1 at sunrise), or -1 by day. */
    static float moon(int minute) {
        if (sun(minute) >= 0) return -1f;
        int night = 1440 - SUNSET + SUNRISE;
        return ((minute - SUNSET + 1440) % 1440) / (float) night;
    }

    /** The console clock's digits, 3 blocks wide and 5 high ("1" lit); ':' is one wide. Null for anything else. */
    static String[] glyph(char c) {
        switch (c) {
            case '0': return new String[]{"111", "101", "101", "101", "111"};
            case '1': return new String[]{"010", "110", "010", "010", "111"};
            case '2': return new String[]{"111", "001", "111", "100", "111"};
            case '3': return new String[]{"111", "001", "111", "001", "111"};
            case '4': return new String[]{"101", "101", "111", "001", "001"};
            case '5': return new String[]{"111", "100", "111", "001", "111"};
            case '6': return new String[]{"111", "100", "111", "101", "111"};
            case '7': return new String[]{"111", "001", "001", "001", "001"};
            case '8': return new String[]{"111", "101", "111", "101", "111"};
            case '9': return new String[]{"111", "101", "111", "001", "111"};
            case ':': return new String[]{"0", "1", "0", "1", "0"};
            default: return null;
        }
    }

    /** Uptime as "65h 43m" (or "43m" under an hour). */
    static String uptime(long ms) {
        long minutes = ms / 60_000;
        return minutes >= 60 ? (minutes / 60) + "h " + (minutes % 60) + "m" : minutes + "m";
    }
}
