#!/usr/bin/env python3
"""Generates placeholder textures + synthesized sounds for THE LAST PLAYER.
Run:  pip install pillow numpy soundfile && python tools/generate_assets.py
Replace the generated sounds with real voice acting / music whenever you like
(keep the same file names and the mod will pick them up)."""
import os, random, math
import numpy as np
import soundfile as sf
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "thelastplayer")
TEX = os.path.join(ROOT, "textures")
SND = os.path.join(ROOT, "sounds")
for d in ("entity", "item", "block"):
    os.makedirs(os.path.join(TEX, d), exist_ok=True)
for d in ("echo", "music"):
    os.makedirs(os.path.join(SND, d), exist_ok=True)
random.seed(7); np.random.seed(7)

# ------------------------------------------------------------------ entity textures
BASES = [(120, 200, 255), (110, 180, 235), (95, 150, 210), (125, 105, 190), (150, 165, 185)]
GLOWS = [(255, 214, 130), (255, 200, 120), (255, 150, 90), (255, 70, 70), (110, 130, 170)]
HEART = [".XX.XX.", "XXXXXXX", ".XXXXX.", "..XXX.."]
FACES = [(8, 8), (0, 8), (16, 8), (24, 8), (8, 0), (16, 0)]  # front,left,right,back,top,bottom

def paint_region(img, x0, y0, w, h, base, alpha=235):
    for x in range(x0, x0 + w):
        for y in range(y0, y0 + h):
            j = random.randint(-9, 9)
            c = tuple(max(0, min(255, v + j)) for v in base)
            if x in (x0, x0 + w - 1) or y in (y0, y0 + h - 1):
                c = tuple(min(255, v + 35) for v in c)
            img.putpixel((x, y), c + (alpha,))

def make_echo(mood):
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    glow = Image.new("RGBA", (64, 32), (0, 0, 0, 255))
    base = BASES[mood]; gc = GLOWS[mood]
    for (x0, y0) in [(8, 8), (0, 8), (16, 8), (24, 8), (8, 0), (16, 0)]:
        paint_region(img, x0, y0, 8, 8, base, 235 if mood < 4 else 200)
    paint_region(img, 32, 0, 8, 7, base)
    dark = (15, 25, 55, 255)
    # eyes on front face (x 8..15, y 8..15)
    eyes = {0: [(9, 9), (10, 9), (9, 10), (10, 10), (12, 9), (13, 9), (12, 10), (13, 10)],
            1: [(9, 9), (10, 9), (9, 10), (10, 10), (12, 9), (13, 9), (12, 10), (13, 10)],
            2: [(9, 8), (10, 8), (9, 9), (10, 9), (9, 10), (10, 10), (12, 8), (13, 8), (12, 9), (13, 9), (12, 10), (13, 10)],
            3: [(9, 8), (10, 8), (9, 9), (10, 9), (9, 10), (10, 10), (12, 10), (13, 10)],
            4: [(9, 10), (10, 10), (12, 10), (13, 10)]}[mood]
    for (x, y) in eyes:
        img.putpixel((x, y), dark)
        glow.putpixel((x, y), (60, 90, 140, 255))
    img.putpixel(eyes[0], (255, 255, 255, 255))
    # heart (chest light)
    for r, row in enumerate(HEART):
        for c, ch in enumerate(row):
            if ch == "X":
                x, y = 8 + c, 12 + r
                img.putpixel((x, y), tuple(min(255, v + 20) for v in gc) + (255,))
                glow.putpixel((x, y), gc + (255,))
    # cracks for damaged moods
    if mood >= 2:
        for (x, y) in [(15, 8), (14, 9), (14, 11), (15, 12), (26, 9), (27, 10), (27, 12), (10, 4), (11, 5), (12, 6)]:
            img.putpixel((x, y), (20, 30, 60, 255))
    if mood >= 3:
        for _ in range(14):
            x, y = random.randint(8, 31), random.randint(8, 15)
            img.putpixel((x, y), (30, 10, 50, 255))
    img.save(os.path.join(TEX, "entity", f"echo_{mood}.png"))
    glow.save(os.path.join(TEX, "entity", f"echo_{mood}_glow.png"))

for m in range(5):
    make_echo(m)

# ------------------------------------------------------------------ item textures
def new16(): return Image.new("RGBA", (16, 16), (0, 0, 0, 0))

shard = new16()
pts = [(8,1),(9,2),(9,3),(10,4),(10,6),(11,8),(11,10),(10,12),(9,14),(8,14),(7,12),(6,10),(5,8),(6,5),(7,3),(7,2)]
for y in range(1, 15):
    for x in range(4, 13):
        # simple blob mask
        if abs(x - 8) <= (6 - abs(y - 7)) * 0.55 + 0.4 and 1 <= y <= 14:
            t = (x - 4) / 8
            c = (int(90 + 110 * t), int(170 + 70 * t), 255, 255)
            shard.putpixel((x, y), c)
for y in range(2, 13): shard.putpixel((7, y), (230, 250, 255, 255))
shard.save(os.path.join(TEX, "item", "blue_glass_shard.png"))

letter = new16()
for x in range(3, 13):
    for y in range(3, 14):
        letter.putpixel((x, y), (222, 208, 170, 255) if (x + y) % 5 else (210, 195, 155, 255))
for y in range(3, 14): letter.putpixel((3, y), (150, 130, 95, 255)); letter.putpixel((12, y), (150, 130, 95, 255))
for x in range(3, 13): letter.putpixel((x, 3), (150, 130, 95, 255)); letter.putpixel((x, 13), (150, 130, 95, 255))
for y in (5, 7, 9, 11):
    for x in range(5, 11 if y != 11 else 8): letter.putpixel((x, y), (80, 65, 45, 255))
letter.putpixel((10, 11), (150, 40, 40, 255)); letter.putpixel((11, 11), (150, 40, 40, 255))
letter.save(os.path.join(TEX, "item", "old_letter.png"))

mem = new16()
for x in range(3, 13):
    for y in range(3, 13):
        mem.putpixel((x, y), (35, 40, 60, 255))
for x in range(3, 13): mem.putpixel((x, 3), (90, 100, 140, 255)); mem.putpixel((x, 12), (90, 100, 140, 255))
for y in range(3, 13): mem.putpixel((3, y), (90, 100, 140, 255)); mem.putpixel((12, y), (90, 100, 140, 255))
for x in range(5, 11):
    for y in range(5, 9): mem.putpixel((x, y), (255, 214, 130, 255) if (x + y) % 2 else (255, 170, 90, 255))
for x in range(6, 10): mem.putpixel((x, 13), (200, 200, 210, 255)); mem.putpixel((x, 14), (200, 200, 210, 255))
mem.save(os.path.join(TEX, "item", "memory_log.png"))

# ------------------------------------------------------------------ block textures
def block(name, fn):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
    for x in range(16):
        for y in range(16):
            img.putpixel((x, y), fn(x, y) + (255,))
    img.save(os.path.join(TEX, "block", name + ".png"))

def metal(x, y, tone=70):
    j = random.randint(-6, 6)
    edge = x in (0, 15) or y in (0, 15)
    v = tone + j + (25 if edge else 0)
    return (v, v + 4, v + 12)

def device(x, y):
    d = math.hypot(x - 7.5, y - 7.5)
    if d < 3.2: return (140, 210, 255)
    if d < 4.6: return (50, 110, 170)
    return metal(x, y, 62)
block("echo_device", device)

def core(x, y):
    d = math.hypot(x - 7.5, y - 7.5)
    if d < 2.8: return (255, 90, 70)
    if d < 4.2: return (255, 190, 120)
    if (x + y) % 7 == 0: return (80, 20, 20)
    return metal(x, y, 40)
block("reset_core", core)

def term_front(x, y):
    if 2 <= x <= 13 and 2 <= y <= 10:
        if y in (3, 5, 7, 9) and 3 <= x <= (12 if y != 9 else 8): return (60, 255, 140)
        return (8, 28, 18)
    return metal(x, y, 50)
block("lab_terminal_front", term_front)
block("lab_terminal_side", lambda x, y: metal(x, y, 55))
block("lab_terminal_top", lambda x, y: metal(x, y, 70))

# ------------------------------------------------------------------ sounds
SR = 22050
def t_(d): return np.linspace(0, d, int(SR * d), False)
def tone(f0, f1, d, vib=0.0, vibf=6.0, harm=(1.0, 0.35, 0.12)):
    t = t_(d); f = np.linspace(f0, f1, len(t)) + vib * np.sin(2 * np.pi * vibf * t)
    ph = 2 * np.pi * np.cumsum(f) / SR
    return sum(a * np.sin((i + 1) * ph) for i, a in enumerate(harm)) / sum(harm)
def env(y, a=0.02, r=0.15):
    n = len(y); e = np.ones(n); na = max(1, int(SR * a)); nr = max(1, int(SR * r))
    e[:na] = np.linspace(0, 1, na); e[-nr:] = np.linspace(1, 0, nr); return y * e
def noise(d): return np.random.uniform(-1, 1, int(SR * d))
def lowpass(y, cutoff):
    S = np.fft.rfft(y); fr = np.fft.rfftfreq(len(y), 1 / SR); S[fr > cutoff] = 0; return np.fft.irfft(S, len(y))
def bitcrush(y, bits=4): q = 2 ** bits; return np.round(y * q) / q
def cat(*parts): return np.concatenate(parts)
def sil(d): return np.zeros(int(SR * d))
def norm(y, peak=0.8): return y / (np.max(np.abs(y)) + 1e-9) * peak
def save(path, y): sf.write(path, norm(y).astype("float32"), SR, format="OGG", subtype="VORBIS")
def S(name, y): save(os.path.join(SND, "echo", name + ".ogg"), y)
def M(name, y): save(os.path.join(SND, "music", name + ".ogg"), y)

S("happy", cat(env(tone(700, 1000, .12), .01, .05), sil(.03), env(tone(900, 1300, .16), .01, .08)))
S("laugh", cat(*[cat(env(tone(850 + 40 * (i % 2), 1000, .09, 20, 30), .005, .04), sil(.05)) for i in range(6)]))
S("curious", cat(env(tone(500, 750, .18, 15, 8), .02, .06), sil(.04), env(tone(800, 1000, .12), .01, .08)))
S("sad", env(tone(560, 330, 1.8, 8, 5), .1, .8) * (0.75 + 0.25 * np.sin(2 * np.pi * 5 * t_(1.8))))
S("fear", env(tone(900, 760, 1.4, 45, 14), .05, .4) * (0.6 + 0.4 * np.sin(2 * np.pi * 18 * t_(1.4))) + 0.05 * noise(1.4))
S("breakdown", env(bitcrush(tone(700, 160, 3.0, 60, 11, (1, .6, .4)) * (0.6 + 0.4 * np.sin(2 * np.pi * 9 * t_(3.0))) + 0.25 * noise(3.0), 4), .02, .6))
g = np.zeros(int(SR * 1.2))
for _ in range(14):
    s = random.randint(0, len(g) - 2000); l = random.randint(300, 2000); g[s:s + l] = bitcrush(np.random.uniform(-1, 1, l), random.randint(2, 4)) * random.uniform(.4, 1)
S("glitch", g)
S("whisper", env(lowpass(noise(2.2), 3800) * (0.5 + 0.5 * np.abs(np.sin(2 * np.pi * 2.2 * t_(2.2)))), .3, .8))

def pad(freqs, d, amp=1.0, det=0.4):
    t = t_(d); y = 0
    for f in freqs:
        y = y + np.sin(2 * np.pi * f * t) + 0.6 * np.sin(2 * np.pi * (f + det) * t) + 0.2 * np.sin(2 * np.pi * 2 * f * t)
    return env(y / len(freqs) * amp, 2.5, 3.0)
def pluck(f, d=1.2):
    t = t_(d); return np.sin(2 * np.pi * f * t) * np.exp(-3.0 * t) + 0.3 * np.sin(2 * np.pi * 2 * f * t) * np.exp(-5 * t)
def place(buf, y, at):
    i = int(SR * at); n = min(len(y), len(buf) - i)
    if n > 0: buf[i:i + n] += y[:n]

D = 28.0
buf = np.zeros(int(SR * D)); place(buf, pad([220, 261.6, 329.6], D), 0)
for k, f in enumerate([440, 523.3, 659.3, 523.3, 392, 440, 587.3, 440]): place(buf, pluck(f) * .5, 1 + k * 3.2)
M("calm", buf)

buf = pad([110, 155.6], D, 1.0, 0.8) + 0.2 * lowpass(noise(D), 400) * (0.5 + 0.5 * np.sin(2 * np.pi * .15 * t_(D)))
for k in range(7): place(buf, pluck(311.1 + (k % 3) * 9, 2) * .35, 2 + k * 3.6)
M("uneasy", buf)

buf = pad([55, 58, 82.4], D, 1.2, 0.3) + 0.25 * lowpass(noise(D), 250 + 0) * (0.4 + 0.6 * np.abs(np.sin(2 * np.pi * .08 * t_(D))))
place(buf, env(tone(900, 905, 3, 3, 0.3, (1, 0, 0)) * .05, 1, 1.5), 9); place(buf, bitcrush(noise(.4), 2) * .12, 17); place(buf, bitcrush(noise(.25), 2) * .1, 22)
M("dread", buf)

buf = pad([146.8, 174.6, 220], D, .8)
mel = [(440, 0), (392, 3.5), (349.2, 7), (392, 10.5), (293.7, 14), (329.6, 18), (261.6, 22)]
for f, a in mel: place(buf, pluck(f, 3.0) * .6, a + 1)
M("sorrow", buf)

buf = pad([110, 130.8, 164.8, 220], D, 1.0, .5)
hb = np.concatenate([np.exp(-30 * t_(.12)) * np.sin(2 * np.pi * 55 * t_(.12)), np.zeros(int(SR * .1)), 0.7 * np.exp(-30 * t_(.12)) * np.sin(2 * np.pi * 50 * t_(.12))])
k = 0.0; rate = 1.0
while k < D - 1: place(buf, hb * 1.3, k); k += rate; rate = max(.45, rate - .012)
M("final", buf)

amb = env(pad([55, 82.5], 6, 1.0, .25), 1, 1)
S("ambient_hum", amb)
print("assets ok")
