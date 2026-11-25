import sys, random
from PIL import Image
OUT = sys.argv[1]
A = OUT + "/assets/ronaldomessi/"
random.seed(7)

def C(h, a=255):
    h = h.lstrip('#'); return (int(h[0:2],16), int(h[2:4],16), int(h[4:6],16), a)

# 3x5 digit font
DIG = {'7': ["###","..#",".#.",".#.",".#."],
       '1': [".#.","##.",".#.",".#.","###"],
       '0': ["###","#.#","#.#","#.#","###"]}

def box(img, u, v, w, h, d, paint):
    faces = {'top':(u+d,v,w,d),'bottom':(u+d+w,v,w,d),'right':(u,v+d,d,h),
             'front':(u+d,v+d,w,h),'left':(u+d+w,v+d,d,h),'back':(u+2*d+w,v+d,w,h)}
    for f,(x0,y0,fw,fh) in faces.items():
        for y in range(fh):
            for x in range(fw):
                c = paint(f, x, y, fw, fh)
                if c is not None: img.putpixel((x0+x, y0+y), c)

def shade(c, k):
    return tuple(max(0,min(255,int(ch*k))) for ch in c[:3]) + (c[3],)

def number(img, ox, oy, txt, col):
    x = ox
    for ch in txt:
        for r,row in enumerate(DIG[ch]):
            for cidx,p in enumerate(row):
                if p == '#': img.putpixel((x+cidx, oy+r), col)
        x += 4

def make_skin(p):
    img = Image.new("RGBA", (64,64), (0,0,0,0))
    skin, hair, eye = p['skin'], p['hair'], p['eye']
    def head(f,x,y,w,h):
        if f == 'top': return hair
        if f == 'bottom': return skin
        if f == 'front':
            if y < p['hairline']: return hair
            if y == 3 and x in (1,6) and p['hairline'] >= 3: return hair
            if y == 4 and x in (1,2,5,6): return (255,255,255,255) if x in (1,6) else eye
            if p.get('beard') and y >= 5:
                if y == 6 and 2 <= x <= 5: return shade(p['beard'],0.6)
                if y == 5 and x in (3,4): return shade(skin,0.85)
                return p['beard'] if (y>=6 or x in (0,1,6,7)) else skin
            if y == 6 and 3 <= x <= 4: return shade(skin,0.7)
            if y == 5 and x in (3,4): return shade(skin,0.88)
            return skin
        if f == 'back': return hair if y < 7 else skin
        # sides
        if y < p['hairline']: return hair
        if p.get('beard') and y >= 4 and x in ((4,5,6,7) if f=='right' else (0,1,2,3)): return p['beard']
        if y == 4 and x == (3 if f=='right' else 4): return shade(skin,0.8)  # ear
        return hair if (f=='right' and x<3 or f=='left' and x>4) and y<5 else skin
    box(img, 0,0, 8,8,8, head)

    def shirt(f,x,y,w,h):
        base = p['shirt'](x, y, f)
        if f=='front' and y==0 and 2<=x<=5: return p['trim'] if x in (2,5) else skin  # neck V
        if f=='front' and y in (2,3) and x==5: return p['badge']
        if f=='back' and 2<=y<=6: pass
        if y >= 8:  # shorts start at row 8? body is 12 tall: shirt 0-8, shorts 9-11
            pass
        if y >= 9: return p['shorts'] if not (y==9) else shade(p['shorts'],0.8)
        return base
    box(img, 16,16, 8,12,4, shirt)
    number(img, 16+4+4+8+1, 16+4+2, p['num'], p['numcol'])   # back face x0 = u+2d+w = 16+8+8=32
    def arm(f,x,y,w,h):
        if f=='top': return p['shirt'](x,0,f)
        if f=='bottom': return skin
        if y < 4: return p['shirt'](x,y,f) if y<3 else p['trim']
        return skin
    box(img, 40,16, 4,12,4, arm)
    box(img, 32,48, 4,12,4, arm)
    def leg(f,x,y,w,h):
        if f=='top': return p['shorts']
        if f=='bottom': return p['boot']
        if y < 3: return p['shorts']
        if y < 5: return skin
        if y < 10: return p['sock'] if y != 5 else shade(p['sock'],0.85)
        return p['boot'] if not (y==11 and x%2==0) else shade(p['boot'],0.7)
    box(img, 0,16, 4,12,4, leg)
    box(img, 16,48, 4,12,4, leg)
    return img

RED, GREEN = C('c8102e'), C('046a38')
SKY, WHITE = C('75aadb'), C('f4f4f4')

ronaldo = dict(skin=C('c99a74'), hair=C('1d1410'), eye=C('3b2a1a'), hairline=2,
    shirt=lambda x,y,f: shade(RED, 0.9) if (f in('front','back') and x in (0,7)) else RED,
    trim=GREEN, badge=C('ffd700'), shorts=GREEN, sock=RED, boot=C('ffcc33'),
    num='7', numcol=C('ffd700'))
messi = dict(skin=C('e2b48f'), hair=C('5a3a22'), eye=C('3d5a2a'), hairline=2, beard=C('5a3a22'),
    shirt=lambda x,y,f: SKY if (x//2)%2==0 else WHITE,
    trim=C('1f2a44'), badge=C('ffd700'), shorts=C('1b1b1b'), sock=WHITE, boot=C('ff6a00'),
    num='10', numcol=C('1f2a44'))

make_skin(ronaldo).save(A+"textures/entity/ronaldo.png")
make_skin(messi).save(A+"textures/entity/messi.png")

# ---- armor layers (64x32) ----
def armor(p, name):
    l1 = Image.new("RGBA",(64,32),(0,0,0,0))
    def body(f,x,y,w,h):
        if y >= 10: return None
        if f=='front' and y==0 and 3<=x<=4: return p['trim']
        if f=='front' and y in (2,3) and x==5: return p['badge']
        return p['shirt'](x,y,f)
    box(l1,16,16,8,12,4,body)
    number(l1, 33, 22, p['num'], p['numcol'])
    def arm(f,x,y,w,h):
        if f=='bottom': return None
        if y < 4 or f=='top': return p['shirt'](x,y,f) if y<3 or f=='top' else p['trim']
        return None
    box(l1,40,16,4,12,4,arm)
    def boots(f,x,y,w,h):
        if f=='top': return None
        if f=='bottom': return p['boot']
        if y < 5: return None
        if y < 9: return p['sock']
        return p['boot'] if not (y==11 and x%2==0) else shade(p['boot'],0.7)
    box(l1,0,16,4,12,4,boots)
    l1.save(A+f"textures/models/armor/{name}_layer_1.png")
    l2 = Image.new("RGBA",(64,32),(0,0,0,0))
    def waist(f,x,y,w,h):
        if f in ('top',): return None
        return p['shorts'] if y >= 8 else None
    box(l2,16,16,8,12,4,waist)
    def legs(f,x,y,w,h):
        if f in ('top','bottom'): return None
        if y < 4: return p['shorts'] if y!=3 else shade(p['shorts'],0.8)
        return None
    box(l2,0,16,4,12,4,legs)
    l2.save(A+f"textures/models/armor/{name}_layer_2.png")
armor(ronaldo,"ronaldo"); armor(messi,"messi")

# ---- item icons 16x16 ----
def from_ascii(rows, pal):
    img = Image.new("RGBA",(16,16),(0,0,0,0))
    for y,row in enumerate(rows):
        for x,ch in enumerate(row):
            if ch in pal: img.putpixel((x,y), pal[ch])
    return img

JERSEY = [
"................",
"...oo.oooo.oo...",
"..oAAoBBBBoAAo..",
".oAAAAABBAAAAAo.",
"oAAAAAAAAAAAAAAo",
"oAAoAAAAAAAAoAAo",
".oooAAAAAAAAooo.",
"...oAAAAAAAAo...",
"...oAAAAAAAAo...",
"...oAAAAAAAAo...",
"...oAAAAAAAAo...",
"...oAAAAAAAAo...",
"...oAAAAAAAAo...",
"...oooooooooo...",
"................",
"................"]
SHORTS = [
"................","................","................",
"...oooooooooo...",
"...oBBBBBBBBo...",
"...oAAAAAAAAo...",
"..oAAAAAAAAAAo..",
"..oAAAAooAAAAo..",
"..oAAAAo.oAAAo..",
"..oAAAo..oAAAo..",
".oAAAAo..oAAAAo.",
".oAAAAo..oAAAAo.",
".ooooo....ooooo.",
"................","................","................"]
BOOTS = [
"................","................","................","................",
"..ooo.....ooo...",
"..oSo.....oSo...",
"..oSo.....oSo...",
"..oSo.....oSo...",
"..oAo.....oAo...",
"..oAAoo...oAAoo.",
"..oAAAAo..oAAAAo",
"..oAAAAAo.oAAAAo",
"..oooooooooooooo",
"...o.o.o..o.o.o.",
"................","................"]

def jersey_icon(p, stripes=False):
    img = from_ascii(JERSEY, {'o':C('202020'),'A':p['shirt'](0,0,'front'),'B':p['trim']})
    if stripes:
        for y in range(16):
            for x in range(16):
                if img.getpixel((x,y))[:3]==SKY[:3] and (x//2)%2==1: img.putpixel((x,y),WHITE)
    number(img, 6 if p['num']=='7' else 4, 7, p['num'], p['numcol'])
    return img
jersey_icon(ronaldo).save(A+"textures/item/ronaldo_jersey.png")
jersey_icon(messi, True).save(A+"textures/item/messi_jersey.png")
from_ascii(SHORTS,{'o':C('202020'),'A':GREEN,'B':RED}).save(A+"textures/item/ronaldo_shorts.png")
from_ascii(SHORTS,{'o':C('101010'),'A':C('2a2a2a'),'B':SKY}).save(A+"textures/item/messi_shorts.png")
from_ascii(BOOTS,{'o':C('202020'),'A':C('ffcc33'),'S':RED}).save(A+"textures/item/ronaldo_boots.png")
from_ascii(BOOTS,{'o':C('202020'),'A':C('ff6a00'),'S':WHITE}).save(A+"textures/item/messi_boots.png")

# soccer ball texture (used on 3D cube faces): white with black pentagon patches
BALL = [
"wwwwwwwwwwwwwwww",
"wwwwwwwbbwwwwwww",
"wwwwwwbbbbwwwwww",
"wwwwwwbbbbwwwwww",
"wwwwwwwbbwwwwwww",
"wbwwwwwggwwwwwbw",
"bbwwwwwwwwwwwwbb",
"bbbwggwwwwggwbbb",
"bbwwwwwwwwwwwwbb",
"wbwwwwwggwwwwwbw",
"wwwwwwwbbwwwwwww",
"wwwwwwbbbbwwwwww",
"wwwwwwbbbbwwwwww",
"wwwwwwwbbwwwwwww",
"wwwwwwwwwwwwwwww",
"wwwwwwwwwwwwwwww"]
from_ascii(BALL,{'w':C('f2f2f2'),'b':C('1a1a1a'),'g':C('c9c9c9')}).save(A+"textures/item/soccer_ball.png")

GOLD = [
"................",
".....oooooo.....",
"...ooYYYYYYoo...",
"..oYYWYYDDYYYo..",
"..oYWYYYDDYYYo..",
".oYYYYYYYYYYYYo.",
".oYDDYYYYYYDDYo.",
".oYDDYYYYYYDDYo.",
".oYYYYYDDYYYYYo.",
".oYYYYYDDYYYYYo.",
"..oYYYYYYYYYYo..",
"..ooYYYYYYYYoo..",
"....ooYYYYoo....",
".....oooooo.....",
"....oBBBBBBo....",
"....oooooooo...."]
from_ascii(GOLD,{'o':C('7a5300'),'Y':C('ffd23f'),'D':C('d39b00'),'W':C('fff6c8'),'B':C('5a3a22')}).save(A+"textures/item/golden_ball.png")

# mod icon 64x64: ball upscaled
Image.open(A+"textures/item/golden_ball.png").resize((64,64), Image.NEAREST).save(A+"icon.png")
print("textures ok")
