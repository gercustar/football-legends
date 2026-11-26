# Текстуры для новой модели футболиста (FootballerModel.java), развёртка 128x64, S=8 -> 1024x512.
# Запуск из корня репозитория:  python tools/textures/realistic.py src/main/resources
import math
import os
import sys

SP = os.path.dirname(os.path.abspath(__file__)) + '/'
exec(open(SP + 'kits.py', encoding='utf-8').read().split('# ---------------- armor')[0])
from PIL import Image, ImageDraw, ImageFilter

# формы остальных легенд и недостающие буквы шрифта берём из legends.py
_leg = open(SP + 'legends.py', encoding='utf-8').read()
exec(_leg[_leg.index('FONT.update'):_leg.index('EYE=')])
exec(open(SP + 'clubs.py', encoding='utf-8').read())

OUT = A + 'textures/entity/footballer/'
os.makedirs(OUT, exist_ok=True)


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3)) + (255,)


def shade_round(X, Y, fw, fh, k=0.16):
    """Округлость: темнее к краям грани."""
    e = abs(X - (fw - 1) / 2) / (fw / 2)
    return 1.04 - k * e * e


def skin_fn(skin):
    def fn(f, X, Y, fw, fh):
        n = 1 + rnd.uniform(-0.015, 0.015)
        return sh(skin, shade_round(X, Y, fw, fh) * n * (0.9 if f == 'bottom' else 1))
    return fn


def cloth(c, X, Y, fw, fh, fold=0.05):
    k = shade_round(X, Y, fw, fh, 0.14)
    k *= 1 + fold * math.sin(X * 0.33 + Y * 0.11 + 2 * math.sin(Y * 0.07))
    k *= 1 + rnd.uniform(-0.02, 0.02)
    return sh(c, k)


def shirt_fn(k, rows):
    raw = k['shirt']
    def fn(f, X, Y, fw, fh):
        if f not in ('top', 'bottom') and Y >= rows * S:
            c = cloth(k['shorts'], X, Y, fw, fh)
            return sh(c, 0.78) if Y < rows * S + 3 else c
        if f not in ('top', 'bottom') and Y >= rows * S - 2:
            return k['collar']
        c = raw(f if f not in ('top', 'bottom') else 'front', X, Y, fw, fh)
        s = 1 + 0.045 * math.sin(X * 0.25 - Y * 0.13 + 1.5 * math.sin(Y * 0.09))
        # тень под грудью и к талии
        s *= 1 - 0.10 * max(0.0, math.cos((Y - 34) / 9.0)) if f == 'front' and 22 < Y < 46 else 1
        return sh(c, s * shade_round(X, Y, fw, fh, 0.12))
    return fn


def sleeve_fn(k, p, left):
    raw = k['shirt']
    def fn(f, X, Y, fw, fh):
        if left and p.get('armband') and f in ('front', 'back', 'left', 'right') and 14 <= Y <= 27:
            if Y in (14, 27): return (235, 235, 235, 255)
            return sh(p['armband'][1] if 18 <= Y <= 23 else p['armband'][0], shade_round(X, Y, fw, fh, 0.1))
        if p.get('stripes'):                       # три полосы вдоль плеча
            if f == 'top' and Y % 8 in (3, 4) and 6 <= Y <= 26: return p['stripes']
        if f == 'bottom':
            return sh(k['cuff'], 0.5)
        if f != 'top':
            if Y >= fh - 3: return k['cuff2']
            if Y >= fh - 7: return k['cuff']
        return sh(raw('left', X, Y, fw, fh), shade_round(X, Y, fw, fh, 0.14) * (1 + 0.05 * math.sin(X * .3 + Y * .35)))
    return fn


def shorts_fn(k, number=False):
    def fn(f, X, Y, fw, fh):
        if f == 'bottom': return sh(k['shorts'], 0.45)
        if f in ('left', 'right') and 13 <= X <= 18 and f != 'top': return cloth(k['shorts_stripe'], X, Y, fw, fh, 0.03)
        c = cloth(k['shorts'], X, Y, fw, fh, 0.07)
        return sh(c, 0.75) if Y >= fh - 2 else c
    return fn


def leg_fn(k, skin):
    sk = skin_fn(skin)
    def fn(f, X, Y, fw, fh):
        if f in ('top', 'bottom'): return sk(f, X, Y, fw, fh)
        if Y < 6 * S:
            c = sk(f, X, Y, fw, fh)
            if f == 'front' and 40 <= Y < 48: c = sh(c, 0.95 + 0.05 * math.cos((X - 16) / 5))   # колено
            return c
        if Y < 6.8 * S: return cloth(k['sock_band'], X, Y, fw, fh, 0.02)
        c = cloth(k['sock'], X, Y, fw, fh, 0.03)
        if f == 'front' and 60 <= Y <= 82 and 7 <= X <= 24: c = sh(c, 1.05)                       # щиток
        if X % 3 == 0: c = sh(c, 0.965)                                                            # рубчик
        return c
    return fn


def boot_fn(k):
    def fn(f, X, Y, fw, fh):
        if f == 'bottom':
            return sh(k['boot2'], 0.55) if (X // 6 + Y // 8) % 2 else sh(k['boot2'], 0.8)
        if f == 'top':
            if 8 <= Y < 30 and 10 <= X <= 21: return (245, 245, 245, 255) if Y % 5 < 2 else sh(k['boot'], 0.8)   # шнурки
            return sh(k['boot'], 1.05)
        if Y >= fh - 3: return sh(k['boot2'], 0.7)
        if f in ('left', 'right') and abs((X if f == 'right' else fw - 1 - X) - 26 + Y) < 3: return k['boot2']
        return sh(k['boot'], shade_round(X, Y, fw, fh, 0.1) * (1 + rnd.uniform(-.02, .02)))
    return fn


def hair_fn(hair, light=1.0):
    def fn(f, X, Y, fw, fh):
        s = math.sin(X * 1.9 + 3 * math.sin(Y * 0.21)) * 0.14 + math.sin(X * 0.6 + Y * 0.35) * 0.09
        return sh(hair, light * (1 + s + rnd.uniform(-0.07, 0.07)) * (0.85 if f == 'bottom' else 1))
    return fn


def curl_fn(hair):
    def fn(f, X, Y, fw, fh):
        a, b = X / 5.0 + 0.7 * math.sin(Y / 4.0), Y / 5.0 + 0.7 * math.sin(X / 4.0)
        s = math.sin(a * math.pi) * math.sin(b * math.pi)
        return sh(hair, (1.15 + 0.55 * s + rnd.uniform(-0.06, 0.06)) * (0.85 if f == 'bottom' else 1))
    return fn


def strands(w, h, root, tip):
    """Зубчатые пряди: тёмные корни, светлые кончики, прозрачно между ними."""
    im = Image.new('RGBA', (w, h), T)
    x = -2
    while x < w + 2:
        half, top, lean = rnd.uniform(3.0, 5.0), rnd.uniform(0, h * 0.32), rnd.uniform(-5, 5)
        for Y in range(int(top), h):
            k = (Y - top) / (h - top)                       # 0 — кончик, 1 — корень
            c0 = x + lean * (1 - k)
            for X in range(int(c0 - half * (0.15 + k)), int(c0 + half * (0.15 + k)) + 1):
                if 0 <= X < w:
                    t = max(0.0, min(1.0, (0.5 - k) / 0.4))
                    im.putpixel((X, Y), sh(mix(root, tip, t), rnd.uniform(0.85, 1.12)))
        x += rnd.uniform(4.0, 6.5)
    return im


def arm_fn(skin, tattoo, gloves=None):
    sk = skin_fn(skin)
    def fn(f, X, Y, fw, fh):
        c = sk(f, X, Y, fw, fh)
        if gloves and Y >= 9.6 * S:                                   # вратарские перчатки
            return sh(gloves, 0.8 if Y >= 11.6 * S or f == 'bottom' else (0.9 + 0.1 * math.sin(X * 0.8)))
        if tattoo and f in ('front', 'back', 'left', 'right') and 38 < Y < 84:
            v = math.sin(X * 0.22 + 2.2 * math.sin(Y * 0.13)) + math.sin(Y * 0.2 + 2 * math.sin(X * 0.17))
            if abs(v) < 0.2: return mix(c, tattoo, 0.6)         # контур
            if v > 1.0: return mix(c, tattoo, 0.3)              # заливка
        return c
    return fn


def head_fn(p):
    skin, hair, style = p['skin'], p['hair'], p['style']
    hf, sk = hair_fn(hair), skin_fn(skin)
    stubbly = lambda c, a: mix(c, hair, a * rnd.uniform(0.75, 1.1))

    def fn(f, X, Y, fw, fh):
        c = sk(f, X, Y, fw, fh)
        if f == 'bottom': return sh(p['beard'] or skin, 0.8)
        if f == 'top':
            if style == 'bald': return sh(c, 1.04)
            if style == 'thin': return stubbly(c, 0.6 if Y < 44 else 0.25)      # Y растёт к лицу
            if style == 'shaved': return hf(f, X, Y, fw, fh) if 18 < X < 45 else stubbly(c, 0.55)
            return hf(f, X, Y, fw, fh)
        if f == 'back':
            if style == 'bald': return stubbly(c, 0.16 * max(0, 1 - abs(Y - 34) / 18))
            if style == 'thin': return stubbly(c, max(0, min(0.75, (46 - Y) / 14)))
            if style == 'shaved': return stubbly(c, max(0, min(0.55, (44 - Y) / 16)))
            edge = 50 + 3 * math.sin(X * 0.35)
            return hf(f, X, Y, fw, fh) if Y < edge - 8 else mix(c, hair, max(0, min(1, (edge - Y) / 10)))
        if f == 'front':
            if style == 'bald': return c
            if style == 'thin':
                line = 4 + 6 * math.exp(-((X - 31.5) / 11) ** 2) + 3 * max(0, abs(X - 31.5) - 24) / 8
                return stubbly(c, 0.6 * max(0.0, min(1.0, (line - Y) / 5 + 0.3)))
            if style == 'shaved':
                if Y < 9 and 18 < X < 45: return hf(f, X, Y, fw, fh)
                return stubbly(c, 0.5) if Y < 10 - 0.15 * abs(X - 31.5) else c
            line = p.get('hairline', 13) - 2.5 * ((X - 31.5) / 32) ** 2 + (3 if X < 6 or X > 57 else 0)
            if Y < line: return hf(f, X, Y, fw, fh)
            return mix(c, hair, 0.45) if Y < line + 2 else c
        # бока: 0 — затылок, 63 — лицо
        x = X if f == 'right' else fw - 1 - X
        if x > 56: edge = 16
        elif x > 46: edge = p['sideburn']
        elif x > 30: edge = 26
        else: edge = 46
        t = max(0.0, min(1.0, (edge - Y) / p['fade'] + 0.25))
        if style == 'bald': t = 0.16 * max(0, 1 - abs(Y - 34) / 18) * max(0.0, min(1.0, (44 - x) / 14))
        if style == 'thin': t *= 0.65 if x < 56 else 0.3
        if style == 'shaved': t *= 0.62 if x < 58 else 0.35
        if t > 0: c = mix(c, hf(f, X, Y, fw, fh), min(1, t)) if style not in ('bald', 'thin', 'shaved') else stubbly(c, t)
        if p['beard'] and x > 26 and Y > 52: c = mix(c, p['beard'], 0.6)
        return c
    return fn


def face(img, p):
    """Лицо 64x64: глаза, брови, нос, губы, тени. Рисуется x4 и уменьшается для мягких краёв."""
    Q = 4
    ox, oy, fw, fh = face_origin(0, 0, 8, 8, 8, 'front')
    L = Image.new('RGBA', (fw * Q, fh * Q), (0, 0, 0, 0))
    d = ImageDraw.Draw(L)
    skin, hair = p['skin'], p['hair']
    q = lambda *a: [v * Q for v in a]
    dark = sh(skin, 0.72)[:3]
    # мягкие тени: глазницы, под носом, под губой, скулы
    soft = Image.new('RGBA', L.size, (0, 0, 0, 0)); s = ImageDraw.Draw(soft)
    for x in (10, 36): s.ellipse(q(x, 25, x + 18, 37), fill=dark + (85,))
    s.ellipse(q(25, 44, 39, 50), fill=dark + (120,))
    s.ellipse(q(24, 56, 40, 62), fill=dark + (90,))
    s.polygon(q(0, 36, 9, 48, 6, 63, 0, 63), fill=dark + (60,))
    s.polygon(q(63, 36, 54, 48, 57, 63, 63, 63), fill=dark + (60,))
    s.rectangle(q(27, 28, 29, 44), fill=dark + (60,)); s.rectangle(q(35, 28, 37, 44), fill=dark + (60,))
    L.alpha_composite(soft.filter(ImageFilter.GaussianBlur(2.2 * Q)))
    # щетина
    if p['stubble']:
        st = Image.new('RGBA', L.size, (0, 0, 0, 0)); s = ImageDraw.Draw(st)
        s.polygon(q(0, 40, 8, 46, 20, 49, 44, 49, 56, 46, 63, 40, 63, 63, 0, 63), fill=hair[:3] + (p['stubble'],))
        L.alpha_composite(st.filter(ImageFilter.GaussianBlur(1.5 * Q)))
    for side in (-1, 1):
        cx = 32 + side * 12.5
        w = 6.2
        # глаз
        d.polygon(q(cx - w, 33, cx - w * .45, 30.2, cx + w * .45, 30.2, cx + w, 33, cx + w * .4, 35.4, cx - w * .4, 35.4), fill=(240, 236, 230, 255))
        d.ellipse(q(cx - 2.7, 30.1, cx + 2.7, 35.5), fill=p['eye'][:3] + (255,))
        d.ellipse(q(cx - 1.3, 31.4, cx + 1.3, 34.0), fill=(15, 10, 8, 255))
        d.ellipse(q(cx + 0.4, 30.9, cx + 1.6, 32.1), fill=(255, 255, 255, 255))
        d.line(q(cx - w, 33, cx - w * .45, 30.1, cx + w * .45, 30.1, cx + w, 33), fill=sh(p.get('browcol', hair), 0.6)[:3] + (255,), width=int(1.1 * Q))
        d.line(q(cx - w * .5, 35.6, cx + w * .5, 35.6), fill=dark + (200,), width=int(0.6 * Q))
        # бровь
        o, i = cx + side * 7.5, cx - side * 6.5
        t = p['brow']
        d.polygon(q(i, 27.2, cx, 24.6, o, 26.6, o, 27.6, cx, 24.6 + t, i, 27.2 + t), fill=sh(p.get('browcol', hair), 0.8)[:3] + (255,))
    # губы
    if p.get('teeth'):
        lip = mix(skin, (165, 70, 66), 0.5)[:3]
        d.polygon(q(20, 51, 32, 52.2, 44, 51, 41, 56.5, 36, 58.6, 28, 58.6, 23, 56.5), fill=lip + (255,))
        d.polygon(q(21.5, 52.2, 32, 53.2, 42.5, 52.2, 40, 55.6, 35.5, 57, 28.5, 57, 24, 55.6), fill=(244, 240, 232, 255))
        for x in (26, 29, 32, 35, 38): d.line(q(x, 53.2, x, 56.4), fill=(205, 198, 190, 255), width=int(0.35 * Q))
    else:
        lip = mix(skin, (165, 70, 66), 0.5)[:3]
        d.polygon(q(23, 53, 29, 51.3, 32, 52.1, 35, 51.3, 41, 53, 35, 53.6, 29, 53.6), fill=lip + (255,))
        d.polygon(q(24, 53.2, 40, 53.2, 36, 56.2, 28, 56.2), fill=mix(lip + (255,), (255, 200, 190), 0.22)[:3] + (255,))
        d.line(q(23.5, 53.2, 40.5, 53.2), fill=sh(lip + (255,), 0.5)[:3] + (255,), width=int(0.7 * Q))
    img.alpha_composite(L.resize((fw, fh), Image.LANCZOS), (ox, oy))


def beard_box(img, p):
    """Борода-коробка 8x3x5 в (64,48): спереди вырез под рот."""
    full = hair_fn(p['beard'])
    thin = p.get('beard_thin', 0)
    hf = (lambda f, X, Y, fw, fh: mix(full(f, X, Y, fw, fh), p['skin'], thin * rnd.uniform(0.75, 1.15))) if thin else full
    def fn(f, X, Y, fw, fh):
        if f in ('top', 'back'): return None
        if f == 'front':
            e = abs(X - 31.5)
            moustache = 8 <= Y <= 10 and e < 11 and not (Y == 8 and e < 2)
            around = 9 < e < 14 and Y >= 9
            chin = Y >= 16 - max(0, e - 6) * 0.2
            jaw = e >= 14 and Y > 15 - (e - 14) * 0.5
            if not (moustache or around or chin or jaw): return None
        if f in ('left', 'right'):
            x = X if f == 'right' else fw - 1 - X          # 0 — затылок, 39 — лицо
            if x < 4 or Y < 8 - max(0, 16 - x) * 0.7: return None
        return hf(f, X, Y, fw, fh)
    paint(img, 64, 48, 8, 3, 5, fn)


def footballer(name, k, p):
    img = Image.new('RGBA', (128 * S, 64 * S), T)
    skin = p['skin']
    sk = skin_fn(skin)
    paint(img, 0, 0, 8, 8, 8, head_fn(p))
    face(img, p)
    paint(img, 16, 16, 8, 12, 4, shirt_fn(k, 10))
    shirt_details(img, dict(k, skin=skin), 16, 16, 10)
    for u, v in ((40, 16), (32, 48)): paint(img, u, v, 4, 12, 4, arm_fn(skin, p.get('tattoo'), p.get('gloves')))
    for u in (64, 80): paint(img, u, 0, 4, 5, 4, sleeve_fn(k, p, u == 80))
    for u, v in ((0, 16), (16, 48)): paint(img, u, v, 4, 12, 4, leg_fn(k, skin))
    for u in (64, 80): paint(img, u, 10, 4, 5, 4, shorts_fn(k))
    for u in (64, 84): paint(img, u, 20, 4, 2, 6, boot_fn(k))
    paint(img, 96, 0, 1, 2, 1, lambda f, X, Y, fw, fh: sh(skin, 0.78 if f == 'bottom' else 0.9 if f in ('left', 'right') else 1.06))
    for u in (100, 106): paint(img, u, 0, 1, 2, 2, lambda f, X, Y, fw, fh: sh(skin, 0.95))
    paint(img, 96, 4, 4, 2, 4, lambda f, X, Y, fw, fh: sh(skin, 0.86))
    hair = curl_fn(p['hair']) if p['style'] == 'curly' else hair_fn(p['hair'])
    for u, v, w, h, dd in ((64, 28, 8, 2, 8), (96, 28, 8, 2, 3), (96, 34, 8, 5, 2), (64, 38, 3, 2, 8),
                           (90, 50, 8, 9, 2), (110, 50, 1, 7, 5)):
        paint(img, u, v, w, h, dd, hair)
    if p.get('tips'):
        img.paste(T, (64 * S, 48 * S, 128 * S, 64 * S))
        for v in (40, 46):
            r1 = strands(8 * S, 5 * S, p['hair'], p['tips'])
            for u in (64, 72): img.paste(r1, (u * S, (v + 8) * S))
        for u in (90, 95, 100): img.paste(strands(4 * S, 5 * S, p['hair'], p['tips']), (u * S, 50 * S))
        def nape(f, X, Y, fw, fh):
            if f == 'top': return sh(p['hair'], 1)
            tip = fh - 4 - 9 * abs(math.sin(X * 0.45 + 0.6))
            if f != 'bottom' and Y > tip: return None
            if f == 'bottom': return None
            return sh(mix(p['hair'], p['tips'], max(0, (Y - 26) / 34) * rnd.uniform(0.5, 1.0)), 1 + 0.15 * math.sin(X * 1.9))
        paint(img, 106, 50, 3, 7, 1, nape)
    if p.get('band'):
        paint(img, 86, 41, 8, 1, 8, lambda f, X, Y, fw, fh: None if f in ('top', 'bottom') else sh(p['band'], 0.9 + 0.1 * math.sin(X * 0.5)))
    if p['beard']: beard_box(img, p)
    # сжатие цветов, прозрачность сохраняется
    if p.get('fine'):                              # без палитры: округление каналов, оттенки не «плывут»
        r, g, b, al = img.split()
        out = Image.merge('RGBA', [c.point(lambda v: min(255, v // 6 * 6 + 3)) for c in (r, g, b)] + [al])
    else:
        out = img.quantize(colors=128, method=Image.Quantize.FASTOCTREE).convert('RGBA')
    out.save(OUT + name + '.png', optimize=True)
    print(name, os.path.getsize(OUT + name + '.png'))


def P(skin, hair, eye, style='short', beard=None, stubble=0, brow=2.2, sideburn=32, fade=14, **kw):
    return dict(skin=C(skin), hair=C(hair), eye=C(eye), style=style, beard=C(beard) if beard else None,
                stubble=stubble, brow=brow, sideburn=sideburn, fade=fade, **kw)


footballer('ronaldo', KITS['ronaldo'], P('c8956c', '1d1512', '4a2c18', stubble=28, brow=2.6, sideburn=30, fade=16))
footballer('messi', KITS['messi'], P('e0b08a', '54361f', '5b3d22', beard='6b4022', stubble=45, sideburn=38, fade=12))
footballer('zidane', K['zidane'], P('d6a582', '2e2018', '4f7a6a', style='bald', stubble=30, brow=2.4))
footballer('ronaldinho', K['ronaldinho'], P('8c5c3c', '15100d', '2c1a10', style='curly', teeth=True, brow=2.0, sideburn=40, band=C('f2f2f2')))
footballer('neymar', K['neymar'], P('b98258', '2a1b12', '5e5a34', style='shaved', stubble=0, brow=2.5, sideburn=40, tips=C('d9a648')))
footballer('mbappe', K['mbappe'], P('7d5234', '120c09', '24140c', sideburn=26, fade=18, brow=2.2))
footballer('modric', K['modric'], P('edc4a2', '8f6d42', '4c7fa8', style='long', browcol=C('6a4c2c'), sideburn=40, band=C('2a2a2a')))
footballer('ramos', K['ramos'], P('d6a480', '4a2f1e', '5a3c22', beard='70452a', beard_thin=0.3, stubble=60, brow=2.5, browcol=C('3a2518'), sideburn=30, fade=22,
    hairline=10, fine=True, tattoo=C('2c3350'), armband=(C('c60b1e'), C('f1bf00')), stripes=SPA_Y))
footballer('xavi', K['xavi'], P('d7a680', '241812', '3c2616', stubble=34, brow=2.5))
footballer('iniesta', K['iniesta'], P('f0cbb0', '6f573f', '4a3828', style='thin', stubble=18, brow=1.9, browcol=C('5a4432')))
footballer('suarez', K['suarez'], P('c8956a', '1a120d', '3a2414', teeth=True, stubble=26, brow=2.5))

# Этап 8: версии игроков в клубных формах
footballer('ronaldo_2008', dict(CLUBS['united'], name='RONALDO', num='7'),
           P('cf9c73', '1a120d', '4a2c18', stubble=8, brow=2.4, sideburn=30, fade=16))
footballer('ronaldo_2017', dict(CLUBS['real'], name='RONALDO', num='7'),
           P('c49068', '1d1512', '4a2c18', stubble=22, brow=2.6, sideburn=28, fade=18))
footballer('messi_2012', dict(CLUBS['barca'], name='MESSI', num='10'),
           P('e2b28c', '5e3c22', '5b3d22', stubble=12, sideburn=40, fade=12))

# Этап 4: судья в чёрной форме
REF_KIT = dict(shirt=solid(C('1b1b1b')), collar=C('e6e6e6'), collar2=C('1b1b1b'), cuff=C('1b1b1b'), cuff2=C('e6e6e6'),
               shorts=C('111111'), shorts_stripe=C('e6e6e6'), sock=C('111111'), sock_band=C('e6e6e6'),
               boot=C('111111'), boot2=C('f0f0f0'), name=' ', num=' ', numcol=C('e6e6e6'), numout=C('1b1b1b'), stars=0)
footballer('referee', REF_KIT, P('d2a07a', '3a2a1e', '4a3018', stubble=30, brow=2.3))

# Этап 5–6: вратарь серии пенальти (перчатки) и тренер академии (спортивный костюм)
KEEPER_KIT = dict(shirt=solid(C('1f8a3a')), collar=C('111111'), collar2=C('1f8a3a'), cuff=C('111111'), cuff2=C('1f8a3a'),
                  shorts=C('111111'), shorts_stripe=C('1f8a3a'), sock=C('111111'), sock_band=C('1f8a3a'),
                  boot=C('f0f0f0'), boot2=C('1f8a3a'), name=' ', num='1', numcol=C('f4f4f4'), numout=C('0d3d1a'), stars=0)
footballer('goalkeeper', KEEPER_KIT, P('d9a882', '3a2a1e', '3c5a7a', stubble=20, brow=2.4, gloves=C('f2f2f2')))
COACH_KIT = dict(shirt=solid(C('1c2a4a')), collar=C('f4f4f4'), collar2=C('1c2a4a'), cuff=C('f4f4f4'), cuff2=C('1c2a4a'),
                 shorts=C('1c2a4a'), shorts_stripe=C('f4f4f4'), sock=C('1c2a4a'), sock_band=C('1c2a4a'),
                 boot=C('f0f0f0'), boot2=C('9a9a9a'), name='COACH', num=' ', numcol=C('f4f4f4'), numout=C('0d142a'), stars=0)
footballer('coach', COACH_KIT, P('d6a07c', '8a8a8a', '4a3018', beard='8f8f8f', beard_thin=0.25, brow=2.4, browcol=C('5a5a5a')))

