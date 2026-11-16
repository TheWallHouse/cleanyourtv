package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * The Nothing look (Aspect.NOTHING) is one look among the others, put on top of the plain base layouts, which stay as
 * CleanYourTV ships them: the Doto dot-matrix font on the clock, date, greeting, focused name and top buttons, the
 * clock in Nothing red, the red dot beside the focused name, and in the screensaver the red clock over the day as
 * 24 dots. The tiles' greyscale with reds (NothingTone), the red network icon, the red name after the greeting and the
 * bundled dot-grid wallpaper are switched on for this look alone where they are made. No other look inherits any of it.
 */
final class NothingLook {
    static final int RED = 0xFFD71921;

    private NothingLook() {
    }

    private static Typeface dot(android.content.Context ctx) {
        return UiFont.get(ctx, R.string.font_dot);
    }

    private static int dp(android.content.Context ctx, float v) {
        return Math.round(v * ctx.getResources().getDisplayMetrics().density);
    }

    /** Home's own views, right after the layout is set and before a theme's font.ttf takes the Doto ones' place. */
    static void home(Activity a) {
        Typeface dot = dot(a);
        TextView hero = a.findViewById(R.id.heroName);
        if (hero != null) {
            hero.setTypeface(dot);
            hero.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.nothing_focus_dot_12, 0, 0, 0);
            hero.setCompoundDrawablePadding(dp(a, 18));
        }
        TextView greeting = a.findViewById(R.id.greeting);
        if (greeting != null) {
            greeting.setTypeface(dot);
            greeting.setTextColor(0xFFFFFFFF);
            greeting.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        }
        TextView clock = a.findViewById(R.id.clock);
        if (clock != null) {
            clock.setTypeface(dot);
            clock.setTextColor(RED);
        }
        TextView date = a.findViewById(R.id.date);
        if (date != null) {
            date.setTypeface(dot);
            date.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        }
        View buttons = a.findViewById(R.id.topButtons);
        if (buttons instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) buttons;
            for (int i = 0; i < g.getChildCount(); i++) if (g.getChildAt(i) instanceof TextView) ((TextView) g.getChildAt(i)).setTypeface(dot);
        }
    }

    /** A tile's name: it wraps its text, so the red dot sits beside it while the tile is focused. */
    static void tileName(TextView name, ViewGroup.LayoutParams lp, int maxWidth) {
        lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        name.setMaxWidth(maxWidth);
        name.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.nothing_focus_dot_6, 0, 0, 0);
        name.setCompoundDrawablePadding(dp(name.getContext(), 6));
    }

    private static void clocks(View v, List<TextClock> out) {
        if (v instanceof TextClock) out.add((TextClock) v);
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) clocks(g.getChildAt(i), out);
        }
    }

    /** The clock screensaver (layout dream_clock): the time in red, Doto throughout, the day as 24 dots under the date. */
    static void dream(View root) {
        List<TextClock> found = new ArrayList<>();
        clocks(root, found);
        if (found.size() < 3) return;
        Typeface dot = dot(root.getContext());
        for (TextClock c : found) c.setTypeface(dot);
        found.get(0).setTextColor(RED);
        TextClock date = found.get(2);
        if (date.getParent() instanceof LinearLayout) {
            LinearLayout column = (LinearLayout) date.getParent();
            LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            l.setMarginStart(dp(root.getContext(), 8));
            l.topMargin = dp(root.getContext(), 22);
            column.addView(new DayDots(root.getContext(), null), column.indexOfChild(date) + 1, l);
        }
    }
}
