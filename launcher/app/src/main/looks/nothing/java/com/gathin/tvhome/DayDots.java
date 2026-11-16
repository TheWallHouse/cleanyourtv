package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import java.util.Calendar;

/**
 * Nothing edition: the day as 24 dots under the screensaver clock. Hours gone by are white,
 * the hours still to come dim grey, and the current hour pulses slowly in Nothing red.
 */
public class DayDots extends View {
    private static final int HOURS = 24;
    private static final long PULSE_MS = 2400;
    private static final long FRAME_MS = 100;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float dot;
    private final float gap;

    public DayDots(Context context, AttributeSet attrs) {
        super(context, attrs);
        float density = getResources().getDisplayMetrics().density;
        dot = 8 * density;
        gap = 6 * density;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = (int) Math.ceil(HOURS * dot + (HOURS - 1) * gap) + getPaddingLeft() + getPaddingRight();
        int h = (int) Math.ceil(dot) + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(resolveSize(w, widthMeasureSpec), resolveSize(h, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        float phase = (SystemClock.uptimeMillis() % PULSE_MS) / (float) PULSE_MS;
        float pulse = 0.35f + 0.65f * (float) (0.5 - 0.5 * Math.cos(phase * 2 * Math.PI));
        float r = dot / 2f;
        float cy = getPaddingTop() + r;
        for (int i = 0; i < HOURS; i++) {
            float cx = getPaddingLeft() + r + i * (dot + gap);
            if (i < hour) {
                paint.setColor(0xD9FFFFFF);
            } else if (i == hour) {
                paint.setColor(((int) (pulse * 255) << 24) | 0xD71921);
            } else {
                paint.setColor(0x33FFFFFF);
            }
            canvas.drawCircle(cx, cy, r, paint);
        }
        // Only the small dot row redraws, ten times a second, while the screensaver is showing.
        if (isShown()) postInvalidateDelayed(FRAME_MS);
    }
}
