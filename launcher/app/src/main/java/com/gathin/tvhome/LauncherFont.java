package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;

/**
 * The theme's own font (font.ttf, sent by a phone app such as WallHouse TV, or pushed like any theme file) in place of
 * the Doto dot-matrix one: the clock, the greeting, the date, the focused app's name, the top buttons and the
 * screensaver. With no font.ttf, or one Android can't read, everything stays as it is. No network: the file arrives
 * over adb through ThemeProvider, like a wallpaper.
 */
final class LauncherFont {
    static final String FILE_NAME = "font.ttf";
    /** Far past any UI font; a bigger file is not one. */
    private static final long MAX_BYTES = 4L << 20;

    private LauncherFont() {}

    /** The theme's font, or null for none (or one that doesn't load). */
    static Typeface custom(Context ctx) {
        File dir = Config.dir(ctx);
        File f = dir == null ? null : new File(dir, FILE_NAME);
        if (f == null || !f.isFile() || f.length() == 0 || f.length() > MAX_BYTES) return null;
        try {
            return Typeface.createFromFile(f);
        } catch (RuntimeException unreadable) {
            return null;
        }
    }

    /** Swaps the theme's font into every TextView under [root] that has the Doto one. */
    static void apply(Context ctx, View root) {
        Typeface font = custom(ctx);
        if (font == null || root == null) return;
        Typeface dot = UiFont.get(ctx, R.string.font_dot);
        swap(root, dot, font);
    }

    private static void swap(View v, Typeface dot, Typeface font) {
        if (v instanceof TextView) {
            TextView t = (TextView) v;
            Typeface now = t.getTypeface();
            if (now != null && (now == dot || now.equals(dot))) t.setTypeface(font);
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) swap(g.getChildAt(i), dot, font);
        }
    }
}
