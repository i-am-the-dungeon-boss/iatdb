from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
icons = Image.open(ROOT / "core/src/main/assets/interfaces/icons.png").convert("RGBA")
# IatdbIconFrame: X=153, Y=0, W=16, H=16
mark = icons.crop((153, 0, 169, 16))

BG = (26, 20, 36, 255)  # #1a1424
FG_PAD_RATIO = 0.18

densities = {
    "ldpi": 36,
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

roots = [
    ROOT / "android/src/main/res",
    ROOT / "android/src/debug/res",
]


def nearest(img: Image.Image, size: int) -> Image.Image:
    return img.resize((size, size), Image.Resampling.NEAREST)


for root in roots:
    for density, size in densities.items():
        folder = root / f"mipmap-{density}"
        folder.mkdir(parents=True, exist_ok=True)

        canvas = Image.new("RGBA", (size, size), BG)
        pad = max(1, int(size * FG_PAD_RATIO))
        inner = size - 2 * pad
        scaled = nearest(mark, inner)
        canvas.paste(scaled, (pad, pad), scaled)
        canvas.save(folder / "ic_launcher.png")

        Image.new("RGBA", (size, size), BG).save(folder / "ic_launcher_background.png")

        fg = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        fg_inner = max(1, int(size * 0.66))
        scaled_fg = nearest(mark, fg_inner)
        ox = (size - fg_inner) // 2
        fg.paste(scaled_fg, (ox, ox), scaled_fg)
        fg.save(folder / "ic_launcher_foreground.png")

        mono = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        white = Image.new("RGBA", (fg_inner, fg_inner), (255, 255, 255, 255))
        white.putalpha(scaled_fg.split()[3])
        mono.paste(white, (ox, ox), white)
        mono.save(folder / "ic_launcher_monochrome.png")

print("icons generated")
