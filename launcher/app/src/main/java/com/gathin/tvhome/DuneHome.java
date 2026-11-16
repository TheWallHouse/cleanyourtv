package com.gathin.tvhome;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextClock;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Dune (Aspect.DUNE): Home as a row of monoliths on a sea of dunes, over the same apps as the tiles. The focused app's
 * name stands huge and widely spaced across the sky, under a small clock; below it the apps as tall cards, the middle
 * one largest and the others smaller and fainter towards the edges. ◀▶ walk the row, OK opens, ▲ opens the inputs and
 * ▼ swaps the dock's apps for all of them and back. Launcher and TV settings are the row's last two cards. The sky,
 * the sun or moons and the dunes are drawn once. The tile grid stays underneath, loaded and hidden, so the order, the
 * dock, hidden apps and the reloads are Home's own; this only lists and opens them.
 */
final class DuneHome {
    private static final long ANIM_MS = 200L;
    /** Card widths in dp by distance from the focused one; past the last a card is not drawn. */
    private static final float[] WIDTH_DP = {128f, 93f, 74f, 60f, 50f};
    private static final float[] FADE = {1f, 0.85f, 0.6f, 0.38f, 0f};
    private static final float RATIO = 1.47f;
    private static final float GAP_DP = 20f;

    private final Context ctx;
    private final DuneTone p;
    private final AppAdapter adapter;
    private final ConsoleHome.Actions actions;
    private final float density;
    private final Typeface wide, light, medium;
    private final boolean motion;
    private final Locale locale;
    private final FrameLayout view;
    private TextView title;
    private TextView down;
    private Row row;
    private boolean all;
    private final List<Item> items = new ArrayList<>();
    private int index;

    private DuneHome(Context ctx, DuneTone p, AppAdapter adapter, ConsoleHome.Actions actions, boolean motion) {
        this.ctx = ctx;
        this.p = p;
        this.adapter = adapter;
        this.actions = actions;
        this.motion = motion;
        density = ctx.getResources().getDisplayMetrics().density;
        wide = ctx.getResources().getFont(R.font.michroma);
        light = ctx.getResources().getFont(R.font.outfit_light);
        medium = ctx.getResources().getFont(R.font.outfit_medium);
        locale = ctx.getResources().getConfiguration().getLocales().get(0);
        view = new FrameLayout(ctx);
    }

    /** Lays the dunes over Home's [root], as the last child, and keeps the row in step with [adapter]. */
    static DuneHome attach(Context ctx, DuneTone p, FrameLayout root, AppAdapter adapter, String clockFormat,
                           boolean motion, ConsoleHome.Actions actions) {
        DuneHome d = new DuneHome(ctx, p, adapter, actions, motion);
        d.build(clockFormat);
        root.addView(d.view, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override public void onChanged() { d.fill(false); }
            @Override public void onItemRangeInserted(int start, int count) { d.fill(false); }
            @Override public void onItemRangeRemoved(int start, int count) { d.fill(false); }
            @Override public void onItemRangeMoved(int from, int to, int count) { d.fill(false); }
        });
        d.fill(true);
        return d;
    }

    void focus() {
        row.requestFocus();
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

    private TextView text(Typeface face, float sp, int color, float spacing) {
        TextView t = new TextView(ctx);
        t.setTypeface(face);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setLetterSpacing(spacing);
        t.setIncludeFontPadding(false);
        t.setFocusable(false);
        return t;
    }

    private FrameLayout.LayoutParams at(int gravity, int top, int bottom, int side) {
        FrameLayout.LayoutParams l = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, gravity);
        l.topMargin = top;
        l.bottomMargin = bottom;
        l.leftMargin = side;
        l.rightMargin = side;
        return l;
    }

    private void build(String clockFormat) {
        view.setClickable(true);   // touches never fall through to the hidden tiles
        view.addView(new Backdrop(ctx), new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextClock clock = new TextClock(ctx);
        clock.setTypeface(wide);
        clock.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        clock.setTextColor(p.muted);
        clock.setLetterSpacing(0.6f);
        clock.setAllCaps(true);
        String time = "12".equals(clockFormat) ? "h:mm" : "HH:mm";
        String time12 = "24".equals(clockFormat) ? "HH:mm" : "h:mm";
        clock.setFormat24Hour(time + "   ·   EEEE d MMM");
        clock.setFormat12Hour(time12 + "   ·   EEEE d MMM");
        view.addView(clock, at(Gravity.TOP | Gravity.CENTER_HORIZONTAL, dp(26), 0, 0));

        title = text(wide, 42, p.fg, 0.5f);
        title.setSingleLine(true);
        title.setGravity(Gravity.CENTER);
        title.setAutoSizeTextTypeUniformWithConfiguration(16, 42, 1, TypedValue.COMPLEX_UNIT_SP);
        FrameLayout.LayoutParams tl = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58), Gravity.TOP);
        tl.topMargin = dp(78);
        tl.leftMargin = dp(40);
        tl.rightMargin = dp(40);
        view.addView(title, tl);

        TextView hint = text(light, 11, p.muted, 0.4f);
        hint.setText(upper(ctx.getString(R.string.dune_open)));
        view.addView(hint, at(Gravity.TOP | Gravity.CENTER_HORIZONTAL, dp(146), 0, 0));

        row = new Row(ctx);
        FrameLayout.LayoutParams rl = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        rl.topMargin = dp(190);
        view.addView(row, rl);

        TextView up = text(light, 10, p.muted, 0.3f);
        up.setText("▲  " + upper(ctx.getString(R.string.console_inputs)));
        view.addView(up, at(Gravity.BOTTOM | Gravity.START, 0, dp(18), dp(44)));
        down = text(light, 10, p.muted, 0.3f);
        view.addView(down, at(Gravity.BOTTOM | Gravity.END, 0, dp(18), dp(44)));
    }

    private static final class Item {
        final String label;
        final int position;   // the app's place in the adapter, or -1 for a settings card
        final Runnable run;

        Item(String label, int position, Runnable run) {
            this.label = label;
            this.position = position;
            this.run = run;
        }
    }

    /** Rebuilds the row: the dock's apps (or all of them), then the two settings cards. */
    private void fill(boolean first) {
        String focusedKey = !first && index < items.size() ? items.get(index).label : null;
        items.clear();
        int n = adapter.appCount();
        int end = all ? n : Math.min(n, Math.max(adapter.dockFilled(), 0));
        if (!all && end == 0) end = n;   // no dock: Home is every app
        for (int i = 0; i < end; i++) {
            if (adapter.isEmptyAt(i)) continue;
            final int pos = i;
            items.add(new Item(adapter.labelAt(i), i, () -> adapter.launchAt(pos)));
        }
        items.add(new Item(ctx.getString(R.string.console_launcher_settings), -1, actions::launcherSettings));
        items.add(new Item(ctx.getString(R.string.console_tv_settings), -1, () -> {
            try {
                ctx.startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (RuntimeException ignored) {
                // A TV without Android's settings screen: nothing to open.
            }
        }));
        int keep = 0;
        if (focusedKey != null) for (int i = 0; i < items.size(); i++) if (items.get(i).label.equals(focusedKey)) keep = i;
        index = first ? 0 : Math.min(keep, items.size() - 1);
        row.jump(index);
        down.setText("▼  " + upper(ctx.getString(all ? R.string.console_home : R.string.console_all)));
        showTitle();
    }

    private void showTitle() {
        title.setText(index < items.size() ? upper(items.get(index).label) : "");
    }

    private void move(int by) {
        int next = Math.max(0, Math.min(items.size() - 1, index + by));
        if (next == index) return;
        index = next;
        showTitle();
        row.slideTo(index);
    }

    /** The row of cards; it holds the focus and takes the D-pad. */
    private final class Row extends View {
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint label = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final RectF box = new RectF();
        private final Rect saved = new Rect();
        private float pos;
        private ValueAnimator anim;

        Row(Context c) {
            super(c);
            setFocusable(true);
            setFocusableInTouchMode(true);
            stroke.setStyle(Paint.Style.STROKE);
            label.setTextAlign(Paint.Align.CENTER);
            label.setTypeface(medium);
        }

        void jump(int i) {
            if (anim != null) anim.cancel();
            pos = i;
            invalidate();
        }

        void slideTo(int i) {
            if (!motion) {
                jump(i);
                return;
            }
            if (anim != null) anim.cancel();
            anim = ValueAnimator.ofFloat(pos, i);
            anim.setDuration(ANIM_MS);
            anim.setInterpolator(new DecelerateInterpolator());
            anim.addUpdateListener(a -> {
                pos = (float) a.getAnimatedValue();
                invalidate();
            });
            anim.start();
        }

        /** Width in px of a card [k] places from the focus, k fractional. */
        private float width(float k) {
            int a = Math.min((int) k, WIDTH_DP.length - 1);
            int b = Math.min(a + 1, WIDTH_DP.length - 1);
            float f = Math.min(1f, k - a);
            return dp(WIDTH_DP[a] + (WIDTH_DP[b] - WIDTH_DP[a]) * f);
        }

        private float alpha(float k) {
            int a = Math.min((int) k, FADE.length - 1);
            int b = Math.min(a + 1, FADE.length - 1);
            float f = Math.min(1f, k - a);
            return FADE[a] + (FADE[b] - FADE[a]) * f;
        }

        @Override protected void onDraw(Canvas canvas) {
            if (items.isEmpty()) return;
            float cx = getWidth() / 2f;
            float bottom = getHeight() - dp(64);
            float gap = dp(GAP_DP);
            // Card centres: walk out from the focus, each card's half width plus the gap plus the next's half width.
            int count = items.size();
            float[] centre = new float[count];
            int base = Math.round(pos);
            centre[base] = cx + (base - pos) * (width(0) + gap);
            for (int i = base + 1; i < count; i++) centre[i] = centre[i - 1] + width(Math.abs(i - 1 - pos)) / 2f + gap + width(Math.abs(i - pos)) / 2f;
            for (int i = base - 1; i >= 0; i--) centre[i] = centre[i + 1] - width(Math.abs(i + 1 - pos)) / 2f - gap - width(Math.abs(i - pos)) / 2f;
            // Far cards first, so the near ones sit on top.
            Integer[] order = new Integer[count];
            for (int i = 0; i < count; i++) order[i] = i;
            java.util.Arrays.sort(order, (a, b) -> Float.compare(Math.abs(b - pos), Math.abs(a - pos)));
            for (int i : order) card(canvas, i, centre[i], bottom, Math.abs(i - pos));
            dots(canvas, cx);
        }

        private void card(Canvas canvas, int i, float centre, float bottom, float k) {
            float a = alpha(k);
            if (a <= 0.01f) return;
            float w = width(k), h = w * RATIO;
            if (centre + w / 2 < 0 || centre - w / 2 > getWidth()) return;
            boolean on = k < 0.5f;
            int alpha = Math.round(a * 255);
            box.set(centre - w / 2, bottom - h, centre + w / 2, bottom);
            float r = dp(5);
            fill.setColor(p.card);
            fill.setAlpha(Math.round(Color.alpha(p.card) * a));
            canvas.drawRoundRect(box, r, r, fill);
            stroke.setColor(on ? p.edge : p.faint);
            stroke.setStrokeWidth(on ? dp(2) : Math.max(1, dp(1)));
            stroke.setAlpha(Math.round(Color.alpha(on ? p.edge : p.faint) * a));
            canvas.drawRoundRect(box, r, r, stroke);
            Item item = items.get(i);
            Drawable art = item.position >= 0 ? adapter.artworkAt(item.position) : null;
            if (art != null) {
                int iw = art.getIntrinsicWidth(), ih = art.getIntrinsicHeight();
                float maxW = w * 0.78f, maxH = h * 0.5f;
                float s = iw > 0 && ih > 0 ? Math.min(maxW / iw, maxH / ih) : 0;
                if (s > 0) {
                    float dw = iw * s, dh = ih * s, top = box.top + h * 0.36f - dh / 2;
                    art.copyBounds(saved);
                    int oldAlpha = art.getAlpha();
                    art.setBounds(Math.round(centre - dw / 2), Math.round(top), Math.round(centre + dw / 2), Math.round(top + dh));
                    art.setAlpha(alpha);
                    art.draw(canvas);
                    art.setAlpha(oldAlpha);
                    art.setBounds(saved);
                }
            }
            label.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, on ? 12 : 9, getResources().getDisplayMetrics()));
            label.setLetterSpacing(on ? 0.25f : 0.15f);
            label.setColor(on ? p.fg : p.muted);
            label.setAlpha(alpha);
            CharSequence name = TextUtils.ellipsize(upper(item.label), label, w - dp(12), TextUtils.TruncateAt.END);
            canvas.drawText(name, 0, name.length(), centre, bottom - dp(on ? 14 : 10), label);
        }

        private void dots(Canvas canvas, float cx) {
            int count = items.size();
            float on = dp(21), off = dp(7.5f), gap = dp(7.5f), y = getHeight() - dp(24), t = Math.max(2, dp(2));
            float total = on + (count - 1) * off + (count - 1) * gap;
            if (total > getWidth() * 0.5f) return;   // too many to read as dots
            float x = cx - total / 2;
            for (int i = 0; i < count; i++) {
                boolean f = i == index;
                fill.setColor(f ? p.fg : p.faint);
                float w = f ? on : off;
                canvas.drawRect(x, y, x + w, y + t, fill);
                x += w + gap;
            }
        }

        @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    move(-1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    move(1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_UP:
                    actions.inputs();
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    all = !all;
                    index = 0;
                    fill(true);
                    return true;
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_NUMPAD_ENTER:
                    if (event.getRepeatCount() == 0 && index < items.size()) items.get(index).run.run();
                    return true;
                default:
                    return super.onKeyDown(keyCode, event);
            }
        }
    }

    /** The sky, stars, the sun (and at night a second moon) and three dunes, drawn once. */
    private final class Backdrop extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        Backdrop(Context c) {
            super(c);
            setFocusable(false);
        }

        /** A dune whose crest runs through [ys] (fractions of the height at x 0, ¼, ½, ¾, 1). */
        private void dune(Canvas canvas, int color, float... ys) {
            float w = getWidth(), h = getHeight();
            path.reset();
            path.moveTo(0, ys[0] * h);
            for (int i = 1; i < ys.length; i++) {
                float x0 = w * (i - 1) / (ys.length - 1), x1 = w * i / (ys.length - 1), mx = (x0 + x1) / 2;
                path.cubicTo(mx, ys[i - 1] * h, mx, ys[i] * h, x1, ys[i] * h);
            }
            path.lineTo(w, h);
            path.lineTo(0, h);
            path.close();
            paint.setShader(null);
            paint.setColor(color);
            canvas.drawPath(path, paint);
        }

        @Override protected void onDraw(Canvas canvas) {
            float w = getWidth(), h = getHeight();
            paint.setShader(new LinearGradient(0, 0, 0, h, p.sky, p.skyAt, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);
            paint.setShader(null);
            if (p.stars > 0) {
                Random r = new Random(3);
                for (int i = 0; i < 160; i++) {
                    float x = r.nextFloat() * w, y = r.nextFloat() * h * 0.58f;
                    float s = (r.nextFloat() < 0.12f ? 1.4f : 0.8f) * density / 1.5f;
                    paint.setColor(0xFFFFFFFF);
                    paint.setAlpha(p.stars);
                    canvas.drawCircle(x, y, Math.max(0.8f, s), paint);
                }
            }
            float sy = p.sunY * h, sr = p.sunR * h, halo = h * 0.44f;
            paint.setShader(new RadialGradient(w / 2, sy, halo, new int[]{(p.sun & 0xFFFFFF) | 0x80000000, p.sun & 0xFFFFFF},
                    null, Shader.TileMode.CLAMP));
            canvas.drawCircle(w / 2, sy, halo, paint);
            paint.setShader(null);
            paint.setColor(p.sun);
            canvas.drawCircle(w / 2, sy, sr, paint);
            if (p.moonR > 0) {
                paint.setAlpha(0xD9);
                canvas.drawCircle(p.moonX * w, p.moonY * h, p.moonR * h, paint);
            }
            dune(canvas, p.dune1, 0.72f, 0.69f, 0.72f, 0.68f, 0.71f);
            dune(canvas, p.dune2, 0.82f, 0.77f, 0.83f, 0.79f, 0.80f);
            dune(canvas, p.dune3, 0.92f, 0.88f, 0.93f, 0.89f, 0.90f);
        }
    }
}
