"""Графика для Google Play: feature graphic 1024x500 и иконка 512x512 (без прозрачности)."""
import os, sys
from PIL import Image, ImageDraw, ImageFont

root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(root, "app", "screenshots", "play")
os.makedirs(out, exist_ok=True)
CREAM, INK, MUTED = (0xEC, 0xE6, 0xDC), (0x14, 0x21, 0x1F), (0x5F, 0x6D, 0x6A)
fonts = os.path.join(root, "app", "src", "main", "res", "font")
eye = Image.open(os.path.join(root, "store", "eye-foreground.png")).convert("RGBA")

# иконка: глаз в тех же пропорциях, что у адаптивной иконки (видимая часть 72 из 108)
icon = Image.new("RGB", (512, 512), CREAM)
s = int(512 * 1.15)  # глаз занимает ~3/4 ширины: запас под скругление, которое рисует Play
big = eye.resize((s, s), Image.LANCZOS)
icon.paste(big, ((512 - s) // 2, (512 - s) // 2), big)
icon.save(os.path.join(out, "icon-512.png"))

# feature graphic: глаз слева, справа название и строка
fg = Image.new("RGB", (1024, 500), CREAM)
crop = eye.crop((150, 280, 1100, 990))  # глаз с мягкой тенью
h = 330
w = int(crop.width * h / crop.height)
crop = crop.resize((w, h), Image.LANCZOS)
x0 = 36
fg.paste(crop, (x0, (500 - h) // 2), crop)
d = ImageDraw.Draw(fg)
title = ImageFont.truetype(os.path.join(fonts, "unbounded_bold.ttf"), 88)
line = ImageFont.truetype(os.path.join(fonts, "onest_medium.ttf"), 34)
tx = x0 + w + 28
maxw = 1024 - tx - 40
# подпись переносим по словам
lines = ["Все переписки и клиенты", "в одном окне"]
if d.textlength(lines[0], font=line) > maxw:
    lines = ["Все переписки", "и клиенты в одном окне"]
th = 88 + 24 + len(lines) * 44
y = (500 - th) // 2 - 6
d.text((tx, y), "Sguardo", font=title, fill=INK)
y += 88 + 30
for l in lines:
    d.text((tx, y), l, font=line, fill=MUTED); y += 44
fg.save(os.path.join(out, "feature-graphic.png"))
print("ok", out)

# Play принимает скриншоты в 24-битном PNG без альфа-канала
for name in sorted(os.listdir(out)):
    if name[0].isdigit() and name.endswith(".png"):
        p = os.path.join(out, name)
        im = Image.open(p)
        if im.mode != "RGB":
            bg = Image.new("RGB", im.size, CREAM)
            bg.paste(im, mask=im.split()[3] if im.mode == "RGBA" else None)
            bg.save(p)
print("скриншоты без альфы")
