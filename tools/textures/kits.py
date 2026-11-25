# HD kits (x8) for armor layers and mob skins + goal block textures
import sys, random
from PIL import Image
A = sys.argv[1] + "/assets/ronaldomessi/"
S = 8
rnd = random.Random(3)
def C(h,a=255): h=h.lstrip('#'); return (int(h[0:2],16),int(h[2:4],16),int(h[4:6],16),a)
def sh(c,k): return tuple(max(0,min(255,int(v*k))) for v in c[:3])+(c[3],)
T=(0,0,0,0)

FONT = {
'R':["####.","#...#","#...#","####.","#.#..","#..#.","#...#"],
'O':[".###.","#...#","#...#","#...#","#...#","#...#",".###."],
'N':["#...#","##..#","#.#.#","#..##","#...#","#...#","#...#"],
'A':[".###.","#...#","#...#","#####","#...#","#...#","#...#"],
'L':["#....","#....","#....","#....","#....","#....","#####"],
'D':["####.","#...#","#...#","#...#","#...#","#...#","####."],
'M':["#...#","##.##","#.#.#","#.#.#","#...#","#...#","#...#"],
'E':["#####","#....","#....","####.","#....","#....","#####"],
'S':[".####","#....","#....",".###.","....#","....#","####."],
'I':["###",".#.",".#.",".#.",".#.",".#.","###"],
'0':[".###.","#...#","#...#","#...#","#...#","#...#",".###."],
'1':["..#..",".##..","#.#..","..#..","..#..","..#..","#####"],
'7':["#####","....#","...#.","..#..",".#...",".#...",".#..."],
}
def text_w(t,sc=1): return sum(len(FONT[c][0])*sc+sc for c in t)-sc
def draw_text(img,x,y,t,col,sc=1,outline=None):
    pts=[]
    cx=x
    for ch in t:
        g=FONT[ch]
        for r,row in enumerate(g):
            for c,p in enumerate(row):
                if p=='#':
                    for dy in range(sc):
                        for dx in range(sc): pts.append((cx+c*sc+dx,y+r*sc+dy))
        cx+=len(g[0])*sc+sc
    if outline:
        s=set(pts)
        for (px,py) in pts:
            for ox in (-1,0,1):
                for oy in (-1,0,1):
                    q=(px+ox,py+oy)
                    if q not in s: img.putpixel(q,outline)
    for p in pts: img.putpixel(p,col)

def faces(u,v,w,h,d):
    return {'top':(u+d,v,w,d),'bottom':(u+d+w,v,w,d),'right':(u,v+d,d,h),
            'front':(u+d,v+d,w,h),'left':(u+d+w,v+d,d,h),'back':(u+2*d+w,v+d,w,h)}
def paint(img,u,v,w,h,d,fn,only=None):
    """fn(face, X, Y, fw, fh) in HD pixels (X,Y local to face)."""
    for f,(x0,y0,fw,fh) in faces(u,v,w,h,d).items():
        if only and f not in only: continue
        x0*=S;y0*=S;fw*=S;fh*=S
        for Y in range(fh):
            for X in range(fw):
                c=fn(f,X,Y,fw,fh)
                if c is not None: img.putpixel((x0+X,y0+Y),c)
def face_origin(u,v,w,h,d,f):
    x0,y0,fw,fh=faces(u,v,w,h,d)[f]; return x0*S,y0*S,fw*S,fh*S

def fabric(c,X,Y,fw):
    k=1.0+rnd.uniform(-0.025,0.025)
    edge=min(X,fw-1-X)
    if edge<3: k*=0.88
    if (X+Y)%9==0: k*=0.97
    return sh(c,k)

# ---------------- kit definitions ----------------
RED=C('b5121b'); RED2=C('8e0d14'); GREEN=C('00653a'); GOLD=C('e8b93a')
SKY=C('6cace4'); WHITE=C('f7f7f7'); NAVY=C('1c2340'); BLACK=C('161616')

def portugal_shirt(f,X,Y,fw,fh):
    base=RED
    # subtle darker diagonal pattern like the real kit
    if ((X+2*Y)//6)%5==0: base=sh(RED,0.93)
    return fabric(base,X,Y,fw)
def argentina_shirt(f,X,Y,fw,fh):
    if f in('front','back'):
        stripe=((X+ (fw//10)) // (fw//5))%2==0   # 5 stripes, sky in the middle & edges
        base=SKY if stripe else WHITE
    elif f in ('left','right'):
        base=SKY if (X//(fw//2))%2==0 else WHITE
    else: base=WHITE
    return fabric(base,X,Y,fw)

KITS={
 'ronaldo':dict(shirt=portugal_shirt,collar=GREEN,collar2=GOLD,cuff=GREEN,cuff2=RED2,
   shorts=GREEN,shorts_stripe=RED,sock=RED2,sock_band=GREEN,boot=C('f2c230'),boot2=C('b88a10'),
   name='RONALDO',num='7',numcol=GOLD,numout=C('5a0a0e'),stars=0),
 'messi':dict(shirt=argentina_shirt,collar=NAVY,collar2=GOLD,cuff=NAVY,cuff2=SKY,
   shorts=BLACK,shorts_stripe=SKY,sock=WHITE,sock_band=SKY,boot=C('ff6a10'),boot2=C('a83a00'),
   name='MESSI',num='10',numcol=NAVY,numout=WHITE,stars=3),
}

def shirt_details(img,k,u,v,full_h_rows):
    """Collar, badge, stars, front number, back name+number on body box at (u,v) 8x12x4."""
    fx,fy,fw,fh=face_origin(u,v,8,12,4,'front')
    # V-collar
    for Y in range(14):
        for X in range(fw):
            dx=abs(X-(fw-1)/2)
            if dx < 12-Y*0.8 and dx > 9-Y*0.8: img.putpixel((fx+X,fy+Y),k['collar'])
            elif dx <= 9-Y*0.8 and Y<10 and k is not None and k.get('skin'): img.putpixel((fx+X,fy+Y),k['skin'])
    for X in range(fw):  # collar back band on top edge
        img.putpixel((fx+X,fy),k['collar']); img.putpixel((fx+X,fy+1),k['collar2'])
    # badge on player's left chest (viewer's right)
    bx,by=fx+fw-22,fy+14
    for Y in range(11):
        for X in range(9):
            if Y<8 or abs(X-4)<=10-Y:
                edge = X in(0,8) or Y==0 or (Y>=8 and abs(X-4)==10-Y)
                img.putpixel((bx+X,by+Y), k['collar2'] if edge else (k['collar'] if Y>3 else WHITE))
    for i in range(k['stars']):  # world cup stars
        sx=bx+i*3; sy=by-4
        for (ox,oy) in ((1,0),(0,1),(1,1),(2,1),(1,2)): img.putpixel((sx+ox,sy+oy),GOLD)
    # small front number (center chest)
    draw_text(img,fx+(fw-text_w(k['num'],2))//2,fy+30,k['num'],k['numcol'],2,k['numout'])
    # back: name + big number
    bx,by,bw,bh=face_origin(u,v,8,12,4,'back')
    for X in range(bw):
        img.putpixel((bx+X,by),k['collar']); img.putpixel((bx+X,by+1),k['collar2'])
    draw_text(img,bx+(bw-text_w(k['name']))//2,by+8,k['name'],k['numcol'],1,k['numout'])
    draw_text(img,bx+(bw-text_w(k['num'],4))//2,by+19,k['num'],k['numcol'],4,k['numout'])

def shirt_body(k,rows):
    def fn(f,X,Y,fw,fh):
        if f=='bottom' and rows<12: return None
        if f not in('top','bottom') and Y>=rows*S: return None
        if f not in('top','bottom') and Y>=rows*S-3: return sh(k['collar'],1.0)  # hem
        return k['shirt'](f,X,Y,fw,fh)
    return fn
def sleeve(k,rows,skin=None):
    def fn(f,X,Y,fw,fh):
        if f=='top': return k['shirt']('left',X,Y,fw,fh)
        if f=='bottom': return skin
        if Y<rows*S-6: return k['shirt']('left',X,Y,fw,fh)
        if Y<rows*S-3: return k['cuff']
        if Y<rows*S: return k['cuff2']
        return skin
    return fn
def shorts_leg(k,rows,skin=None,below=None):
    def fn(f,X,Y,fw,fh):
        if f=='top': return k['shorts']
        if f=='bottom': return below
        if Y<rows*S:
            if f in('right','left') and 13<=X<=18: return k['shorts_stripe']
            if Y>=rows*S-2: return sh(k['shorts'],0.8)
            return fabric(k['shorts'],X,Y,fw)
        return below
    return fn
def sock_boot(k,start,top_face=None):
    def fn(f,X,Y,fw,fh):
        if f=='top': return top_face
        if f=='bottom': return sh(k['boot2'],0.8)
        if Y<start*S: return None
        if Y<(start+1)*S: return k['sock_band']
        if Y<9.5*S: return fabric(k['sock'],X,Y,fw)
        # boots
        if Y>=fh-2: return k['boot2']
        if f=='front' and Y<10.5*S: return sh(k['boot'],0.9)
        if (X//3+Y//3)%7==0: return sh(k['boot'],1.1)
        return k['boot']
    return fn

# ---------------- armor ----------------
for name,k in KITS.items():
    l1=Image.new("RGBA",(64*S,32*S),T)
    paint(l1,16,16,8,12,4,shirt_body(k,10))
    shirt_details(l1,dict(k,skin=None),16,16,10)
    paint(l1,40,16,4,12,4,sleeve(k,5))
    paint(l1,0,16,4,12,4,sock_boot(k,5))
    l1.save(A+f"textures/models/armor/{name}_layer_1.png")
    l2=Image.new("RGBA",(64*S,32*S),T)
    paint(l2,16,16,8,12,4,lambda f,X,Y,fw,fh,k=k: None if f=='top' or Y<8*S else (k['shorts'] if Y<11*S else k['shorts_stripe']) if f!='bottom' else k['shorts'])
    paint(l2,0,16,4,12,4,shorts_leg(k,5))
    l2.save(A+f"textures/models/armor/{name}_layer_2.png")

# ---------------- mob skins ----------------
def head(skin,hair,eye,beard=None,brows=None):
    sk=skin
    def fn(f,X,Y,fw,fh):
        x,y=X//S,Y//S
        if f=='top': return fabric(hair,X,Y,fw)
        if f=='bottom': return beard or sk
        if f=='back': return fabric(hair,X,Y,fw) if y<7 else sk
        if f=='front':
            if y<2: return fabric(hair,X,Y,fw)
            if y==2 and (x in(0,7)): return hair
            if y==3 and x in (1,2,5,6) and Y%S<3: return brows or hair
            if y==4 and x in(1,6): return WHITE if not (x==1 and X%S>=4 or x==6 and X%S<4) else eye
            if y==4 and x in(2,5): return eye if (x==2 and X%S<4 or x==5 and X%S>=4) else WHITE
            if beard:
                if y==6 and 3<=x<=4 and 2<=Y%S<=5: return C('7a3b2e')
                if y>=6 or (y==5 and x in(0,1,6,7)) or (y==5 and x in(2,5) and Y%S>4): return fabric(beard,X,Y,fw)
            else:
                if y==6 and 3<=x<=4 and 3<=Y%S<=5: return C('8a4a3a')
            if y==5 and x in (3,4) and Y%S>=5: return sh(sk,0.85)
            return sk
        # sides
        if y<2: return fabric(hair,X,Y,fw)
        near_face = x>=4 if f=='right' else x<=3
        if y<4 and not near_face: return fabric(hair,X,Y,fw)
        if beard and y>=4 and near_face: return fabric(beard,X,Y,fw)
        if y==4 and x==(2 if f=='right' else 5): return sh(sk,0.85)
        return sk
    return fn

def make_skin(k,skin,hair,eye,beard=None):
    img=Image.new("RGBA",(64*S,64*S),T)
    paint(img,0,0,8,8,8,head(skin,hair,eye,beard))
    paint(img,16,16,8,12,4,lambda f,X,Y,fw,fh: (k['shorts'] if Y>=9*S else shirt_body(k,9)(f,X,Y,fw,fh)) if f!='top' else k['shirt'](f,X,Y,fw,fh))
    shirt_details(img,dict(k,skin=skin),16,16,9)
    for (u,v) in ((40,16),(32,48)): paint(img,u,v,4,12,4,sleeve(k,5,skin))
    for (u,v) in ((0,16),(16,48)):
        paint(img,u,v,4,12,4,lambda f,X,Y,fw,fh: shorts_leg(k,4,below=None)(f,X,Y,fw,fh) or (skin if Y<5*S else sock_boot(k,5)(f,X,Y,fw,fh)) if f not in('top','bottom') else (k['shorts'] if f=='top' else k['boot2']))
    return img
make_skin(KITS['ronaldo'],C('c99a74'),C('1d1410'),C('3b2a1a')).save(A+"textures/entity/ronaldo.png")
make_skin(KITS['messi'],C('e2b48f'),C('5a3a22'),C('3d5a2a'),beard=C('5f3d24')).save(A+"textures/entity/messi.png")

# ---------------- goal blocks + goal item (16x16) ----------------
import os
os.makedirs(A+"textures/block",exist_ok=True)
post=Image.new("RGBA",(16,16))
for y in range(16):
    for x in range(16):
        k=0.97+0.03*((x*7+y*3)%5)/4
        if x in(0,15) or y in(0,15): k=0.85
        post.putpixel((x,y),sh(C('f4f4f4'),k))
post.save(A+"textures/block/goal_post.png")
net=Image.new("RGBA",(16,16),T)
for y in range(16):
    for x in range(16):
        if x%4==0 or y%4==0: net.putpixel((x,y),C('eeeeee'))
        if (x%4==0 and y%4==0): net.putpixel((x,y),C('c8c8c8'))
net.save(A+"textures/block/goal_net.png")
g=Image.new("RGBA",(16,16),T)
for y in range(3,15):
    for x in range(1,15):
        if x in(1,2,13,14) or y in(3,4): g.putpixel((x,y),C('f4f4f4') if not (x in(1,14) or y==3) else C('bdbdbd'))
        elif (x%3==0 or y%3==0): g.putpixel((x,y),C('9a9a9a'))
g.save(A+"textures/item/goal.png")
print("ok")
