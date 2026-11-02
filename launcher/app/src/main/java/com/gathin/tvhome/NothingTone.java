package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Nothing edition: app banners and icons in greyscale ("mono"), or greyscale with their reds
 * turned into Nothing red ("mono_red", the default). Set in Launcher settings or with "nothing_tiles" in config.json.
 * Each picture is converted once, when the tiles are loaded.
 */
final class NothingTone {
    static final String OFF = "off";
    static final String MONO = "mono";
    static final String MONO_RED = "mono_red";
    /** Launcher settings' "Apps style" choice, which wins over the theme. */
    static final String KEY = "nothing_tiles";
    private static final int MAX_WIDTH = 640;

    private NothingTone() {
    }

    private static JSONObject read(Context ctx) {
        File external = Config.dir(ctx) != null ? new File(Config.dir(ctx), Config.FILE_NAME) : null;
        File copy = new File(ctx.getFilesDir(), Config.FILE_NAME);
        for (File file : new File[] {external, copy}) {
            if (file == null || !file.isFile()) continue;
            try {
                return new JSONObject(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
            } catch (Exception ignored) {
                // An unreadable file keeps the tiles in colour.
            }
        }
        return null;
    }

    /**
     * The apps style: Launcher settings' choice first, then "nothing_tiles" from the pushed config.json, and
     * mono_red when neither says, the Nothing edition's own look.
     */
    static String mode(Context ctx) {
        String chosen = ctx.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE).getString(KEY, null);
        if (valid(chosen)) return chosen;
        JSONObject json = read(ctx);
        String themed = json == null ? null : json.optString(KEY, null);
        return valid(themed) ? themed : MONO_RED;
    }

    static boolean valid(String mode) {
        return OFF.equals(mode) || MONO.equals(mode) || MONO_RED.equals(mode);
    }

    static Drawable apply(Context ctx, Drawable drawable, String mode) {
        if (drawable == null || OFF.equals(mode)) return drawable;
        int w = drawable.getIntrinsicWidth();
        int h = drawable.getIntrinsicHeight();
        if (w <= 0 || h <= 0) return drawable;
        if (w > MAX_WIDTH) {
            h = Math.max(1, h * MAX_WIDTH / w);
            w = MAX_WIDTH;
        }
        Bitmap bmp;
        try {
            bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError e) {
            return drawable;
        }
        Canvas canvas = new Canvas(bmp);
        android.graphics.Rect old = drawable.copyBounds();
        drawable.setBounds(0, 0, w, h);
        drawable.draw(canvas);
        drawable.setBounds(old);

        boolean keepRed = MONO_RED.equals(mode);
        int[] px = new int[w * h];
        bmp.getPixels(px, 0, w, 0, 0, w, h);
        for (int i = 0; i < px.length; i++) {
            int c = px[i];
            int a = c >>> 24;
            int r = (c >> 16) & 0xFF;
            int g = (c >> 8) & 0xFF;
            int b = c & 0xFF;
            if (keepRed && r >= 110 && r > g * 1.6f && r > b * 1.6f && r - Math.min(g, b) > 70) {
                float k = Math.min(1f, r / 200f);
                px[i] = (a << 24) | ((int) (0xD7 * k) << 16) | ((int) (0x19 * k) << 8) | (int) (0x21 * k);
            } else {
                int l = (r * 299 + g * 587 + b * 114) / 1000;
                px[i] = (a << 24) | (l << 16) | (l << 8) | l;
            }
        }
        bmp.setPixels(px, 0, w, 0, 0, w, h);
        return new BitmapDrawable(ctx.getResources(), bmp);
    }
}
