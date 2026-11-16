package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Legend (Aspect.MAP): Home as an old hand-drawn map, over the same apps as the tiles. The dock's apps are castles
 * joined by dotted roads, with Launcher settings as one more; the D-pad jumps to the nearest place that way, OK opens
 * it. The greeting, clock and date sit in a cartouche; Inputs and the installed-app picker are two buttons top right;
 * a ribbon along the bottom names the focused place. The lands, mountains, forests, rivers and their names are made
 * up and drawn once, in a 1280×720 map scaled to the screen. The tile grid stays underneath, loaded and hidden, so
 * the order, the dock, hidden apps and the reloads are Home's own; this only opens them.
 */
final class MapHome {
    static final float MAP_W = 1280f, MAP_H = 720f;
    private static final long MINUTE_MS = 60_000L;
    private static final int MAX_APPS = 7;
    /** Where the apps stand, in dock order, and where Launcher settings stands. */
    private static final float[][] SLOTS = {{600, 330}, {800, 240}, {960, 390}, {830, 510}, {540, 540}, {340, 420}, {470, 380}};
    private static final float[] SETTINGS_AT = {1050, 210};
    private static final int SETTINGS = 7;
    /** Roads between places, by slot (SETTINGS for Launcher settings). */
    private static final int[][] ROADS = {{0, 1}, {1, SETTINGS}, {0, 2}, {0, 3}, {3, 4}, {4, 5}};

    /** What Legend's buttons and settings place open. */
    interface Actions {
        /** Hands each of the TV's inputs to [sink], in the Inputs dropdown's order. */
        void listInputs(DuneHome.InputSink sink);
        void launcherSettings();
    }

    private final Context ctx;
    private final MapTone p;
    private final AppAdapter adapter;
    private final Actions actions;
    private final String clockFormat;
    private final Typeface cinzel, cinzelBold, italic;
    private final Locale locale;
    private final FrameLayout view;
    private Places places;
    private String greeting = "";
    private final List<Target> targets = new ArrayList<>();
    private int focused;
    /** The viewer has moved on the map: a reload keeps their place; before that the first app is focused. */
    private boolean moved;

    private MapHome(Context ctx, MapTone p, AppAdapter adapter, String clockFormat, Actions actions) {
        this.ctx = ctx;
        this.p = p;
        this.adapter = adapter;
        this.actions = actions;
        this.clockFormat = clockFormat;
        cinzel = ctx.getResources().getFont(R.font.cinzel);
        cinzelBold = ctx.getResources().getFont(R.font.cinzel_bold);
        italic = Typeface.create(Typeface.SERIF, Typeface.ITALIC);
        locale = ctx.getResources().getConfiguration().getLocales().get(0);
        view = new FrameLayout(ctx);
    }

    /** Lays the map over Home's [root], as the last child. [greetingView] is Home's greeting, which the cartouche repeats. */
    static MapHome attach(Context ctx, MapTone p, FrameLayout root, AppAdapter adapter, TextView greetingView,
                             String clockFormat, Actions actions) {
        MapHome l = new MapHome(ctx, p, adapter, clockFormat, actions);
        l.build();
        root.addView(l.view, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        Overlay.isolate(root, l.view);
        if (greetingView != null) {
            l.greeting = String.valueOf(greetingView.getText());
            greetingView.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int d) {}
                @Override public void onTextChanged(CharSequence s, int a, int b, int d) {}
                @Override public void afterTextChanged(Editable s) {
                    l.greeting = s.toString();
                    l.places.invalidate();
                }
            });
        }
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override public void onChanged() { l.fill(); }
            @Override public void onItemRangeInserted(int start, int count) { l.fill(); }
            @Override public void onItemRangeRemoved(int start, int count) { l.fill(); }
            @Override public void onItemRangeMoved(int from, int to, int count) { l.fill(); }
        });
        l.fill();
        l.tick();
        return l;
    }

    void focus() {
        places.requestFocus();
    }

    boolean hasFocus() {
        return view.hasFocus();
    }

    private void build() {
        view.setClickable(true);   // touches never fall through to the hidden tiles
        view.addView(new Backdrop(ctx), new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        places = new Places(ctx);
        view.addView(places, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    /** A place or a button, at a point of the map. */
    private static final class Target {
        final String label;
        final float x, y;
        final int slot;   // SLOTS index, SETTINGS, or -1 for a button
        final Runnable run;
        final RectF box = new RectF();

        Target(String label, float x, float y, int slot, Runnable run) {
            this.label = label;
            this.x = x;
            this.y = y;
            this.slot = slot;
            this.run = run;
        }
    }

    private void fill() {
        String was = moved && focused < targets.size() ? targets.get(focused).label : null;
        targets.clear();
        int n = adapter.appCount();
        int end = Math.min(n, Math.max(adapter.dockFilled(), 0));
        if (end == 0) end = n;   // no dock: the first apps
        int slot = 0;
        for (int i = 0; i < end && slot < MAX_APPS; i++) {
            if (adapter.isEmptyAt(i)) continue;
            final int pos = i;
            targets.add(new Target(adapter.labelAt(i), SLOTS[slot][0], SLOTS[slot][1], slot, () -> adapter.launchAt(pos)));
            slot++;
        }
        targets.add(new Target(ctx.getString(R.string.look_settings), SETTINGS_AT[0], SETTINGS_AT[1], SETTINGS, () -> Overlay.openTvSettings(ctx)));
        targets.add(new Target(ctx.getString(R.string.look_inputs), 0, 0, -1, () -> openScroll(false)));
        targets.add(new Target(ctx.getString(R.string.map_all_apps), 0, 0, -1, () -> openScroll(true)));
        focused = 0;
        if (was != null) for (int i = 0; i < targets.size(); i++) if (targets.get(i).label.equals(was)) focused = i;
        places.layoutButtons();
        places.invalidate();
    }

    /** An entry on the unrolled scroll: an input or an app. */
    private static final class Entry {
        final String label;
        final int icon;          // an input's icon, or 0
        final int position;      // an app's place in the adapter, or -1
        final Runnable run;

        Entry(String label, int icon, int position, Runnable run) {
            this.label = label;
            this.icon = icon;
            this.position = position;
            this.run = run;
        }
    }

    /** The scroll over the map: null when rolled up; otherwise the inputs (a list) or every app (a grid). */
    private List<Entry> scroll;
    private boolean scrollApps;
    private int scrollIndex, scrollTop;

    private void openScroll(boolean apps) {
        List<Entry> out = new ArrayList<>();
        if (apps) {
            for (int i = 0; i < adapter.appCount(); i++) {
                if (adapter.isEmptyAt(i)) continue;
                final int pos = i;
                out.add(new Entry(adapter.labelAt(i), 0, i, () -> adapter.launchAt(pos)));
            }
        } else {
            actions.listInputs((icon, label, state, open) -> out.add(new Entry(label, icon, -1, open)));
        }
        if (out.isEmpty()) return;
        scroll = out;
        scrollApps = apps;
        scrollIndex = 0;
        scrollTop = 0;
        places.invalidate();
    }

    private void closeScroll() {
        scroll = null;
        places.invalidate();
    }

    private void tick() {
        Runnable minute = new Runnable() {
            @Override public void run() {
                if (!view.isAttachedToWindow()) return;
                places.invalidate();
                view.postDelayed(this, MINUTE_MS - System.currentTimeMillis() % MINUTE_MS);
            }
        };
        view.postDelayed(minute, MINUTE_MS - System.currentTimeMillis() % MINUTE_MS);
    }

    /** The nearest target from the focused one in the D-pad's direction [dx, dy], or -1. */
    private int nearest(float dx, float dy) {
        Target from = targets.get(focused);
        float fx = from.box.centerX(), fy = from.box.centerY();
        int best = -1;
        float bestScore = Float.MAX_VALUE;
        for (int i = 0; i < targets.size(); i++) {
            if (i == focused) continue;
            Target t = targets.get(i);
            float vx = t.box.centerX() - fx, vy = t.box.centerY() - fy;
            float along = vx * dx + vy * dy, across = Math.abs(vx * dy - vy * dx);
            if (along <= 0 || across > along * 2f) continue;
            float score = along + across * 2f;
            if (score < bestScore) {
                bestScore = score;
                best = i;
            }
        }
        return best;
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

    /** [a] towards [b] by [t] (0..1), opaque: the scroll's wooden rollers between the ink and the parchment. */
    private static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    private static int alpha(int argb, float a) {
        return (Math.round((argb >>> 24) * a) << 24) | (argb & 0xFFFFFF);
    }

    /** The places, roads, buttons, cartouche and ribbon; it holds the focus and takes the D-pad. */
    private final class Places extends View {
        private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint road = stroke(0, 2.2f);
        private final Path path = new Path();
        private final RectF r = new RectF();

        Places(Context c) {
            super(c);
            setFocusable(true);
            setFocusableInTouchMode(true);
            road.setPathEffect(new DashPathEffect(new float[]{1.5f, 8f}, 0));
        }

        /** The two buttons' boxes, top right, right to left. */
        void layoutButtons() {
            text.setTypeface(cinzel);
            text.setTextSize(14);
            text.setLetterSpacing(0.1f);
            float right = MAP_W - 48;
            for (int i = targets.size() - 1; i >= 0; i--) {
                Target t = targets.get(i);
                if (t.slot != -1) continue;
                float w = text.measureText(t.label) + 32;
                t.box.set(right - w, 44, right, 80);
                right -= w + 14;
            }
            for (Target t : targets) if (t.slot != -1) t.box.set(t.x - 20, t.y - 20, t.x + 20, t.y + 20);
        }

        @Override protected void onDraw(Canvas canvas) {
            if (targets.isEmpty()) return;
            canvas.save();
            canvas.scale(getWidth() / MAP_W, getHeight() / MAP_H);
            roads(canvas);
            for (int i = 0; i < targets.size(); i++) if (targets.get(i).slot != -1 && i != focused) castle(canvas, targets.get(i), false);
            if (targets.get(focused).slot != -1) castle(canvas, targets.get(focused), true);
            for (int i = 0; i < targets.size(); i++) if (targets.get(i).slot == -1) button(canvas, targets.get(i), i == focused);
            cartouche(canvas);
            ribbon(canvas, scroll != null ? scroll.get(scrollIndex).label : targets.get(focused).label);
            if (scroll != null) scroll(canvas);
            canvas.restore();
        }

        private float[] at(int slot) {
            return slot == SETTINGS ? SETTINGS_AT : SLOTS[slot];
        }

        private boolean has(int slot) {
            for (Target t : targets) if (t.slot == slot) return true;
            return false;
        }

        private void link(Canvas canvas, int a, int b) {
            if (!has(a) || !has(b)) return;
            float[] p1 = at(a), p2 = at(b);
            path.reset();
            path.moveTo(p1[0], p1[1]);
            path.quadTo((p1[0] + p2[0]) / 2 + 30, (p1[1] + p2[1]) / 2 - 30, p2[0], p2[1]);
            canvas.drawPath(path, road);
        }

        private void roads(Canvas canvas) {
            road.setColor(alpha(p.ink, 0.75f));
            for (int[] r : ROADS) link(canvas, r[0], r[1]);
            if (has(6)) {
                link(canvas, 5, 6);
                link(canvas, 6, 0);
            } else {
                link(canvas, 5, 0);
            }
        }

        private void castle(Canvas canvas, Target t, boolean on) {
            float size = on ? 40 : 26, k = size / 30f;
            float left = t.x - size / 2, top = t.y - size + 4;
            if (on) {
                Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
                glow.setShader(new RadialGradient(t.x, top + size / 2, 46, alpha(p.accent, 0.55f), p.accent & 0xFFFFFF, Shader.TileMode.CLAMP));
                canvas.drawCircle(t.x, top + size / 2, 46, glow);
            }
            canvas.save();
            canvas.translate(left, top);
            canvas.scale(k, k);
            path.reset();
            path.moveTo(6, 28);
            path.lineTo(6, 12);
            path.lineTo(4, 12);
            path.lineTo(4, 7);
            path.lineTo(8, 7);
            path.lineTo(8, 10);
            path.lineTo(11, 10);
            path.lineTo(11, 7);
            path.lineTo(15, 7);
            path.lineTo(15, 10);
            path.lineTo(19, 10);
            path.lineTo(19, 7);
            path.lineTo(23, 7);
            path.lineTo(23, 10);
            path.lineTo(26, 10);
            path.lineTo(26, 12);
            path.lineTo(24, 12);
            path.lineTo(24, 28);
            path.close();
            canvas.drawPath(path, fill(on ? p.accent : p.land2));
            canvas.drawPath(path, stroke(p.ink, 1.6f));
            path.reset();
            path.moveTo(12.5f, 28);
            path.lineTo(12.5f, 21);
            path.arcTo(12.5f, 18.5f, 17.5f, 23.5f, 180, 180, false);
            path.lineTo(17.5f, 28);
            path.close();
            canvas.drawPath(path, fill(alpha(p.ink, 0.75f)));
            path.reset();
            path.moveTo(15, 7);
            path.lineTo(15, 1);
            path.lineTo(21, 3);
            path.lineTo(15, 5);
            canvas.drawPath(path, fill(on ? p.onAccent : p.accent));
            canvas.drawPath(path, stroke(p.ink, 1f));
            canvas.restore();
            // The name, on a patch of the labels' ground.
            text.setTypeface(on ? cinzelBold : cinzel);
            text.setTextSize(on ? 22 : 15);
            text.setLetterSpacing(0.08f);
            String name = TextUtils.ellipsize(t.label, new android.text.TextPaint(text), 200, TextUtils.TruncateAt.END).toString();
            float w = text.measureText(name), y = top + size + 4;
            Paint.FontMetrics fm = text.getFontMetrics();
            r.set(t.x - w / 2 - 10, y, t.x + w / 2 + 10, y + (fm.descent - fm.ascent) + 4);
            canvas.drawRect(r, fill(p.card));
            if (on) canvas.drawRect(r, stroke(p.accent, 1.5f));
            text.setColor(on ? p.accent : p.ink);
            text.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(name, t.x, y + 2 - fm.ascent, text);
            text.setTextAlign(Paint.Align.LEFT);
        }

        private void button(Canvas canvas, Target t, boolean on) {
            canvas.drawRect(t.box, fill(on ? p.accent : p.card));
            canvas.drawRect(t.box, stroke(p.ink, 1.5f));
            text.setTypeface(cinzel);
            text.setTextSize(14);
            text.setLetterSpacing(0.1f);
            text.setColor(on ? p.onAccent : p.ink);
            text.setTextAlign(Paint.Align.CENTER);
            Paint.FontMetrics fm = text.getFontMetrics();
            canvas.drawText(t.label, t.box.centerX(), t.box.centerY() - (fm.ascent + fm.descent) / 2, text);
            text.setTextAlign(Paint.Align.LEFT);
        }

        private void cartouche(Canvas canvas) {
            Date now = new Date();
            boolean h24 = "24".equals(clockFormat) || (!"12".equals(clockFormat) && DateFormat.is24HourFormat(ctx));
            String clock = DateFormat.format(h24 ? "H:mm" : "h:mm", now).toString();
            String date = DateFormat.format(DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM"), now).toString();
            if (!date.isEmpty()) date = date.substring(0, 1).toUpperCase(locale) + date.substring(1);
            String hello = greeting.isEmpty() ? "" : greeting.substring(0, 1).toUpperCase(locale) + greeting.substring(1).toLowerCase(locale);
            text.setTextAlign(Paint.Align.CENTER);
            text.setTypeface(cinzelBold);
            text.setTextSize(52);
            text.setLetterSpacing(0.04f);
            float wClock = text.measureText(clock);
            text.setTypeface(cinzel);
            text.setTextSize(13);
            text.setLetterSpacing(0.15f);
            float wDate = text.measureText(date);
            text.setTypeface(italic);
            text.setTextSize(16);
            text.setLetterSpacing(0.06f);
            float wHello = text.measureText(hello);
            float w = Math.max(wClock, Math.max(wDate, wHello)) + 60, cx = 44 + w / 2;
            r.set(44, 38, 44 + w, 38 + 116);
            canvas.drawRect(r, fill(p.card));
            canvas.drawRect(r, stroke(p.ink, 2f));
            r.inset(-6, -6);
            canvas.drawRect(r, stroke(p.ink, 1f));
            text.setColor(p.ink);
            canvas.drawText(hello, cx, 70, text);
            text.setTypeface(cinzelBold);
            text.setTextSize(52);
            text.setLetterSpacing(0.04f);
            text.setColor(p.accent);
            canvas.drawText(clock, cx, 124, text);
            text.setTypeface(cinzel);
            text.setTextSize(13);
            text.setLetterSpacing(0.15f);
            text.setColor(p.ink);
            canvas.drawText(date, cx, 144, text);
            text.setTextAlign(Paint.Align.LEFT);
        }

        private void ribbon(Canvas canvas, String name) {
            float l = 390, rt = MAP_W - 390, t = MAP_H - 30 - 56, b = MAP_H - 30, notch = (rt - l) * 0.04f;
            path.reset();
            path.moveTo(l, t);
            path.lineTo(rt, t);
            path.lineTo(rt - notch, (t + b) / 2);
            path.lineTo(rt, b);
            path.lineTo(l, b);
            path.lineTo(l + notch, (t + b) / 2);
            path.close();
            canvas.drawPath(path, fill(p.accent));
            String travel = ctx.getString(R.string.map_travel);
            text.setTypeface(cinzelBold);
            text.setTextSize(24);
            text.setLetterSpacing(0.12f);
            String shown = TextUtils.ellipsize(name, new android.text.TextPaint(text), 240, TextUtils.TruncateAt.END).toString();
            float wName = text.measureText(shown);
            text.setTypeface(italic);
            text.setTextSize(15);
            text.setLetterSpacing(0.06f);
            float wTravel = text.measureText(travel);
            float x = (l + rt) / 2 - (wName + 14 + wTravel) / 2, base = (t + b) / 2 + 8;
            text.setColor(p.onAccent);
            text.setTypeface(cinzelBold);
            text.setTextSize(24);
            text.setLetterSpacing(0.12f);
            canvas.drawText(shown, x, base, text);
            text.setTypeface(italic);
            text.setTextSize(15);
            text.setLetterSpacing(0.06f);
            canvas.drawText(travel, x + wName + 14, base - 1, text);
        }

        private boolean longPressed;

        private boolean onSettings(int keyCode) {
            return scroll == null && !targets.isEmpty() && targets.get(focused).slot == SETTINGS && (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                    || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER);
        }

        @Override public boolean onKeyLongPress(int keyCode, KeyEvent event) {
            if (onSettings(keyCode)) {
                longPressed = true;
                actions.launcherSettings();
                return true;
            }
            return super.onKeyLongPress(keyCode, event);
        }

        @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
            if (onSettings(keyCode)) {
                if (!longPressed && event.isTracking() && !event.isCanceled()) targets.get(focused).run.run();
                longPressed = false;
                return true;
            }
            return super.onKeyUp(keyCode, event);
        }

        // The unrolled scroll: a parchment between two wooden rollers, its title over a flourish, the entries in ink
        // with the chosen one in the seal's red; a list of inputs or a grid of every app with their banners.
        private final float SL = 250, ST = 92, SR = 1030, SB = 628;
        private final int APP_COLS = 4, APP_ROWS = 3, INPUT_ROWS = 7;
        private final float ROW_H = 54, CELL_H = 126;

        private void roller(Canvas canvas, float y) {
            r.set(SL - 22, y - 16, SR + 22, y + 16);
            canvas.drawRoundRect(r, 16, 16, fill(mix(p.ink, p.land, 0.45f)));
            canvas.drawRoundRect(r, 16, 16, stroke(p.ink, 2f));
            Paint grain = stroke(alpha(p.ink, 0.35f), 1f);
            canvas.drawLine(SL - 8, y - 5, SR + 8, y - 5, grain);
            canvas.drawLine(SL - 8, y + 6, SR + 8, y + 6, grain);
            canvas.drawCircle(SL - 30, y, 10, fill(p.ink));
            canvas.drawCircle(SR + 30, y, 10, fill(p.ink));
        }

        private void scroll(Canvas canvas) {
            canvas.drawColor(alpha(0xFF000000, 0.45f));
            r.set(SL, ST, SR, SB);
            canvas.drawRect(r, fill(p.land));
            Paint edge = stroke(alpha(p.ink, 0.8f), 2f);
            canvas.drawLine(SL, ST, SL, SB, edge);
            canvas.drawLine(SR, ST, SR, SB, edge);
            canvas.drawRect(SL + 14, ST + 18, SR - 14, SB - 18, stroke(alpha(p.ink, 0.35f), 1f));
            roller(canvas, ST);
            roller(canvas, SB);
            // Title and flourish.
            String title = ctx.getString(scrollApps ? R.string.map_all_apps : R.string.look_inputs);
            text.setTypeface(cinzelBold);
            text.setTextSize(30);
            text.setLetterSpacing(0.12f);
            text.setColor(p.ink);
            text.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(title, (SL + SR) / 2, ST + 66, text);
            float cx = (SL + SR) / 2, fy = ST + 84;
            Paint line = stroke(alpha(p.ink, 0.7f), 1.4f);
            canvas.drawLine(cx - 160, fy, cx - 12, fy, line);
            canvas.drawLine(cx + 12, fy, cx + 160, fy, line);
            path.reset();
            path.moveTo(cx, fy - 6);
            path.lineTo(cx + 7, fy);
            path.lineTo(cx, fy + 6);
            path.lineTo(cx - 7, fy);
            path.close();
            canvas.drawPath(path, fill(p.accent));
            if (scrollApps) appGrid(canvas);
            else inputList(canvas);
            text.setTypeface(italic);
            text.setTextSize(14);
            text.setLetterSpacing(0.05f);
            text.setColor(alpha(p.ink, 0.75f));
            canvas.drawText(ctx.getString(R.string.map_scroll_hint), cx, SB - 30, text);
            text.setTextAlign(Paint.Align.LEFT);
        }

        private void inputList(Canvas canvas) {
            float left = SL + 90, right = SR - 90, top = ST + 108;
            for (int i = scrollTop; i < Math.min(scroll.size(), scrollTop + INPUT_ROWS); i++) {
                Entry e = scroll.get(i);
                boolean on = i == scrollIndex;
                float y = top + (i - scrollTop) * ROW_H;
                r.set(left, y, right, y + ROW_H - 8);
                if (on) {
                    canvas.drawRect(r, fill(p.accent));
                    canvas.drawCircle(right - 24, y + (ROW_H - 8) / 2, 10, fill(p.onAccent));
                    canvas.drawCircle(right - 24, y + (ROW_H - 8) / 2, 6, fill(p.accent));
                } else {
                    canvas.drawLine(left + 20, y + ROW_H - 8, right - 20, y + ROW_H - 8, stroke(alpha(p.ink, 0.25f), 1f));
                }
                int ink = on ? p.onAccent : p.ink;
                android.graphics.drawable.Drawable d = ctx.getDrawable(e.icon);
                if (d != null) {
                    d = d.mutate();
                    d.setTint(ink);
                    d.setBounds(Math.round(left + 20), Math.round(y + 10), Math.round(left + 46), Math.round(y + 36));
                    d.draw(canvas);
                }
                text.setTypeface(on ? cinzelBold : cinzel);
                text.setTextSize(20);
                text.setLetterSpacing(0.08f);
                text.setColor(ink);
                text.setTextAlign(Paint.Align.LEFT);
                canvas.drawText(e.label, left + 64, y + 31, text);
            }
            text.setTextAlign(Paint.Align.CENTER);
        }

        private final android.graphics.Rect saved = new android.graphics.Rect();

        private void appGrid(Canvas canvas) {
            float left = SL + 40, top = ST + 104, cw = (SR - SL - 80) / APP_COLS;
            for (int i = scrollTop * APP_COLS; i < Math.min(scroll.size(), (scrollTop + APP_ROWS) * APP_COLS); i++) {
                Entry e = scroll.get(i);
                boolean on = i == scrollIndex;
                int row = i / APP_COLS - scrollTop, col = i % APP_COLS;
                float x = left + col * cw, y = top + row * CELL_H;
                float bw = cw - 34, bh = bw * 9 / 16, bx = x + 17, by = y + 6;
                r.set(bx - 4, by - 4, bx + bw + 4, by + bh + 4);
                canvas.drawRect(r, fill(alpha(p.ink, 0.12f)));
                if (on) canvas.drawRect(r, stroke(p.accent, 4f));
                android.graphics.drawable.Drawable art = adapter.artworkAt(e.position);
                if (art != null) {
                    int iw = art.getIntrinsicWidth(), ih = art.getIntrinsicHeight();
                    float sc = iw > 0 && ih > 0 ? Math.min(bw / iw, bh / ih) : 0;
                    if (sc > 0) {
                        float dw = iw * sc, dh = ih * sc;
                        art.copyBounds(saved);
                        art.setBounds(Math.round(bx + (bw - dw) / 2), Math.round(by + (bh - dh) / 2),
                                Math.round(bx + (bw + dw) / 2), Math.round(by + (bh + dh) / 2));
                        art.draw(canvas);
                        art.setBounds(saved);
                    }
                }
                text.setTypeface(on ? cinzelBold : cinzel);
                text.setTextSize(13);
                text.setLetterSpacing(0.06f);
                text.setColor(on ? p.accent : p.ink);
                text.setTextAlign(Paint.Align.CENTER);
                String name = TextUtils.ellipsize(e.label, new android.text.TextPaint(text), bw, TextUtils.TruncateAt.END).toString();
                canvas.drawText(name, x + cw / 2, by + bh + 24, text);
            }
            // More rows above or below: small arrows on the parchment.
            text.setTypeface(cinzelBold);
            text.setTextSize(16);
            text.setColor(alpha(p.ink, 0.7f));
            if (scrollTop > 0) canvas.drawText("▲", SR - 40, top + 12, text);
            if ((scrollTop + APP_ROWS) * APP_COLS < scroll.size()) canvas.drawText("▼", SR - 40, top + APP_ROWS * CELL_H - 10, text);
        }

        private boolean scrollKey(int keyCode, KeyEvent event) {
            int n = scroll.size(), i = scrollIndex;
            int cols = scrollApps ? APP_COLS : 1, rows = scrollApps ? APP_ROWS : INPUT_ROWS;
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                case KeyEvent.KEYCODE_ESCAPE:
                    closeScroll();
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT: if (scrollApps && i % cols > 0) i--; break;
                case KeyEvent.KEYCODE_DPAD_RIGHT: if (scrollApps && i % cols < cols - 1 && i + 1 < n) i++; break;
                case KeyEvent.KEYCODE_DPAD_UP: if (i - cols >= 0) i -= cols; break;
                case KeyEvent.KEYCODE_DPAD_DOWN: if (i + cols < n) i += cols; else if (scrollApps && i / cols < (n - 1) / cols) i = n - 1; break;
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_NUMPAD_ENTER:
                    if (event.getRepeatCount() == 0) {
                        Runnable run = scroll.get(i).run;
                        closeScroll();
                        run.run();
                    }
                    return true;
                default:
                    return true;   // nothing under the scroll moves while it is open
            }
            scrollIndex = i;
            int row = i / cols;
            if (row < scrollTop) scrollTop = row;
            if (row >= scrollTop + rows) scrollTop = row - rows + 1;
            invalidate();
            return true;
        }

        @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
            if (scroll != null) return scrollKey(keyCode, event);
            if (targets.isEmpty()) return super.onKeyDown(keyCode, event);
            int next = -1;
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_LEFT: next = nearest(-1, 0); break;
                case KeyEvent.KEYCODE_DPAD_RIGHT: next = nearest(1, 0); break;
                case KeyEvent.KEYCODE_DPAD_UP: next = nearest(0, -1); break;
                case KeyEvent.KEYCODE_DPAD_DOWN: next = nearest(0, 1); break;
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_NUMPAD_ENTER:
                    // Settings, as Nothing's gear: on release the TV's, held the launcher's (onKeyLongPress).
                    if (targets.get(focused).slot == SETTINGS) {
                        if (event.getRepeatCount() == 0) event.startTracking();
                        return true;
                    }
                    if (event.getRepeatCount() == 0) targets.get(focused).run.run();
                    return true;
                default:
                    return super.onKeyDown(keyCode, event);
            }
            if (next >= 0) {
                moved = true;
                focused = next;
                invalidate();
            }
            return true;
        }
    }

    /** The map itself, drawn once: sea, coasts, mountains, forests, rivers, names, the frame, the compass. */
    private final class Backdrop extends View {
        Backdrop(Context c) {
            super(c);
            setFocusable(false);
        }

        @Override protected void onDraw(Canvas canvas) {
            canvas.save();
            canvas.scale(getWidth() / MAP_W, getHeight() / MAP_H);
            MapDrawing.draw(canvas, p, italic, cinzelBold, ctx);
            canvas.restore();
        }
    }
}
