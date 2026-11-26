# Этап 8 и 10: клубные формы (иконки + HD на игроке) и иконка комплекта стадиона.
# Запуск из корня репозитория:  python tools/textures/stage8.py src/main/resources
import os
import sys

SP = os.path.dirname(os.path.abspath(__file__)) + '/'
exec(open(SP + 'kits.py', encoding='utf-8').read().split('# ---------------- armor')[0])
_leg = open(SP + 'legends.py', encoding='utf-8').read()
exec(_leg[_leg.index('FONT.update'):_leg.index('EYE=')])
exec(open(SP + 'clubs.py', encoding='utf-8').read())
from PIL import Image

JERSEY = ["................", "...aa.aaaa.aa...", "..abbaccccabba..", ".abbbbbccbbbbba.", "abbbbbbbbbbbbbba",
          "abbabbbbbbbbabba", ".aaabbbbbbbbaaa.", "...abbdddbbba...", "...abbbbdbbba...", "...abbbdbbbba...",
          "...abbbdbbbba...", "...abbbdbbbba...", "...abbbbbbbba...", "...aaaaaaaaaa...", "................", "................"]
SHORTS = ["................", "................", "................", "...aaaaaaaaaa...", "...abbbbbbbba...",
          "...acccccccca...", "..acccccccccca..", "..accccaacccca..", "..acccca.accca..", "..accca..accca..",
          ".acccca..acccca.", ".acccca..acccca.", ".aaaaa....aaaaa.", "................", "................", "................"]
BOOTS = ["................", "................", "................", "................", "..aaa.....aaa...",
         "..aba.....aba...", "..aba.....aba...", "..aba.....aba...", "..aca.....aca...", "..accaa...accaa.",
         "..acccca..acccca", "..accccca.acccca", "..aaaaaaaaaaaaaa", "...a.a.a..a.a.a.", "................", "................"]


def icon(rows, pal, out, stripe=None):
    im = Image.new("RGBA", (16, 16), T)
    for y, r in enumerate(rows):
        for x, ch in enumerate(r[:16]):
            if ch not in pal: continue
            c = pal[ch]
            if stripe and ch == 'b': c = stripe[(x // 2) % 2]
            im.putpixel((x, y), c)
    im.save(out)


ICON = {  # outline, body, collar/waistband, number/shorts, sock, boot
    'real': dict(a=C('8a8a8a'), b=C('f4f4f4'), c=GOLD_R, d=PURPLE, sb=GOLD_R, sc=C('f4f4f4'), sock=C('f4f4f4'), boot=C('1a1a1a')),
    'barca': dict(a=C('1a1a1a'), b=BLAU, c=YEL_B, d=YEL_B, sb=GRANA, sc=NAVY_B, sock=NAVY_B, boot=C('ff6a10')),
    'united': dict(a=C('1a1a1a'), b=MU_RED, c=BLK, d=C('ffffff'), sb=BLK, sc=C('ffffff'), sock=BLK, boot=C('d9b23a')),
}
for club, k in CLUBS.items():
    p = ICON[club]
    icon(JERSEY, dict(a=p['a'], b=p['b'], c=p['c'], d=p['d']), A + f"textures/item/{club}_jersey.png",
         stripe=(BLAU, GRANA) if club == 'barca' else None)
    icon(SHORTS, dict(a=p['a'], b=p['sb'], c=p['sc']), A + f"textures/item/{club}_shorts.png")
    icon(BOOTS, dict(a=C('1a1a1a'), b=p['sock'], c=p['boot']), A + f"textures/item/{club}_boots.png")

    # форма на игроке (как kits.py)
    l1 = Image.new("RGBA", (64 * S, 32 * S), T)
    paint(l1, 16, 16, 8, 12, 4, shirt_body(k, 10))
    shirt_details(l1, dict(k, skin=None), 16, 16, 10)
    paint(l1, 40, 16, 4, 12, 4, sleeve(k, 5))
    paint(l1, 0, 16, 4, 12, 4, sock_boot(k, 5))
    l2 = Image.new("RGBA", (64 * S, 32 * S), T)
    paint(l2, 16, 16, 8, 12, 4, lambda f, X, Y, fw, fh, k=k: None if f == 'top' or Y < 8 * S else (k['shorts'] if Y < 11 * S else k['shorts_stripe']) if f != 'bottom' else k['shorts'])
    paint(l2, 0, 16, 4, 12, 4, shorts_leg(k, 5))
    for n, im in ((1, l1), (2, l2)):
        im.quantize(colors=64, method=Image.Quantize.FASTOCTREE).convert("RGBA").save(A + f"textures/models/armor/{club}_layer_{n}.png", optimize=True)

# иконка комплекта стадиона
G1, G2, W, R, B, L, D = C('3f9b3a'), C('348a30'), C('f4f4f4'), C('b0202a'), C('2a6f9b'), C('fff3b0'), C('5a5a5a')
STADIUM = ["L..............L", "D..............D", "RRRRRRRRRRRRRRRR", "RRRRRRRRRRRRRRRR",
           "WWWWWWWWWWWWWWWW", "WgGgGgGWgGgGgGgW", "WgGgGgWWWgGgGgGW", "WWWgGgWgWgGgGWWW",
           "WgWgGgWWWgGgGWgW", "WWWgGgGWgGgGgWWW", "WgGgGgGWgGgGgGgW", "WWWWWWWWWWWWWWWW",
           "BBBBBBBBBBBBBBBB", "BBBBBBBBBBBBBBBB", "D..............D", "................"]
icon(STADIUM, dict(g=G1, G=G2, W=W, R=R, B=B, L=L, D=D), A + "textures/item/stadium_kit.png")
print("ok")

# Этап 3: Золотой свисток
WH = ["................", "................", "................", "................",
      "....gggggggg....", "...gGGGGGGGGg...", "..gGGGgggGGGg...", "..gGGg...gGGgk..",
      "..gGGg...gGGgkk.", "..gGGGgggGGGg.kk", "...gGGGGGGGg...k", "....ggggggg....k",
      "...............k", "..............k.", "................", "................"]
icon(WH, dict(g=C('b8860b'), G=C('ffd23f'), k=C('8a6a2a')), A + "textures/item/golden_whistle.png")

