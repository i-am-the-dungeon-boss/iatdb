"""Zoomed contact sheet for eyeballing a sprite sheet.

Usage: python sprite_sheet.py <sheet.png> <preview.png> [--frame 16x16] [--zoom 8]
Draws each frame at an integer zoom with a grid and frame indices, and can put
two sheets side by side so a before/after edit is easy to compare.
"""

import sys
from pathlib import Path

from PIL import Image, ImageDraw

from sprite_dump import asset_relative
from sprite_film import film_for
from spritekit import frame_size

CHECKER = ((60, 60, 68), (46, 46, 54))
GRID = (110, 110, 130)
LABEL = (235, 230, 215)


def checkerboard(size, cell=8):
    img = Image.new("RGB", size, CHECKER[0])
    d = ImageDraw.Draw(img)
    for y in range(0, size[1], cell):
        for x in range(0, size[0], cell):
            if (x // cell + y // cell) % 2:
                d.rectangle([x, y, x + cell - 1, y + cell - 1], fill=CHECKER[1])
    return img


def contact_sheet(src, dst, frame=None, zoom=8, columns=8):
    im = Image.open(src).convert("RGBA")
    rel = asset_relative(src)
    film = film_for(rel) if rel else None
    fw, fh = film["frame"] if (frame is None and film) else frame_size(im, frame)
    w, h = im.size
    frames = [(x, y) for y in range(0, h, fh) for x in range(0, w, fw)]
    frames = [(x, y) for x, y in frames
              if im.crop((x, y, min(x + fw, w), min(y + fh, h))).getbbox()]
    if not frames:
        raise ValueError("%s has no non-empty frames" % src)

    cols = min(columns, len(frames))
    rows = (len(frames) + cols - 1) // cols
    pad, label = 6, 12
    cw, ch = fw * zoom + pad, fh * zoom + pad + label
    out = Image.new("RGBA", (cols * cw + pad, rows * ch + pad), (30, 30, 36, 255))
    draw = ImageDraw.Draw(out)

    for i, (fx, fy) in enumerate(frames):
        cell = im.crop((fx, fy, fx + fw, fy + fh)).resize(
            (fw * zoom, fh * zoom), Image.NEAREST)
        ox = pad + (i % cols) * cw
        oy = pad + (i // cols) * ch + label
        tile = checkerboard(cell.size).convert("RGBA")
        tile.alpha_composite(cell)
        out.paste(tile, (ox, oy))
        draw.rectangle([ox, oy, ox + cell.width, oy + cell.height], outline=GRID)
        draw.text((ox + 1, oy - label + 1), "%d  %d,%d" % (i, fx, fy), fill=LABEL)

    out.convert("RGB").save(dst)
    print("wrote %s  (%d frames at %dx, %dx%d each)"
          % (dst, len(frames), zoom, fw, fh))


if __name__ == "__main__":
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    opts = {a.split("=")[0].lstrip("-"): a.split("=")[1]
            for a in sys.argv[1:] if a.startswith("--") and "=" in a}
    contact_sheet(args[0], args[1], opts.get("frame"), int(opts.get("zoom", 8)))
