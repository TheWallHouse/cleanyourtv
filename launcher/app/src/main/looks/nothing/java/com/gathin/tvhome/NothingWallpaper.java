package com.gathin.tvhome;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Nothing edition: the dot-grid wallpaper ships inside the app, so a TV set up without a computer (only the APK)
 * gets it too. It is copied once to the app's private files and used when no theme put a wallpaper on the TV.
 */
final class NothingWallpaper {
    private static final String NAME = "nothing_wallpaper.jpg";

    private NothingWallpaper() {
    }

    static File file(Context ctx) {
        File out = new File(ctx.getFilesDir(), NAME);
        if (out.isFile() && out.length() > 0) return out;
        try (InputStream in = ctx.getResources().openRawResource(R.raw.nothing_wallpaper);
             OutputStream os = new FileOutputStream(out)) {
            byte[] buf = new byte[16384];
            for (int n; (n = in.read(buf)) > 0; ) os.write(buf, 0, n);
            return out;
        } catch (Exception e) {
            out.delete();
            return null;
        }
    }
}
