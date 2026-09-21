"""PNG -> .spr text. Usage: python sprite_dump.py <in.png> <out.spr> [--frame 16x16]"""

import sys
from pathlib import Path

from PIL import Image

from sprite_film import film_for
from spritekit import (ROOT, TRANSPARENT, assign_symbols, canonical_blank,
                       frame_size, indexed_pixels, luminance, read_palette)


def asset_relative(path):
    """Path as the game names it, e.g. 'sprites/rat.png', or None if outside."""
    try:
        return Path(path).resolve().relative_to(
            ROOT / "core/src/main/assets").as_posix()
    except ValueError:
        return None


def dump(src, dst, frame=None):
    im = Image.open(src)
    palette, mode, transparency = read_palette(im)
    pixels = indexed_pixels(im)
    used = sorted({i for row in pixels for i in row})
    symbols = assign_symbols(palette, used)
    blank = canonical_blank(palette, used)
    rel = asset_relative(src)
    film = film_for(rel) if rel else None
    if frame is None and film:
        fw, fh = film["frame"]
    else:
        fw, fh = frame_size(im, frame)
    w, h = im.size
    labels = {}
    if film:
        for name, indices in sorted(film["animations"].items()):
            for slot, idx in enumerate(indices):
                labels.setdefault(idx, []).append("%s[%d]" % (name, slot))

    out = ["# spritekit v1", "source %s" % Path(src).as_posix(),
           "mode %s" % mode, "size %dx%d" % (w, h), "frame %dx%d" % (fw, fh)]
    if film:
        out.append("# film from %s" % ", ".join(sorted(film["classes"])))
    if isinstance(transparency, (bytes, bytearray)):
        out.append("transparency table %s" % transparency.hex())
    elif transparency is not None:
        out.append("transparency index %d" % transparency)
    out.append("palette")
    for idx in sorted(used, key=lambda i: (symbols[i] == TRANSPARENT,
                                           symbols[i][0], luminance(palette[i]))):
        r, g, b, a = palette[idx]
        out.append("  %s idx=%-3d #%02x%02x%02x a=%-3d lum=%6.1f"
                   % (symbols[idx], idx, r, g, b, a, luminance(palette[idx])))
    out.append("grid")
    for fy in range(0, h, fh):
        for fx in range(0, w, fw):
            # Skip a frame only when it is entirely the canonical blank, so a
            # frame of some other alpha-0 colour is still written out verbatim.
            if blank is not None and all(
                    pixels[y][x] == blank
                    for y in range(fy, min(fy + fh, h))
                    for x in range(fx, min(fx + fw, w))):
                continue
            index = (fy // fh) * (w // fw) + fx // fw
            note = "  # %d %s" % (index, " ".join(labels.get(index, []))) if film                 else "  # %d" % index
            out.append("frame %d,%d%s" % (fx, fy, note))
            for y in range(fy, min(fy + fh, h)):
                cells = "".join(symbols[pixels[y][x]]
                                for x in range(fx, min(fx + fw, w)))
                out.append("  %s" % cells)
    Path(dst).write_text("\n".join(out) + "\n", encoding="utf-8")
    print("wrote %s  (%d colours, %dx%d frames)" % (dst, len(used), fw, fh))


if __name__ == "__main__":
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    frame = next((a.split("=", 1)[1] if "=" in a else sys.argv[sys.argv.index(a) + 1]
                  for a in sys.argv if a.startswith("--frame")), None)
    dump(args[0], args[1], frame)
