package com.gathin.tvhome;

import android.content.Context;

/**
 * WallHouse Launcher's looks: how Home is drawn over the same apps, dock and settings. "nothing" is the Nothing
 * edition's own; "windows" (Terminal · Windows) and "console" (Terminal · Console) are the terminal-styled ones;
 * "cyberpunk" is a night-city game menu (CyberpunkHome).
 * Chosen in Launcher settings or from the phone (settings.json "aspect"), or by a theme (config.json "aspect").
 * Unlike the other choices it survives a theme push, as the language does: a theme is colours, not the layout.
 */
final class Aspect {
    static final String KEY = "aspect";
    static final String NOTHING = "nothing";
    static final String WINDOWS = "windows";
    static final String CONSOLE = "console";
    static final String CYBERPUNK = "cyberpunk";

    /**
     * The looks this build can draw, in Launcher settings' order; settings.json lists them ("aspects") so the phone
     * offers only these. One not here yet is refused like any unknown value.
     */
    static final String[] ALL = {NOTHING, WINDOWS, CONSOLE, CYBERPUNK};

    private Aspect() {
    }

    static boolean valid(String aspect) {
        for (String a : ALL) if (a.equals(aspect)) return true;
        return false;
    }

    /** The look Home uses: the TV's (or phone's) choice, then the theme's, then Nothing. */
    static String of(String chosen, String themed) {
        if (valid(chosen)) return chosen;
        return valid(themed) ? themed : NOTHING;
    }

    /** The look Home uses now, as {@link #of} decides it from this TV's choice and its theme. */
    static String mode(Context ctx) {
        String themed = null;
        java.io.File external = Config.dir(ctx) != null ? new java.io.File(Config.dir(ctx), Config.FILE_NAME) : null;
        java.io.File copy = new java.io.File(ctx.getFilesDir(), Config.FILE_NAME);
        for (java.io.File file : new java.io.File[] {external, copy}) {
            if (file == null || !file.isFile()) continue;
            try {
                themed = new org.json.JSONObject(new String(java.nio.file.Files.readAllBytes(file.toPath()),
                        java.nio.charset.StandardCharsets.UTF_8)).optString(KEY, null);
                break;
            } catch (Exception ignored) {
                // An unreadable theme leaves the choice, or Nothing.
            }
        }
        return of(chosen(ctx), themed);
    }

    static String chosen(Context ctx) {
        return ctx.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE).getString(KEY, null);
    }

    static boolean terminal(String aspect) {
        return WINDOWS.equals(aspect) || CONSOLE.equals(aspect);
    }
}
