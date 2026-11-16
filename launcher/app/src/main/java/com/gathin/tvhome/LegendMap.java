package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Legend's map, in a 1280×720 space: a made-up land with a ragged coast and three islands, mountain ranges and a
 * volcano, forests, hills, rivers and a lake, region names, a double frame, a compass rose and a scale. The same seed
 * draws the same map every time. Drawn once into a view's display list; no bitmaps.
 */
final class LegendMap {
    private static final float[][] MAIN = {{330, 120}, {470, 95}, {620, 118}, {760, 88}, {900, 112}, {1040, 96}, {1150, 140},
            {1195, 230}, {1170, 320}, {1205, 400}, {1160, 480}, {1180, 560}, {1100, 630}, {960, 615}, {860, 660}, {720, 640},
            {600, 668}, {470, 630}, {360, 655}, {240, 610}, {150, 560}, {170, 470}, {110, 400}, {160, 330}, {230, 300}, {250, 230}};
    private static final float[][][] ISLES = {
            {{1222, 150}, {1250, 170}, {1240, 205}, {1215, 195}},
            {{60, 250}, {96, 236}, {112, 268}, {80, 290}, {56, 280}},
            {{990, 672}, {1030, 662}, {1046, 684}, {1004, 694}},
    };

    private LegendMap() {
    }

    /** mulberry32, as the mock-up draws it, so the map is the one that was approved. */
    static final class Rng {
        private int seed;

        Rng(int seed) {
            this.seed = seed;
        }

        float next() {
            seed += 0x6D2B79F5;
            int t = (seed ^ (seed >>> 15)) * (1 | seed);
            t = (t + ((t ^ (t >>> 7)) * (61 | t))) ^ t;
            return (float) (((t ^ (t >>> 14)) & 0xFFFFFFFFL) / 4294967296.0);
        }
    }

    /** [points] with each edge split [depth] times at a midpoint pushed sideways: a hand-drawn coast. */
    static float[][] rough(float[][] points, Rng r, int depth) {
        List<float[]> pts = new ArrayList<>();
        Collections.addAll(pts, points);
        for (int d = 0; d < depth; d++) {
            List<float[]> out = new ArrayList<>();
            for (int i = 0; i < pts.size(); i++) {
                float[] a = pts.get(i), b = pts.get((i + 1) % pts.size());
                float len = (float) Math.hypot(b[0] - a[0], b[1] - a[1]);
                float k = (r.next() - 0.5f) * len * 0.28f;
                float nx = -(b[1] - a[1]) / (len == 0 ? 1 : len), ny = (b[0] - a[0]) / (len == 0 ? 1 : len);
                out.add(a);
                out.add(new float[]{(a[0] + b[0]) / 2 + nx * k, (a[1] + b[1]) / 2 + ny * k});
            }
            pts = out;
        }
        return pts.toArray(new float[0][]);
    }

    static boolean inside(float[][] pts, float x, float y) {
        boolean c = false;
        for (int i = 0, j = pts.length - 1; i < pts.length; j = i++) {
            float[] a = pts[i], b = pts[j];
            if ((a[1] > y) != (b[1] > y) && x < (b[0] - a[0]) * (y - a[1]) / (b[1] - a[1]) + a[0]) c = !c;
        }
        return c;
    }

    private static void closed(Path path, float[][] pts) {
        path.moveTo(pts[0][0], pts[0][1]);
        for (int i = 1; i < pts.length; i++) path.lineTo(pts[i][0], pts[i][1]);
        path.close();
    }

    private static void star(Path path, int n, float outer, float inner, double rot) {
        for (int i = 0; i < n; i++) {
            double a = rot + i * Math.PI * 2 / n, b = a + Math.PI / n, c = a - Math.PI / n;
            path.moveTo(0, 0);
            path.lineTo((float) (Math.cos(c) * inner), (float) (Math.sin(c) * inner));
            path.lineTo((float) (Math.cos(a) * outer), (float) (Math.sin(a) * outer));
            path.lineTo((float) (Math.cos(b) * inner), (float) (Math.sin(b) * inner));
            path.close();
        }
    }

    private static int alpha(int argb, float a) {
        return (Math.round(255 * a) << 24) | (argb & 0xFFFFFF);
    }

    private static Paint stroke(int color, float width) {
        Paint s = new Paint(Paint.ANTI_ALIAS_FLAG);
        s.setStyle(Paint.Style.STROKE);
        s.setColor(color);
        s.setStrokeWidth(width);
        s.setStrokeJoin(Paint.Join.ROUND);
        s.setStrokeCap(Paint.Cap.ROUND);
        return s;
    }

    private static Paint fill(int color) {
        Paint f = new Paint(Paint.ANTI_ALIAS_FLAG);
        f.setColor(color);
        return f;
    }

    private static final class Peak {
        final float x, y, h;

        Peak(float x, float y, float h) {
            this.x = x;
            this.y = y;
            this.h = h;
        }
    }

    private static void range(List<Peak> peaks, Rng r, float x0, float y0, float x1, float y1, int n, float h0, float h1) {
        for (int i = 0; i < n; i++) {
            float f = i / (float) (n - 1), h = h0 + r.next() * (h1 - h0);
            float x = x0 + (x1 - x0) * f + (r.next() - 0.5f) * 14;
            float y = y0 + (y1 - y0) * f + (float) Math.sin(f * 6) * 10 + (r.next() - 0.5f) * 8;
            peaks.add(new Peak(x, y, h));
        }
    }

    private static void forest(List<float[]> trees, Rng r, float cx, float cy, float rx, float ry, int n) {
        for (int i = 0; i < n; i++) {
            double a = r.next() * Math.PI * 2;
            float d = (float) Math.sqrt(r.next());
            trees.add(new float[]{cx + (float) Math.cos(a) * rx * d, cy + (float) Math.sin(a) * ry * d, 4 + r.next() * 2});
        }
    }

    private static void label(Canvas canvas, Paint text, String s, float x, float y, float size, float spacingPx, float degrees, boolean fromCorner) {
        text.setTextSize(size);
        text.setLetterSpacing(spacingPx / size);
        float w = text.measureText(s);
        canvas.save();
        if (fromCorner) canvas.rotate(degrees, x, y);
        else canvas.rotate(degrees, x + w / 2, y + size / 2);
        canvas.drawText(s, x, y + size, text);
        canvas.restore();
    }

    static void draw(Canvas canvas, LegendTone p, Typeface italic, Typeface bold, Context ctx) {
        Rng r = new Rng(11);
        float[][] main = rough(MAIN, r, 3);
        float[][][] isles = new float[ISLES.length][][];
        for (int i = 0; i < ISLES.length; i++) isles[i] = rough(ISLES[i], r, 2);
        Path lands = new Path();
        closed(lands, main);
        for (float[][] isle : isles) closed(lands, isle);

        canvas.drawColor(p.sea);
        Paint grain = fill(alpha(p.ink, 0.07f));
        for (int i = 0; i < 1400; i++) {
            float x = r.next() * 1280, y = r.next() * 720, s = 0.5f + r.next() * 0.9f;
            canvas.drawRect(Math.round(x), Math.round(y), Math.round(x) + s, Math.round(y) + s, grain);
        }
        Path waves = new Path();
        for (int i = 0; i < 140; i++) {
            float x = 30 + r.next() * 1220, y = 30 + r.next() * 660;
            boolean land = inside(main, x, y) || inside(main, x + 14, y) || inside(main, x - 14, y);
            for (float[][] isle : isles) if (inside(isle, x, y)) land = true;
            if (land || (x < 340 && y < 210) || (x > 380 && x < 900 && y > 630)) continue;
            x = Math.round(x);
            y = Math.round(y);
            waves.moveTo(x, y);
            waves.quadTo(x + 4, y - 4, x + 8, y);
            waves.quadTo(x + 12, y + 4, x + 16, y);
        }
        canvas.drawPath(waves, stroke(alpha(p.ink, 0.35f), 1.2f));
        canvas.drawPath(lands, stroke(alpha(p.ink, 0.05f), 34));
        canvas.drawPath(lands, stroke(alpha(p.ink, 0.06f), 22));
        canvas.drawPath(lands, stroke(alpha(p.ink, 0.09f), 12));
        canvas.drawPath(lands, fill(p.land));
        canvas.drawPath(lands, stroke(alpha(p.ink, 0.85f), 2.2f));

        canvas.drawOval(674, 400, 766, 440, fill(alpha(p.water, 0.35f)));
        canvas.drawOval(674, 400, 766, 440, stroke(alpha(p.ink, 0.8f), 1.6f));
        Path rivers = new Path();
        rivers.moveTo(700, 196);
        rivers.cubicTo(690, 260, 740, 330, 716, 400);
        rivers.moveTo(716, 440);
        rivers.cubicTo(700, 490, 680, 530, 716, 590);
        rivers.cubicTo(752, 650, 700, 640, 706, 660);
        rivers.moveTo(1112, 330);
        rivers.cubicTo(1060, 360, 1030, 400, 1010, 450);
        rivers.cubicTo(990, 500, 950, 540, 962, 612);
        rivers.moveTo(226, 470);
        rivers.cubicTo(260, 500, 300, 530, 318, 580);
        rivers.cubicTo(336, 630, 300, 630, 296, 650);
        rivers.moveTo(1010, 450);
        rivers.cubicTo(1040, 470, 1060, 500, 1050, 540);
        canvas.drawPath(rivers, stroke(alpha(p.water, 0.9f), 3));
        canvas.drawPath(rivers, stroke(alpha(p.ink, 0.5f), 0.8f));

        List<Peak> peaks = new ArrayList<>();
        range(peaks, r, 640, 182, 1000, 168, 13, 26, 46);
        range(peaks, r, 1128, 280, 1140, 455, 8, 28, 44);
        range(peaks, r, 205, 340, 236, 520, 7, 22, 34);
        peaks.add(new Peak(1130, 506, 40));
        Collections.sort(peaks, (a, b) -> Float.compare(a.y, b.y));
        List<float[]> trees = new ArrayList<>();
        forest(trees, r, 445, 250, 70, 36, 42);
        forest(trees, r, 700, 585, 60, 22, 26);
        forest(trees, r, 365, 560, 45, 28, 22);
        forest(trees, r, 905, 300, 34, 18, 14);
        Collections.sort(trees, (a, b) -> Float.compare(a[1], b[1]));
        Path hills = new Path();
        for (int i = 0; i < 22; i++) {
            float x = Math.round(430 + r.next() * 200), y = Math.round(440 + r.next() * 70);
            hills.moveTo(x, y);
            hills.quadTo(x + 9, y - 9, x + 18, y);
        }
        for (int i = 0; i < 12; i++) {
            float x = Math.round(1000 + r.next() * 90), y = Math.round(470 + r.next() * 90);
            hills.moveTo(x, y);
            hills.quadTo(x + 8, y - 8, x + 16, y);
        }
        canvas.drawPath(hills, stroke(alpha(p.ink, 0.6f), 1.4f));

        // Stains in the parchment.
        Paint stain = new Paint(Paint.ANTI_ALIAS_FLAG);
        float[][] stains = {{210, 140, 220}, {1150, 640, 160}, {880, 90, 110}};
        for (float[] s : stains) {
            stain.setShader(new RadialGradient(s[0], s[1], s[2], new int[]{alpha(p.stain, 0.35f), alpha(p.stain, 0.12f), alpha(p.stain, 0f)},
                    new float[]{0f, 0.7f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawCircle(s[0], s[1], s[2], stain);
        }

        Paint face = fill(p.land2);
        Paint edge = stroke(p.ink, 1.6f);
        Paint hatch = stroke(alpha(p.ink, 0.7f), 1f);
        Path m = new Path();
        for (Peak k : peaks) {
            float w = k.h * 1.7f, H = k.h + 4, ox = k.x - w / 2 - 2, oy = k.y - H;
            float lx = ox + 2, cx = ox + w / 2 + 2, rx = ox + w + 2, by = oy + H, ty = oy + 4;
            m.reset();
            m.moveTo(lx, by);
            m.lineTo(cx, ty);
            m.lineTo(rx, by);
            m.close();
            canvas.drawPath(m, face);
            canvas.drawPath(m, edge);
            m.reset();
            for (int j = 1; j <= 5; j++) {
                float f = j / 6f, sx = cx + (rx - cx) * f, sy = ty + (by - ty) * f;
                m.moveTo(sx, sy);
                m.lineTo(sx - 3 - f * 4, by);
            }
            m.moveTo(cx, ty);
            m.lineTo(cx - 4, ty + H * 0.35f);
            canvas.drawPath(m, hatch);
        }
        Paint crown = fill(p.tree);
        Paint ink = stroke(p.ink, 1.1f);
        Paint trunk = stroke(p.ink, 1.3f);
        for (float[] t : trees) {
            canvas.drawLine(t[0], t[1], t[0], t[1] - 6, trunk);
            canvas.drawCircle(t[0], t[1] - 10, t[2], crown);
            canvas.drawCircle(t[0], t[1] - 10, t[2], ink);
        }
        // The volcano's fire and smoke.
        m.reset();
        m.moveTo(1124, 470);
        m.lineTo(1130, 454);
        m.lineTo(1136, 470);
        m.close();
        canvas.drawPath(m, fill(alpha(p.ember, 0.9f)));
        m.reset();
        m.moveTo(1130, 452);
        m.rCubicTo(-6, -10, 6, -16, 0, -26);
        m.rCubicTo(-5, -8, 5, -12, 2, -20);
        canvas.drawPath(m, stroke(alpha(p.ink, 0.55f), 1.3f));

        // Region names.
        Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        text.setTypeface(italic);
        text.setColor(alpha(p.ink, 0.72f));
        label(canvas, text, ctx.getString(R.string.legend_region_north), 760, 116, 18, 7, -3, false);
        label(canvas, text, ctx.getString(R.string.legend_region_wood), 386, 282, 16, 5, 0, false);
        label(canvas, text, ctx.getString(R.string.legend_region_moors), 452, 478, 16, 6, 4, false);
        label(canvas, text, ctx.getString(R.string.legend_region_sea), 1092, 594, 18, 5, -12, false);
        label(canvas, text, ctx.getString(R.string.legend_region_ash), 1158, 270, 14, 4, 90, true);
        label(canvas, text, ctx.getString(R.string.legend_region_isles), 40, 300, 13, 3, -8, false);

        // The frame.
        canvas.drawRect(14, 14, 1266, 706, stroke(alpha(p.ink, 0.8f), 2.5f));
        canvas.drawRect(22, 22, 1258, 698, stroke(alpha(p.ink, 0.6f), 1f));
        Paint corner = stroke(alpha(p.ink, 0.7f), 1.5f);
        canvas.drawLine(14, 34, 34, 14, corner);
        canvas.drawLine(1246, 14, 1266, 34, corner);
        canvas.drawLine(14, 686, 34, 706, corner);
        canvas.drawLine(1246, 706, 1266, 686, corner);

        // The compass rose.
        canvas.save();
        canvas.translate(96, 618);
        canvas.drawCircle(0, 0, 46, stroke(alpha(p.ink, 0.8f), 1.5f));
        Paint dashed = stroke(alpha(p.ink, 0.6f), 0.8f);
        dashed.setPathEffect(new DashPathEffect(new float[]{2, 3}, 0));
        canvas.drawCircle(0, 0, 40, dashed);
        Path rose = new Path();
        star(rose, 16, 30, 5, -Math.PI / 2 + Math.PI / 16);
        canvas.drawPath(rose, fill(alpha(p.ink, 0.25f)));
        canvas.drawPath(rose, stroke(alpha(p.ink, 0.7f), 0.8f));
        rose.reset();
        star(rose, 8, 42, 7, -Math.PI / 2);
        canvas.drawPath(rose, fill(alpha(p.ink, 0.6f)));
        canvas.drawPath(rose, stroke(alpha(p.ink, 0.85f), 1f));
        canvas.drawCircle(0, 0, 4, fill(p.accent));
        canvas.drawCircle(0, 0, 4, stroke(p.ink, 1f));
        canvas.restore();
        Paint n = new Paint(Paint.ANTI_ALIAS_FLAG);
        n.setTypeface(bold);
        n.setTextSize(14);
        n.setColor(p.ink);
        canvas.drawText("N", 89, 562, n);

        // The scale, in leagues.
        canvas.save();
        canvas.translate(1040, 668);
        Paint bar = stroke(alpha(p.ink, 0.8f), 1.4f);
        canvas.drawLine(0, 0, 160, 0, bar);
        for (int i = 0; i <= 4; i++) canvas.drawLine(i * 40, i % 2 == 0 ? -5 : -4, i * 40, i % 2 == 0 ? 5 : 4, bar);
        Paint block = fill(alpha(p.ink, 0.7f));
        canvas.drawRect(0, -2.5f, 40, 2.5f, block);
        canvas.drawRect(80, -2.5f, 120, 2.5f, block);
        canvas.restore();
        text.setColor(alpha(p.ink, 0.8f));
        label(canvas, text, ctx.getString(R.string.legend_leagues), 1086, 640, 13, 2, 0, false);
    }
}
