package com.gathin.tvhome;

/**
 * The colours of Legend (Aspect.MAP): an old hand-drawn map on parchment, in silver ink at night, or burnt in
 * embers. Picked in Launcher settings or from the phone (settings.json "legend_tone"). The map's lands and names are
 * made up: no world from a book or a film. Kept free of Android so it is unit-tested.
 */
final class MapTone {
    static final String KEY = "legend_tone";
    static final String PARCHMENT = "parchment";
    static final String NIGHT = "night";
    static final String EMBERS = "embers";
    /** In Launcher settings' order; the first is the default. */
    static final String[] ALL = {PARCHMENT, NIGHT, EMBERS};

    final String name;
    /** Sea, land, a mountain's face, ink, the focused place, text on it, rivers, trees, the volcano, stains, labels' ground. */
    final int sea, land, land2, ink, accent, onAccent, water, tree, ember, stain, card;

    private MapTone(String name, int sea, int land, int land2, int ink, int accent, int onAccent, int water, int tree,
                       int ember, int stain, int card) {
        this.name = name;
        this.sea = sea;
        this.land = land;
        this.land2 = land2;
        this.ink = ink;
        this.accent = accent;
        this.onAccent = onAccent;
        this.water = water;
        this.tree = tree;
        this.ember = ember;
        this.stain = stain;
        this.card = card;
    }

    static boolean valid(String name) {
        for (String n : ALL) if (n.equals(name)) return true;
        return false;
    }

    /** The tone by name; an unknown or missing one is parchment. */
    static MapTone of(String name) {
        switch (name == null ? "" : name) {
            case NIGHT:
                return new MapTone(NIGHT, 0xFF0C1320, 0xFF172234, 0xFF1A2639, 0xFFC9D6E3, 0xFFC9D6E3, 0xFF0D1420,
                        0xFF6F9CC9, 0xFF3F5A66, 0xFFE0A050, 0xFF000000, 0xEB0E1420);
            case EMBERS:
                return new MapTone(EMBERS, 0xFF1A0B06, 0xFF2C140B, 0xFF30160C, 0xFFF2C79A, 0xFFFF6A1A, 0xFF1A0A05,
                        0xFFC9541C, 0xFF6A3A1C, 0xFFFF6A1A, 0xFF000000, 0xEB1E0C07);
            default:
                return new MapTone(PARCHMENT, 0xFFDCC697, 0xFFF0E2BD, 0xFFEFE0B8, 0xFF3B2A17, 0xFF8A2B1C, 0xFFF6EAD0,
                        0xFF4F6F7A, 0xFF6F7A4A, 0xFFB5401F, 0xFF7A4F1E, 0xEBF6EAD0);
        }
    }
}
