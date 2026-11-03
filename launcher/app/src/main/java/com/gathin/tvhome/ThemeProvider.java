package com.gathin.tvhome;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Iterator;

/**
 * Lets tools/theme.sh write the theme files into {@link Config#dir} on firmware where the adb shell is
 * locked out of Android/data (TCL G07, Android 11). Only the files {@link ThemeFiles} allows, and only for
 * callers holding WRITE_SECURE_SETTINGS, which the adb shell has and ordinary apps cannot get.
 *
 *   content write --uri content://com.gathin.tvhome.theme/config.json < /data/local/tmp/config.json
 *   content write --uri content://com.gathin.tvhome.theme/wallpapers/dusk.jpg < /data/local/tmp/dusk.jpg
 *   content delete --uri content://com.gathin.tvhome.theme/config.json
 *   content delete --uri content://com.gathin.tvhome.theme/wallpapers      (every image in the collection)
 *   content read  --uri content://com.gathin.tvhome.theme/settings.json   (Launcher settings' choices, see SettingsFile)
 *   content write --uri content://com.gathin.tvhome.theme/settings.json < /data/local/tmp/settings.json
 *
 * Once a write closes, Home rebuilds with it if it is on screen, or at its next resume, as after Launcher settings.
 */
public class ThemeProvider extends ContentProvider {

    private File file(Uri uri) throws FileNotFoundException {
        String path = ThemeFiles.path(uri.getPathSegments());
        File dir = Config.dir(getContext());
        if (dir == null || path == null) throw new FileNotFoundException(String.valueOf(uri));
        File f = new File(dir, path);
        File parent = f.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) throw new FileNotFoundException(String.valueOf(uri));
        return f;
    }

    private static final String TAG = "TvHome.ThemeProvider";

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (uri.getPathSegments().size() == 1 && SettingsFile.NAME.equals(uri.getPathSegments().get(0))) {
            return settings(mode.contains("w"));
        }
        File f = file(uri);
        if (mode.contains("w")) {
            // Home picks up a pushed theme without a force stop, which would switch the remote helpers off.
            Context ctx = getContext();
            return openForWrite(f, () -> changed(ctx));
        }
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    /** settings.json is not a file on disk: a read is a snapshot of the choices, a write is applied when it closes. */
    private ParcelFileDescriptor settings(boolean write) throws FileNotFoundException {
        Context ctx = getContext();
        if (ctx == null) throw new FileNotFoundException(SettingsFile.NAME);
        File tmp = new File(ctx.getCacheDir(), write ? "settings-in.json" : "settings-out.json");
        if (!write) {
            try {
                Files.write(tmp.toPath(), snapshot(ctx).toString(2).getBytes(StandardCharsets.UTF_8));
            } catch (IOException | JSONException e) {
                throw new FileNotFoundException(SettingsFile.NAME + ": " + e);
            }
            return ParcelFileDescriptor.open(tmp, ParcelFileDescriptor.MODE_READ_ONLY);
        }
        return openForWrite(tmp, () -> {
            apply(ctx, tmp);
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
        });
    }

    /** [f] for writing, with [closed] run on the main thread once the writer closes it. */
    private static ParcelFileDescriptor openForWrite(File f, Runnable closed) throws FileNotFoundException {
        try {
            return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_WRITE_ONLY
                    | ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_TRUNCATE,
                    new Handler(Looper.getMainLooper()), e -> closed.run());
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw new FileNotFoundException(f + ": " + e);
        }
    }

    private static JSONObject snapshot(Context ctx) throws JSONException {
        // A theme pushed just before is taken in first, so the snapshot shows what Home will show.
        Config c = Config.load(ctx);
        SharedPreferences choices = ctx.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
        JSONObject out = new JSONObject();
        for (String key : SettingsFile.KEYS.keySet()) {
            Object v = choices.getAll().get(key);
            if (v != null) out.put(key, v);
        }
        try {
            @SuppressWarnings("deprecation") int code = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionCode;
            out.put("version", code);
        } catch (android.content.pm.PackageManager.NameNotFoundException ignored) {
            // Its own package: always there.
        }
        out.put("keys", new JSONArray(SettingsFile.KEYS.keySet()));
        // What Home uses now, the theme's value where nothing was chosen on the TV.
        JSONObject now = new JSONObject();
        now.put("columns", c.columns);
        now.put(Config.ROWS_BELOW_DOCK_KEY, c.rowsBelowDock);
        now.put(Config.DOCK_OPACITY_KEY, c.dockOpacity);
        now.put(Config.CLOCK_FORMAT_KEY, c.clockFormat);
        now.put(NothingTone.KEY, NothingTone.mode(ctx));
        now.put(Config.GREETING_NAME_KEY, c.greetingName);
        now.put("animations", c.animations);
        now.put("advanced_effects", c.advancedEffects);
        now.put("dock_apps", new JSONArray(c.dockApps));
        out.put("effective", now);
        return out;
    }

    private static void apply(Context ctx, File in) {
        JSONObject json;
        try {
            json = new JSONObject(new String(Files.readAllBytes(in.toPath()), StandardCharsets.UTF_8));
        } catch (IOException | JSONException e) {
            Log.w(TAG, "settings.json not applied: " + e);
            return;
        }
        // A theme pushed just before resets the choices first; these then apply on top of it.
        Config.load(ctx);
        SharedPreferences choices = ctx.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = choices.edit();
        for (Iterator<String> it = json.keys(); it.hasNext(); ) {
            String key = it.next();
            Object v = SettingsFile.valid(key, json.opt(key), json.isNull(key));
            if (v == null) {
                Log.w(TAG, "settings.json: skipped " + key);
            } else if (v == SettingsFile.REMOVE) {
                edit.remove(key);
            } else if (v instanceof Integer) {
                edit.putInt(key, (Integer) v);
            } else if (v instanceof Boolean) {
                edit.putBoolean(key, (Boolean) v);
            } else {
                edit.putString(key, (String) v);
            }
        }
        edit.commit();
        changed(ctx);
    }

    /** Counts a change as Launcher settings does, so Home rebuilds: now if it is on screen, else when it resumes. */
    private static void changed(Context ctx) {
        if (ctx == null) return;
        SharedPreferences choices = ctx.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
        choices.edit().putLong(Config.SETTINGS_REVISION_KEY, choices.getLong(Config.SETTINGS_REVISION_KEY, 0) + 1).commit();
        MainActivity.settingsPushed();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        File dir = Config.dir(getContext());
        if (dir != null && ThemeFiles.isCollection(uri.getPathSegments())) {
            File[] images = new File(dir, ThemeFiles.WALLPAPERS).listFiles((d, name) -> ThemeFiles.collectionImage(name));
            int n = 0;
            if (images != null) for (File f : images) if (f.delete()) n++;
            return n;
        }
        try {
            return file(uri).delete() ? 1 : 0;
        } catch (FileNotFoundException e) {
            return 0;
        }
    }

    @Override public boolean onCreate() { return true; }
    @Override public String getType(Uri uri) { return null; }
    @Override public Cursor query(Uri uri, String[] p, String s, String[] a, String o) { return null; }
    @Override public Uri insert(Uri uri, ContentValues values) { return null; }
    @Override public int update(Uri uri, ContentValues v, String s, String[] a) { return 0; }
}
