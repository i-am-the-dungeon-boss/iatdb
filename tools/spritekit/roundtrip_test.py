"""Regression: every palette-based asset must survive dump -> build unchanged.

Run: python roundtrip_test.py
True-colour sheets (the fireball gradients, the icon atlases) are expected to be
rejected outright rather than quantised, so they are listed here explicitly.
"""

import contextlib
import io
import sys
import tempfile
from pathlib import Path

from PIL import Image

from sprite_build import build
from sprite_dump import dump
from spritekit import ROOT

ASSETS = ROOT / "core/src/main/assets"
TRUECOLOUR = {"icons.png", "icons_tinted.png",
              "fireball-short.png", "fireball-tall.png"}


def main():
    tmp = Path(tempfile.mkdtemp())
    failures = []
    checked = 0
    for png in sorted(ASSETS.rglob("*.png")):
        spr, out = tmp / (png.stem + ".spr"), tmp / (png.stem + ".out.png")
        try:
            with contextlib.redirect_stdout(io.StringIO()):
                dump(str(png), str(spr))
                build(str(spr), str(out))
        except ValueError as e:
            if png.name in TRUECOLOUR and "too large to symbolise" in str(e):
                continue
            failures.append((png.name, str(e)))
            continue
        before = Image.open(png).convert("RGBA")
        after = Image.open(out).convert("RGBA")
        checked += 1
        # Compare the pixels outright. ImageChops.difference(...).getbbox() is
        # not usable here: Pillow 11 made getbbox() alpha-only by default, so on
        # these fully opaque sheets it reports no difference no matter what.
        if before.size != after.size or list(before.getdata()) != list(after.getdata()):
            failures.append((png.name, "pixels differ after round trip"))

    print("round trip: %d sheets exact, %d failed" % (checked, len(failures)))
    for name, why in failures:
        print("  FAIL %s - %s" % (name, why))
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
