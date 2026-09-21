"""Recover frame geometry and animation names from the Java sprite classes.

Guessing a frame size from the sheet dimensions is wrong more often than not
(RatSprite is 16x15, not 16x16), so the film is read from the source of truth:
the TextureFilm constructor and Animation.frames() calls in the sprite class.
"""

import re
from pathlib import Path

from spritekit import ROOT

JAVA = ROOT / "core/src/main/java/com/shatteredpixel/shatteredpixeldungeon"
ASSET_CONST = re.compile(r'String\s+(\w+)\s*=\s*"([^"]+\.png)"')
TEXTURE = re.compile(r"texture\(\s*Assets\.\w+\.(\w+)\s*\)")
FILM = re.compile(r"new\s+TextureFilm\(\s*texture\s*,\s*(\d+)\s*,\s*(\d+)\s*\)")
ANIM = re.compile(r"(\w+)\s*\.frames\(\s*frames\s*,\s*([0-9,\s]+)\)")


def asset_paths():
    """Assets.java constant name -> asset path."""
    text = (JAVA / "Assets.java").read_text(encoding="utf-8")
    return {name: path for name, path in ASSET_CONST.findall(text)}


def film_for(asset_path):
    """Return the film for a sheet, or None if no sprite class references it.

    A sheet is often shared by several classes at different frame offsets (the
    rat sheet carries the rat, the albino and the sewer rat), so every matching
    class is collected and its animations kept under that class's name.
    """
    consts = asset_paths()
    wanted = [n for n, p in consts.items() if p == asset_path.replace("\\", "/")]
    if not wanted:
        return None
    frame, classes = None, {}
    for java in sorted((JAVA / "sprites").rglob("*.java")):
        text = java.read_text(encoding="utf-8")
        tex = TEXTURE.search(text)
        if not tex or tex.group(1) not in wanted:
            continue
        film = FILM.search(text)
        if film:
            size = (int(film.group(1)), int(film.group(2)))
            if frame is not None and size != frame:
                raise ValueError("%s: %s disagrees on frame size %s vs %s"
                                 % (asset_path, java.name, size, frame))
            frame = size
        animations = {}
        for name, frames in ANIM.findall(text):
            animations[name] = [int(f) for f in frames.replace(" ", "").split(",") if f]
        if animations:
            classes[java.stem] = animations
    if frame is None:
        return None
    return {"classes": classes, "frame": frame,
            "animations": {"%s.%s" % (cls, a): f
                           for cls, anims in classes.items()
                           for a, f in anims.items()}}


def frame_labels(film, columns):
    """frame index -> 'idle[1]' style labels, for annotating dumps and previews."""
    labels = {}
    for name, indices in sorted(film["animations"].items()):
        for slot, frame in enumerate(indices):
            labels.setdefault(frame, []).append("%s[%d]" % (name, slot))
    return labels


if __name__ == "__main__":
    import sys
    f = film_for(sys.argv[1])
    if f is None:
        print("no film found for %s" % sys.argv[1])
    else:
        print("frame=%dx%d  classes: %s"
              % (*f["frame"], ", ".join(sorted(f["classes"]))))
        for name, idx in sorted(f["animations"].items()):
            print("  %-28s %s" % (name, idx))
