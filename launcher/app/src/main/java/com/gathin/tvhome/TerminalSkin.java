package com.gathin.tvhome;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
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
 * Terminal · Windows (Aspect.WINDOWS) over Home's own views: a bar across the top like a tiling desktop's (numbered
 * rows, the date and time in the middle, the palette on the right), a terminal window in place of the greeting and
 * clock (a prompt with the greeting, the WALLHOUSE TV wordmark, the palette, the network and the time on), square
 * top buttons, and the monospaced font everywhere. The tiles, the focus frame and the dock's window are drawn by
 * AppAdapter, GlassView and DockDecoration from the same palette. Everything else (focus, keys, the dock, the
 * settings) is Home as it is: nothing here takes focus.
 */
final class TerminalSkin {
    private static final int ROWS_SHOWN = 4;
    private static final long MINUTE_MS = 60_000L;
    private static final long BLINK_MS = 550L;

    private final Context ctx;
    private final TerminalPalette p;
    private final float density;
    private final Typeface mono;
    private final Typeface monoBold;
    private final TextView[] rows = new TextView[ROWS_SHOWN];
    private TextView prompt;
    private TextView info;
    private final String host;
    private String greetingText = "";
    private String networkText = "";
    private boolean cursorOn = true;
    private final boolean blink;

    private TerminalSkin(Context ctx, TerminalPalette p, boolean blink) {
        this.ctx = ctx;
        this.p = p;
        this.blink = blink;
        density = ctx.getResources().getDisplayMetrics().density;
        mono = ctx.getResources().getFont(R.font.jetbrains_mono);
        monoBold = ctx.getResources().getFont(R.font.jetbrains_mono_bold);
        host = hostName(ctx);
    }

    static Typeface font(Context ctx) {
        return ctx.getResources().getFont(R.font.jetbrains_mono);
    }

    /**
     * Puts the look on Home. [root] is Home's FrameLayout, [leftBlock] the greeting and clock column (its children are
     * hidden and the terminal window takes their place), [topButtons] the pills, [greeting] and [wifi] the views whose
     * text the window repeats, [hero] the focused app's name.
     */
    static TerminalSkin apply(Context ctx, TerminalPalette p, FrameLayout root, ViewGroup leftBlock, ViewGroup topButtons,
                              TextView greeting, TextView wifi, TextView hero, String clockFormat, boolean blink) {
        TerminalSkin s = new TerminalSkin(ctx, p, blink);
        s.monoEverywhere(root);
        root.setBackgroundColor(p.bg);
        s.addBar(root, clockFormat);
        s.addWindow(leftBlock);
        s.squareButtons(topButtons);
        if (hero != null) {
            hero.setTypeface(s.monoBold);
            hero.setTextColor(p.accent);
            hero.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
            hero.setLetterSpacing(0f);
        }
        s.follow(greeting, text -> s.greetingText = text.toLowerCase(Locale.getDefault()));
        s.follow(wifi, text -> s.networkText = text);
        s.tick(root);
        return s;
    }

    /** The focused tile moved: its row lights up in the bar (1 is the dock). */
    void onFocus(int position, int columns) {
        int row = position < columns ? 0 : 1 + (position - columns) / Math.max(1, columns);
        row = Math.min(row, ROWS_SHOWN - 1);
        for (int i = 0; i < ROWS_SHOWN; i++) {
            boolean on = i == row;
            rows[i].setTextColor(on ? p.accent : p.muted);
            rows[i].setTypeface(on ? monoBold : mono);
            rows[i].getPaint().setUnderlineText(on);
            rows[i].invalidate();
        }
    }

    private void monoEverywhere(View v) {
        if (v instanceof TextView) {
            TextView t = (TextView) v;
            Typeface now = t.getTypeface();
            t.setTypeface(now != null && now.isBold() ? monoBold : mono);
            t.setLetterSpacing(0f);
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) monoEverywhere(g.getChildAt(i));
        }
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
        t.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return t;
    }

    private void addBar(FrameLayout root, String clockFormat) {
        FrameLayout bar = new FrameLayout(ctx);
        bar.setBackgroundColor(TerminalPalette.withAlpha(p.panel, 0xEB));
        bar.setPadding(dp(24), 0, dp(24), 0);
        bar.setFocusable(false);

        LinearLayout left = new LinearLayout(ctx);
        left.setGravity(Gravity.CENTER_VERTICAL);
        TextView mark = text(15, p.fg, false);
        mark.setText("▣");
        left.addView(mark);
        for (int i = 0; i < ROWS_SHOWN; i++) {
            TextView n = text(15, i == 0 ? p.accent : p.muted, i == 0);
            n.setText(String.valueOf(i + 1));
            n.setPadding(dp(14), 0, 0, 0);
            if (i == 0) n.getPaint().setUnderlineText(true);
            rows[i] = n;
            left.addView(n);
        }
        bar.addView(left, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.START | Gravity.CENTER_VERTICAL));

        TextClock clock = new TextClock(ctx);
        clock.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        clock.setTextColor(p.fg);
        clock.setTypeface(mono);
        clock.setFocusable(false);
        String h24 = "EEE d MMM   HH:mm", h12 = "EEE d MMM   h:mm a";
        clock.setFormat24Hour("12".equals(clockFormat) ? h12 : h24);
        clock.setFormat12Hour("24".equals(clockFormat) ? h24 : h12);
        bar.addView(clock, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        TextView right = text(15, p.muted, false);
        right.setText(p.name);
        bar.addView(right, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.END | Gravity.CENTER_VERTICAL));

        // Under the focus frame (the last child), over everything else.
        root.addView(bar, Math.max(0, root.getChildCount() - 1), new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(38), Gravity.TOP));
        // Home's column starts below the bar.
        View column = (View) ((View) ((View) root.findViewById(R.id.leftBlock)).getParent()).getParent();
        column.setPadding(column.getPaddingLeft(), column.getPaddingTop() + dp(38), column.getPaddingRight(), column.getPaddingBottom());
    }

    private void addWindow(ViewGroup leftBlock) {
        for (int i = 0; i < leftBlock.getChildCount(); i++) leftBlock.getChildAt(i).setVisibility(View.GONE);
        LinearLayout window = new LinearLayout(ctx);
        window.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(TerminalPalette.withAlpha(p.panel, 0xEB));
        bg.setStroke(dp(2), p.border);
        window.setBackground(bg);
        window.setPadding(dp(20), dp(16), dp(28), dp(16));

        prompt = text(15, p.fg, false);
        window.addView(prompt);

        TextView word = text(40, p.accent, true);
        SpannableStringBuilder w = new SpannableStringBuilder("WALLHOUSE");
        int start = w.length();
        w.append("TV");
        w.setSpan(new ForegroundColorSpan(p.accent2), start, w.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        word.setText(w);
        word.setLetterSpacing(0.04f);
        LinearLayout.LayoutParams wl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wl.topMargin = dp(12);
        wl.bottomMargin = dp(12);
        window.addView(word, wl);

        info = text(14, p.fg, false);
        info.setLineSpacing(dp(4), 1f);
        window.addView(info);

        leftBlock.addView(window, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void squareButtons(ViewGroup buttons) {
        for (int i = 0; i < buttons.getChildCount(); i++) {
            View v = buttons.getChildAt(i);
            StateListDrawable states = new StateListDrawable();
            states.addState(new int[]{android.R.attr.state_focused}, box(p.panel, p.accent));
            states.addState(new int[]{}, box(TerminalPalette.withAlpha(p.panel, 0xD9), p.border));
            v.setBackground(states);
            if (v instanceof TextView) {
                TextView t = (TextView) v;
                t.setTextColor(p.fg);
                t.setCompoundDrawableTintList(ColorStateList.valueOf(p.fg));
            }
        }
    }

    private GradientDrawable box(int fill, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setStroke(dp(2), stroke);
        return d;
    }

    private interface OnText {
        void set(String text);
    }

    /** Repeats [source]'s text in the window as Home updates it (the greeting each hour, the network as it changes). */
    private void follow(TextView source, OnText onText) {
        if (source == null) return;
        onText.set(String.valueOf(source.getText()));
        source.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                onText.set(s.toString());
                render();
            }
        });
    }

    /** The prompt's cursor blinks (with animations on), and the time on is counted each minute, while Home is up. */
    private void tick(View root) {
        render();
        Runnable minute = new Runnable() {
            @Override public void run() {
                if (!root.isAttachedToWindow()) return;
                render();
                root.postDelayed(this, MINUTE_MS);
            }
        };
        root.postDelayed(minute, MINUTE_MS);
        if (!blink) return;
        Runnable cursor = new Runnable() {
            @Override public void run() {
                if (!root.isAttachedToWindow()) return;
                cursorOn = !cursorOn;
                renderPrompt();
                root.postDelayed(this, BLINK_MS);
            }
        };
        root.postDelayed(cursor, BLINK_MS);
    }

    private void render() {
        renderPrompt();
        long minutes = SystemClock.elapsedRealtime() / MINUTE_MS;
        String up = ctx.getString(R.string.terminal_uptime_value, (int) (minutes / 60), (int) (minutes % 60));
        SpannableStringBuilder b = new SpannableStringBuilder();
        line(b, ctx.getString(R.string.terminal_theme), p.name);
        b.append('\n');
        line(b, ctx.getString(R.string.terminal_network), networkText);
        b.append('\n');
        line(b, ctx.getString(R.string.terminal_uptime), up);
        info.setText(b);
    }

    /** "key    value", the keys padded to one width: the font is monospaced, so the values line up. */
    private void line(SpannableStringBuilder b, String key, String value) {
        int start = b.length();
        b.append(String.format(Locale.ROOT, "%-8s", key));
        b.setSpan(new ForegroundColorSpan(p.accent), start, b.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        b.append(value);
    }

    private void renderPrompt() {
        SpannableStringBuilder b = new SpannableStringBuilder();
        int a = b.length();
        b.append("tv@").append(host);
        b.setSpan(new ForegroundColorSpan(p.green), a, b.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        a = b.length();
        b.append(":~$");
        b.setSpan(new ForegroundColorSpan(p.muted), a, b.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        b.append(' ').append(greetingText).append(' ');
        a = b.length();
        b.append('█');
        if (!cursorOn) b.setSpan(new ForegroundColorSpan(0), a, b.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        prompt.setText(b);
    }

    /** The TV's name as a host name ("Habitación 2" → habitacion-2), or "home" when it has none. */
    static String hostName(Context ctx) {
        String name = null;
        try {
            name = Settings.Global.getString(ctx.getContentResolver(), Settings.Global.DEVICE_NAME);
        } catch (RuntimeException ignored) {
            // Some boxes don't let apps read it.
        }
        return slug(name);
    }

    static String slug(String name) {
        if (name == null) return "home";
        String s = java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        if (s.length() > 20) s = s.substring(0, 20).replaceAll("-+$", "");
        return s.isEmpty() ? "home" : s;
    }
}
