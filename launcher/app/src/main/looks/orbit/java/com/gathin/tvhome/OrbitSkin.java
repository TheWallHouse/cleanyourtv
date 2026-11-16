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
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import java.util.Locale;

/**
 * Orbit (Aspect.ORBIT) over Home's own views: the tiles, the dock, focus, keys and settings are Nothing's as they are.
 * Behind them, in place of the wallpaper, the Earth's rim seen from orbit (its glow, its night side's city lights, the
 * stars, a faint orbit with a satellite), drawn once. In place of the greeting and clock, the focused app's name in
 * thin wide capitals as the "mission in progress", with its place in the list as a progress line; under the top
 * buttons a strip of telemetry (local time, date, the network, the time on). Nothing here takes focus.
 */
final class OrbitSkin {
    private static final long MINUTE_MS = 60_000L;
    static final float MAP_W = 1280f, MAP_H = 720f;

    private final Context ctx;
    private final OrbitTone p;
    private final AppAdapter adapter;
    private final float density;
    private final Typeface light, medium;
    private final Locale locale;
    private TextView mission, name, uptime, link;
    private View progress;
    private int progressWidth;

    private OrbitSkin(Context ctx, OrbitTone p, AppAdapter adapter) {
        this.ctx = ctx;
        this.p = p;
        this.adapter = adapter;
        density = ctx.getResources().getDisplayMetrics().density;
        light = ctx.getResources().getFont(R.font.barlow_light);
        medium = ctx.getResources().getFont(R.font.barlow_medium);
        locale = ctx.getResources().getConfiguration().getLocales().get(0);
    }

    /**
     * Puts the look on Home. [below] is the view the scenery goes under (the sky), [hidden] views it replaces (the sky,
     * the ambient glow, the top shelf), [leftBlock] the greeting and clock column, [wifi] the network pill.
     */
    static OrbitSkin apply(Context ctx, OrbitTone p, AppAdapter adapter, FrameLayout root, View below, View[] hidden,
                           ViewGroup leftBlock, TextView wifi, String clockFormat) {
        OrbitSkin s = new OrbitSkin(ctx, p, adapter);
        root.setBackgroundColor(0xFF000000);
        int at = below != null ? Math.max(0, root.indexOfChild(below)) : 0;
        root.addView(new Backdrop(ctx, p), at, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        for (View v : hidden) if (v != null) v.setVisibility(View.GONE);
        s.ownFonts(root);
        s.addMission(leftBlock);
        s.addTelemetry(root, clockFormat);
        if (wifi != null) {
            s.link.setText(s.upper(String.valueOf(wifi.getText())));
            wifi.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence c, int a, int b, int d) {}
                @Override public void onTextChanged(CharSequence c, int a, int b, int d) {}
                @Override public void afterTextChanged(Editable e) { s.link.setText(s.upper(e.toString())); }
            });
        }
        s.tick(root);
        return s;
    }

    /** Barlow on Home's own text (the top buttons and the rest) in place of Nothing's dot-matrix and other fonts. */
    private void ownFonts(View v) {
        if (v instanceof TextView && !(v instanceof android.widget.TextClock)) {
            TextView t = (TextView) v;
            t.setTypeface(medium);
            t.setLetterSpacing(0.12f);
            t.setAllCaps(true);
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) ownFonts(g.getChildAt(i));
        }
    }

    private int dp(float v) {
        return Math.round(v * density);
    }

    private String upper(String s) {
        return s == null ? "" : s.toUpperCase(locale);
    }

    private TextView text(Typeface face, float sp, int color, float spacing) {
        TextView t = new TextView(ctx);
        t.setTypeface(face);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setLetterSpacing(spacing);
        t.setIncludeFontPadding(false);
        t.setFocusable(false);
        t.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return t;
    }

    private void addMission(ViewGroup leftBlock) {
        for (int i = 0; i < leftBlock.getChildCount(); i++) leftBlock.getChildAt(i).setVisibility(View.GONE);
        LinearLayout column = new LinearLayout(ctx);
        column.setOrientation(LinearLayout.VERTICAL);

        LinearLayout mark = new LinearLayout(ctx);
        mark.setGravity(Gravity.CENTER_VERTICAL);
        TextView word = text(medium, 10, p.fg, 0.6f);
        word.setText("WALLHOUSE");
        mark.addView(word);
        View bar = new View(ctx);
        bar.setBackgroundColor(p.muted);
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(Math.max(1, dp(1)), dp(11));
        bl.leftMargin = dp(12);
        bl.rightMargin = dp(12);
        mark.addView(bar, bl);
        TextView look = text(light, 10, p.muted, 0.6f);
        look.setText(upper(ctx.getString(R.string.aspect_orbit)));
        mark.addView(look);
        column.addView(mark);

        mission = text(light, 9, p.muted, 0.4f);
        LinearLayout.LayoutParams ml = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ml.topMargin = dp(30);
        column.addView(mission, ml);

        name = text(light, 46, p.fg, 0.14f);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams nl = new LinearLayout.LayoutParams(dp(520), dp(54));
        nl.topMargin = dp(8);
        column.addView(name, nl);

        FrameLayout track = new FrameLayout(ctx);
        progressWidth = dp(280);
        View line = new View(ctx);
        line.setBackgroundColor(p.line);
        track.addView(line, new FrameLayout.LayoutParams(progressWidth, Math.max(1, dp(1)), Gravity.CENTER_VERTICAL));
        progress = new View(ctx);
        progress.setBackgroundColor(p.accent);
        track.addView(progress, new FrameLayout.LayoutParams(0, Math.max(2, dp(2)), Gravity.CENTER_VERTICAL));
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(progressWidth, dp(4));
        tl.topMargin = dp(10);
        column.addView(track, tl);

        TextView launch = text(medium, 9, p.fg, 0.4f);
        launch.setText(upper(ctx.getString(R.string.orbit_launch)));
        LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ll.topMargin = dp(10);
        column.addView(launch, ll);
        leftBlock.addView(column, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private LinearLayout cell(String key, View value) {
        LinearLayout c = new LinearLayout(ctx);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(14), 0, 0, 0);
        TextView k = text(light, 8, p.muted, 0.3f);
        k.setText(upper(key));
        c.addView(k);
        LinearLayout.LayoutParams vl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        vl.topMargin = dp(4);
        c.addView(value, vl);
        return c;
    }

    private TextClock clock(String h24, String h12, String clockFormat) {
        TextClock c = new TextClock(ctx);
        c.setTypeface(light);
        c.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        c.setTextColor(p.fg);
        c.setLetterSpacing(0.15f);
        c.setAllCaps(true);
        c.setFocusable(false);
        c.setFormat24Hour("12".equals(clockFormat) ? h12 : h24);
        c.setFormat12Hour("24".equals(clockFormat) ? h24 : h12);
        return c;
    }

    private void addTelemetry(FrameLayout root, String clockFormat) {
        LinearLayout strip = new LinearLayout(ctx);
        strip.setFocusable(false);
        View[] values = {
                clock("HH:mm:ss", "h:mm:ss a", clockFormat),
                clock("EEE dd MMM", "EEE dd MMM", clockFormat),
                link = text(light, 13, p.fg, 0.15f),
                uptime = text(light, 13, p.fg, 0.15f),
        };
        int[] keys = {R.string.orbit_time, R.string.orbit_date, R.string.orbit_link, R.string.orbit_up};
        link.setSingleLine(true);
        link.setMaxWidth(dp(150));
        link.setEllipsize(TextUtils.TruncateAt.END);
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                View rule = new View(ctx);
                rule.setBackgroundColor(p.line);
                LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(Math.max(1, dp(1)), dp(26));
                rl.leftMargin = dp(14);
                strip.addView(rule, rl);
            }
            strip.addView(cell(ctx.getString(keys[i]), values[i]));
        }
        FrameLayout.LayoutParams sl = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
        sl.topMargin = dp(26);
        sl.rightMargin = dp(48);
        root.addView(strip, sl);
        renderUptime();
    }

    private void renderUptime() {
        long minutes = SystemClock.elapsedRealtime() / MINUTE_MS;
        uptime.setText(upper(ctx.getString(R.string.look_uptime_value, (int) (minutes / 60), (int) (minutes % 60))));
    }

    private void tick(View root) {
        Runnable minute = new Runnable() {
            @Override public void run() {
                if (!root.isAttachedToWindow()) return;
                renderUptime();
                root.postDelayed(this, MINUTE_MS);
            }
        };
        root.postDelayed(minute, MINUTE_MS);
    }

    /** The focused tile moved: its name becomes the mission, its place the progress line. */
    void onFocus(int position) {
        int total = 0, place = 0;
        for (int i = 0; i < adapter.appCount(); i++) {
            if (adapter.isEmptyAt(i)) continue;
            total++;
            if (i <= position) place = total;
        }
        if (adapter.isEmptyAt(position)) place = 0;
        Overlay.fit(name, place > 0 ? upper(adapter.labelAt(position)) : "", 46, 18, dp(520));
        mission.setText(upper(ctx.getString(R.string.orbit_mission)) + (place > 0 ? String.format(Locale.ROOT, "  ·  %02d / %02d", place, total) : ""));
        ViewGroup.LayoutParams l = progress.getLayoutParams();
        l.width = total > 0 ? Math.round(progressWidth * place / (float) total) : 0;
        progress.setLayoutParams(l);
    }

    // ---- Inputs, as a mission-control panel ----------------------------------------------------------------

    /** One input on the panel. */
    private static final class Input {
        final int icon;
        final String label;
        final boolean connected;
        final Runnable open;

        Input(int icon, String label, boolean connected, Runnable open) {
            this.icon = icon;
            this.label = label;
            this.connected = connected;
            this.open = open;
        }
    }

    /**
     * Shows the inputs over Home as Orbit's own panel: the ground dimmed, a black panel with hairlines, the inputs
     * numbered with their icons, the chosen one framed with a bar in the accent; ▲▼ choose, OK switches, Back closes.
     * [list] hands each input in (MainActivity.listInputs); [back] is where focus returns.
     */
    void showInputs(FrameLayout root, java.util.function.Consumer<DuneHome.InputSink> list, View back) {
        java.util.List<Input> inputs = new java.util.ArrayList<>();
        list.accept((icon, label, state, open) -> inputs.add(new Input(icon, label,
                state == android.media.tv.TvInputManager.INPUT_STATE_CONNECTED, open)));
        if (inputs.isEmpty()) return;
        Panel panel = new Panel(ctx, inputs, root, back);
        root.addView(panel, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        panel.requestFocus();
    }

    private final class Panel extends View {
        private final java.util.List<Input> inputs;
        private final FrameLayout root;
        private final View back;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final android.graphics.RectF box = new android.graphics.RectF();
        private int index;

        Panel(Context c, java.util.List<Input> inputs, FrameLayout root, View back) {
            super(c);
            this.inputs = inputs;
            this.root = root;
            this.back = back;
            setFocusable(true);
            setFocusableInTouchMode(true);
            setClickable(true);
            // Above Home's raised views (the pills, the focused tile), which would otherwise draw through it.
            setOutlineProvider(null);
            setElevation(dp(100));
        }

        private void close() {
            root.removeView(this);
            if (back != null) back.requestFocus();
        }

        private void text(String s, Typeface face, float sp, float spacing, int color, float x, float baseline, Paint.Align align) {
            paint.setShader(null);
            paint.setStyle(Paint.Style.FILL);
            paint.setTypeface(face);
            paint.setTextSize(sp * getResources().getDisplayMetrics().scaledDensity);
            paint.setLetterSpacing(spacing);
            paint.setColor(color);
            paint.setTextAlign(align);
            canvas.drawText(s, x, baseline, paint);
        }

        private Canvas canvas;

        @Override protected void onDraw(Canvas c) {
            canvas = c;
            c.drawColor(0xB3000000);
            float w = dp(420), rowH = dp(46), head = dp(74), foot = dp(44);
            float h = head + inputs.size() * rowH + foot;
            float left = (getWidth() - w) / 2f, top = (getHeight() - h) / 2f;
            box.set(left, top, left + w, top + h);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFF050810);
            c.drawRect(box, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1, dp(1)));
            paint.setColor(p.line);
            c.drawRect(box, paint);
            // Corner ticks and a satellite on its orbit, top right.
            paint.setColor(p.accent);
            float t = dp(10);
            c.drawLine(left, top, left + t, top, paint);
            c.drawLine(left, top, left, top + t, paint);
            c.drawLine(box.right, box.bottom, box.right - t, box.bottom, paint);
            c.drawLine(box.right, box.bottom, box.right, box.bottom - t, paint);
            paint.setColor(p.muted);
            c.drawArc(box.right - dp(70), top - dp(20), box.right + dp(20), top + dp(46), 150, 120, false, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(p.accent);
            c.drawCircle(box.right - dp(56), top + dp(4), dp(2.5f), paint);
            // Heading.
            float x = left + dp(24);
            text(upper(ctx.getString(R.string.look_inputs)), medium, 12, 0.5f, p.fg, x, top + dp(32), Paint.Align.LEFT);
            text(upper(ctx.getString(R.string.orbit_inputs_sub)), light, 8, 0.4f, p.muted, x, top + dp(50), Paint.Align.LEFT);
            paint.setColor(p.line);
            c.drawRect(x, top + head - dp(10), box.right - dp(24), top + head - dp(10) + Math.max(1, dp(1)), paint);
            // Rows.
            for (int i = 0; i < inputs.size(); i++) {
                Input in = inputs.get(i);
                boolean on = i == index;
                float y = top + head + i * rowH;
                if (on) {
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(0x14FFFFFF);
                    c.drawRect(left + dp(12), y, box.right - dp(12), y + rowH - dp(6), paint);
                    paint.setColor(p.accent);
                    c.drawRect(left + dp(12), y, left + dp(15), y + rowH - dp(6), paint);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setColor(p.line);
                    c.drawRect(left + dp(12), y, box.right - dp(12), y + rowH - dp(6), paint);
                }
                float mid = y + (rowH - dp(6)) / 2f;
                text(String.format(java.util.Locale.ROOT, "%02d", i + 1), light, 9, 0.2f, on ? p.accent : p.muted, x + dp(4), mid + dp(4), Paint.Align.LEFT);
                android.graphics.drawable.Drawable d = ctx.getDrawable(in.icon);
                if (d != null) {
                    d = d.mutate();
                    d.setTint(on ? p.fg : p.muted);
                    int s = dp(16);
                    d.setBounds(Math.round(x + dp(34)), Math.round(mid - s / 2f), Math.round(x + dp(34)) + s, Math.round(mid - s / 2f) + s);
                    d.draw(c);
                }
                text(upper(in.label), on ? medium : light, 13, 0.18f, on ? p.fg : p.muted, x + dp(62), mid + dp(5), Paint.Align.LEFT);
                if (in.connected) text(upper(ctx.getString(R.string.connected)), light, 8, 0.3f, p.accent, box.right - dp(28), mid + dp(3), Paint.Align.RIGHT);
                else if (on) text("▶", medium, 10, 0f, p.accent, box.right - dp(28), mid + dp(4), Paint.Align.RIGHT);
            }
            text(upper(ctx.getString(R.string.orbit_inputs_hint)), light, 8, 0.3f, p.muted, (left + box.right) / 2f, box.bottom - dp(18), Paint.Align.CENTER);
        }

        @Override public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
            switch (keyCode) {
                case android.view.KeyEvent.KEYCODE_DPAD_UP:
                    if (index > 0) index--;
                    invalidate();
                    return true;
                case android.view.KeyEvent.KEYCODE_DPAD_DOWN:
                    if (index < inputs.size() - 1) index++;
                    invalidate();
                    return true;
                case android.view.KeyEvent.KEYCODE_DPAD_CENTER:
                case android.view.KeyEvent.KEYCODE_ENTER:
                case android.view.KeyEvent.KEYCODE_NUMPAD_ENTER:
                    if (event.getRepeatCount() == 0) {
                        Runnable open = inputs.get(index).open;
                        close();
                        open.run();
                    }
                    return true;
                case android.view.KeyEvent.KEYCODE_BACK:
                case android.view.KeyEvent.KEYCODE_ESCAPE:
                    close();
                    return true;
                default:
                    return true;   // nothing under the panel moves while it is open
            }
        }
    }

    /** The planet's rim, its glow and lights, the stars and a faint orbit, in a 1280×720 space; drawn once. */
    private static final class Backdrop extends View {
        private final OrbitTone p;

        Backdrop(Context c, OrbitTone p) {
            super(c);
            this.p = p;
            setFocusable(false);
            setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        private static int alpha(int argb, int a) {
            return (a << 24) | (argb & 0xFFFFFF);
        }

        private static float[] at(double deg) {
            double a = Math.toRadians(deg);
            return new float[]{(float) (640 + 760 * Math.cos(a)), (float) (820 + 600 * Math.sin(a))};
        }

        private static Path arc(double from, double to) {
            Path path = new Path();
            for (double g = from; g <= to + 0.01; g += 1) {
                float[] q = at(g);
                if (g == from) path.moveTo(q[0], q[1]);
                else path.lineTo(q[0], q[1]);
            }
            return path;
        }

        @Override protected void onDraw(Canvas canvas) {
            canvas.drawColor(0xFF000000);
            canvas.save();
            canvas.scale(getWidth() / MAP_W, getHeight() / MAP_H);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            MapDrawing.Rng r = new MapDrawing.Rng(5);
            paint.setColor(alpha(0xFFFFFF, p.starsAlpha));
            for (int i = 0; i < 220; i++) {
                float x = r.next() * 1280, y = r.next() * 560, s = r.next() < 0.08f ? 1.3f : 0.6f;
                canvas.drawCircle(x, y, s, paint);
            }
            float cx = 640, cy = 1760, radius = 1180;
            paint.setShader(new RadialGradient(cx, cy, 1330, new int[]{alpha(p.atm, 97), alpha(p.atm, 97), alpha(p.atm, 31), alpha(p.atm, 0)},
                    new float[]{0f, 0.86f, 0.9f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawCircle(cx, cy, 1330, paint);
            paint.setShader(new LinearGradient(0, 580, 0, 720, p.groundTop, p.groundLow, Shader.TileMode.CLAMP));
            canvas.drawCircle(cx, cy, radius, paint);
            paint.setShader(null);
            if (p.lightsAlpha > 0) {
                paint.setColor(alpha(p.lights, p.lightsAlpha));
                for (int i = 0; i < 420; i++) {
                    float x = r.next() * 1280, y = 585 + (float) Math.pow(r.next(), 1.6) * 135;
                    float top = cy - (float) Math.sqrt(Math.max(0, radius * radius - (x - cx) * (x - cx)));
                    float s = r.next() < 0.1f ? 1.1f : 0.55f;
                    if (y < top + 6) continue;
                    canvas.drawRect(x, y, x + s, y + s, paint);
                }
            }
            Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
            rim.setStyle(Paint.Style.STROKE);
            rim.setColor(alpha(p.atm, 31));
            rim.setStrokeWidth(22);
            canvas.drawCircle(cx, cy, radius, rim);
            rim.setColor(alpha(p.atm, 77));
            rim.setStrokeWidth(8);
            canvas.drawCircle(cx, cy, radius, rim);
            rim.setColor(alpha(p.limb, 230));
            rim.setStrokeWidth(1.6f);
            canvas.drawCircle(cx, cy, radius, rim);
            if (p.sunX >= 0) {
                float sx = p.sunX * 1280, sy = 588;
                canvas.save();
                canvas.scale(1f, 90f / 260f, sx, sy);
                paint.setShader(new RadialGradient(sx, sy, 260, new int[]{0xF2FFFFFF, alpha(p.sun, 140), alpha(p.sun, 0)},
                        new float[]{0f, 0.12f, 1f}, Shader.TileMode.CLAMP));
                canvas.drawCircle(sx, sy, 260, paint);
                paint.setShader(null);
                canvas.restore();
            }
            Paint orbit = new Paint(Paint.ANTI_ALIAS_FLAG);
            orbit.setStyle(Paint.Style.STROKE);
            orbit.setColor(alpha(p.fg, 41));
            orbit.setStrokeWidth(1);
            orbit.setPathEffect(new DashPathEffect(new float[]{2, 6}, 0));
            canvas.drawPath(arc(196, 344), orbit);
            orbit.setPathEffect(null);
            orbit.setColor(alpha(p.accent, 115));
            orbit.setStrokeWidth(1.2f);
            canvas.drawPath(arc(196, 282), orbit);
            float[] sat = at(282);
            paint.setColor(p.accent);
            canvas.drawCircle(sat[0], sat[1], 3, paint);
            orbit.setColor(alpha(p.accent, 128));
            orbit.setStrokeWidth(1);
            canvas.drawCircle(sat[0], sat[1], 8, orbit);
            canvas.restore();
        }
    }
}
