package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;

import java.time.LocalDateTime;

/**
 * Terminal · Console's screensaver: the classic terminal clock in blocks (DreamTime.glyph) in the palette's accent,
 * the date under it, and a session waiting at its prompt with the cursor blinking.
 */
final class ConsoleDream extends LookDream {
    private static final float BLOCK = 46, GAP = 6, SPACE = 34;
    private final TerminalPalette p;
    private final Typeface mono, bold;

    ConsoleDream(Context ctx, TerminalPalette p) {
        super(ctx);
        this.p = p;
        mono = ctx.getResources().getFont(R.font.jetbrains_mono);
        bold = ctx.getResources().getFont(R.font.jetbrains_mono_bold);
    }

    @Override
    protected long frameMs() {
        return 500L;   // the cursor's blink
    }

    @Override
    protected void drawBackdrop(Canvas c) {
        c.drawColor(p.bg);
    }

    @Override
    protected void drawLive(Canvas c, LocalDateTime now) {
        String hm = time(now);
        float w = -SPACE;
        for (char ch : hm.toCharArray()) w += DreamTime.glyph(ch)[0].length() * (BLOCK + GAP) + SPACE;
        float x = (W - w) / 2, top = 170;
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        for (char ch : hm.toCharArray()) {
            String[] g = DreamTime.glyph(ch);
            paint.setColor(p.accent);
            for (int r = 0; r < g.length; r++) {
                for (int col = 0; col < g[r].length(); col++) {
                    if (g[r].charAt(col) != '1') continue;
                    float l = x + col * (BLOCK + GAP), t = top + r * (BLOCK + GAP);
                    c.drawRect(l, t, l + BLOCK, t + BLOCK, paint);
                }
            }
            x += g[0].length() * (BLOCK + GAP) + SPACE;
        }
        String ampm = amPm(now);
        if (!ampm.isEmpty()) text(c, ampm, x - SPACE + 16, top + 5 * (BLOCK + GAP) - GAP, 30, p.accent2, bold, Paint.Align.LEFT, 0);

        text(c, date(now, "EEEEdMMMMy"), 640, 490, 28, p.muted, mono, Paint.Align.CENTER, 0);

        String user = "tv@wallhouse", path = ":~$", cmd = " sleep ∞ ";
        float px = 56;
        text(c, user, px, 660, 22, p.green, mono, Paint.Align.LEFT, 0);
        px += width(user, 22, mono, 0);
        text(c, path, px, 660, 22, p.muted, mono, Paint.Align.LEFT, 0);
        px += width(path, 22, mono, 0);
        text(c, cmd, px, 660, 22, p.fg, mono, Paint.Align.LEFT, 0);
        px += width(cmd, 22, mono, 0);
        if (System.currentTimeMillis() / 500 % 2 == 0) fill(c, px, 642, px + 14, 666, p.fg);
        text(c, p.name, 1224, 660, 18, p.muted, mono, Paint.Align.RIGHT, 0);
    }
}
