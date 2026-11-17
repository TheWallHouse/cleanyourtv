package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;

import java.time.LocalDateTime;

/**
 * Dune's screensaver: the dunes under a sun that crosses the sky with the real hour (DreamTime.sun): it rises on
 * the left at six, is high at one and sets on the right at eight. At night the sky is the night tone's and the two
 * moons cross it instead. The time is huge and widely spaced, like an app's name on Home.
 */
final class DuneDream extends LookDream {
    private final DuneTone day, night;
    private final Typeface wide;
    private final Path dunes1 = new Path(), dunes2 = new Path(), dunes3 = new Path(), arc = new Path();

    DuneDream(Context ctx, DuneTone p) {
        super(ctx);
        // By day the chosen tone; the night tone already is a night, so it is the night for every tone.
        day = DuneTone.NIGHT.equals(p.name) ? DuneTone.of(DuneTone.DUSK) : p;
        night = DuneTone.of(DuneTone.NIGHT);
        wide = ctx.getResources().getFont(R.font.michroma);
        dunes1.moveTo(0, 520);
        dunes1.cubicTo(260, 490, 460, 540, 700, 505);
        dunes1.cubicTo(940, 470, 1100, 480, 1280, 510);
        close(dunes1);
        dunes2.moveTo(0, 590);
        dunes2.cubicTo(300, 550, 600, 610, 860, 570);
        dunes2.cubicTo(1120, 530, 1180, 555, 1280, 575);
        close(dunes2);
        dunes3.moveTo(0, 660);
        dunes3.cubicTo(320, 630, 700, 680, 1000, 645);
        dunes3.cubicTo(1300, 610, 1220, 640, 1280, 650);
        close(dunes3);
        for (int i = 0; i <= 60; i++) {
            float[] q = at(i / 60f);
            if (i == 0) arc.moveTo(q[0], q[1]);
            else arc.lineTo(q[0], q[1]);
        }
    }

    private static void close(Path p) {
        p.lineTo(1280, 720);
        p.lineTo(0, 720);
        p.close();
    }

    /** A point on the sky's arc, t 0 (left horizon) to 1 (right horizon). */
    private static float[] at(float t) {
        return new float[]{140 + t * 1000, 520 - (float) Math.sin(Math.PI * t) * 380};
    }

    @Override
    protected void drawLive(Canvas c, LocalDateTime now) {
        int minute = now.getHour() * 60 + now.getMinute();
        float sun = DreamTime.sun(minute);
        DuneTone p = sun >= 0 ? day : night;
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(0, 0, 0, H, p.sky, p.skyAt, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, W, H, paint);
        paint.setShader(null);
        if (p.stars > 0) {
            java.util.Random r = new java.util.Random(3);
            paint.setColor(alpha(0xFFFFFF, p.stars));
            for (int i = 0; i < 90; i++) c.drawCircle(r.nextFloat() * W, r.nextFloat() * 380, r.nextFloat() < 0.1f ? 1.4f : 0.7f, paint);
        }
        Paint dashed = new Paint(Paint.ANTI_ALIAS_FLAG);
        dashed.setStyle(Paint.Style.STROKE);
        dashed.setStrokeWidth(1.2f);
        dashed.setColor(alpha(p.sun, 56));
        dashed.setPathEffect(new DashPathEffect(new float[]{3, 9}, 0));
        c.drawPath(arc, dashed);

        if (sun >= 0) {
            float[] q = at(sun);
            disc(c, q[0], q[1], 58, p.sun);
        } else {
            float[] q = at(DreamTime.moon(minute));
            disc(c, q[0], q[1], 46, p.sun);
            disc(c, q[0] + 110, q[1] - 70, 16, p.sun);
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(p.dune1);
        c.drawPath(dunes1, paint);
        paint.setColor(p.dune2);
        c.drawPath(dunes2, paint);
        paint.setColor(p.dune3);
        c.drawPath(dunes3, paint);

        String hm = time(now);
        text(c, hm, 640, 250, hm.length() > 5 ? 104 : 120, p.fg, wide, Paint.Align.CENTER, 0.5f);
        String sub = date(now, "EEEEdMMM").replace(",", "").replace(".", "");
        String ampm = amPm(now);
        if (!ampm.isEmpty()) sub = ampm + "  ·  " + sub;
        text(c, sub.toUpperCase(locale), 640, 320, 20, p.muted, wide, Paint.Align.CENTER, 0.7f);
    }

    /** The sun or a moon with its halo. */
    private void disc(Canvas c, float x, float y, float r, int color) {
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new RadialGradient(x, y, r * 3.4f, new int[]{alpha(color, 140), alpha(color, 0)}, null,
                Shader.TileMode.CLAMP));
        c.drawCircle(x, y, r * 3.4f, paint);
        paint.setShader(null);
        paint.setColor(color);
        c.drawCircle(x, y, r, paint);
    }
}
