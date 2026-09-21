# spritekit

Text round-trip for the pixel-art sheets, so sprites can be read, diffed and
edited as source rather than as opaque PNGs.

## Why

An indexed PNG gives no clue which colour is a shadow and which is a highlight —
palette indices are arbitrary. `spritekit` re-expresses a sheet as a grid of
two-character symbols: a hue-family letter plus a brightness level ordered dark
to light. `a0` is the darkest colour of ramp `a`, `a7` a light one, `..` is
transparent. Shading structure becomes readable, and an edit is a text edit.

## Tools

| Script             | Does                                                        |
| ------------------ | ----------------------------------------------------------- |
| `sprite_dump.py`   | PNG -> `.spr` text, with palette header and per-frame labels |
| `sprite_build.py`  | `.spr` -> PNG, restoring the original indexed palette        |
| `sprite_sheet.py`  | zoomed contact sheet with a frame grid, for eyeballing       |
| `sprite_film.py`   | reads frame size and animation names out of the Java classes |
| `roundtrip_test.py`| asserts every palette sheet survives dump -> build unchanged  |

```bash
python sprite_dump.py ../../core/src/main/assets/sprites/rat.png src/rat.spr
python sprite_build.py src/rat.spr ../../core/src/main/assets/iatdb/sprites/rat.png
python sprite_sheet.py ../../core/src/main/assets/sprites/rat.png /tmp/preview.png --zoom=8
python roundtrip_test.py
```

## Frame geometry comes from the Java, not from guessing

Frame sizes are not square and cannot be inferred from the sheet: `RatSprite` is
16x15, `GooSprite` is 20x14. `sprite_film.py` parses the `TextureFilm`
constructor and the `Animation.frames()` calls in `core/.../sprites/*.java`, so a
dump labels each frame with the animation it belongs to. A sheet shared by
several classes (rat.png carries `RatSprite`, `AlbinoSprite` and
`FetidRatSprite`) reports all of them.

## Where new art goes

Mod-authored sheets live under `core/src/main/assets/iatdb/sprites/`, kept
separate from the upstream `core/src/main/assets/sprites/` tree so a base-game
merge never collides with our art. Their `.spr` sources live in `src/` here and
are not shipped in the build.

## Limits

- True-colour sheets (thousands of colours, e.g. the fireball gradients) are
  rejected rather than quantised. Round-tripping them would silently lose data.
- Ramp membership is inferred from hue and luminance, not from artist intent. It
  is reliable on this game's art but can mis-split a ramp that shifts hue as it
  brightens.
- Palette-swaps, recolours, small edits, mirroring and frame assembly are the
  operations this makes precise. Drawing a new creature from a blank grid is not.
