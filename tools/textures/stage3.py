import sys
from PIL import Image
A=sys.argv[1]+"/assets/ronaldomessi/"
S=8
def C(h,a=255): h=h.lstrip('#'); return (int(h[0:2],16),int(h[2:4],16),int(h[4:6],16),a)
def sh(c,k): return tuple(max(0,min(255,int(v*k))) for v in c[:3])+(c[3],)
T=(0,0,0,0)
def from_ascii(rows,pal):
    im=Image.new("RGBA",(16,16),T)
    for y,r in enumerate(rows):
        for x,ch in enumerate(r):
            if ch in pal: im.putpixel((x,y),pal[ch])
    return im
def faces(u,v,w,h,d):
    return {'top':(u+d,v,w,d),'bottom':(u+d+w,v,w,d),'right':(u,v+d,d,h),'front':(u+d,v+d,w,h),'left':(u+d+w,v+d,d,h),'back':(u+2*d+w,v+d,w,h)}
def paint(img,u,v,w,h,d,fn):
    for f,(x0,y0,fw,fh) in faces(u,v,w,h,d).items():
        x0*=S;y0*=S;fw*=S;fh*=S
        for Y in range(fh):
            for X in range(fw):
                c=fn(f,X,Y,fw,fh)
                if c is not None: img.putpixel((x0+X,y0+Y),c)

# ---- boots: (main, accent, sole, stud) ----
BOOTS={'mercurial':(C('ff2d78'),C('d7ff1f'),C('1a1a1a'),C('d7ff1f')),
       'f50':(C('ffe14d'),C('111111'),C('f0f0f0'),C('111111')),
       'predator':(C('d0102a'),C('101010'),C('ffffff'),C('101010'))}
BOOT_ICON=[
"................","................","................","................",
"...............",
"..mmm......mmm..",
"..mam......mam..",
"..mmm......mmm..",
"..mmmm.....mmmm.",
"..mmammm...mmammm",
"..mmmmmmm..mmmmmmm",
"..sssssss..sssssss",
"...t.t.t....t.t.t.",
"................","................","................"]
for n,(m,a,s,t) in BOOTS.items():
    rows=[r[:16].ljust(16,'.') for r in BOOT_ICON]
    from_ascii(rows,{'m':m,'a':a,'s':s,'t':t}).save(A+f"textures/item/{n}_boots.png")
    l1=Image.new("RGBA",(64*S,32*S),T)
    def fn(f,X,Y,fw,fh,m=m,a=a,s=s,t=t,n=n):
        if f=='top': return None
        if f=='bottom': return t if (X//4+Y//4)%3==0 else s
        if Y<8.5*S: return None
        if Y>=fh-3: return s
        if n=='predator' and f in('front','right','left') and (X//5)%3==0 and Y<10.5*S: return a   # ребристая зона удара
        if n=='mercurial' and (X+Y)%14<3: return a        # «молнии»-полосы
        if n=='f50' and Y%10<2: return a
        if f=='front' and Y<9.2*S: return sh(m,1.1)
        return m
    paint(l1,0,16,4,12,4,fn)
    l1.save(A+f"textures/models/armor/{n}_layer_1.png")
    Image.new("RGBA",(64*S,32*S),T).save(A+f"textures/models/armor/{n}_layer_2.png")

# ---- captain armband ----
from_ascii([
"................","................","................","................",
"..yyyyyyyyyyyy..",
".ybbbbbbbbbbbby.",
".ybbbbYYYbbbbby.",
".ybbbYbbbbbbbby.",
".ybbbYbbbbbbbby.",
".ybbbbYYYbbbbby.",
".ybbbbbbbbbbbby.",
"..yyyyyyyyyyyy..",
"................","................","................","................"],
{'y':C('b8860b'),'b':C('ffd23f'),'Y':C('202020')}).save(A+"textures/item/captain_armband.png")
# ---- shin guards ----
from_ascii([
"................","...oooo..oooo...","..oWWWWooWWWWo..","..oWBBWooWBBWo..","..oWBBWooWBBWo..","..oWWWWooWWWWo..",
"..oWBBWooWBBWo..","..oWBBWooWBBWo..","..oWWWWooWWWWo..","..oWBBWooWBBWo..","..oWWWWooWWWWo..","...oWWo..oWWo...",
"...oooo..oooo...","................","................","................"],
{'o':C('1d2a44'),'W':C('e9eef5'),'B':C('3f7fd9')}).save(A+"textures/item/shin_guards.png")
# ---- energy drink can ----
from_ascii([
"................","......ssss......",".....sggggs.....",".....kkkkkk.....",".....kGGGGk.....",".....kGkkGk.....",
".....kGGGkk.....",".....kkGGGk.....",".....kGkkGk.....",".....kGGGGk.....",".....kkkkkk.....",".....kllllk.....",
".....kkkkkk.....",".....sggggs.....","......ssss......","................"],
{'s':C('8a8a8a'),'g':C('c9c9c9'),'k':C('121212'),'G':C('39ff14'),'l':C('1e90ff')}).save(A+"textures/item/energy_drink.png")
# ---- training cone block texture ----
cone=Image.new("RGBA",(16,16),T)
for y in range(16):
    for x in range(16):
        c=C('ff7a00')
        if 6<=y<=8: c=C('f5f5f5')
        if (x+y)%7==0: c=sh(c,0.92)
        cone.putpixel((x,y),c)
cone.save(A+"textures/block/training_cone.png")
print("ok")
