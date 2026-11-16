package com.gathin.tvhome;

/**
 * The colours of Orbit (Aspect.ORBIT): the Earth's night side from orbit, the sun rising over its rim, or a red
 * planet. Picked in Launcher settings or from the phone (settings.json "orbit_tone"). A mood, not anyone's brand: no
 * names or logos of a space company. Kept free of Android so it is unit-tested.
 */
final class OrbitTone {
    static final String KEY = "orbit_tone";
    static final String NIGHT = "night";
    static final String SUNRISE = "sunrise";
    static final String MARS = "mars";
    /** In Launcher settings' order; the first is the default. */
    static final String[] ALL = {NIGHT, SUNRISE, MARS};

    final String name;
    /** Text, dimmed text, hairlines, the accent. */
    final int fg, muted, line, accent;
    /** The atmosphere's glow, the planet's rim, its ground top and bottom, the city lights and their alpha (0..255). */
    final int atm, limb, groundTop, groundLow, lights, lightsAlpha;
    /** The stars' alpha (0..255); the sun on the rim, as a fraction of the width (below 0: none), and its colour. */
    final int starsAlpha;
    final float sunX;
    final int sun;

    private OrbitTone(String name, int fg, int muted, int line, int accent, int atm, int limb, int groundTop, int groundLow,
                      int lights, int lightsAlpha, int starsAlpha, float sunX, int sun) {
        this.name = name;
        this.fg = fg;
        this.muted = muted;
        this.line = line;
        this.accent = accent;
        this.atm = atm;
        this.limb = limb;
        this.groundTop = groundTop;
        this.groundLow = groundLow;
        this.lights = lights;
        this.lightsAlpha = lightsAlpha;
        this.starsAlpha = starsAlpha;
        this.sunX = sunX;
        this.sun = sun;
    }

    static boolean valid(String name) {
        for (String n : ALL) if (n.equals(name)) return true;
        return false;
    }

    /** The tone by name; an unknown or missing one is night. */
    static OrbitTone of(String name) {
        switch (name == null ? "" : name) {
            case SUNRISE:
                return new OrbitTone(SUNRISE, 0xFFFFFFFF, 0xFFA39A92, 0x2EFFFFFF, 0xFFFFB066, 0xFFFF8A3C, 0xFFFFD9A8,
                        0xFF141A26, 0xFF03050A, 0xFFFFB35C, 0x4D, 0x59, 0.79f, 0xFFFFB066);
            case MARS:
                return new OrbitTone(MARS, 0xFFFFF4EE, 0xFFA8928A, 0x2EFFDCC8, 0xFFFF7A4D, 0xFFFF8A65, 0xFFFFC2A8,
                        0xFF6A2A14, 0xFF1E0904, 0xFF000000, 0, 0xB3, -1f, 0xFFFFFFFF);
            default:
                return new OrbitTone(NIGHT, 0xFFFFFFFF, 0xFF8D96A8, 0x2EFFFFFF, 0xFFFFFFFF, 0xFF4AA3FF, 0xFF9FD0FF,
                        0xFF0B1526, 0xFF02040A, 0xFFFFB35C, 0x8C, 0x8C, -1f, 0xFFFFFFFF);
        }
    }
}
