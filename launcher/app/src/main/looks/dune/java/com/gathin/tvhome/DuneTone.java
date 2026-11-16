package com.gathin.tvhome;

/**
 * The colours of Dune (Aspect.DUNE): a sea of dunes under a low sun at dusk, a white sun at noon, or two moons at
 * night. Picked in Launcher settings or from the phone (settings.json "dune_tone"). A mood, not anyone's world: no
 * names, logos or pictures of a film or a book. Kept free of Android so it is unit-tested.
 */
final class DuneTone {
    static final String KEY = "dune_tone";
    static final String DUSK = "dusk";
    static final String NOON = "noon";
    static final String NIGHT = "night";
    /** In Launcher settings' order; the first is the default. */
    static final String[] ALL = {DUSK, NOON, NIGHT};

    final String name;
    /** The sky, top to horizon, and where each colour sits (0 top, 1 bottom). */
    final int[] sky;
    final float[] skyAt;
    /** The sun (or moon): colour, centre height and radius as fractions of the screen's height. */
    final int sun;
    final float sunY, sunR;
    /** A second, smaller moon: centre and radius as fractions of the screen's width and height; radius 0 for none. */
    final float moonX, moonY, moonR;
    /** Stars' alpha, 0..255. */
    final int stars;
    /** The three dunes, far to near. */
    final int dune1, dune2, dune3;
    /** Text, dimmed text, a card's fill, the focused card's edge, the other cards' edge. */
    final int fg, muted, card, edge, faint;

    private DuneTone(String name, int[] sky, float[] skyAt, int sun, float sunY, float sunR, float moonX, float moonY,
                     float moonR, int stars, int dune1, int dune2, int dune3, int fg, int muted, int card, int edge, int faint) {
        this.name = name;
        this.sky = sky;
        this.skyAt = skyAt;
        this.sun = sun;
        this.sunY = sunY;
        this.sunR = sunR;
        this.moonX = moonX;
        this.moonY = moonY;
        this.moonR = moonR;
        this.stars = stars;
        this.dune1 = dune1;
        this.dune2 = dune2;
        this.dune3 = dune3;
        this.fg = fg;
        this.muted = muted;
        this.card = card;
        this.edge = edge;
        this.faint = faint;
    }

    static boolean valid(String name) {
        for (String n : ALL) if (n.equals(name)) return true;
        return false;
    }

    /** The tone by name; an unknown or missing one is dusk. */
    static DuneTone of(String name) {
        switch (name == null ? "" : name) {
            case NOON:
                return new DuneTone(NOON, new int[]{0xFF9C7148, 0xFFD6AF84, 0xFFF1DCBC}, new float[]{0f, 0.46f, 0.72f},
                        0xFFFFFAF0, 0.583f, 0.083f, 0f, 0f, 0f, 0,
                        0xFFDCB88C, 0xFFB98A5C, 0xFF7A4F2C,
                        0xFF24160A, 0xFF4F3720, 0x8CFFF6E8, 0xD93C230F, 0x403C230F);
            case NIGHT:
                return new DuneTone(NIGHT, new int[]{0xFF02040A, 0xFF0A1226, 0xFF1B2C4D}, new float[]{0f, 0.5f, 0.74f},
                        0xFFE6EEFC, 0.653f, 0.097f, 0.617f, 0.417f, 0.031f, 0xCC,
                        0xFF22324F, 0xFF152038, 0xFF090F1C,
                        0xFFE6ECF5, 0xFFA7B5CC, 0x9E060A16, 0xE6E6EEFC, 0x38E6EEFC);
            default:
                return new DuneTone(DUSK, new int[]{0xFF1C0D22, 0xFF6E2C22, 0xFFD97C3E, 0xFFF2B26A}, new float[]{0f, 0.42f, 0.66f, 0.74f},
                        0xFFFFE0B0, 0.694f, 0.133f, 0f, 0f, 0f, 0x40,
                        0xFFA8552B, 0xFF6E3418, 0xFF2A140A,
                        0xFFFFF3E4, 0xFFF0CAA4, 0x9E1E0C05, 0xE6FFE0B0, 0x40FFE0B0);
        }
    }
}
