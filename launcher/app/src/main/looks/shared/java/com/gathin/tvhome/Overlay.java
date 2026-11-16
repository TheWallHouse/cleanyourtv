package com.gathin.tvhome;

import android.graphics.Paint;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

/** Helpers for the looks that cover Home with a view of their own (ConsoleHome, CyberpunkHome, DuneHome, MapHome). */
final class Overlay {
    private Overlay() {
    }

    /**
     * Keeps focus inside [overlay]: Home's own views under it (the hidden tiles, the top buttons) can no longer take
     * it, so the D-pad never wanders under the look and the focused row is always the one shown.
     */
    static void isolate(FrameLayout root, View overlay) {
        for (int i = 0; i < root.getChildCount(); i++) {
            View v = root.getChildAt(i);
            if (v == overlay) continue;
            v.setFocusable(false);
            if (v instanceof ViewGroup) ((ViewGroup) v).setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        }
    }

    /**
     * Settings in every look works as Nothing's gear: a short press opens the TV's settings, a long press the
     * launcher's. This is the short one.
     */
    static void openTvSettings(android.content.Context ctx) {
        try {
            ctx.startActivity(new android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (RuntimeException ignored) {
            // A TV without Android's settings screen: nothing to open.
        }
    }

    /** Sets [text] on [t] at the largest size from [maxSp] down to [minSp] that fits [widthPx], letter spacing included. */
    static void fit(TextView t, String text, float maxSp, float minSp, int widthPx) {
        Paint paint = new Paint(t.getPaint());
        float scaled = t.getResources().getDisplayMetrics().scaledDensity;
        float sp = maxSp;
        while (sp > minSp) {
            paint.setTextSize(sp * scaled);
            if (paint.measureText(text) <= widthPx) break;
            sp -= 1f;
        }
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setText(text);
    }
}
