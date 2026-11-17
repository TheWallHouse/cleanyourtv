package com.gathin.tvhome;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.TrafficStats;
import android.os.StatFs;
import android.os.SystemClock;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.time.temporal.WeekFields;
import java.util.Locale;

/**
 * Terminal · Windows' screensaver: a tiling desktop at rest. A bar with the workspaces and the time, and three
 * windows: a big clock, this month's calendar with today marked, and a small system monitor (memory, storage,
 * network, uptime, apps). Every ten minutes the clock swaps sides, as a tiling manager would retile.
 */
final class WindowsDream extends LookDream {
    private final TerminalPalette p;
    private final Typeface mono, bold;
    private final int apps;
    private long lastRx = -1, lastAt;
    private String net = "–";

    WindowsDream(Context ctx, TerminalPalette p) {
        super(ctx);
        this.p = p;
        mono = ctx.getResources().getFont(R.font.jetbrains_mono);
        bold = ctx.getResources().getFont(R.font.jetbrains_mono_bold);
        int count;
        try {
            count = ctx.getPackageManager().queryIntentActivities(
                    new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), 0).size();
        } catch (RuntimeException e) {
            count = -1;
        }
        apps = count;
    }

    @Override
    protected void drawBackdrop(Canvas c) {
        c.drawColor(p.bg);
        fill(c, 0, 0, W, 40, p.panel);
    }

    @Override
    protected void drawLive(Canvas c, LocalDateTime now) {
        String hm = time(now);
        text(c, "▣ ", 24, 27, 17, p.muted, mono, Paint.Align.LEFT, 0);
        float x = 24 + width("▣ ", 17, mono, 0);
        text(c, "1", x, 27, 17, p.accent, bold, Paint.Align.LEFT, 0);
        fill(c, x, 31, x + width("1", 17, bold, 0), 33, p.accent);
        text(c, " 2 3 4", x + width("1", 17, bold, 0), 27, 17, p.muted, mono, Paint.Align.LEFT, 0);
        text(c, date(now, "EEEdMMM") + "   " + hm + amPm(now), 640, 27, 17, p.fg, mono, Paint.Align.CENTER, 0);
        text(c, p.name, 1256, 27, 17, p.muted, mono, Paint.Align.RIGHT, 0);

        boolean swapped = now.getMinute() / 10 % 2 == 1;
        float clockX = swapped ? 486 : 24, sideX = swapped ? 24 : 812;
        clockWindow(c, clockX, now, hm);
        calendar(c, sideX, now.toLocalDate());
        monitor(c, sideX);
    }

    private void window(Canvas c, float x, float y, float w, float h, String title) {
        fill(c, x, y, x + w, y + h, p.panel);
        fill(c, x, y, x + w, y + 30, alpha(p.border, 90));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setColor(p.border);
        c.drawRect(x, y, x + w, y + h, paint);
        text(c, title, x + 14, y + 21, 15, p.muted, mono, Paint.Align.LEFT, 0);
    }

    private void clockWindow(Canvas c, float x, LocalDateTime now, String hm) {
        window(c, x, 60, 770, 636, "~ tty-clock");
        float cx = x + 385;
        float size = hm.length() > 5 ? 200 : 230;
        text(c, hm, cx, 440, size, p.accent, bold, Paint.Align.CENTER, -0.03f);
        String ampm = amPm(now);
        if (!ampm.isEmpty()) text(c, ampm, x + 740, 270, 28, p.accent2, bold, Paint.Align.RIGHT, 0);
        // The hour going by.
        fill(c, cx - 300, 490, cx + 300, 496, p.border);
        fill(c, cx - 300, 490, cx - 300 + 600 * now.getMinute() / 60f, 496, p.accent2);
        text(c, date(now, "EEEEdMMMM"), cx, 540, 22, p.muted, mono, Paint.Align.CENTER, 0);
    }

    private void calendar(Canvas c, float x, LocalDate today) {
        window(c, x, 60, 444, 300, "~ cal");
        float cx = x + 222;
        text(c, date(today.atStartOfDay(), "MMMMy"), cx, 122, 18, p.fg, bold, Paint.Align.CENTER, 0);
        DayOfWeek first = WeekFields.of(locale).getFirstDayOfWeek();
        float col0 = x + 38;
        for (int i = 0; i < 7; i++) {
            String d = first.plus(i).getDisplayName(TextStyle.SHORT_STANDALONE, locale);
            d = d.replace(".", "");
            d = d.substring(0, Math.min(2, d.length())).toLowerCase(locale);
            text(c, d, col0 + i * 52, 150, 16, p.muted, mono, Paint.Align.CENTER, 0);
        }
        LocalDate start = today.withDayOfMonth(1);
        int offset = (start.getDayOfWeek().getValue() - first.getValue() + 7) % 7;
        int days = today.lengthOfMonth();
        int rows = (offset + days + 6) / 7;
        float rowH = rows > 5 ? 28 : 34;
        for (int d = 1; d <= days; d++) {
            int idx = offset + d - 1;
            float cxDay = col0 + idx % 7 * 52, y = 186 + idx / 7 * rowH;
            boolean isToday = d == today.getDayOfMonth();
            if (isToday) fill(c, cxDay - 20, y - 22, cxDay + 20, y + 7, p.accent);
            text(c, String.valueOf(d), cxDay, y, 17, isToday ? p.bg : p.fg, isToday ? bold : mono, Paint.Align.CENTER, 0);
        }
    }

    private void monitor(Canvas c, float x) {
        window(c, x, 378, 444, 318, "~ btop");
        long[] mem = memory();
        bar(c, x, 450, "mem", mem == null ? 0 : mem[0] / (float) mem[1], p.accent,
                mem == null ? "–" : mem[0] + " MB");
        float[] disk = storage();
        bar(c, x, 500, "disk", disk[0], p.green, disk[1] < 0 ? "–" : String.format(Locale.ROOT, "%.1f GB", disk[1]));
        long rate = network();
        bar(c, x, 550, "net", Math.min(1f, rate / (2f * 1024 * 1024)), p.accent2, net);
        text(c, "up", x + 18, 610, 16, p.accent2, mono, Paint.Align.LEFT, 0);
        text(c, DreamTime.uptime(SystemClock.elapsedRealtime()), x + 418, 610, 15, p.fg, mono, Paint.Align.RIGHT, 0);
        text(c, "apps", x + 18, 650, 16, p.accent2, mono, Paint.Align.LEFT, 0);
        text(c, apps < 0 ? "–" : String.valueOf(apps), x + 418, 650, 15, p.fg, mono, Paint.Align.RIGHT, 0);
    }

    private void bar(Canvas c, float x, float y, String label, float f, int color, String value) {
        text(c, label, x + 18, y, 16, p.accent2, mono, Paint.Align.LEFT, 0);
        fill(c, x + 88, y - 14, x + 298, y + 2, alpha(p.border, 100));
        fill(c, x + 88, y - 14, x + 88 + 210 * Math.max(0, Math.min(1, f)), y + 2, color);
        text(c, value, x + 418, y, 15, p.fg, mono, Paint.Align.RIGHT, 0);
    }

    private static long[] memory() {
        try {
            return MemInfo.usedOfTotalMb(Files.readAllLines(Paths.get("/proc/meminfo")));
        } catch (Exception e) {
            return null;
        }
    }

    /** {used fraction, free GB} of the app storage. */
    private float[] storage() {
        try {
            StatFs fs = new StatFs(getContext().getFilesDir().getPath());
            long total = fs.getTotalBytes(), free = fs.getAvailableBytes();
            return new float[]{total > 0 ? 1f - free / (float) total : 0, free / 1e9f};
        } catch (RuntimeException e) {
            return new float[]{0, -1};
        }
    }

    /** Bytes per second received since the last minute's frame, with net set to its text. */
    private long network() {
        long rx = TrafficStats.getTotalRxBytes(), at = SystemClock.elapsedRealtime();
        long rate = 0;
        if (rx == TrafficStats.UNSUPPORTED) {
            net = "–";
        } else if (lastRx >= 0 && at > lastAt) {
            rate = (rx - lastRx) * 1000 / (at - lastAt);
            net = rate >= 1024 * 1024 ? String.format(Locale.ROOT, "↓ %.1f MB/s", rate / 1048576f)
                    : "↓ " + rate / 1024 + " KB/s";
        }
        lastRx = rx;
        lastAt = at;
        return rate;
    }
}
