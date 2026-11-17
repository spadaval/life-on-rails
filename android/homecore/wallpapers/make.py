"""Draws the lab's three wallpapers into ../src/wasmJsMain/resources/wallpapers/ (python3, no packages).

dark: deep space with a faint nebula. pale: warm paper, the worst case for light text. mixed: a
bright sky over dark ground, so one screen holds both cases. 540x1260, half the cover screen's
pixels; the lab crops them to either screen.
"""
import math, os, random, struct, zlib

W, H = 540, 1260
OUT = os.path.join(os.path.dirname(__file__), "..", "src", "wasmJsMain", "resources", "wallpapers")

def png(path, px):
    raw = b"".join(b"\0" + bytes(px[y * W * 3:(y + 1) * W * 3]) for y in range(H))
    def chunk(t, d): return struct.pack(">I", len(d)) + t + d + struct.pack(">I", zlib.crc32(t + d))
    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", W, H, 8, 2, 0, 0, 0)) + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))

def lerp(a, b, t): return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))
def clamp(c): return [max(0, min(255, int(round(v)))) for v in c]

def draw(fn):
    px = bytearray(W * H * 3)
    for y in range(H):
        for x in range(W):
            px[(y * W + x) * 3:(y * W + x) * 3 + 3] = bytes(clamp(fn(x, y)))
    return px

def glow(x, y, cx, cy, r): return math.exp(-((x - cx) ** 2 + (y - cy) ** 2) / (2 * r * r))

random.seed(7)
stars = {(random.randrange(W), random.randrange(H)): random.uniform(90, 230) for _ in range(260)}

def dark(x, y):
    c = lerp((10, 12, 22), (3, 3, 6), y / H)
    n = glow(x, y, 380, 420, 170) * 0.9 + glow(x, y, 160, 900, 220) * 0.5
    c = lerp(c, (52, 30, 70), n * 0.6)
    s = stars.get((x, y))
    return lerp(c, (230, 225, 215), s / 255) if s else c

def pale(x, y):
    c = lerp((246, 241, 232), (222, 216, 206), y / H)
    return lerp(c, (255, 252, 246), glow(x, y, 120, 260, 260) * 0.7)

def mixed(x, y):
    horizon = 700 + 40 * math.sin(x / 90)
    if y < horizon:
        c = lerp((150, 196, 236), (250, 244, 228), y / horizon)
        return lerp(c, (255, 252, 240), glow(x, y, 400, 560, 60) * 1.0)
    return lerp((44, 52, 38), (8, 10, 8), (y - horizon) / (H - horizon))

os.makedirs(OUT, exist_ok=True)
for name, fn in [("dark", dark), ("pale", pale), ("mixed", mixed)]:
    png(os.path.join(OUT, name + ".png"), draw(fn))
    print(name, os.path.getsize(os.path.join(OUT, name + ".png")))
