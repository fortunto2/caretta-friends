#!/usr/bin/env python3
"""Compose Google Play phone screenshots from raw EN app captures.

Play rules: each side 320..3840 px, aspect within 16:9..9:16, and the longer
side must be <= 2x the shorter. We use 1080x2160 (exactly 2x) with a sea
gradient, a bold caption, and the app screenshot floating with rounded corners.
"""
from PIL import Image, ImageDraw, ImageFont, ImageFilter
from pathlib import Path

HERE = Path(__file__).parent
RAW = HERE / "raw"
OUT = HERE / "play"
OUT.mkdir(exist_ok=True)

W, H = 1080, 2160
TOP = (18, 138, 146)     # sea
BOT = (10, 74, 80)       # deep sea
TITLE_F = "/System/Library/Fonts/Supplemental/Arial Bold.ttf"
SUB_F = "/System/Library/Fonts/Supplemental/Arial.ttf"

SHOTS = [
    ("en_01_map.png",     "Every nest on one shared map", "Snap a photo — it's pinned on the map"),
    ("en_02_learn.png",   "Field guides & turtle facts",  "What to do the moment you find a nest"),
    ("en_03_beaches.png", "Protected nesting beaches",    "Know your patrol stretch by heart"),
    ("en_05_nest.png",    "Hatch countdown & updates",    "Follow every nest to the sea"),
]


def gradient(w, h, top, bot):
    base = Image.new("RGB", (w, h), top)
    draw = ImageDraw.Draw(base)
    for y in range(h):
        t = y / h
        draw.line([(0, y), (w, y)],
                  fill=tuple(int(top[i] + (bot[i] - top[i]) * t) for i in range(3)))
    return base


def rounded(img, radius):
    mask = Image.new("L", img.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, img.size[0], img.size[1]], radius, fill=255)
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    out.paste(img, (0, 0), mask)
    return out


def wrap(draw, text, font, max_w):
    words, lines, cur = text.split(), [], ""
    for w in words:
        test = (cur + " " + w).strip()
        if draw.textlength(test, font=font) <= max_w:
            cur = test
        else:
            lines.append(cur); cur = w
    if cur:
        lines.append(cur)
    return lines


def centered(draw, lines, font, y, fill, gap):
    for ln in lines:
        w = draw.textlength(ln, font=font)
        draw.text(((W - w) / 2, y), ln, font=font, fill=fill)
        y += font.size + gap
    return y


def compose(raw_name, title, sub, idx):
    canvas = gradient(W, H, TOP, BOT)
    draw = ImageDraw.Draw(canvas)
    title_font = ImageFont.truetype(TITLE_F, 66)
    sub_font = ImageFont.truetype(SUB_F, 40)

    y = 96
    y = centered(draw, wrap(draw, title, title_font, W - 150), title_font, y, (255, 255, 255), 8)
    y += 8
    y = centered(draw, wrap(draw, sub, sub_font, W - 180), sub_font, y, (215, 235, 233), 6)

    shot = Image.open(RAW / raw_name).convert("RGB")
    top_area = int(y) + 44
    avail_h = H - top_area - 70
    max_w = int(W * 0.80)
    scale = min(max_w / shot.width, avail_h / shot.height)
    sw, sh = int(shot.width * scale), int(shot.height * scale)
    shot = shot.resize((sw, sh), Image.LANCZOS)
    shot = rounded(shot, 46)

    x = (W - sw) // 2
    yimg = top_area
    # soft shadow
    shadow = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    sd = ImageDraw.Draw(shadow)
    sd.rounded_rectangle([x, yimg + 14, x + sw, yimg + sh + 14], 46, fill=(0, 0, 0, 90))
    shadow = shadow.filter(ImageFilter.GaussianBlur(26))
    canvas = Image.alpha_composite(canvas.convert("RGBA"), shadow)
    canvas.alpha_composite(shot, (x, yimg))

    out = OUT / f"{idx:02d}_{raw_name.replace('en_', '').replace('.png', '')}_play.png"
    canvas.convert("RGB").save(out, "PNG")
    print("wrote", out.name, canvas.size)


for i, (raw, title, sub) in enumerate(SHOTS, 1):
    compose(raw, title, sub, i)
print("done →", OUT)
