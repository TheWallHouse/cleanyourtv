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
 * Map's screensaver: a sea chart in the look's ink with the time in a cartouche, lit by the hour (the morning sun
 * from the east, the evening's from the west, moonlight at night), and a ship sailing a lane marked with the hours
 * from one midnight port (a crescent moon) to the other: the hours behind it in colour, the rest dotted. The coasts are drawn
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
        route.cubicTo(520, 700, 790, 700, PORT_E_X, PORT_E_Y);
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
        // The hours along the lane: a tick each, a longer one with its numeral every six.
        float len = measure.getLength();
        float[] tan = new float[2];
        Paint tick = stroke(alpha(p.ink, 170), 1.4f);
        String[] numerals = {"VI", "XII", "XVIII"};
        for (int h = 1; h < 24; h++) {
            measure.getPosTan(len * h / 24f, pos, tan);
            float nx = tan[1], ny = -tan[0];   // the lane's normal, pointing south of it
            if (ny < 0) { nx = -nx; ny = -ny; }
            float k = h % 6 == 0 ? 12 : 6;
            c.drawLine(pos[0] + nx * 6, pos[1] + ny * 6, pos[0] + nx * (6 + k), pos[1] + ny * (6 + k), tick);
            if (h % 6 == 0) text(c, numerals[h / 6 - 1], pos[0] + nx * 40, pos[1] + ny * 40 + 6, 17, p.ink, bold,
                    Paint.Align.CENTER, 0.05f);
        }
        moon(c, PORT_W_X, PORT_W_Y);
        moon(c, PORT_E_X, PORT_E_Y);

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

    /** A port at midnight: a crescent moon in a ring. */
    private void moon(Canvas c, float x, float y) {
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(p.card | 0xFF000000);
        c.drawCircle(x, y, 17, paint);
        c.drawCircle(x, y, 17, stroke(p.ink, 1.6f));
        Path disc = new Path(), bite = new Path();
        disc.addCircle(x, y, 11, Path.Direction.CW);
        bite.addCircle(x + 6, y - 4, 10, Path.Direction.CW);
        disc.op(bite, Path.Op.DIFFERENCE);
        paint.setColor(p.ember);
        c.drawPath(disc, paint);
    }

    @Override
    protected void drawLive(Canvas c, LocalDateTime now) {
        light(c, now.getHour() * 60 + now.getMinute());
        float day = DreamTime.day(now.getHour(), now.getMinute(), 0);
        float len = measure.getLength();
        done.reset();
        measure.getSegment(0, len * day, done, true);
        c.drawPath(done, stroke(alpha(p.ember, 90), 9f));
        c.drawPath(done, stroke(p.ember, 3.5f));
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

    /**
     * The light of the hour over the chart. By day the sun comes from the east (the right) in the morning and from
     * the west in the evening: that side glows warm while the other stays in shadow. At night it is all shadow, with
     * a little moonlight.
     */
    private void light(Canvas c, int minute) {
        float sun = DreamTime.sun(minute);
        paint.setStyle(Paint.Style.FILL);
        if (sun >= 0) {
            float x = W * (1.15f - 1.3f * sun);   // east to west
            // Warmer and lower at dawn and dusk, paler at noon.
            float low = 1f - (float) Math.sin(Math.PI * sun);
            int warm = mix(0xFFFFE6B0, 0xFFFF8A3C, low);
            paint.setShader(new RadialGradient(x, 120, 1000, new int[]{alpha(warm, 70 + Math.round(60 * low)), alpha(warm, 0)},
                    null, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, W, H, paint);
            paint.setShader(new RadialGradient(x, 200, 1500, new int[]{alpha(SHADOW, 0), alpha(SHADOW, 40),
                    alpha(SHADOW, 90 + Math.round(70 * low))}, new float[]{0f, 0.35f, 1f}, Shader.TileMode.CLAMP));
        } else {
            float t = DreamTime.moon(minute);
            float x = W * (1.1f - 1.2f * t);
            paint.setShader(new RadialGradient(x, 80, 1300, new int[]{alpha(0x9FB8E0, 40), alpha(SHADOW, 150),
                    alpha(SHADOW, 190)}, new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        }
        c.drawRect(0, 0, W, H, paint);
        paint.setShader(null);
    }

    private static final int SHADOW = 0xFF060A14;

    private static int mix(int a, int b, float f) {
        int r = Math.round(((a >> 16) & 0xFF) * (1 - f) + ((b >> 16) & 0xFF) * f);
        int g = Math.round(((a >> 8) & 0xFF) * (1 - f) + ((b >> 8) & 0xFF) * f);
        int bl = Math.round((a & 0xFF) * (1 - f) + (b & 0xFF) * f);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
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
