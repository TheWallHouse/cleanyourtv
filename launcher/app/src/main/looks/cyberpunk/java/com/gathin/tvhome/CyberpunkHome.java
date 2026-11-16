package com.gathin.tvhome;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Cyberpunk (Aspect.CYBERPUNK): Home as a night-city game menu, over the same apps as the tiles. On the left a status
 * panel (a big glitching clock, the date, the TV, the network, the apps, the time on); on the right the apps as slanted
 * bars, in three tabs (the dock's, all of them, and the system's: inputs and settings); a neon help bar along the
 * bottom. Behind, drawn once: a slanted band, two slanted rules, the launcher's name in outline and scanlines.
 * The tile grid stays underneath, loaded and hidden, so the order, the dock, hidden apps and the reloads are Home's
 * own; this only lists and opens them.
 */
final class CyberpunkHome {
    private static final long MINUTE_MS = 60_000L;
    private static final long BLINK_MS = 700L;
    private static final int TAB_HOME = 0, TAB_ALL = 1, TAB_SYSTEM = 2;
    /** tan 14°: every slant on screen leans the same way. */
    private static final float SLANT = 0.249f;

    private final Context ctx;
    private final CyberpunkTone p;
    private final AppAdapter adapter;
    private final ConsoleHome.Actions actions;
    private final float density;
    private final Typeface font, bold;
    private final String host;
    private final boolean motion;
    private final FrameLayout view;
    private final Locale locale;
    private TextView online;
    private LinearLayout info;
    private final TextView[] tabs = new TextView[3];
    private final View[] tabLines = new View[3];
    private LinearLayout list;
    private ScrollView scroll;
    private int tab = TAB_HOME;
    private String networkText = "";
    private boolean cursorOn = true;

    private CyberpunkHome(Context ctx, CyberpunkTone p, AppAdapter adapter, ConsoleHome.Actions actions, boolean motion) {
        this.ctx = ctx;
        this.p = p;
        this.adapter = adapter;
        this.actions = actions;
        this.motion = motion;
        density = ctx.getResources().getDisplayMetrics().density;
        font = ctx.getResources().getFont(R.font.chakra_petch);
        bold = ctx.getResources().getFont(R.font.chakra_petch_bold);
        host = TerminalSkin.hostName(ctx);
        locale = ctx.getResources().getConfiguration().getLocales().get(0);
        view = new FrameLayout(ctx);
    }

    /**
     * Lays the menu over Home's [root], as the last child, and keeps its list in step with [adapter]. [wifi] is the
     * network pill, whose text the status panel repeats. [motion] lets the cursor blink and the focused bar slide.
     */
    static CyberpunkHome attach(Context ctx, CyberpunkTone p, FrameLayout root, AppAdapter adapter, TextView wifi,
                                String clockFormat, boolean motion, ConsoleHome.Actions actions) {
        CyberpunkHome c = new CyberpunkHome(ctx, p, adapter, actions, motion);
        c.build(clockFormat);
        root.addView(c.view, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        Overlay.isolate(root, c.view);
        if (wifi != null) {
            c.networkText = String.valueOf(wifi.getText());
            wifi.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int d) {}
                @Override public void onTextChanged(CharSequence s, int a, int b, int d) {}
                @Override public void afterTextChanged(Editable s) {
                    c.networkText = s.toString();
                    c.renderInfo();
                }
            });
        }
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override public void onChanged() { c.fill(false); }
            @Override public void onItemRangeInserted(int start, int count) { c.fill(false); }
            @Override public void onItemRangeRemoved(int start, int count) { c.fill(false); }
            @Override public void onItemRangeMoved(int from, int to, int count) { c.fill(false); }
        });
        c.fill(false);
        c.tick();
        return c;
    }

    /** Puts focus on the list's first bar (Home came back, or the list was rebuilt). */
    void focus() {
        if (list != null && list.getChildCount() > 0) list.getChildAt(0).requestFocus();
    }

    boolean hasFocus() {
        return view.hasFocus();
    }

    private int dp(float v) {
        return Math.round(v * density);
    }

    private String upper(String s) {
        return s == null ? "" : s.toUpperCase(locale);
    }

    private TextView text(float sp, int color, boolean heavy, float spacing) {
        TextView t = new TextView(ctx);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setTypeface(heavy ? bold : font);
        t.setIncludeFontPadding(false);
        t.setLetterSpacing(spacing);
        t.setFocusable(false);
        return t;
    }

    private TextClock clock(float sp, int color, String clockFormat) {
        TextClock c = new TextClock(ctx);
        c.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        c.setTextColor(color);
        c.setTypeface(bold);
        c.setIncludeFontPadding(false);
        c.setFormat24Hour("12".equals(clockFormat) ? "h:mm" : "HH:mm");
        c.setFormat12Hour("24".equals(clockFormat) ? "HH:mm" : "h:mm");
        return c;
    }

    private void build(String clockFormat) {
        view.setBackgroundColor(CyberpunkTone.BG);
        view.setClickable(true);   // touches never fall through to the hidden tiles
        view.addView(new Backdrop(ctx), new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout column = new LinearLayout(ctx);
        column.setOrientation(LinearLayout.VERTICAL);
        view.addView(column, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout panes = new LinearLayout(ctx);
        panes.setPadding(dp(42), dp(34), dp(42), dp(14));
        column.addView(panes, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // Left: the status panel.
        LinearLayout left = new LinearLayout(ctx);
        left.setOrientation(LinearLayout.VERTICAL);
        panes.addView(left, new LinearLayout.LayoutParams(dp(250), ViewGroup.LayoutParams.MATCH_PARENT));
        online = text(10, p.accent2, true, 0.3f);
        left.addView(online);
        // The clock three times over: the two behind, a step either side, are the glitch.
        FrameLayout clocks = new FrameLayout(ctx);
        clocks.setPadding(dp(3), dp(8), dp(3), dp(4));
        TextClock behindLeft = clock(80, p.accent2, clockFormat);
        behindLeft.setTranslationX(-dp(3));
        TextClock behindRight = clock(80, p.third, clockFormat);
        behindRight.setTranslationX(dp(3));
        clocks.addView(behindLeft);
        clocks.addView(behindRight);
        clocks.addView(clock(80, p.accent, clockFormat));
        left.addView(clocks);
        TextClock date = new TextClock(ctx);
        date.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        date.setTextColor(p.fg);
        date.setTypeface(bold);
        date.setLetterSpacing(0.25f);
        date.setAllCaps(true);
        date.setFormat24Hour("EEE · dd · MMM");
        date.setFormat12Hour("EEE · dd · MMM");
        left.addView(date);
        info = new LinearLayout(ctx);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(0, dp(22), 0, 0);
        left.addView(info, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Right: the tabs over the bars.
        LinearLayout right = new LinearLayout(ctx);
        right.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        rl.leftMargin = dp(90);
        panes.addView(right, rl);
        LinearLayout tabRow = new LinearLayout(ctx);
        tabRow.setGravity(Gravity.END);
        int[] names = {R.string.look_home, R.string.look_all, R.string.look_system};
        for (int i = 0; i < 3; i++) {
            LinearLayout cell = new LinearLayout(ctx);
            cell.setOrientation(LinearLayout.VERTICAL);
            TextView t = text(12, p.muted, true, 0.2f);
            String label = upper(ctx.getString(names[i]));
            t.setText(label);
            cell.addView(t);
            // The underline is as wide as the label: a MATCH_PARENT line would stretch the cell across the row.
            View line = new View(ctx);
            LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams((int) Math.ceil(t.getPaint().measureText(label)), dp(2.5f));
            ll.topMargin = dp(6);
            cell.addView(line, ll);
            LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cl.leftMargin = dp(20);
            tabs[i] = t;
            tabLines[i] = line;
            tabRow.addView(cell, cl);
        }
        right.addView(tabRow);
        scroll = new ScrollView(ctx);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        scroll.setPadding(0, dp(14), 0, dp(10));
        list = new LinearLayout(ctx);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams scl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        scl.topMargin = dp(10);
        right.addView(scroll, scl);

        // Bottom: the help bar.
        LinearLayout help = new LinearLayout(ctx);
        help.setGravity(Gravity.CENTER_VERTICAL);
        help.setBackgroundColor(p.accent);
        help.setPadding(dp(42), 0, dp(42), 0);
        TextView keys = text(11, CyberpunkTone.INK, true, 0.2f);
        keys.setText(upper(ctx.getString(R.string.look_hint)));
        help.addView(keys);
        help.addView(new View(ctx), new LinearLayout.LayoutParams(0, 1, 1f));
        TextView sign = text(11, CyberpunkTone.INK, true, 0.2f);
        sign.setText("WALLHOUSE // " + upper(ctx.getString(R.string.aspect_cyberpunk)));
        help.addView(sign);
        column.addView(help, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(33)));
    }

    private static final class Row {
        final String label;
        final Runnable run;
        /** A long press's action, or null (Settings: the launcher's). */
        Runnable longRun;

        Row(String label, Runnable run) {
            this.label = label;
            this.run = run;
        }
    }

    private List<Row> rows() {
        List<Row> out = new ArrayList<>();
        int n = adapter.appCount();
        if (tab == TAB_SYSTEM) {
            out.add(new Row(ctx.getString(R.string.look_inputs), actions::inputs));
            Row settings = new Row(ctx.getString(R.string.look_settings), () -> Overlay.openTvSettings(ctx));
            settings.longRun = actions::launcherSettings;
            out.add(settings);
            return out;
        }
        int end = tab == TAB_HOME ? Math.min(n, Math.max(adapter.dockFilled(), 0)) : n;
        if (tab == TAB_HOME && end == 0) end = n;   // no dock: Home is every app
        for (int i = 0; i < end; i++) {
            if (adapter.isEmptyAt(i)) continue;
            final int pos = i;
            out.add(new Row(adapter.labelAt(i), () -> adapter.launchAt(pos)));
        }
        return out;
    }

    /** Rebuilds the bars for the current tab; [focusFirst] puts focus on the first (a tab change). */
    private void fill(boolean focusFirst) {
        boolean had = list.hasFocus();
        int focusedIndex = -1;
        if (had) for (int i = 0; i < list.getChildCount(); i++) if (list.getChildAt(i).hasFocus()) focusedIndex = i;
        list.removeAllViews();
        List<Row> rows = rows();
        for (int i = 0; i < rows.size(); i++) list.addView(bar(rows.get(i), i + 1));
        for (int i = 0; i < 3; i++) {
            boolean on = i == tab;
            tabs[i].setTextColor(on ? p.accent : p.muted);
            tabLines[i].setBackgroundColor(on ? p.accent : 0);
        }
        renderInfo();
        if (list.getChildCount() == 0) return;
        if (focusFirst || !view.hasFocus() || (had && focusedIndex < 0)) {
            list.getChildAt(0).requestFocus();
            scroll.scrollTo(0, 0);
        } else if (had) {
            list.getChildAt(Math.min(focusedIndex, list.getChildCount() - 1)).requestFocus();
        }
    }

    private View bar(Row r, int number) {
        int shadow = dp(6);
        int height = dp(50);
        int lean = Math.round((height - shadow) * SLANT);
        LinearLayout b = new LinearLayout(ctx);
        b.setGravity(Gravity.CENTER_VERTICAL);
        b.setFocusable(true);
        b.setClickable(true);
        b.setPadding(lean + dp(18), 0, lean + shadow + dp(14), shadow);
        Bar shape = new Bar(shadow, lean);
        b.setBackground(shape);
        TextView num = text(11, p.muted, true, 0.15f);
        num.setText(String.format(Locale.ROOT, "%02d", number));
        b.addView(num);
        TextView label = text(19, p.fg, true, 0.1f);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setText(upper(r.label));
        LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        ll.leftMargin = dp(16);
        b.addView(label, ll);
        TextView arrow = text(16, CyberpunkTone.INK, true, 0f);
        arrow.setText("▶");
        arrow.setVisibility(View.INVISIBLE);
        b.addView(arrow);
        b.setOnFocusChangeListener((v, focused) -> {
            shape.focused = focused;
            shape.invalidateSelf();
            num.setTextColor(focused ? CyberpunkTone.INK : p.muted);
            label.setTextColor(focused ? CyberpunkTone.INK : p.fg);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, focused ? 23 : 19);
            arrow.setVisibility(focused ? View.VISIBLE : View.INVISIBLE);
            float x = focused ? -dp(18) : 0f;
            if (motion) b.animate().translationX(x).setDuration(120).start();
            else b.setTranslationX(x);
        });
        b.setOnClickListener(v -> r.run.run());
        if (r.longRun != null) b.setOnLongClickListener(v -> { r.longRun.run(); return true; });
        b.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                tab = (tab + (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ? 1 : 2)) % 3;
                fill(true);
                return true;
            }
            return false;
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
        lp.leftMargin = dp(20);
        lp.bottomMargin = dp(4);
        b.setLayoutParams(lp);
        return b;
    }

    private void renderInfo() {
        long minutes = SystemClock.elapsedRealtime() / MINUTE_MS;
        int apps = 0;
        for (int i = 0; i < adapter.appCount(); i++) if (!adapter.isEmptyAt(i)) apps++;
        info.removeAllViews();
        infoRow(ctx.getString(R.string.look_tv), host);
        infoRow(ctx.getString(R.string.look_network), networkText);
        infoRow(ctx.getString(R.string.look_apps), ctx.getString(R.string.look_apps_value, apps));
        infoRow(ctx.getString(R.string.look_uptime), ctx.getString(R.string.look_uptime_value, (int) (minutes / 60), (int) (minutes % 60)));
        online.setText(upper(ctx.getString(R.string.cyberpunk_online)) + (cursorOn ? " ▮" : "  "));
    }

    private void infoRow(String key, String value) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.VERTICAL);
        LinearLayout line = new LinearLayout(ctx);
        TextView k = text(11, p.muted, false, 0.15f);
        k.setText(upper(key));
        line.addView(k);
        TextView v = text(11, p.fg, false, 0.15f);
        v.setText(upper(value));
        v.setGravity(Gravity.END);
        v.setSingleLine(true);
        v.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams vl = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        vl.leftMargin = dp(12);
        line.addView(v, vl);
        row.addView(line);
        View rule = new View(ctx);
        rule.setBackgroundColor(CyberpunkTone.withAlpha(p.accent, 0x38));
        LinearLayout.LayoutParams rul = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(1)));
        rul.topMargin = dp(5);
        rul.bottomMargin = dp(8);
        row.addView(rule, rul);
        info.addView(row);
    }

    private void tick() {
        Runnable minute = new Runnable() {
            @Override public void run() {
                if (!view.isAttachedToWindow()) return;
                renderInfo();
                view.postDelayed(this, MINUTE_MS);
            }
        };
        view.postDelayed(minute, MINUTE_MS);
        if (!motion) return;
        Runnable cursor = new Runnable() {
            @Override public void run() {
                if (!view.isAttachedToWindow()) return;
                cursorOn = !cursorOn;
                online.setText(upper(ctx.getString(R.string.cyberpunk_online)) + (cursorOn ? " ▮" : "  "));
                view.postDelayed(this, BLINK_MS);
            }
        };
        view.postDelayed(cursor, BLINK_MS);
    }

    /** A slanted bar; focused, it is filled with the neon over a hard shadow in the second colour. */
    private final class Bar extends Drawable {
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final int shadow, lean;
        boolean focused;

        Bar(int shadow, int lean) {
            this.shadow = shadow;
            this.lean = lean;
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(dp(1.5f));
        }

        private void shape(float dx, float dy) {
            Rect r = getBounds();
            float l = r.left + dx, t = r.top + dy, rt = r.right - shadow + dx, bt = r.bottom - shadow + dy;
            path.reset();
            path.moveTo(l + lean, t);
            path.lineTo(rt, t);
            path.lineTo(rt - lean, bt);
            path.lineTo(l, bt);
            path.close();
        }

        @Override public void draw(Canvas canvas) {
            if (focused) {
                shape(shadow, shadow);
                fill.setColor(p.accent2);
                canvas.drawPath(path, fill);
                shape(0, 0);
                fill.setColor(p.accent);
                canvas.drawPath(path, fill);
            } else {
                shape(0, 0);
                fill.setColor(CyberpunkTone.BAR);
                canvas.drawPath(path, fill);
                stroke.setColor(CyberpunkTone.withAlpha(p.accent, 0x40));
                canvas.drawPath(path, stroke);
            }
        }

        @Override public void setAlpha(int alpha) {}
        @Override public void setColorFilter(ColorFilter filter) {}
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    /** The scenery behind, drawn once: a slanted band and two rules, the name in outline, scanlines. */
    private final class Backdrop extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        Backdrop(Context c) {
            super(c);
            setFocusable(false);
        }

        /** A slanted strip [x0..x1] wide where it crosses the middle of the screen. */
        private void strip(Canvas canvas, float x0, float x1, int color) {
            float h = getHeight(), lean = h / 2f * SLANT;
            path.reset();
            path.moveTo(x0 + lean, 0);
            path.lineTo(x1 + lean, 0);
            path.lineTo(x1 - lean, h);
            path.lineTo(x0 - lean, h);
            path.close();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color);
            canvas.drawPath(path, paint);
        }

        @Override protected void onDraw(Canvas canvas) {
            float w = getWidth(), h = getHeight();
            strip(canvas, -dp(90), dp(300), CyberpunkTone.withAlpha(p.accent, 0x14));
            strip(canvas, dp(320), dp(324.5f), p.accent);
            strip(canvas, dp(332), dp(333.5f), CyberpunkTone.withAlpha(p.accent2, 0xB3));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1.2f));
            paint.setColor(CyberpunkTone.withAlpha(p.accent, 0x1E));
            paint.setTypeface(bold);
            paint.setTextSize(dp(170));
            paint.setLetterSpacing(-0.03f);
            paint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText("WALLHOUSE", w + dp(22), dp(262), paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0x40000000);
            float step = Math.max(3, dp(3));
            for (float y = 0; y < h; y += step) canvas.drawRect(0, y, w, y + 1, paint);
        }
    }
}
