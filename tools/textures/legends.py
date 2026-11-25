import sys
import os
SP=os.path.dirname(os.path.abspath(__file__))+'/'
src=open(SP+'kits.py').read().split('# ---------------- armor')[0]
sys.argv=[sys.argv[0], sys.argv[1]]
exec(src)   # helpers: C, sh, FONT, draw_text, faces, paint, face_origin, fabric, shirt_details, shirt_body, sleeve, shorts_leg, sock_boot ...
from PIL import Image
FONT.update({
'Z':["#####","....#","...#.","..#..",".#...","#....","#####"],
'H':["#...#","#...#","#...#","#####","#...#","#...#","#...#"],
'Y':["#...#","#...#",".#.#.","..#..","..#..","..#..","..#.."],
'J':["..###","...#.","...#.","...#.","...#.","#..#.",".##.."],
'B':["####.","#...#","#...#","####.","#...#","#...#","####."],
'P':["####.","#...#","#...#","####.","#....","#....","#...."],
'C':[".####","#....","#....","#....","#....","#....",".####"],
' ':["..","..","..","..","..","..",".."],
'4':["...#.","..##.",".#.#.","#..#.","#####","...#.","...#."],
})
def solid(col, alt=None, period=None):
    def f(face,X,Y,fw,fh):
        c=col
        if alt and period and ((X//period)+(Y//period))%2==1: c=alt
        return fabric(c,X,Y,fw)
    return f
NAVY_FR=C('1f2d5a'); RED_FR=C('e21b2c'); WHITE=C('f7f7f7')
YEL=C('ffdc02'); GRN=C('00923f'); BLU=C('1f3a93')
CRO_R=C('e21b2c'); SPA_R=C('c60b1e'); SPA_Y=C('f1bf00')
K=dict(
 zidane=dict(shirt=solid(NAVY_FR),collar=WHITE,collar2=RED_FR,cuff=WHITE,cuff2=RED_FR,shorts=WHITE,shorts_stripe=NAVY_FR,
   sock=RED_FR,sock_band=WHITE,boot=C('111111'),boot2=C('444444'),name='ZIDANE',num='10',numcol=WHITE,numout=C('101a38'),stars=2),
 mbappe=dict(shirt=solid(NAVY_FR),collar=C('c9a24a'),collar2=RED_FR,cuff=C('c9a24a'),cuff2=NAVY_FR,shorts=WHITE,shorts_stripe=RED_FR,
   sock=RED_FR,sock_band=NAVY_FR,boot=C('d7ff1f'),boot2=C('5a6a00'),name='MBAPPE',num='10',numcol=WHITE,numout=C('101a38'),stars=2),
 ronaldinho=dict(shirt=solid(YEL),collar=GRN,collar2=GRN,cuff=GRN,cuff2=YEL,shorts=BLU,shorts_stripe=WHITE,
   sock=WHITE,sock_band=GRN,boot=C('f5f5f5'),boot2=C('b0b0b0'),name='RONALDINHO',num='10',numcol=GRN,numout=C('fff59a'),stars=5),
 neymar=dict(shirt=solid(YEL),collar=GRN,collar2=BLU,cuff=GRN,cuff2=YEL,shorts=BLU,shorts_stripe=YEL,
   sock=WHITE,sock_band=YEL,boot=C('ff2d78'),boot2=C('7a0030'),name='NEYMAR JR',num='10',numcol=BLU,numout=C('fff59a'),stars=5),
 modric=dict(shirt=solid(CRO_R,WHITE,8),collar=C('0b2f8a'),collar2=CRO_R,cuff=C('0b2f8a'),cuff2=WHITE,shorts=WHITE,shorts_stripe=CRO_R,
   sock=C('0b2f8a'),sock_band=CRO_R,boot=C('ff7a00'),boot2=C('7a3a00'),name='MODRIC',num='10',numcol=C('0b2f8a'),numout=WHITE,stars=0),
 ramos=dict(shirt=solid(SPA_R),collar=SPA_Y,collar2=SPA_R,cuff=SPA_Y,cuff2=SPA_R,shorts=C('1f2a6b'),shorts_stripe=SPA_Y,
   sock=SPA_R,sock_band=SPA_Y,boot=C('101010'),boot2=C('ffffff'),name='RAMOS',num='4',numcol=SPA_Y,numout=C('5a0008'),stars=1),
)
EYE=C('2a1a10'); W=C('ffffff')
def head(skin,hair,style,beard=None,eye=EYE,teeth=False,band=None):
    def fn(f,X,Y,fw,fh):
        x,y=X//S,Y//S; px,py=X%S,Y%S
        H=lambda: fabric(hair,X,Y,fw)
        if f=='top':
            if style=='bald': return skin if not (x in(0,7) or y in(0,7)) else sh(skin,0.9)
            if style=='mohawk': return H() if 2<=x<=5 else sh(hair,0.55) if style=='mohawk' else skin
            if style=='buzz': return sh(hair,1.0) if (X+Y)%3 else sh(skin,0.6)
            return H()
        if f=='bottom': return beard or skin
        if f=='back':
            if style=='bald': return skin if y<4 else (sh(hair,0.9) if y<6 and (X+Y)%3 else skin)
            if style in('long','curly'): return H()
            if style=='mohawk': return H() if 2<=x<=5 and y<3 else (sh(hair,0.55) if y<6 else skin)
            if style=='buzz': return sh(hair,1.0) if y<5 and (X+Y)%3 else skin
            return H() if y<6 else skin
        if f=='front':
            if band and y==1: return band
            if style=='bald':
                pass
            elif style=='buzz':
                if y==0: return sh(hair,1.0) if (X+Y)%3 else sh(skin,0.6)
            elif style=='mohawk':
                if y==0 and 2<=x<=5: return H()
            elif style=='curly':
                if y<2 or (x in(0,7) and y<7): return H()
            elif style=='long':
                if y<2 or (x in(0,7) and y<6): return H()
            else:
                if y<2: return H()
            if y==3 and x in(1,2,5,6) and py<3: return sh(hair,0.9) if style!='bald' else sh(skin,0.7)
            if y==4 and x in(1,2,5,6):
                inner = (x in(2,5)) ; pupil = (x==2 and px<5) or (x==5 and px>=3)
                return eye if inner and pupil else W if x in (1,2,5,6) else skin
            if teeth and y==6 and 2<=x<=5:
                return W if 2<=py<=5 else C('6a2a20')
            if beard:
                if y==6 and 3<=x<=4 and 3<=py<=5: return C('6a2a20')
                if y>=6 or (y==5 and x in(0,1,6,7)): return fabric(beard,X,Y,fw)
            elif y==6 and 3<=x<=4 and 3<=py<=5: return C('7a3a30')
            if y==5 and x in(3,4) and py>=5: return sh(skin,0.85)
            return skin
        # sides
        near_face = x>=4 if f=='right' else x<=3
        if band and y==1: return band
        if style=='bald': return sh(hair,0.9) if (y in(3,4) and not near_face and (X+Y)%3) else skin
        if style in('long','curly'):
            if y<6 or not near_face: return H()
        elif style=='mohawk':
            if y<3 and not near_face: return sh(hair,0.55)
        elif style=='buzz':
            if y<3 and (X+Y)%3: return sh(hair,1.0)
        else:
            if y<2 or (y<4 and not near_face): return H()
        if beard and y>=4 and near_face: return fabric(beard,X,Y,fw)
        if y==4 and x==(2 if f=='right' else 5): return sh(skin,0.85)
        return skin
    return fn

def legend_skin(name,skin,hair,style,beard=None,teeth=False,band=None,tattoo=None):
    k=K[name]
    img=Image.new("RGBA",(64*S,64*S),T)
    paint(img,0,0,8,8,8,head(skin,hair,style,beard,teeth=teeth,band=band))
    paint(img,16,16,8,12,4,lambda f,X,Y,fw,fh: (k['shorts'] if Y>=9*S else shirt_body(k,9)(f,X,Y,fw,fh)) if f!='top' else k['shirt'](f,X,Y,fw,fh))
    shirt_details(img,dict(k,skin=skin),16,16,9)
    def armfn(f,X,Y,fw,fh):
        c=sleeve(k,5,skin)(f,X,Y,fw,fh)
        if tattoo and c==skin and f in('right','left','front','back') and 5*S<=Y<10*S and (X*3+Y*5)%11<3: return tattoo
        return c
    for (u,v) in ((40,16),(32,48)): paint(img,u,v,4,12,4,armfn)
    for (u,v) in ((0,16),(16,48)):
        paint(img,u,v,4,12,4,lambda f,X,Y,fw,fh: shorts_leg(k,4,below=None)(f,X,Y,fw,fh) or (skin if Y<5*S else sock_boot(k,5)(f,X,Y,fw,fh)) if f not in('top','bottom') else (k['shorts'] if f=='top' else k['boot2']))
    img.save(A+f"textures/entity/{name}.png", optimize=True)

legend_skin('zidane',  C('d9a882'), C('3a2a20'), 'bald')
legend_skin('mbappe',  C('6b4428'), C('140c08'), 'buzz')
legend_skin('ronaldinho', C('8a5a3a'), C('120c08'), 'curly', teeth=True, band=C('f5f5f5'))
legend_skin('neymar',  C('b07a52'), C('c9a24a'), 'mohawk', beard=None)
legend_skin('modric',  C('efc7a6'), C('8a6a40'), 'long')
legend_skin('ramos',   C('d2a07a'), C('2a1c14'), 'long', beard=C('2a1c14'), tattoo=C('3a3a5a'))
print('ok')
