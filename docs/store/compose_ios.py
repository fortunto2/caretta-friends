#!/usr/bin/env python3
"""Compose App Store 6.9" iPhone screenshots (1320×2868) from native iOS captures.

Usage:  ./compose_ios.py [lang ...]      (default: every language below)

Raw captures live in ios-raw/<lang>/ios_<tab>.png and are produced by launching the
app in the simulator with -AppleLanguages "(<lang>)" and -startTab <tab>; the app
picks its interface language from the device and opens the requested tab, so no
tapping is needed. Composed output goes to ios/<lang>/.
"""
import sys
from PIL import Image, ImageDraw, ImageFont, ImageFilter
from pathlib import Path

HERE = Path(__file__).parent
RAW_ROOT = HERE / "ios-raw"
OUT_ROOT = HERE / "ios"

W, H = 1320, 2868
TOP = (18, 138, 146)
BOT = (10, 74, 80)
TITLE_F = "/System/Library/Fonts/Supplemental/Arial Bold.ttf"
SUB_F = "/System/Library/Fonts/Supplemental/Arial.ttf"

# One caption set per App Store locale. Order defines the order on the product page.
CAPTIONS = {
    "en": [
        ("ios_map.png",     "Every nest on one shared map", "Snap a photo — it's pinned on the map"),
        ("ios_beaches.png", "Protected nesting beaches",    "Sorted by distance from where you stand"),
        ("ios_learn.png",   "Field guides & turtle facts",  "In English, Turkish & Russian"),
        ("ios_profile.png", "Track your impact",            "Hatchlings you helped reach the sea"),
    ],
    "ru": [
        ("ios_map.png",     "Все гнёзда на одной карте",   "Сфотографировал — точка уже на карте"),
        ("ios_beaches.png", "Пляжи под охраной",           "По расстоянию от того места, где ты стоишь"),
        ("ios_learn.png",   "Полевой гайд и факты",        "На русском, турецком и английском"),
        ("ios_profile.png", "Твой вклад — в цифрах",       "Черепашата, которым ты помог дойти до моря"),
    ],
    "tr": [
        ("ios_map.png",     "Bütün yuvalar tek haritada",  "Fotoğrafı çek — yuva haritaya işlenir"),
        ("ios_beaches.png", "Koruma altındaki plajlar",    "Bulunduğun yere en yakından sıralı"),
        ("ios_learn.png",   "Saha rehberi ve bilgiler",    "Türkçe, İngilizce ve Rusça"),
        ("ios_profile.png", "Katkını takip et",            "Denize ulaşmasına yardım ettiğin yavrular"),
    ],
}


def gradient(w, h, top, bot):
    base = Image.new("RGB", (w, h), top)
    d = ImageDraw.Draw(base)
    for y in range(h):
        t = y / h
        d.line([(0, y), (w, y)], fill=tuple(int(top[i] + (bot[i] - top[i]) * t) for i in range(3)))
    return base


def rounded(img, r):
    m = Image.new("L", img.size, 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, img.size[0], img.size[1]], r, fill=255)
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    out.paste(img, (0, 0), m)
    return out


def wrap(d, text, font, max_w):
    words, lines, cur = text.split(), [], ""
    for w in words:
        t = (cur + " " + w).strip()
        if d.textlength(t, font=font) <= max_w:
            cur = t
        else:
            lines.append(cur); cur = w
    if cur:
        lines.append(cur)
    return lines


def centered(d, lines, font, y, fill, gap):
    for ln in lines:
        w = d.textlength(ln, font=font)
        d.text(((W - w) / 2, y), ln, font=font, fill=fill)
        y += font.size + gap
    return y


def compose(raw, title, sub, idx, raw_dir, out_dir):
    canvas = gradient(W, H, TOP, BOT)
    d = ImageDraw.Draw(canvas)
    tf = ImageFont.truetype(TITLE_F, 74)
    sf = ImageFont.truetype(SUB_F, 44)
    y = 110
    y = centered(d, wrap(d, title, tf, W - 170), tf, y, (255, 255, 255), 8)
    y += 10
    y = centered(d, wrap(d, sub, sf, W - 200), sf, y, (215, 235, 233), 6)

    shot = Image.open(raw_dir / raw).convert("RGB")
    top_area = int(y) + 54
    avail_h = H - top_area - 80
    max_w = int(W * 0.84)
    scale = min(max_w / shot.width, avail_h / shot.height)
    sw, sh = int(shot.width * scale), int(shot.height * scale)
    shot = rounded(shot.resize((sw, sh), Image.LANCZOS), 54)
    x = (W - sw) // 2
    yimg = top_area
    shadow = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle([x, yimg + 16, x + sw, yimg + sh + 16], 54, fill=(0, 0, 0, 95))
    shadow = shadow.filter(ImageFilter.GaussianBlur(30))
    canvas = Image.alpha_composite(canvas.convert("RGBA"), shadow)
    canvas.alpha_composite(shot, (x, yimg))
    out = out_dir / f"{idx:02d}_{raw.replace('ios_', '').replace('.png', '')}_appstore.png"
    canvas.convert("RGB").save(out, "PNG")
    print("  wrote", out.name, canvas.size)


langs = sys.argv[1:] or list(CAPTIONS)
for lang in langs:
    raw_dir = RAW_ROOT / lang
    out_dir = OUT_ROOT / lang
    if not raw_dir.is_dir():
        print(f"skip {lang}: no {raw_dir}")
        continue
    out_dir.mkdir(parents=True, exist_ok=True)
    print(lang)
    for i, (r, t, s) in enumerate(CAPTIONS[lang], 1):
        compose(r, t, s, i, raw_dir, out_dir)
print("done →", OUT_ROOT)
