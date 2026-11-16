package com.gathin.tvhome;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.SystemClock;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
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
 * Terminal · Console (Aspect.CONSOLE): Home as a terminal session, over the same apps as the tiles. On the left a
 * fastfetch-style block (the TV, Android, the apps, the network, the palette, the time on) over a big clock; on the
 * right the apps as a list, in three tabs (the dock's, all of them, and the system's: inputs and settings); a tmux
 * status line along the bottom. The tile grid stays underneath, loaded and hidden, so the order, the dock, hidden
 * apps and the reloads are Home's own; this only lists and opens them.
 */
final class ConsoleHome {
    private static final long MINUTE_MS = 60_000L;
    private static final long BLINK_MS = 550L;
    private static final int TAB_HOME = 0, TAB_ALL = 1, TAB_SYSTEM = 2;
    private static final String ART =
            "  \\    /\n" +
            " ┌──────────┐\n" +
            " │ ▓▓▓▓▓▓▓▓ │\n" +
            " │ ▓▓▓▓▓▓▓▓ │\n" +
            " │ ▓▓▓▓▓▓▓▓ │\n" +
            " └────┬┬────┘\n" +
            "    ══╧╧══";

    /** What a system row does. */
    interface Actions {
        void inputs();
        void launcherSettings();
    }

    private final Context ctx;
    private final TerminalPalette p;
    private final AppAdapter adapter;
    private final Actions actions;
    private final float density;
    private final Typeface mono, monoBold;
    private final String host;
    private final boolean blink;
    private final FrameLayout view;
    private TextView info;
    private TextView openPrompt;
    private final TextView[] tabs = new TextView[3];
    private final TextView[] statusTabs = new TextView[3];
    private LinearLayout list;
    private ScrollView scroll;
    private int tab = TAB_HOME;
    private String networkText = "";
    private boolean cursorOn = true;

    private ConsoleHome(Context ctx, TerminalPalette p, AppAdapter adapter, Actions actions, boolean blink) {
        this.ctx = ctx;
        this.p = p;
        this.adapter = adapter;
        this.actions = actions;
        this.blink = blink;
        density = ctx.getResources().getDisplayMetrics().density;
        mono = ctx.getResources().getFont(R.font.jetbrains_mono);
        monoBold = ctx.getResources().getFont(R.font.jetbrains_mono_bold);
        host = TerminalSkin.hostName(ctx);
        view = new FrameLayout(ctx);
    }

    /**
     * Lays the console over Home's [root] (under nothing: it is the last child but the focus frame, which it hides), and
     * keeps its list in step with [adapter]. [wifi] is the network pill, whose text the fastfetch block repeats.
     */
    static ConsoleHome attach(Context ctx, TerminalPalette p, FrameLayout root, AppAdapter adapter, TextView wifi,
                              String clockFormat, boolean blink, Actions actions) {
        ConsoleHome c = new ConsoleHome(ctx, p, adapter, actions, blink);
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

    /** Puts focus on the list's first row (Home came back, or the list was rebuilt). */
    void focus() {
        if (list != null && list.getChildCount() > 0) list.getChildAt(0).requestFocus();
    }

    boolean hasFocus() {
        return view.hasFocus();
    }

    private int dp(float v) {
        return Math.round(v * density);
    }

    private TextView text(float sp, int color, boolean bold) {
        TextView t = new TextView(ctx);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setTypeface(bold ? monoBold : mono);
        t.setIncludeFontPadding(false);
        t.setFocusable(false);
        return t;
    }

    private CharSequence prompt(String command, boolean cursor) {
        SpannableStringBuilder b = new SpannableStringBuilder();
        int a = 0;
        b.append("tv@").append(host);
        b.setSpan(new ForegroundColorSpan(p.green), a, b.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        a = b.length();
        b.append(":~$");
        b.setSpan(new ForegroundColorSpan(p.muted), a, b.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        b.append(' ').append(command);
        if (cursor) {
            b.append(' ');
            a = b.length();
            b.append('█');
            if (!cursorOn) b.setSpan(new ForegroundColorSpan(0), a, b.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return b;
    }

    private void build(String clockFormat) {
        view.setBackgroundColor(p.bg);
        view.setClickable(true);   // touches never fall through to the hidden tiles
        LinearLayout column = new LinearLayout(ctx);
        column.setOrientation(LinearLayout.VERTICAL);
        view.addView(column, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout panes = new LinearLayout(ctx);
        panes.setPadding(dp(44), dp(34), dp(44), dp(18));
        column.addView(panes, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // Left: fastfetch over the clock.
        LinearLayout left = new LinearLayout(ctx);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setPadding(0, 0, dp(36), 0);
        panes.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.44f));
        TextView ff = text(15, p.fg, false);
        ff.setText(prompt("fastfetch", false));
        left.addView(ff);
        LinearLayout fetch = new LinearLayout(ctx);
        fetch.setPadding(0, dp(22), 0, 0);
        TextView art = text(15, p.accent, true);
        art.setText(ART);
        art.setLineSpacing(0, 1.05f);
        fetch.addView(art);
        info = text(15, p.fg, false);
        info.setLineSpacing(dp(5), 1f);
        info.setPadding(dp(26), 0, 0, 0);
        fetch.addView(info);
        left.addView(fetch);
        LinearLayout swatches = new LinearLayout(ctx);
        swatches.setPadding(0, dp(22), 0, 0);
        for (int colour : new int[]{p.accent, p.accent2, p.green, p.muted, p.border}) {
            View s = new View(ctx);
            s.setBackgroundColor(colour);
            LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(dp(32), dp(16));
            sl.rightMargin = dp(6);
            swatches.addView(s, sl);
        }
        left.addView(swatches);
        left.addView(new View(ctx), new LinearLayout.LayoutParams(1, 0, 1f));
        TextClock clock = new TextClock(ctx);
        clock.setTextSize(TypedValue.COMPLEX_UNIT_SP, 104);
        clock.setTextColor(p.fg);
        clock.setTypeface(monoBold);
        clock.setIncludeFontPadding(false);
        clock.setLetterSpacing(-0.04f);
        clock.setFormat24Hour("12".equals(clockFormat) ? "h:mm" : "HH:mm");
        clock.setFormat12Hour("24".equals(clockFormat) ? "HH:mm" : "h:mm");
        left.addView(clock);
        TextClock date = new TextClock(ctx);
        date.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        date.setTextColor(p.muted);
        date.setTypeface(mono);
        date.setFormat24Hour("EEEE, d MMMM");
        date.setFormat12Hour("EEEE, d MMMM");
        date.setPadding(0, dp(6), 0, 0);
        left.addView(date);

        View rule = new View(ctx);
        rule.setBackgroundColor(p.border);
        panes.addView(rule, new LinearLayout.LayoutParams(dp(2), ViewGroup.LayoutParams.MATCH_PARENT));

        // Right: the apps.
        LinearLayout right = new LinearLayout(ctx);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setPadding(dp(36), 0, 0, 0);
        panes.addView(right, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.56f));
        openPrompt = text(15, p.fg, false);
        right.addView(openPrompt);
        LinearLayout tabRow = new LinearLayout(ctx);
        tabRow.setPadding(0, dp(18), 0, dp(14));
        int[] names = {R.string.console_home, R.string.console_all, R.string.console_system};
        for (int i = 0; i < 3; i++) {
            TextView t = text(15, p.muted, false);
            t.setText((i + 1) + " " + ctx.getString(names[i]));
            t.setPadding(dp(10), dp(3), dp(10), dp(3));
            LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tl.rightMargin = dp(12);
            tabs[i] = t;
            tabRow.addView(t, tl);
        }
        right.addView(tabRow);
        scroll = new ScrollView(ctx);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setFillViewport(false);
        GradientDrawable box = new GradientDrawable();
        box.setColor(0);
        box.setStroke(dp(2), p.border);
        scroll.setBackground(box);
        scroll.setPadding(dp(2), dp(2), dp(2), dp(2));
        list = new LinearLayout(ctx);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams scl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        right.addView(scroll, scl);
        TextView hint = text(13, p.muted, false);
        hint.setText(R.string.console_hint);
        hint.setPadding(0, dp(12), 0, 0);
        right.addView(hint);

        // Bottom: the status line.
        LinearLayout status = new LinearLayout(ctx);
        status.setGravity(Gravity.CENTER_VERTICAL);
        status.setBackgroundColor(p.panel);
        status.setPadding(0, 0, dp(22), 0);
        TextView session = text(14, p.bg, true);
        session.setText(host);
        session.setBackgroundColor(p.green);
        session.setGravity(Gravity.CENTER_VERTICAL);
        session.setPadding(dp(14), 0, dp(14), 0);
        status.addView(session, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
        for (int i = 0; i < 3; i++) {
            TextView t = text(14, p.muted, false);
            t.setPadding(dp(14), 0, 0, 0);
            statusTabs[i] = t;
            status.addView(t);
        }
        status.addView(new View(ctx), new LinearLayout.LayoutParams(0, 1, 1f));
        TextClock now = new TextClock(ctx);
        now.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        now.setTextColor(p.fg);
        now.setTypeface(mono);
        now.setFormat24Hour("12".equals(clockFormat) ? "h:mm a" : "HH:mm");
        now.setFormat12Hour("24".equals(clockFormat) ? "HH:mm" : "h:mm a");
        status.addView(now);
        View top = new View(ctx);
        top.setBackgroundColor(p.border);
        column.addView(top, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(2)));
        column.addView(status, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(34)));
    }

    private static final class Row {
        final String label;
        final int position;   // the app's place in the adapter, or -1 for a system row
        final Runnable run;

        Row(String label, int position, Runnable run) {
            this.label = label;
            this.position = position;
            this.run = run;
        }
    }

    private List<Row> rows() {
        List<Row> out = new ArrayList<>();
        int n = adapter.appCount();
        if (tab == TAB_SYSTEM) {
            out.add(new Row(ctx.getString(R.string.console_inputs), -1, actions::inputs));
            out.add(new Row(ctx.getString(R.string.console_launcher_settings), -1, actions::launcherSettings));
            out.add(new Row(ctx.getString(R.string.console_tv_settings), -1, () -> {
                try {
                    ctx.startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                } catch (RuntimeException ignored) {
                    // A TV without Android's settings screen: nothing to open.
                }
            }));
            return out;
        }
        int end = tab == TAB_HOME ? Math.min(n, Math.max(adapter.dockFilled(), 0)) : n;
        if (tab == TAB_HOME && end == 0) end = n;   // no dock: Home is every app
        for (int i = 0; i < end; i++) {
            if (adapter.isEmptyAt(i)) continue;
            final int pos = i;
            out.add(new Row(adapter.labelAt(i), i, () -> adapter.launchAt(pos)));
        }
        return out;
    }

    /** Rebuilds the list for the current tab; [focusFirst] puts focus on its first row (a tab change). */
    private void fill(boolean focusFirst) {
        boolean had = list.hasFocus();
        int focusedIndex = -1;
        if (had) for (int i = 0; i < list.getChildCount(); i++) if (list.getChildAt(i).hasFocus()) focusedIndex = i;
        list.removeAllViews();
        List<Row> rows = rows();
        for (int i = 0; i < rows.size(); i++) list.addView(row(rows.get(i), i == rows.size() - 1));
        for (int i = 0; i < 3; i++) {
            boolean on = i == tab;
            tabs[i].setTextColor(on ? p.bg : p.muted);
            tabs[i].setBackgroundColor(on ? p.accent : 0);
            tabs[i].setTypeface(on ? monoBold : mono);
            statusTabs[i].setText((i + 1) + ":" + ctx.getString(i == 0 ? R.string.console_home : i == 1 ? R.string.console_all : R.string.console_system) + (on ? "*" : ""));
            statusTabs[i].setTextColor(on ? p.accent : p.muted);
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

    private View row(Row r, boolean last) {
        TextView t = text(18, p.fg, true);
        t.setFocusable(true);
        t.setClickable(true);
        t.setSingleLine(true);
        t.setEllipsize(android.text.TextUtils.TruncateAt.END);
        t.setPadding(dp(14), dp(10), dp(14), dp(10));
        t.setText("  " + r.label);
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_focused}, new ColorDrawable(TerminalPalette.withAlpha(p.accent, 0x2E)));
        bg.addState(new int[]{}, new ColorDrawable(0));
        t.setBackground(bg);
        t.setOnFocusChangeListener((v, focused) -> {
            t.setText((focused ? "> " : "  ") + r.label);
            t.setTextColor(focused ? p.accent : p.fg);
        });
        t.setOnClickListener(v -> r.run.run());
        t.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                tab = (tab + (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ? 1 : 2)) % 3;
                fill(true);
                return true;
            }
            return false;
        });
        if (!last) {
            // A hairline between rows, drawn by the row below's top inset.
            LinearLayout wrap = new LinearLayout(ctx);
            wrap.setOrientation(LinearLayout.VERTICAL);
            wrap.addView(t);
            View line = new View(ctx);
            line.setBackgroundColor(TerminalPalette.withAlpha(p.border, 0x99));
            wrap.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(1))));
            wrap.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
            return wrap;
        }
        return t;
    }

    private void renderInfo() {
        long minutes = SystemClock.elapsedRealtime() / MINUTE_MS;
        String up = ctx.getString(R.string.terminal_uptime_value, (int) (minutes / 60), (int) (minutes % 60));
        int apps = 0;
        for (int i = 0; i < adapter.appCount(); i++) if (!adapter.isEmptyAt(i)) apps++;
        SpannableStringBuilder b = new SpannableStringBuilder();
        line(b, ctx.getString(R.string.console_tv), host);
        line(b, ctx.getString(R.string.console_os), "Android " + Build.VERSION.RELEASE);
        line(b, ctx.getString(R.string.console_apps), ctx.getString(R.string.console_apps_value, apps));
        line(b, ctx.getString(R.string.terminal_network), networkText);
        line(b, ctx.getString(R.string.terminal_theme), p.name);
        line(b, ctx.getString(R.string.terminal_uptime), up);
        b.delete(b.length() - 1, b.length());
        info.setText(b);
        openPrompt.setText(prompt(ctx.getString(R.string.console_open), blink));
    }

    private void line(SpannableStringBuilder b, String key, String value) {
        int start = b.length();
        b.append(String.format(Locale.ROOT, "%-9s", key));
        b.setSpan(new ForegroundColorSpan(p.accent), start, b.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        b.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), start, b.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        b.append(value == null ? "" : value).append('\n');
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
        if (!blink) return;
        Runnable cursor = new Runnable() {
            @Override public void run() {
                if (!view.isAttachedToWindow()) return;
                cursorOn = !cursorOn;
                openPrompt.setText(prompt(ctx.getString(R.string.console_open), true));
                view.postDelayed(this, BLINK_MS);
            }
        };
        view.postDelayed(cursor, BLINK_MS);
    }
}
