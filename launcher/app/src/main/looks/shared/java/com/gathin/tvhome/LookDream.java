package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Random;

/**
 * A look's own screensaver, in place of the plain clock (ClockScene) that Nothing dresses up: one view that draws
 * the whole scene in a 1280×720 space scaled to the screen. What never changes (drawBackdrop) is drawn once into a
 * picture; the rest (drawLive) is redrawn every frameMs(). Every minute the whole scene drifts a few pixels, slightly
 * zoomed so no edge shows, so nothing stays on the same pixels long enough to burn in.
 * Used by ClockDream (the system screensaver) and ClockActivity (Home left idle); no network, no images.
 */
abstract class LookDream extends View {
    static final float W = 1280f, H = 720f;
    /** How far the scene drifts each way, in the 1280×720 space, and the zoom that keeps the edges off screen. */
    private static final float DRIFT = 14f, ZOOM = 1f + 2.6f * DRIFT / W;
    /** The backdrop's picture is never bigger than this: it is a gradient and shapes, and a 1080p one is 8 MB. */
    private static final int MAX_BACKDROP_W = 1280;

    /** The look's screensaver, or null for Nothing, which keeps the clock screensaver it dresses itself. */
    static LookDream create(Context ctx, String aspect) {
        android.content.SharedPreferences prefs = ctx.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
        switch (aspect) {
            case Aspect.WINDOWS:
                return new WindowsDream(ctx, TerminalPalette.of(prefs.getString(TerminalPalette.KEY, null)));
            case Aspect.CONSOLE:
                return new ConsoleDream(ctx, TerminalPalette.of(prefs.getString(TerminalPalette.KEY, null)));
            case Aspect.CYBERPUNK:
                return new CyberpunkDream(ctx, CyberpunkTone.of(prefs.getString(CyberpunkTone.KEY, null)));
            case Aspect.DUNE:
                return new DuneDream(ctx, DuneTone.of(prefs.getString(DuneTone.KEY, null)));
            case Aspect.MAP:
                return new MapDream(ctx, MapTone.of(prefs.getString(MapTone.KEY, null)));
            case Aspect.ORBIT:
                return new OrbitDream(ctx, OrbitTone.of(prefs.getString(OrbitTone.KEY, null)));
            default:
                return null;
        }
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final RectF full = new RectF(0, 0, W, H);
    protected final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    protected final boolean is24;
    protected final Locale locale;
    private Bitmap backdrop;
    private boolean hasBackdrop = true;
    private float dx, dy;
    private long lastMinute = -1;
    private boolean running;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            long now = System.currentTimeMillis();
            long minute = now / 60_000;
            if (minute != lastMinute) {
                if (lastMinute >= 0) {
                    dx = (random.nextFloat() * 2 - 1) * DRIFT;
                    dy = (random.nextFloat() * 2 - 1) * DRIFT;
                }
                lastMinute = minute;
            }
            invalidate();
            long every = frameMs();
            handler.postDelayed(this, every - now % every + 20);   // just after the second (or minute) turns
        }
    };

    LookDream(Context ctx) {
        super(ctx);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        is24 = ClockFormat.is24(ctx, Config.load(ctx).clockFormat);
        locale = ctx.getResources().getConfiguration().getLocales().get(0);
    }

    /** How often drawLive's picture changes: every minute unless the look shows seconds or a blinking cursor. */
    protected long frameMs() {
        return 60_000L;
    }

    /** What never changes, in the 1280×720 space; drawn once. Leave it out to draw everything in drawLive. */
    protected void drawBackdrop(Canvas c) {
        hasBackdrop = false;
    }

    /** The rest of the scene at this moment, in the 1280×720 space, over the backdrop. */
    protected abstract void drawLive(Canvas c, LocalDateTime now);

    void start() {
        if (running) return;
        running = true;
        handler.removeCallbacks(tick);
        tick.run();
    }

    void stop() {
        running = false;
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onDetachedFromWindow() {
        stop();
        if (backdrop != null) {
            backdrop.recycle();
            backdrop = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (getWidth() == 0 || getHeight() == 0) return;
        canvas.drawColor(0xFF000000);
        canvas.save();
        canvas.scale(getWidth() / W, getHeight() / H);
        canvas.translate(W / 2 + dx, H / 2 + dy);
        canvas.scale(ZOOM, ZOOM);
        canvas.translate(-W / 2, -H / 2);
        if (hasBackdrop) {
            if (backdrop == null) backdrop = paintBackdrop();
            if (backdrop != null) canvas.drawBitmap(backdrop, null, full, bitmapPaint);
        }
        drawLive(canvas, LocalDateTime.now());
        canvas.restore();
    }

    private Bitmap paintBackdrop() {
        int w = Math.min(getWidth(), MAX_BACKDROP_W);
        int h = Math.round(w * getHeight() / (float) getWidth());
        Bitmap bmp;
        try {
            bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError e) {
            hasBackdrop = false;
            return null;
        }
        Canvas c = new Canvas(bmp);
        c.scale(w / W, h / H);
        drawBackdrop(c);
        if (!hasBackdrop) {   // the look has none after all
            bmp.recycle();
            return null;
        }
        return bmp;
    }

    // Helpers for the looks.

    /** The time as Home shows it: "16:40", or "4:40" when the launcher (or the TV) uses 12 hours. */
    protected String time(LocalDateTime now) {
        return now.format(DateTimeFormatter.ofPattern(is24 ? "HH:mm" : "h:mm", locale));
    }

    /** "PM"/"AM" in 12-hour mode, else "". */
    protected String amPm(LocalDateTime now) {
        return is24 ? "" : now.format(DateTimeFormatter.ofPattern("a", locale));
    }

    /** The date in this language's own order from a skeleton such as "EEEEdMMMM" ("lunes, 17 de noviembre"). */
    protected String date(LocalDateTime now, String skeleton) {
        String pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, skeleton);
        return now.format(DateTimeFormatter.ofPattern(pattern, locale));
    }

    protected static int alpha(int argb, int a) {
        return (a << 24) | (argb & 0xFFFFFF);
    }

    /** Text with this paint's style: size in the 1280×720 space, spacing in ems, align LEFT/CENTER/RIGHT. */
    protected void text(Canvas c, String s, float x, float y, float size, int color, Typeface face, Paint.Align align,
                        float spacing) {
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setTypeface(face);
        paint.setTextSize(size);
        paint.setLetterSpacing(spacing);
        paint.setTextAlign(Paint.Align.LEFT);
        float w = paint.measureText(s);
        // Android spreads letter spacing half before and half after each letter, and measures both ends: shift
        // by the half so the first (or last) letter's ink sits on x.
        float half = spacing * size / 2;
        float left = align == Paint.Align.LEFT ? x - half : align == Paint.Align.CENTER ? x - w / 2 : x - w + half;
        c.drawText(s, left, y, paint);
        paint.setLetterSpacing(0);
    }

    protected float width(String s, float size, Typeface face, float spacing) {
        paint.setTypeface(face);
        paint.setTextSize(size);
        paint.setLetterSpacing(spacing);
        float w = paint.measureText(s) - spacing * size;
        paint.setLetterSpacing(0);
        return w;
    }

    protected void fill(Canvas c, float l, float t, float r, float b, int color) {
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        c.drawRect(l, t, r, b, paint);
    }
}
