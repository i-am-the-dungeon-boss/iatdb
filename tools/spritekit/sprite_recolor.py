"""Recolour chosen frames of a .spr without disturbing the rest of the sheet.

Editing a palette entry in place changes every frame that uses it: the sheets
share ramps between variants, so recolouring the rat's fur also speckles the
fetid rat. This allocates fresh palette entries for the new colours and swaps
them in only within the frames asked for, leaving every other frame untouched.

Usage:
  python sprite_recolor.py in.spr out.spr --frames=0,1,6 --map=b5:#cfb27a,b9:#d9c088

A colour may be given as #RRGGBB, keeping the entry's existing alpha, or as
#RRGGBBAA to set the alpha too -- semi-transparent overlays such as the shore
surf need a stronger alpha to read as white over dark water.
"""

import re
import sys
from pathlib import Path

from spritekit import luminance

PALETTE_LINE = re.compile(r"^  (\S\S) idx=(\d+)\s+#([0-9a-f]{6}) a=(\d+)")
FRAME_LINE = re.compile(r"^frame (\d+),(\d+)\s*#\s*(\d+)")
LEVELS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"


# Any character the symboliser can emit, so a crowded sheet still has a spare.
CANDIDATES = ("STUVWXYZabcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQR"
              "0123456789" "!$%&*+-/<=>?@^_~")


def free_letter(symbols):
    """A ramp letter no existing symbol uses, so new entries cannot collide."""
    used = {s[0] for s in symbols}
    for c in CANDIDATES:
        if c not in used:
            return c
    raise ValueError("no free ramp letter left for the recoloured entries")


def recolour(src, dst, frames, mapping):
    lines = Path(src).read_text(encoding="utf-8").splitlines()
    symbols, max_idx = {}, -1
    grid_at = None
    for i, line in enumerate(lines):
        m = PALETTE_LINE.match(line)
        if m:
            symbols[m.group(1)] = (int(m.group(2)), m.group(3), int(m.group(4)))
            max_idx = max(max_idx, int(m.group(2)))
        elif line == "grid":
            grid_at = i

    missing = [s for s in mapping if s not in symbols]
    if missing:
        raise ValueError("symbols not in palette: %s" % ", ".join(missing))

    letter = free_letter(symbols)
    new_lines, swap = [], {}
    for slot, (old, hexcolour) in enumerate(sorted(mapping.items())):
        max_idx += 1
        sym = letter + LEVELS[slot]
        swap[old] = sym
        alpha = symbols[old][2]
        if len(hexcolour.lstrip("#")) == 8:
            alpha = int(hexcolour.lstrip("#")[6:8], 16)
        clean = hexcolour.lstrip("#")[:6]
        rgb = tuple(int(clean[k:k + 2], 16) for k in (0, 2, 4))
        new_lines.append("  %s idx=%-3d #%s a=%-3d lum=%6.1f"
                         % (sym, max_idx, clean, alpha, luminance(rgb)))

    out, current, changed = [], None, 0
    for i, line in enumerate(lines):
        if i == grid_at:
            out.extend(new_lines)
        m = FRAME_LINE.match(line)
        if m:
            current = int(m.group(3))
        elif line.startswith("  ") and current in frames and i > (grid_at or 0):
            cells = [line[2:][k:k + 2] for k in range(0, len(line) - 2, 2)]
            swapped = [swap.get(c, c) for c in cells]
            changed += sum(1 for a, b in zip(cells, swapped) if a != b)
            line = "  " + "".join(swapped)
        out.append(line)

    Path(dst).write_text("\n".join(out) + "\n", encoding="utf-8")
    print("recoloured %d pixels across frames %s; %d new palette entries"
          % (changed, sorted(frames), len(new_lines)))


if __name__ == "__main__":
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    opts = dict(a.lstrip("-").split("=", 1) for a in sys.argv[1:] if a.startswith("--"))
    frames = {int(f) for f in opts["frames"].split(",")}
    mapping = dict(p.split(":") for p in opts["map"].split(","))
    recolour(args[0], args[1], frames, mapping)
