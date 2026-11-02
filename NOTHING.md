# CleanYourTV · Nothing edition

A fork of [CleanYourTV](https://github.com/Gathin23/cleanyourtv) that gives its launcher the look of
**Nothing OS**: pure black, a dot-matrix font, one red, white lines. Everything else is CleanYourTV as it is:
the same launcher, the same debloat lists and tools, no ads, no network permission.

*[Versión en castellano más abajo](#en-castellano).*

![Home](themes/nothing/screenshot.jpg)

![Screensaver](docs/nothing/screensaver.jpg)

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

---

## En castellano

Un fork de [CleanYourTV](https://github.com/Gathin23/cleanyourtv) que le da al launcher el aspecto de
**Nothing OS**: negro puro, tipografía de puntos, un solo rojo y líneas blancas. Lo demás es CleanYourTV tal
cual: el mismo launcher, las mismas listas y herramientas, sin anuncios y sin permiso de internet.

**Qué cambia:**

- **Tipografía de puntos (Doto)** en el reloj, el saludo, la fecha, los botones de arriba y el nombre de la app
  seleccionada, con los dos puntos del reloj alineados con los números.
- **Rojo Nothing** en el reloj y en el nombre del saludo. El icono de red sale rojo con conexión y gris sin ella.
- **Punto rojo** junto al nombre de la app seleccionada.
- **Apps en blanco y negro** (`nothing_tiles: mono`) o en blanco y negro conservando su rojo (`mono_red`).
- **Salvapantallas** con el reloj en puntos rojos y la **barra del día**: 24 puntos, con la hora actual
  latiendo en rojo.
- **Tema `nothing`** listo para usar, con fondo de puntos y un punto rojo.

**Instalación:** igual que CleanYourTV, desde la rama `nothing` de este fork (comandos arriba). Tu nombre en
rojo en el saludo se pone con `"greeting_name"` en el `config.json` del tema.

Probado en una Vestel Cosmos con Android TV 11.
