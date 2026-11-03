package com.gathin.tvhome;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class SettingsFileTest {

    private static Object v(String key, Object value) {
        return SettingsFile.valid(key, value, false);
    }

    @Test
    public void onlyLauncherSettingsKeysAreAccepted() {
        assertNull(v("dock_apps", "x"));            // a theme key: config.json, not settings.json
        assertNull(v("theme_signature", "x"));      // bookkeeping the launcher keeps for itself
        assertNull(v(Config.SETTINGS_REVISION_KEY, 3));
        assertTrue(SettingsFile.KEYS.containsKey(NothingTone.KEY));
    }

    @Test
    public void numbersKeepToWhatTheTvOffers() {
        assertEquals(5, v("columns", 5));
        assertEquals(7, v("columns", 7.0));
        assertNull(v("columns", 8));
        assertNull(v("columns", 5.5));
        assertNull(v("columns", "5"));
        assertEquals(0, v(Config.ROWS_BELOW_DOCK_KEY, 0));
        assertNull(v(Config.ROWS_BELOW_DOCK_KEY, 4));
        assertEquals(70, v(Config.DOCK_OPACITY_KEY, 70));
        assertNull(v(Config.DOCK_OPACITY_KEY, 60));
    }

    @Test
    public void choicesKeepToTheirValues() {
        assertEquals("24", v(Config.CLOCK_FORMAT_KEY, "24"));
        assertNull(v(Config.CLOCK_FORMAT_KEY, "25"));
        assertEquals(NothingTone.MONO, v(NothingTone.KEY, "mono"));
        assertNull(v(NothingTone.KEY, "sepia"));
        assertEquals("none", v(Config.WALLPAPER_CHOICE_KEY, "none"));
        assertEquals("dusk.jpg", v(Config.WALLPAPER_CHOICE_KEY, "dusk.jpg"));
        assertNull(v(Config.WALLPAPER_CHOICE_KEY, "../config.json"));
        assertEquals(Boolean.FALSE, v("animations", false));
        assertNull(v("animations", "false"));
    }

    @Test
    public void theGreetingNameIsOneShortLine() {
        assertEquals("amado líder", v(Config.GREETING_NAME_KEY, "  amado\nlíder "));
        assertEquals("", v(Config.GREETING_NAME_KEY, ""));
        assertNull(v(Config.GREETING_NAME_KEY, "x".repeat(SettingsFile.NAME_MAX + 1)));
    }

    @Test
    public void openOnStartIsAPackageAndEmptyTurnsItOff() {
        assertEquals("org.jellyfin.androidtv", v(OpenOnStart.KEY, "org.jellyfin.androidtv"));
        assertSame(SettingsFile.REMOVE, v(OpenOnStart.KEY, ""));
        assertNull(v(OpenOnStart.KEY, "rm -rf /"));
    }

    @Test
    public void nullRemovesAKnownChoiceOnly() {
        assertSame(SettingsFile.REMOVE, SettingsFile.valid("columns", null, true));
        assertNull(SettingsFile.valid("dock_apps", null, true));
    }
}
