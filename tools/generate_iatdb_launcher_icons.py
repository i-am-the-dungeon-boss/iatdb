import struct
from io import BytesIO
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
icons = Image.open(ROOT / "core/src/main/assets/interfaces/icons.png").convert("RGBA")
# IatdbIconFrame: X=153, Y=0, W=16, H=16
mark = icons.crop((153, 0, 169, 16))

BG = (26, 20, 36, 255)  # #1a1424
BLACK = (0, 0, 0, 255)
TITLE_COLOR = (230, 225, 210, 255)
FG_PAD_RATIO = 0.18

densities = {
    "ldpi": 36,
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

android_roots = [
    ROOT / "android/src/main/res",
    ROOT / "android/src/debug/res",
]

# filename -> pixel size (points * scale)
ios_app_icons = {
    "Icon-20.png": 20,
    "Icon-20@2x.png": 40,
    "Icon-20@3x.png": 60,
    "Icon-29.png": 29,
    "Icon-29@2x.png": 58,
    "Icon-29@3x.png": 87,
    "Icon-40.png": 40,
    "Icon-40@2x.png": 80,
    "Icon-40@3x.png": 120,
    "Icon-60@2x.png": 120,
    "Icon-60@3x.png": 180,
    "Icon-76.png": 76,
    "Icon-76@2x.png": 152,
    "Icon-83.5@2x.png": 167,
    "Icon-1024.png": 1024,
}

# LaunchScreen imagesets: 1x logical size from storyboard resources
ios_banners = {
    ROOT / "ios/assets/Assets.xcassets/Banner.imageset": {
        "Banner.png": (420, 300),
        "Banner@2x.png": (840, 600),
        "Banner@3x.png": (1260, 900),
    },
    ROOT / "ios/assets/Assets.xcassets/BannerWide.imageset": {
        "BannerWide.png": (720, 180),
        "BannerWide@2x.png": (1440, 360),
        "BannerWide@3x.png": (2160, 540),
    },
}


def nearest(img: Image.Image, size: int) -> Image.Image:
    return img.resize((size, size), Image.Resampling.NEAREST)


def app_icon(size: int) -> Image.Image:
    canvas = Image.new("RGBA", (size, size), BG)
    pad = max(1, int(size * FG_PAD_RATIO))
    inner = max(1, size - 2 * pad)
    scaled = nearest(mark, inner)
    canvas.paste(scaled, (pad, pad), scaled)
    return canvas


def adaptive_foreground(size: int) -> Image.Image:
    fg = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    fg_inner = max(1, int(size * 0.66))
    scaled_fg = nearest(mark, fg_inner)
    ox = (size - fg_inner) // 2
    fg.paste(scaled_fg, (ox, ox), scaled_fg)
    return fg


def load_pixel_font(size: int) -> ImageFont.FreeTypeFont:
    path = ROOT / "core/src/main/assets/fonts/pixel_font.ttf"
    return ImageFont.truetype(str(path), size=size)


def draw_centered_text(draw: ImageDraw.ImageDraw, text: str, y: int, width: int, font, fill):
    bbox = draw.textbbox((0, 0), text, font=font)
    tw = bbox[2] - bbox[0]
    x = (width - tw) // 2
    draw.text((x, y), text, font=font, fill=fill)


def launch_banner(width: int, height: int, wide: bool) -> Image.Image:
    """Black splash matching Android cold-start, with IATDB mark + title text."""
    canvas = Image.new("RGBA", (width, height), BLACK)
    draw = ImageDraw.Draw(canvas)

    # Keep mark on an integer pixel grid for crisp nearest-neighbor upscale.
    mark_size = max(32, (height * 2 // 5) if not wide else (height * 3 // 5))
    mark_size = mark_size - (mark_size % 16)  # multiple of source 16px
    mark_size = max(16, mark_size)
    scaled = nearest(mark, mark_size)

    if wide:
        # Mark left-of-center, title to the right-ish / or mark above tiny text
        title_font = load_pixel_font(max(12, height // 5))
        sub_font = load_pixel_font(max(10, height // 7))
        gap = max(8, width // 40)
        line1 = "I AM THE"
        line2 = "DUNGEON BOSS"
        # Measure text block
        b1 = draw.textbbox((0, 0), line1, font=sub_font)
        b2 = draw.textbbox((0, 0), line2, font=title_font)
        text_w = max(b1[2] - b1[0], b2[2] - b2[0])
        text_h = (b1[3] - b1[1]) + max(4, height // 20) + (b2[3] - b2[1])
        block_w = mark_size + gap + text_w
        x0 = (width - block_w) // 2
        my = (height - mark_size) // 2
        canvas.paste(scaled, (x0, my), scaled)
        tx = x0 + mark_size + gap
        ty = (height - text_h) // 2
        draw.text((tx, ty), line1, font=sub_font, fill=TITLE_COLOR)
        draw.text((tx, ty + (b1[3] - b1[1]) + max(4, height // 20)), line2, font=title_font, fill=TITLE_COLOR)
    else:
        title_font = load_pixel_font(max(14, height // 12))
        sub_font = load_pixel_font(max(12, height // 16))
        my = max(8, (height // 2) - mark_size - max(12, height // 20))
        mx = (width - mark_size) // 2
        canvas.paste(scaled, (mx, my), scaled)
        y = my + mark_size + max(10, height // 24)
        draw_centered_text(draw, "I AM THE", y, width, sub_font, TITLE_COLOR)
        b = draw.textbbox((0, 0), "I AM THE", font=sub_font)
        y2 = y + (b[3] - b[1]) + max(4, height // 40)
        draw_centered_text(draw, "DUNGEON BOSS", y2, width, title_font, TITLE_COLOR)

    return canvas


def generate_android():
    for root in android_roots:
        for density, size in densities.items():
            folder = root / f"mipmap-{density}"
            folder.mkdir(parents=True, exist_ok=True)

            app_icon(size).save(folder / "ic_launcher.png")
            Image.new("RGBA", (size, size), BG).save(folder / "ic_launcher_background.png")
            fg = adaptive_foreground(size)
            fg.save(folder / "ic_launcher_foreground.png")

            mono = Image.new("RGBA", (size, size), (0, 0, 0, 0))
            fg_inner = max(1, int(size * 0.66))
            scaled_fg = nearest(mark, fg_inner)
            ox = (size - fg_inner) // 2
            white = Image.new("RGBA", (fg_inner, fg_inner), (255, 255, 255, 255))
            white.putalpha(scaled_fg.split()[3])
            mono.paste(white, (ox, ox), white)
            mono.save(folder / "ic_launcher_monochrome.png")


def generate_ios():
    icon_dir = ROOT / "ios/assets/Assets.xcassets/AppIcon.appiconset"
    icon_dir.mkdir(parents=True, exist_ok=True)
    for filename, size in ios_app_icons.items():
        app_icon(size).save(icon_dir / filename)

    for folder, files in ios_banners.items():
        folder.mkdir(parents=True, exist_ok=True)
        wide = "BannerWide" in folder.name
        for filename, (w, h) in files.items():
            launch_banner(w, h, wide=wide).save(folder / filename)


# Desktop window icons + packaging icons (see DesktopLauncher / desktop/build.gradle)
desktop_png_sizes = (16, 32, 48, 64, 128, 256)
desktop_ico_sizes = (16, 32, 48, 64, 128, 256)
desktop_icns_sizes = (16, 32, 64, 128, 256, 512)


def png_bytes(img: Image.Image) -> bytes:
    buf = BytesIO()
    img.save(buf, format="PNG")
    return buf.getvalue()


def save_ico(path: Path, images: list[Image.Image]):
    """Write a multi-size ICO with embedded PNGs (keeps nearest-neighbor pixels)."""
    images = sorted(images, key=lambda im: im.size[0])
    payloads = [png_bytes(im) for im in images]
    # ICONDIR + N * ICONDIRENTRY (16 bytes) then image blobs
    offset = 6 + 16 * len(images)
    entries = bytearray()
    for img, payload in zip(images, payloads):
        w, h = img.size
        entries += struct.pack(
            "<BBBBHHII",
            0 if w >= 256 else w,
            0 if h >= 256 else h,
            0,  # color palette
            0,  # reserved
            1,  # color planes
            32,  # bits per pixel
            len(payload),
            offset,
        )
        offset += len(payload)
    with path.open("wb") as f:
        f.write(struct.pack("<HHH", 0, 1, len(images)))
        f.write(entries)
        for payload in payloads:
            f.write(payload)


def generate_desktop():
    icon_dir = ROOT / "desktop/src/main/assets/icons"
    icon_dir.mkdir(parents=True, exist_ok=True)

    pngs = {size: app_icon(size) for size in desktop_png_sizes}
    for size, img in pngs.items():
        img.save(icon_dir / f"icon_{size}.png")

    save_ico(icon_dir / "windows.ico", [pngs[s] for s in desktop_ico_sizes])

    # Pillow ICNS writer accepts append_images for extra resolutions
    icns_imgs = [app_icon(size) for size in desktop_icns_sizes]
    icns_imgs[0].save(
        icon_dir / "mac.icns",
        format="ICNS",
        append_images=icns_imgs[1:],
    )


if __name__ == "__main__":
    generate_android()
    generate_ios()
    generate_desktop()
    print("android + ios + desktop icons/banners generated")

