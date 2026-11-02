# CleanYourTV · Nothing edition

A fork of [CleanYourTV](https://github.com/Gathin23/cleanyourtv) that gives its launcher the look of
**Nothing OS**: pure black, a dot-matrix font, one red, white lines. Everything else is CleanYourTV as it is:
the same launcher, the same debloat lists and tools, no ads, no network permission.

![Home: before and after](docs/nothing/before-after-home.jpg)

![Screensaver: before and after](docs/nothing/before-after-screensaver.jpg)

## What changes

| | Nothing edition |
|---|---|
| **Font** | [Doto](https://github.com/oliverlalan/Doto), a dot-matrix font (OFL), for the clock, greeting, date, top buttons and the focused app's name. Built in as a Latin subset with round dots; the colon's two crosses are moved up one dot so they line up with the digits |
| **Red** | The clock and the name after the greeting (`greeting_name`) in Nothing red `#D71921`. With `wifi_status_color`, the network icon is red while online and grey without a connection |
| **Focus** | A red dot beside the focused app's name, in the grid and on the big name above the dock |
| **Apps** | `nothing_tiles`: app banners in greyscale (`mono`) or greyscale with their reds kept in Nothing red (`mono_red`), so YouTube and Netflix keep their red. Converted once when the tiles load |
| **Screensaver** | The clock screensaver in red dots, and under the date the day as 24 dots: hours gone by in white, the hours to come in grey, the current hour pulsing red |
| **Theme** | [`themes/nothing/`](themes/nothing/): black, a faint dot grid with one red dot, frosted-glass dock, white rim focus, no colour glow, `mono_red` apps |

Not changed: permissions (still no `INTERNET`), `minSdk` 26, the Home guard, the debloat flow. The APK is
about 580 KB, a little over upstream's 576 KB gate, because of the font.

## Only the changes

This repository is a full copy of CleanYourTV, because the launcher has to be built whole. What this fork adds
is a handful of commits on top: **[see only the changes](https://github.com/Gathin23/cleanyourtv/compare/main...TheWallHouse:cleanyourtv:nothing)**. Every other file is CleanYourTV as it is.

| File | What it does here |
|---|---|
| `launcher/app/src/main/res/font/doto.ttf` | New: the Doto font, Latin subset, colon aligned |
| `launcher/FONT-LICENSE-DOTO-OFL.txt` | New: the font's licence |
| `launcher/app/src/main/res/values/fonts.xml` | `font_dot`, pointing at Doto |
| `launcher/app/src/main/res/values/styles.xml` | Top buttons in Doto |
| `launcher/app/src/main/res/layout/activity_main.xml` | Clock, greeting, date and app name in Doto; red clock; red dot beside the app name |
| `launcher/app/src/main/res/layout/item_app.xml` | Red dot beside the focused tile's name |
| `launcher/app/src/main/res/layout/dream_clock.xml` | Screensaver in red dots, with the day-dots row |
| `launcher/app/src/main/res/layout-iw/*.xml` | The same two changes for right-to-left languages |
| `launcher/app/src/main/res/drawable/nothing_focus_dot_*.xml` | New: the red focus dot |
| `launcher/app/src/main/res/color/pill_icon_*.xml` | Network icon: red online, grey offline |
| `launcher/app/src/main/java/com/gathin/tvhome/MainActivity.java` | The name after the greeting in red |
| `launcher/app/src/main/java/com/gathin/tvhome/AppAdapter.java` | Tiles go through NothingTone; the name wraps its text so the dot sits beside it |
| `launcher/app/src/main/java/com/gathin/tvhome/NothingTone.java` | New: `nothing_tiles`, greyscale apps with their reds kept |
| `launcher/app/src/main/java/com/gathin/tvhome/DayDots.java` | New: the 24 dots of the day under the screensaver clock |
| `themes/nothing/` | New: the theme (config, wallpaper, screenshot) |
| `themes/README.md` | The theme's row and the `nothing_tiles` key |
| `.github/README.md`, `NOTHING.md`, `docs/nothing/*.jpg` | This documentation (GitHub shows `.github/README.md` on the front page; CleanYourTV's `README.md` is left as it is) |

To bring in what CleanYourTV adds later: `git fetch upstream && git merge upstream/main` on the `nothing` branch.

## Install

Exactly as CleanYourTV ([README](README.md)), from this fork and the `nothing` branch:

```bash
git clone -b nothing https://github.com/TheWallHouse/cleanyourtv.git
cd cleanyourtv
TV_HOST=192.168.1.50 launcher/build-and-install.sh    # build the launcher, install it, make it Home
tools/theme.sh themes/nothing                        # the Nothing look (no rebuild)
```

For the screensaver, pick **Clock** in Settings → Device Preferences → Screen saver, or:
`tools/tv.py 'settings put secure screensaver_components com.gathin.tvhome/.ClockDream; settings put secure screensaver_enabled 1'`

Your own touches go in the theme's `config.json`, for example `"greeting_name": "your name"` (shown in red)
or `"nothing_tiles": "mono"` for plain black and white.

**Undo:** `tools/restore.sh` as in CleanYourTV puts the stock launcher back; `tools/theme.sh --reset` removes
the theme.

## Tested on

Vestel Cosmos (MediaTek), Android TV 11, 1.3 GB RAM. The Hebrew (right-to-left) layouts carry the same changes
but were only built, not seen on a TV.

## Credits

- [CleanYourTV](https://github.com/Gathin23/cleanyourtv) by Gathin23 and its contributors, MIT.
- [Doto](https://github.com/oliverlalan/Doto) by Oliver Lalan, SIL Open Font License 1.1
  ([`launcher/FONT-LICENSE-DOTO-OFL.txt`](launcher/FONT-LICENSE-DOTO-OFL.txt)). Subset and with the colon
  adjusted; the OFL allows it and the font keeps its licence.
- Nothing OS is a trademark of Nothing Technology Limited. This fork is not affiliated with Nothing; it only
  borrows the style.
