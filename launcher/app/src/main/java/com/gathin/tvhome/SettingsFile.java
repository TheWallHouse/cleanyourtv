package com.gathin.tvhome;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * settings.json through ThemeProvider: the choices Launcher settings keeps on the TV (Config.SETTINGS_NAME), so a
 * phone app over adb (WallHouse TV) can read them and change them without a theme push, which would reset them.
 *
 * Reading gives every key that is set, plus "version" (the launcher's version code) and "keys" (the ones it accepts).
 * Writing takes any of {@link #KEYS}; a JSON null removes a choice, so the theme decides again. A key it doesn't
 * know, or a value out of range, is skipped: the rest still apply. Kept free of Android so it is unit-tested.
 */
final class SettingsFile {
    static final String NAME = "settings.json";

    enum Kind { INT, BOOL, STRING }

    /** What a removal looks like in {@link #valid}: the key goes, and the theme's value is back. */
    static final Object REMOVE = new Object();

    /** The keys a phone may read and write, with their type, in Launcher settings' order. */
    static final Map<String, Kind> KEYS;
    static {
        Map<String, Kind> k = new LinkedHashMap<>();
        k.put("columns", Kind.INT);
        k.put(Config.ROWS_BELOW_DOCK_KEY, Kind.INT);
        k.put(Config.DOCK_OPACITY_KEY, Kind.INT);
        k.put(Config.CLOCK_FORMAT_KEY, Kind.STRING);
        k.put(Config.WALLPAPER_CHOICE_KEY, Kind.STRING);
        k.put(NothingTone.KEY, Kind.STRING);
        k.put(Config.GREETING_NAME_KEY, Kind.STRING);
        k.put("animations", Kind.BOOL);
        k.put("advanced_effects", Kind.BOOL);
        k.put(OpenOnStart.KEY, Kind.STRING);
        KEYS = Collections.unmodifiableMap(k);
    }

    /** Longest name the greeting shows; the TV's own dialog keeps it short too. */
    static final int NAME_MAX = 40;

    private static final Pattern PACKAGE = Pattern.compile("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+");

    private SettingsFile() {}

    /**
     * [value] for [key] as it may be stored: an Integer, Boolean or String, {@link #REMOVE} for a JSON null, or null
     * when the key is unknown or the value is not one Launcher settings could have saved.
     */
    static Object valid(String key, Object value, boolean isNull) {
        Kind kind = KEYS.get(key);
        if (kind == null) return null;
        if (isNull) return REMOVE;
        switch (kind) {
            case INT: {
                if (!(value instanceof Number)) return null;
                double d = ((Number) value).doubleValue();
                if (d != Math.rint(d)) return null;
                int n = (int) d;
                if ("columns".equals(key)) return n >= 4 && n <= 7 ? n : null;
                if (Config.ROWS_BELOW_DOCK_KEY.equals(key)) return n >= 0 && n <= 3 ? n : null;
                if (Config.DOCK_OPACITY_KEY.equals(key)) return n == 30 || n == 50 || n == 70 || n == 100 ? n : null;
                return null;
            }
            case BOOL:
                return value instanceof Boolean ? value : null;
            default: {
                if (!(value instanceof String)) return null;
                String s = (String) value;
                if (Config.CLOCK_FORMAT_KEY.equals(key)) return "system".equals(s) || "12".equals(s) || "24".equals(s) ? s : null;
                if (NothingTone.KEY.equals(key)) return NothingTone.OFF.equals(s) || NothingTone.MONO.equals(s) || NothingTone.MONO_RED.equals(s) ? s : null;
                if (Config.WALLPAPER_CHOICE_KEY.equals(key)) return "none".equals(s) || ThemeFiles.collectionImage(s) ? s : null;
                if (OpenOnStart.KEY.equals(key)) return s.isEmpty() ? REMOVE : PACKAGE.matcher(s).matches() ? s : null;
                if (Config.GREETING_NAME_KEY.equals(key)) {
                    String name = s.replaceAll("[\\p{Cntrl}]", " ").trim();
                    return name.length() <= NAME_MAX ? name : null;
                }
                return null;
            }
        }
    }
}
