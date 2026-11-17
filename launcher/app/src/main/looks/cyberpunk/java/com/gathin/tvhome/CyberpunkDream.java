package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;

import java.time.LocalDateTime;
import java.util.Random;

/**
 * Cyberpunk's screensaver: the time huge with the menu's glitch (its three colours a little apart, jerking to new
 * offsets now and then), the date, and the day as 24 neon segments: hours gone lit, this one blinking in the third
 * colour. Over it the diagonal stripe, the outlined name and scanlines, as on Home.
 */
final class CyberpunkDream extends LookDream {
    private final CyberpunkTone p;
    private final Typeface font, bold;
    private final Random random = new Random();
    private final Path segment = new Path();
    private float glitchA = -6, glitchB = 6;
    private long lastGlitch = -1;

    CyberpunkDream(Context ctx, CyberpunkTone p) {
        super(ctx);
        this.p = p;
        font = ctx.getResources().getFont(R.font.chakra_petch);
        bold = ctx.getResources().getFont(R.font.chakra_petch_bold);
    }

    @Override
    protected long frameMs() {
        return 1000L;   // the current hour's segment blinks
    }

    @Override
    protected void drawBackdrop(Canvas c) {
        c.drawColor(CyberpunkTone.BG);
        Path stripe = new Path();
        stripe.moveTo(-40, 720);
        stripe.lineTo(220, 720);
        stripe.lineTo(520, 0);
        stripe.lineTo(260, 0);
        stripe.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(CyberpunkTone.withAlpha(p.accent, 18));
        c.drawPath(stripe, paint);
        line(c, 560, 566, p.accent);
        line(c, 578, 580, CyberpunkTone.withAlpha(p.accent2, 178));
        paint.setTypeface(bold);
        paint.setTextSize(240);
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setColor(CyberpunkTone.withAlpha(p.accent, 30));
        c.drawText("WALLHOUSE", 1300, 300, paint);
        paint.setTextAlign(Paint.Align.LEFT);
        text(c, "00", 160, 656, 15, p.muted, font, Paint.Align.LEFT, 0.25f);
        text(c, "24", 1120, 656, 15, p.muted, font, Paint.Align.RIGHT, 0.25f);
    }

    /** A thin slanted neon line from (top, 0) down to 300 px to the left at the bottom. */
    private void line(Canvas c, float from, float to, int color) {
        Path path = new Path();
        path.moveTo(from, 0);
        path.lineTo(to, 0);
        path.lineTo(to - 300, 720);
        path.lineTo(from - 300, 720);
        path.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        c.drawPath(path, paint);
    }

    @Override
    protected void drawLive(Canvas c, LocalDateTime now) {
        // The glitch jerks to new offsets every few seconds, then holds.
        long step = System.currentTimeMillis() / 7_000;
        if (step != lastGlitch) {
            lastGlitch = step;
            glitchA = -(3 + random.nextFloat() * 9);
            glitchB = 3 + random.nextFloat() * 9;
        }
        text(c, "STANDBY // SYSTEM ONLINE ▮", 80, 90, 20, p.accent2, bold, Paint.Align.LEFT, 0.3f);
        String hm = time(now);
        float size = hm.length() > 5 ? 250 : 290;
        text(c, hm, 640 + glitchA, 450, size, p.accent2, bold, Paint.Align.CENTER, 0);
        text(c, hm, 640 + glitchB, 450, size, p.third, bold, Paint.Align.CENTER, 0);
        text(c, hm, 640, 450, size, p.accent, bold, Paint.Align.CENTER, 0);
        String ampm = amPm(now);
        if (!ampm.isEmpty()) text(c, ampm, 1150, 250, 34, p.fg, bold, Paint.Align.RIGHT, 0.2f);
        String day = date(now, "EEEdMMM").replace(",", "").replace(".", "").toUpperCase(locale)
                .trim().replaceAll("\\s+", " · ");
        text(c, day, 640, 530, 30, p.fg, font, Paint.Align.CENTER, 0.33f);

        int hour = now.getHour();
        boolean blink = System.currentTimeMillis() / 1000 % 2 == 0;
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 24; i++) {
            float x = 160 + i * 40;
            segment.reset();
            segment.moveTo(x + 8, 590);
            segment.lineTo(x + 34, 590);
            segment.lineTo(x + 26, 620);
            segment.lineTo(x, 620);
            segment.close();
            paint.setColor(i < hour ? p.accent : i == hour ? (blink ? p.third : CyberpunkTone.withAlpha(p.third, 90)) : 0xFF2A2A38);
            c.drawPath(segment, paint);
        }
        paint.setColor(0x59000000);
        for (int y = 0; y < 720; y += 4) c.drawRect(0, y, W, y + 1, paint);
    }
}
