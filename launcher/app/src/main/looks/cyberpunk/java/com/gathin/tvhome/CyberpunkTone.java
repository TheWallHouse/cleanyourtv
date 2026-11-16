package com.gathin.tvhome;

/**
 * The colours of Cyberpunk (Aspect.CYBERPUNK): a night-city game menu in one neon colour, with a second for the focus
 * shadow and a third for the clock's glitch. Picked in Launcher settings or from the phone (settings.json
 * "cyberpunk_tone"). A mood, not anyone's world: no names, logos or pictures of a film or a game. Kept free of
 * Android so it is unit-tested.
 */
final class CyberpunkTone {
    static final String KEY = "cyberpunk_tone";
    static final String YELLOW = "yellow";
    static final String PINK = "pink";
    static final String CYAN = "cyan";
    /** In Launcher settings' order; the first is the default. */
    static final String[] ALL = {YELLOW, PINK, CYAN};

    static final int BG = 0xFF08080D;
    static final int BAR = 0xD914141E;
    static final int INK = 0xFF0A0A0A;

    final String name;
    /** Text, dimmed text, the neon, the focus shadow, the glitch's third colour. */
    final int fg, muted, accent, accent2, third;

    private CyberpunkTone(String name, int fg, int muted, int accent, int accent2, int third) {
        this.name = name;
        this.fg = fg;
        this.muted = muted;
        this.accent = accent;
        this.accent2 = accent2;
        this.third = third;
    }

    static boolean valid(String name) {
        for (String n : ALL) if (n.equals(name)) return true;
        return false;
    }

    /** The tone by name; an unknown or missing one is yellow. */
    static CyberpunkTone of(String name) {
        switch (name == null ? "" : name) {
            case PINK:
                return new CyberpunkTone(PINK, 0xFFF4F1FA, 0xFF958AAB, 0xFFFF2A6D, 0xFF00F0FF, 0xFFFCEE0A);
            case CYAN:
                return new CyberpunkTone(CYAN, 0xFFEFFAFC, 0xFF7F9CA7, 0xFF00F0FF, 0xFFB026FF, 0xFF39FF14);
            default:
                return new CyberpunkTone(YELLOW, 0xFFF4F3EA, 0xFF8F8BA3, 0xFFFCEE0A, 0xFF00F0FF, 0xFFFF2A6D);
        }
    }

    /** [argb] at [alpha] (0..255). */
    static int withAlpha(int argb, int alpha) {
        return (alpha << 24) | (argb & 0xFFFFFF);
    }
}
