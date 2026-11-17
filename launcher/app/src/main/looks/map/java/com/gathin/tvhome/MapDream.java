package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;

import java.time.LocalDateTime;

/**
 * Map's screensaver: a sea chart in the look's ink with the time in a cartouche, and a ship sailing from one port
 * at midnight to the other at the next midnight: the route behind it inked, the rest dotted. The coasts are drawn
 * with the map's own hand (MapDrawing.rough) and the same seed, so the chart is the same every night.
 */
final class MapDream extends LookDream {
    private static final float[][] WEST = {{0, 250}, {120, 236}, {210, 290}, {300, 270}, {350, 340}, {330, 430},
            {372, 520}, {320, 600}, {360, 680}, {300, 730}, {0, 730}};
    private static final float[][] EAST = {{1290, 230}, {1160, 250}, {1080, 220}, {1000, 300}, {1020, 380},
            {960, 470}, {990, 560}, {940, 640}, {990, 730}, {1290, 730}};
    private static final float PORT_W_X = 352, PORT_W_Y = 520, PORT_E_X = 966, PORT_E_Y = 500;

    private final MapTone p;
    private final Typeface cinzel, bold;
    private final Path route = new Path(), done = new Path();
    private final PathMeasure measure;
    private final float[] pos = new float[2];

    MapDream(Context ctx, MapTone p) {
        super(ctx);
        this.p = p;
        cinzel = ctx.getResources().getFont(R.font.cinzel);
        bold = ctx.getResources().getFont(R.font.cinzel_bold);
        route.moveTo(PORT_W_X, PORT_W_Y);
        route.cubicTo(450, 420, 520, 640, 640, 560);
        route.cubicTo(760, 480, 820, 400, PORT_E_X, PORT_E_Y);
        measure = new PathMeasure(route, false);
    }

    private static Paint stroke(int color, float width) {
        Paint s = new Paint(Paint.ANTI_ALIAS_FLAG);
        s.setStyle(Paint.Style.STROKE);
        s.setStrokeWidth(width);
        s.setStrokeCap(Paint.Cap.ROUND);
        s.setStrokeJoin(Paint.Join.ROUND);
        s.setColor(color);
        return s;
    }

    private static Path coast(float[][] pts) {
        Path path = new Path();
        for (int i = 0; i < pts.length; i++) {
            if (i == 0) path.moveTo(pts[i][0], pts[i][1]);
            else path.lineTo(pts[i][0], pts[i][1]);
        }
        path.close();
        return path;
    }

    @Override
    protected void drawBackdrop(Canvas c) {
        MapDrawing.Rng r = new MapDrawing.Rng(11);
        c.drawColor(p.sea);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(alpha(p.ink, 18));
        for (int i = 0; i < 1400; i++) {
            float x = r.next() * W, y = r.next() * H, s = 0.5f + r.next() * 0.9f;
            c.drawRect(Math.round(x), Math.round(y), Math.round(x) + s, Math.round(y) + s, paint);
        }
        Paint wave = stroke(alpha(p.ink, 70), 1.2f);
        for (int i = 0; i < 40; i++) {
            float x = 400 + r.next() * 520, y = 300 + r.next() * 400;
            Path w = new Path();
            w.moveTo(x, y);
            w.quadTo(x + 7, y - 5, x + 14, y);
            w.quadTo(x + 21, y - 5, x + 28, y);
            c.drawPath(w, wave);
        }
        for (float[][] land : new float[][][]{WEST, EAST}) {
            Path shore = coast(MapDrawing.rough(land, r, 3));
            paint.setColor(p.land);
            c.drawPath(shore, paint);
            c.drawPath(shore, stroke(alpha(p.ink, 160), 2f));
        }
        // Trees and hills on the lands, a few strokes each.
        Paint ink = stroke(alpha(p.ink, 140), 1.4f);
        for (int i = 0; i < 26; i++) {
            float x = 30 + r.next() * 240, y = 330 + r.next() * 340;
            c.drawLine(x, y, x, y - 10, ink);
            c.drawCircle(x, y - 14, 6, stroke(alpha(p.tree, 200), 1.4f));
        }
        for (int i = 0; i < 9; i++) {
            float x = 1060 + r.next() * 180, y = 320 + r.next() * 300;
            Path hill = new Path();
            hill.moveTo(x - 20, y);
            hill.lineTo(x, y - 26);
            hill.lineTo(x + 20, y);
            c.drawPath(hill, ink);
        }
        c.drawPath(route, dotted());
        port(c, PORT_W_X, PORT_W_Y);
        port(c, PORT_E_X, PORT_E_Y);
        String midnight = getContext().getString(R.string.map_midnight);
        text(c, midnight, PORT_W_X - 12, PORT_W_Y + 34, 15, p.ink, bold, Paint.Align.CENTER, 0.05f);
        text(c, midnight, PORT_E_X + 12, PORT_E_Y + 34, 15, p.ink, bold, Paint.Align.CENTER, 0.05f);

        // The cartouche, framed twice.
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(p.card);
        c.drawRect(440, 80, 840, 270, paint);
        c.drawRect(440, 80, 840, 270, stroke(p.ink, 2.5f));
        c.drawRect(430, 70, 850, 280, stroke(p.ink, 1f));
        // The compass.
        c.save();
        c.translate(1160, 120);
        c.drawCircle(0, 0, 44, stroke(alpha(p.ink, 200), 1.5f));
        Path rose = new Path();
        rose.moveTo(0, -60); rose.lineTo(8, -8); rose.lineTo(0, 0); rose.lineTo(-8, -8); rose.close();
        rose.moveTo(0, 60); rose.lineTo(8, 8); rose.lineTo(0, 0); rose.lineTo(-8, 8); rose.close();
        rose.moveTo(-60, 0); rose.lineTo(-8, -8); rose.lineTo(0, 0); rose.lineTo(-8, 8); rose.close();
        rose.moveTo(60, 0); rose.lineTo(8, -8); rose.lineTo(0, 0); rose.lineTo(8, 8); rose.close();
        paint.setColor(alpha(p.ink, 130));
        c.drawPath(rose, paint);
        c.drawPath(rose, stroke(p.ink, 1f));
        c.restore();
        text(c, "N", 1160, 48, 14, p.ink, bold, Paint.Align.CENTER, 0);
        // Age at the edges, and the border.
        paint.setShader(new RadialGradient(640, 330, 860, new int[]{alpha(p.stain, 0), alpha(p.stain, 0), alpha(p.stain, 90)},
                new float[]{0f, 0.6f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, W, H, paint);
        paint.setShader(null);
        c.drawRect(2.5f, 2.5f, W - 2.5f, H - 2.5f, stroke(alpha(p.ink, 180), 5f));
    }

    private Paint dotted() {
        Paint d = stroke(alpha(p.ink, 180), 3f);
        d.setPathEffect(new DashPathEffect(new float[]{2, 10}, 0));
        return d;
    }

    private void port(Canvas c, float x, float y) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(p.ink);
        c.drawCircle(x, y, 8, paint);
    }

    @Override
    protected void drawLive(Canvas c, LocalDateTime now) {
        float day = DreamTime.day(now.getHour(), now.getMinute(), 0);
        float len = measure.getLength();
        done.reset();
        measure.getSegment(0, len * day, done, true);
        c.drawPath(done, stroke(p.accent, 3.5f));
        measure.getPosTan(len * day, pos, null);
        ship(c, pos[0], pos[1]);

        String hm = time(now);
        text(c, hm, 640, 200, hm.length() > 5 ? 92 : 104, p.accent, bold, Paint.Align.CENTER, 0);
        String sub = date(now, "EEEEdMMMM");
        String ampm = amPm(now);
        if (!ampm.isEmpty()) sub = ampm + " · " + sub;
        sub = sub.substring(0, 1).toUpperCase(locale) + sub.substring(1);
        text(c, sub, 640, 245, 18, p.ink, cinzel, Paint.Align.CENTER, 0.15f);
    }

    private void ship(Canvas c, float x, float y) {
        c.save();
        c.translate(x, y);
        Path hull = new Path();
        hull.moveTo(-16, 4);
        hull.lineTo(16, 4);
        hull.lineTo(10, 14);
        hull.lineTo(-10, 14);
        hull.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(p.ink);
        c.drawPath(hull, paint);
        Path sail = new Path();
        sail.moveTo(0, -20);
        sail.lineTo(14, -4);
        sail.lineTo(0, -4);
        sail.close();
        paint.setColor(p.card | 0xFF000000);
        c.drawPath(sail, paint);
        Paint line = stroke(p.ink, 2f);
        c.drawPath(sail, line);
        c.drawLine(0, 4, 0, -22, line);
        c.restore();
    }
}
