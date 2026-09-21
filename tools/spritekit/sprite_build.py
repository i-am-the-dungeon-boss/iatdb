"""'.spr' text -> PNG. Usage: python sprite_build.py <in.spr> <out.png>"""

import sys
from pathlib import Path

from PIL import Image

from spritekit import TRANSPARENT


def parse(path):
    header, palette, frames = {}, {}, []
    section, current = None, None
    for line in Path(path).read_text(encoding="utf-8").splitlines():
        if not line.strip() or line.startswith("#"):
            continue
        if not line.startswith(" "):
            key, _, rest = line.partition(" ")
            if key == "palette":
                section = "palette"
            elif key == "grid":
                section = "grid"
            elif key == "frame" and section == "grid":
                x, y = rest.split("#")[0].strip().split(",")
                current = (int(x), int(y), [])
                frames.append(current)
            else:
                header[key] = rest
            continue
        body = line.strip()
        if section == "palette":
            symbol, rest = body[:2], body[2:]
            idx = int(rest.split("idx=")[1].split()[0])
            rgb = rest.split("#")[1][:6]
            alpha = int(rest.split("a=")[1].split()[0])
            palette[symbol] = (idx, (int(rgb[0:2], 16), int(rgb[2:4], 16),
                                    int(rgb[4:6], 16), alpha))
        elif section == "grid":
            current[2].append(body)
    return header, palette, frames


def build(src, dst):
    header, palette, frames = parse(src)
    w, h = (int(v) for v in header["size"].split("x"))
    mode = header.get("mode", "RGBA")
    # A fully opaque sheet has no transparent entry; index 0 is then only a
    # placeholder, since every pixel is covered by an explicit frame.
    if mode == "P":
        fill = palette[TRANSPARENT][0] if TRANSPARENT in palette else 0
        canvas = [[fill] * w for _ in range(h)]
    else:
        # Pixels outside every frame (a 14x15 film leaves a strip on a 128x16
        # sheet) keep the sheet's own blank colour, which is not always
        # (0, 0, 0, 0) -- ghost.png pads with transparent white.
        blank = palette[TRANSPARENT][1] if TRANSPARENT in palette else (0, 0, 0, 0)
        canvas = [[blank] * w for _ in range(h)]

    for fx, fy, rows in frames:
        for dy, row in enumerate(rows):
            cells = [row[i:i + 2] for i in range(0, len(row), 2)]
            for dx, cell in enumerate(cells):
                if cell not in palette:
                    raise ValueError("unknown symbol %r at frame %d,%d row %d"
                                     % (cell, fx, fy, dy))
                idx, rgba = palette[cell]
                canvas[fy + dy][fx + dx] = idx if mode == "P" else rgba

    if mode == "P":
        im = Image.new("P", (w, h))
        flat = [0] * 768
        for idx, rgba in palette.values():
            flat[3 * idx:3 * idx + 3] = list(rgba[:3])
        im.putpalette(flat)
        im.putdata([canvas[y][x] for y in range(h) for x in range(w)])
        if "transparency" in header:
            kind, _, value = header["transparency"].partition(" ")
            tr = bytes.fromhex(value) if kind == "table" else int(value)
            im.info["transparency"] = tr
            im.save(dst, transparency=tr)
            print("wrote %s (indexed)" % dst)
            return
    else:
        im = Image.new("RGBA", (w, h))
        im.putdata([canvas[y][x] for y in range(h) for x in range(w)])
    im.save(dst)
    print("wrote %s (%s)" % (dst, mode))


if __name__ == "__main__":
    build(sys.argv[1], sys.argv[2])
