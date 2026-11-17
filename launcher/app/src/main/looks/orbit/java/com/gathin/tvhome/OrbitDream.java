package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.os.SystemClock;

import java.time.LocalDateTime;

/**
 * Orbit's screensaver: Home's planet (OrbitSkin.planet) with a satellite that goes once along the orbit a day, so
 * where it is tells the time; the trail behind it is drawn, the rest dotted. Over it the time thin with its seconds,
 * the date and the time since the TV started, as telemetry, and the mission header on standby.
 */
final class OrbitDream extends LookDream {
    /** The orbit Home draws: an ellipse round (640, 820), of which 196° to 344° shows over the planet. */
    private static final float FROM = 196f, TO = 344f;
    private final OrbitTone p;
    private final Typeface light, medium;
    private final Path orbit = new Path(), trail = new Path();
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);

    OrbitDream(Context ctx, OrbitTone p) {
        super(ctx);
        this.p = p;
        light = ctx.getResources().getFont(R.font.barlow_light);
        medium = ctx.getResources().getFont(R.font.barlow_medium);
        arc(orbit, FROM, TO);
        line.setStyle(Paint.Style.STROKE);
    }

    private static float[] at(double deg) {
        double a = Math.toRadians(deg);
        return new float[]{(float) (640 + 760 * Math.cos(a)), (float) (820 + 600 * Math.sin(a))};
    }

    private static void arc(Path path, float from, float to) {
        path.reset();
        for (float g = from; g <= to + 0.01f; g += 1f) {
            float[] q = at(Math.min(g, to));
            if (g == from) path.moveTo(q[0], q[1]);
            else path.lineTo(q[0], q[1]);
        }
        float[] end = at(to);
        path.lineTo(end[0], end[1]);
    }

    @Override
    protected long frameMs() {
        return 1000L;   // the seconds
    }

    @Override
    protected void drawBackdrop(Canvas c) {
        c.drawColor(0xFF000000);
        OrbitSkin.planet(c, p);
        line.setColor(alpha(p.fg, 41));
        line.setStrokeWidth(1);
        line.setPathEffect(new DashPathEffect(new float[]{2, 6}, 0));
        c.drawPath(orbit, line);
        line.setPathEffect(null);
    }

    @Override
    protected void drawLive(Canvas c, LocalDateTime now) {
        float day = DreamTime.day(now.getHour(), now.getMinute(), now.getSecond());
        float deg = FROM + (TO - FROM) * day;
        arc(trail, FROM, deg);
        line.setColor(alpha(p.accent, 150));
        line.setStrokeWidth(1.4f);
        c.drawPath(trail, line);
        float[] sat = at(deg);
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(p.accent);
        c.drawCircle(sat[0], sat[1], 4, paint);
        line.setColor(alpha(p.accent, 128));
        line.setStrokeWidth(1);
        c.drawCircle(sat[0], sat[1], 11, line);

        Context ctx = getContext();
        String brand = "WALLHOUSE ";
        text(c, brand, 72, 64, 13, p.fg, medium, Paint.Align.LEFT, 0.6f);
        String state = "| " + ctx.getString(R.string.aspect_orbit) + " · " + ctx.getString(R.string.orbit_standby);
        text(c, state.toUpperCase(locale), 72 + width(brand, 13, medium, 0.6f) + 8, 64, 13, p.muted, light,
                Paint.Align.LEFT, 0.6f);

        String hm = time(now), sec = String.format(locale, ":%02d", now.getSecond());
        float big = 170, small = 70, spacing = 0.08f;
        float wHm = width(hm, big, light, spacing), wSec = width(sec, small, light, spacing);
        float x = 640 - (wHm + 10 + wSec) / 2;
        text(c, hm, x, 330, big, p.fg, light, Paint.Align.LEFT, spacing);
        text(c, sec, x + wHm + 10, 330, small, p.muted, light, Paint.Align.LEFT, spacing);
        String ampm = amPm(now);
        if (!ampm.isEmpty()) text(c, ampm, x + wHm + 10, 262, 30, p.muted, medium, Paint.Align.LEFT, 0.2f);

        String tele = date(now, "EEEdMMM").replace(",", "").replace(".", "") + "  ·  "
                + DreamTime.uptime(SystemClock.elapsedRealtime()) + " " + ctx.getString(R.string.orbit_up);
        text(c, tele.toUpperCase(locale), 640, 395, 20, p.muted, light, Paint.Align.CENTER, 0.5f);
    }
}
