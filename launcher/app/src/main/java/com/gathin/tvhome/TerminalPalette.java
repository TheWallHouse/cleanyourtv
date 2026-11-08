package com.gathin.tvhome;

/**
 * The colours of the terminal looks (Aspect.WINDOWS and CONSOLE): four well-known terminal palettes, picked in
 * Launcher settings or from the phone (settings.json "terminal_palette"). Kept free of Android so it is unit-tested.
 */
final class TerminalPalette {
    static final String KEY = "terminal_palette";
    static final String TOKYO_NIGHT = "tokyo-night";
    static final String CATPPUCCIN = "catppuccin";
    static final String GRUVBOX = "gruvbox";
    static final String NORD = "nord";
    /** In Launcher settings' order; the first is the default. */
    static final String[] ALL = {TOKYO_NIGHT, CATPPUCCIN, GRUVBOX, NORD};

    final String name;
    /** Ground, panels (window and bar), tiles, borders, text, dimmed text, the two accents and the prompt's green. */
    final int bg, panel, tile, border, fg, muted, accent, accent2, green;

    private TerminalPalette(String name, int bg, int panel, int tile, int border, int fg, int muted, int accent, int accent2, int green) {
        this.name = name;
        this.bg = bg;
        this.panel = panel;
        this.tile = tile;
        this.border = border;
        this.fg = fg;
        this.muted = muted;
        this.accent = accent;
        this.accent2 = accent2;
        this.green = green;
    }

    static boolean valid(String name) {
        for (String n : ALL) if (n.equals(name)) return true;
        return false;
    }

    /** The palette by name; an unknown or missing one is Tokyo Night. */
    static TerminalPalette of(String name) {
        switch (name == null ? "" : name) {
            case CATPPUCCIN:
                return new TerminalPalette(CATPPUCCIN, 0xFF1E1E2E, 0xFF181825, 0xFF262637, 0xFF45475A, 0xFFCDD6F4, 0xFFA6ADC8, 0xFF89B4FA, 0xFFCBA6F7, 0xFFA6E3A1);
            case GRUVBOX:
                return new TerminalPalette(GRUVBOX, 0xFF282828, 0xFF1D2021, 0xFF32302F, 0xFF504945, 0xFFEBDBB2, 0xFFBDAE93, 0xFFFABD2F, 0xFFFE8019, 0xFFB8BB26);
            case NORD:
                return new TerminalPalette(NORD, 0xFF2E3440, 0xFF272C36, 0xFF353C4A, 0xFF4C566A, 0xFFECEFF4, 0xFFD8DEE9, 0xFF88C0D0, 0xFFB48EAD, 0xFFA3BE8C);
            default:
                return new TerminalPalette(TOKYO_NIGHT, 0xFF1A1B26, 0xFF16161E, 0xFF1F2335, 0xFF414868, 0xFFC0CAF5, 0xFF8A93BD, 0xFF7AA2F7, 0xFFBB9AF7, 0xFF9ECE6A);
        }
    }

    /** [argb] at [alpha] (0..255), for panels laid over a wallpaper. */
    static int withAlpha(int argb, int alpha) {
        return (alpha << 24) | (argb & 0xFFFFFF);
    }
}
