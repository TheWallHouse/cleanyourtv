package com.gathin.tvhome;

import android.service.dreams.DreamService;

/**
 * Screensaver: the look's own (LookDream), or for Nothing the launcher's thin clock and date on black, over a slow
 * ambient glow (ClockScene).
 * No network, no images: cheap enough for a 1 GB TV.
 */
public class ClockDream extends DreamService {

    private ClockScene scene;
    private LookDream look;

    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(LauncherLocale.wrap(base));   // the theme's locale, as ClockActivity
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);   // any key wakes the TV back to where it was
        setFullscreen(true);
        setScreenBright(false);
        String aspect = Aspect.mode(this);
        look = LookDream.create(this, aspect);
        if (look != null) {   // every look but Nothing draws its own
            setContentView(look);
            return;
        }
        setContentView(R.layout.dream_clock);
        if (Aspect.NOTHING.equals(aspect)) NothingLook.dream(getWindow().getDecorView());
        LauncherFont.apply(this, getWindow().getDecorView());
        ClockFormat.apply(getWindow().getDecorView(), Config.load(this).clockFormat);
        scene = new ClockScene(getWindow().getDecorView());
        scene.useHomeWallpaper(this);
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        if (look != null) look.start();
        else scene.start();
    }

    @Override
    public void onDreamingStopped() {
        if (look != null) look.stop();
        if (scene != null) scene.stop();
        super.onDreamingStopped();
    }

    @Override
    public void onDetachedFromWindow() {
        if (look != null) look.stop();
        if (scene != null) scene.stop();
        super.onDetachedFromWindow();
    }
}
