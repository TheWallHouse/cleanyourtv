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
 * one largest and the others smaller and fainter towards the edges. ◀▶ walk the row and OK opens; ▼ goes down to a menu
 * bar with icons (Home, All apps, Inputs, Settings), where ◀▶ choose, OK picks and ▲ goes back up. Home and All apps
 * choose which apps the row shows; the one shown is marked in the bar. The sky,
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
    private final Actions actions;
    private final float density;
    private final Typeface wide, light, medium;
    private final boolean motion;
    private final Locale locale;
    private final FrameLayout view;
    private TextView title;
    /** Focus is in the menu bar under the row, on [menuIndex]; else on the row. */
    private boolean inMenu;
    private int menuIndex;
    private static final int MENU_HOME = 0, MENU_ALL = 1, MENU_INPUTS = 2, MENU_SETTINGS = 3;
    private Row row;
    private static final int LIST_HOME = 0, LIST_ALL = 1, LIST_INPUTS = 2;
    /** What the row shows: the dock's apps, all of them, or the TV's inputs. */
    private int list = LIST_HOME;
    private final List<Item> items = new ArrayList<>();
    private int index;
    /** The viewer has moved along the row: a reload keeps their card; before that the row starts at the first app. */
    private boolean moved;

    /** What the menu bar opens. */
    interface Actions {
        /** Hands each of the TV's inputs to [sink], in the Inputs dropdown's order. */
        void listInputs(InputSink sink);
        void launcherSettings();
    }

    /** One input: its icon, its name, its connection state (TvInputManager's, or -1 unknown) and what opens it. */
    interface InputSink {
        void add(int icon, String label, int state, Runnable open);
    }

    private DuneHome(Context ctx, DuneTone p, AppAdapter adapter, Actions actions, boolean motion) {
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
                           boolean motion, Actions actions) {
        DuneHome d = new DuneHome(ctx, p, adapter, actions, motion);
        d.build(clockFormat);
        root.addView(d.view, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        Overlay.isolate(root, d.view);
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
        title.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> { if (r - l != or - ol) showTitle(); });
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

    }

    private static final class Item {
        final String label;
        final int position;   // the app's place in the adapter, or -1 for an input
        final Runnable run;
        final int icon;       // an input's icon, or 0
        final boolean connected;

        Item(String label, int position, Runnable run) {
            this(label, position, run, 0, false);
        }

        Item(String label, int position, Runnable run, int icon, boolean connected) {
            this.label = label;
            this.position = position;
            this.run = run;
            this.icon = icon;
            this.connected = connected;
        }
    }

    /** Rebuilds the row: the dock's apps (or all of them), then the two settings cards. */
    private void fill(boolean first) {
        String focusedKey = !first && moved && index < items.size() ? items.get(index).label : null;
        items.clear();
        if (list == LIST_INPUTS) {
            actions.listInputs((icon, label, state, open) ->
                    items.add(new Item(label, -1, open, icon, state == android.media.tv.TvInputManager.INPUT_STATE_CONNECTED)));
        } else {
            boolean all = list == LIST_ALL;
            int n = adapter.appCount();
            int end = all ? n : Math.min(n, Math.max(adapter.dockFilled(), 0));
            if (!all && end == 0) end = n;   // no dock: Home is every app
            for (int i = 0; i < end; i++) {
                if (adapter.isEmptyAt(i)) continue;
                final int pos = i;
                items.add(new Item(adapter.labelAt(i), i, () -> adapter.launchAt(pos)));
            }
        }
        int keep = 0;
        if (focusedKey != null) for (int i = 0; i < items.size(); i++) if (items.get(i).label.equals(focusedKey)) keep = i;
        index = first || !moved ? 0 : Math.max(0, Math.min(keep, items.size() - 1));
        row.jump(index);
        showTitle();
    }

    private void showTitle() {
        int width = title.getWidth() > 0 ? title.getWidth() : view.getResources().getDisplayMetrics().widthPixels - dp(80);
        Overlay.fit(title, index < items.size() ? upper(items.get(index).label) : "", 42, 16, width);
    }

    /** OK on the menu bar's [which]. */
    private void pick(int which) {
        switch (which) {
            case MENU_HOME:
            case MENU_ALL:
            case MENU_INPUTS:
                list = which == MENU_HOME ? LIST_HOME : which == MENU_ALL ? LIST_ALL : LIST_INPUTS;
                moved = false;
                inMenu = false;
                fill(true);
                break;
            default:
                actions.launcherSettings();
        }
    }

    private void move(int by) {
        int next = Math.max(0, Math.min(items.size() - 1, index + by));
        if (next == index) return;
        moved = true;
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
            float cx = getWidth() / 2f;
            menu(canvas, cx);
            if (items.isEmpty()) return;
            float bottom = getHeight() - dp(100);
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
            fill.setAlpha(on ? 0xF0 : Math.round(Color.alpha(p.card) * a));
            canvas.drawRoundRect(box, r, r, fill);
            stroke.setColor(on && !inMenu ? p.edge : p.faint);
            stroke.setStrokeWidth(on ? dp(2) : Math.max(1, dp(1)));
            stroke.setAlpha(Math.round(Color.alpha(on ? p.edge : p.faint) * a));
            canvas.drawRoundRect(box, r, r, stroke);
            Item item = items.get(i);
            if (item.icon != 0) {
                Drawable d = ctx.getDrawable(item.icon);
                if (d != null) {
                    d = d.mutate();
                    d.setTint(on ? p.fg : p.muted);
                    d.setAlpha(alpha);
                    float s = w * 0.36f, top = box.top + h * 0.36f - s / 2;
                    d.setBounds(Math.round(centre - s / 2), Math.round(top), Math.round(centre + s / 2), Math.round(top + s));
                    d.draw(canvas);
                }
                if (item.connected) {
                    fill.setColor(p.fg);
                    fill.setAlpha(alpha);
                    canvas.drawCircle(centre, bottom - dp(on ? 34 : 26), dp(3), fill);
                }
            }
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
            float on = dp(21), off = dp(7.5f), gap = dp(7.5f), y = getHeight() - dp(86), t = Math.max(2, dp(2));
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

        private final int[] menuIcons = {R.drawable.ic_home, R.drawable.ic_apps, R.drawable.ic_input, R.drawable.ic_settings};
        private final int[] menuNames = {R.string.console_home, R.string.console_all, R.string.console_inputs, R.string.legend_settings};
        private final RectF pill = new RectF();

        /** The menu bar along the bottom: icon and name for each, the focused one filled, the row's list marked. */
        private void menu(Canvas canvas, float cx) {
            label.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10, getResources().getDisplayMetrics()));
            label.setLetterSpacing(0.3f);
            label.setTypeface(medium);
            float icon = dp(15), padX = dp(16), gapIcon = dp(9), gap = dp(12), h = dp(36);
            float[] widths = new float[menuNames.length];
            float total = 0;
            for (int i = 0; i < menuNames.length; i++) {
                widths[i] = padX + icon + gapIcon + label.measureText(upper(ctx.getString(menuNames[i]))) + padX;
                total += widths[i] + (i > 0 ? gap : 0);
            }
            float x = cx - total / 2, top = getHeight() - dp(22) - h;
            boolean lightInk = (Color.red(p.fg) + Color.green(p.fg) + Color.blue(p.fg)) > 384;
            int onText = lightInk ? 0xFF1B1209 : 0xFFFFF6E8;
            for (int i = 0; i < menuNames.length; i++) {
                boolean focused = inMenu && i == menuIndex;
                boolean shown = (i == MENU_HOME && list == LIST_HOME) || (i == MENU_ALL && list == LIST_ALL)
                        || (i == MENU_INPUTS && list == LIST_INPUTS);
                pill.set(x, top, x + widths[i], top + h);
                if (focused) {
                    fill.setColor(p.fg);
                    canvas.drawRoundRect(pill, h / 2, h / 2, fill);
                } else {
                    stroke.setColor(p.faint);
                    stroke.setStrokeWidth(Math.max(1, dp(1)));
                    canvas.drawRoundRect(pill, h / 2, h / 2, stroke);
                }
                int ink = focused ? onText : shown ? p.fg : p.muted;
                Drawable d = ctx.getDrawable(menuIcons[i]);
                if (d != null) {
                    d = d.mutate();
                    d.setTint(ink);
                    float iy = top + (h - icon) / 2;
                    d.setBounds(Math.round(x + padX), Math.round(iy), Math.round(x + padX + icon), Math.round(iy + icon));
                    d.draw(canvas);
                }
                label.setColor(ink);
                label.setTextAlign(Paint.Align.LEFT);
                Paint.FontMetrics fm = label.getFontMetrics();
                canvas.drawText(upper(ctx.getString(menuNames[i])), x + padX + icon + gapIcon, top + h / 2 - (fm.ascent + fm.descent) / 2, label);
                if (shown && !focused) {
                    fill.setColor(p.fg);
                    canvas.drawCircle(pill.centerX(), pill.bottom + dp(6), dp(2), fill);
                }
                x += widths[i] + gap;
            }
            label.setTextAlign(Paint.Align.CENTER);
        }

        @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
            if (inMenu) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        menuIndex = Math.max(0, menuIndex - 1);
                        invalidate();
                        return true;
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                        menuIndex = Math.min(menuNames.length - 1, menuIndex + 1);
                        invalidate();
                        return true;
                    case KeyEvent.KEYCODE_DPAD_UP:
                        inMenu = false;
                        invalidate();
                        return true;
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                        return true;
                    case KeyEvent.KEYCODE_DPAD_CENTER:
                    case KeyEvent.KEYCODE_ENTER:
                    case KeyEvent.KEYCODE_NUMPAD_ENTER:
                        if (event.getRepeatCount() == 0) pick(menuIndex);
                        invalidate();
                        return true;
                    default:
                        return super.onKeyDown(keyCode, event);
                }
            }
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    move(-1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    move(1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_UP:
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    inMenu = true;
                    menuIndex = list == LIST_ALL ? MENU_ALL : list == LIST_INPUTS ? MENU_INPUTS : MENU_HOME;
                    invalidate();
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
