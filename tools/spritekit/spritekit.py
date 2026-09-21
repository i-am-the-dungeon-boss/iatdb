"""Shared helpers for the iatdb sprite text round-trip.

A .spr file is a plain-text view of an indexed sprite sheet. Each palette entry
gets a two-character symbol built from a hue family letter and a brightness
level, so shadow, midtone and highlight are legible in the grid itself rather
than hidden behind arbitrary palette indices.
"""

import colorsys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
TRANSPARENT = ".."
HUE_BUCKETS = 12
# Ramp letters used when one hue family outgrows a single level alphabet.
LEVEL_SPILL = ("nopqrstuvwxyz"
               "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
               "0123456789"
               "!$%&*+-/<=>?@^_~")
GREY_SAT = 0.12


def luminance(rgb):
    return 0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2]


def hue_family(rgba):
    """Group a colour into a ramp. Achromatic colours share the 'k' family."""
    r, g, b = (c / 255 for c in rgba[:3])
    h, _, s = colorsys.rgb_to_hls(r, g, b)
    if s < GREY_SAT:
        return "k"
    bucket = int(h * HUE_BUCKETS + 0.5) % HUE_BUCKETS
    return "abcdefghijlm"[bucket]


def alpha_for_index(transparency, index):
    """PNG transparency is either a single index or a per-index alpha table."""
    if transparency is None:
        return 255
    if isinstance(transparency, (bytes, bytearray)):
        return transparency[index] if index < len(transparency) else 255
    return 0 if index == transparency else 255


def read_palette(im):
    """Return (index -> RGBA) for the image, plus the mode we must rebuild."""
    if im.mode == "P":
        raw = im.getpalette() or []
        transparency = im.info.get("transparency")
        entries = {}
        for i in range(len(raw) // 3):
            r, g, b = raw[3 * i], raw[3 * i + 1], raw[3 * i + 2]
            entries[i] = (r, g, b, alpha_for_index(transparency, i))
        return entries, "P", transparency
    rgba = im.convert("RGBA")
    colours = sorted({c for c in rgba.getdata()}, key=lambda c: (c[3], luminance(c)))
    return dict(enumerate(colours)), "RGBA", None


def assign_symbols(palette, used):
    """Map palette index -> two-char symbol, ordered dark to light per family."""
    # Only one fully transparent colour can own the '..' symbol. A sheet may
    # hold several distinct RGB values at alpha 0 -- invisible, but they must
    # still round trip exactly, so the rest get ordinary symbols.
    blanks = [i for i in used if palette[i][3] == 0]
    canonical = min(blanks) if blanks else None
    families = {}
    for idx in used:
        if idx == canonical:
            continue
        families.setdefault(hue_family(palette[idx]), []).append(idx)
    levels = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    spare = [c for c in LEVEL_SPILL if c not in families]
    symbols = {}
    for fam in sorted(families):
        indices = sorted(families[fam], key=lambda i: luminance(palette[i]))
        # A family wider than one level alphabet spills into extra ramp letters,
        # still ordered dark to light so the split stays readable.
        for start in range(0, len(indices), len(levels)):
            chunk = indices[start:start + len(levels)]
            if start and not spare:
                raise ValueError("palette too large to symbolise: %d colours"
                                 % len(used))
            letter = fam if start == 0 else spare.pop(0)
            for level, idx in enumerate(chunk):
                symbols[idx] = letter + levels[level]
    if canonical is not None:
        symbols[canonical] = TRANSPARENT
    return symbols


def canonical_blank(palette, used):
    """The palette index that '..' stands for, or None on an opaque sheet."""
    blanks = [i for i in used if palette[i][3] == 0]
    return min(blanks) if blanks else None


def frame_size(im, override):
    if override:
        w, h = override.lower().split("x")
        return int(w), int(h)
    w, h = im.size
    return (h, h) if w % h == 0 else (w, h)


def indexed_pixels(im):
    """Yield the image as a 2D list of palette indices."""
    if im.mode == "P":
        src = im
    else:
        rgba = im.convert("RGBA")
        palette, _, _ = read_palette(rgba)
        lookup = {c: i for i, c in palette.items()}
        w, h = rgba.size
        data = list(rgba.getdata())
        return [[lookup[data[y * w + x]] for x in range(w)] for y in range(h)]
    w, h = src.size
    data = list(src.getdata())
    return [[data[y * w + x] for x in range(w)] for y in range(h)]
