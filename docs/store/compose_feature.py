#!/usr/bin/env python3
"""Compose the Google Play feature graphic (1024x500) from the current app icon.

Regenerated whenever the icon changes — the previous one still carried the flat green turtle
months after the hatchling icon shipped, because the graphic bakes the icon in and nothing links
the two. Same sea gradient and typography as the store screenshots (compose_play.py), so the
listing reads as one set.
"""
from PIL import Image, ImageDraw, ImageFont, ImageFilter
from pathlib import Path

HERE = Path(__file__).parent
ICON = HERE / "android" / "graphics" / "icon-512.png"
OUT = HERE / "android" / "graphics" / "feature-graphic-1024x500.png"

W, H = 1024, 500
TOP = (18, 138, 146)     # sea — same as the screenshots
BOT = (10, 74, 80)       # deep sea
TITLE_F = "/System/Library/Fonts/Supplemental/Arial Bold.ttf"
SUB_F = "/System/Library/Fonts/Supplemental/Arial.ttf"

TITLE = "Caretta Friends"
SUBTITLE = "Protect sea-turtle nests"


def gradient(w: int, h: int, top: tuple, bot: tuple) -> Image.Image:
    img = Image.new("RGB", (w, h))
    draw = ImageDraw.Draw(img)
    for y in range(h):
        t = y / max(h - 1, 1)
        draw.line(
            [(0, y), (w, y)],
            fill=tuple(round(top[i] + (bot[i] - top[i]) * t) for i in range(3)),
        )
    return img


def main() -> None:
    canvas = gradient(W, H, TOP, BOT)

    # Icon on the left, with a soft drop shadow so it lifts off the gradient.
    side = 300
    icon = Image.open(ICON).convert("RGBA").resize((side, side), Image.LANCZOS)
    # The store icon is a rounded square painted onto WHITE, not onto transparency — pasted as-is
    # it shows four white corners against the gradient. Re-cut the corners.
    mask = Image.new("L", (side, side), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, side - 1, side - 1], radius=round(side * 0.22), fill=255)
    icon.putalpha(mask)
    x, y = 78, (H - side) // 2

    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    shadow.paste(Image.new("RGBA", (side, side), (0, 0, 0, 110)), (x, y + 10), icon)
    canvas.paste(
        Image.alpha_composite(canvas.convert("RGBA"), shadow.filter(ImageFilter.GaussianBlur(18))).convert("RGB"),
        (0, 0),
    )
    canvas.paste(icon, (x, y), icon)

    draw = ImageDraw.Draw(canvas)
    title = ImageFont.truetype(TITLE_F, 72)
    sub = ImageFont.truetype(SUB_F, 34)

    tx = x + side + 60
    # Two title lines, so the words never crowd the right edge on a narrow crop.
    draw.text((tx, 150), "Caretta", font=title, fill=(255, 255, 255))
    draw.text((tx, 232), "Friends", font=title, fill=(255, 255, 255))
    draw.text((tx, 330), SUBTITLE, font=sub, fill=(226, 240, 238))

    canvas.save(OUT, "PNG")
    print(f"{OUT}  {canvas.size[0]}x{canvas.size[1]}")


if __name__ == "__main__":
    main()
