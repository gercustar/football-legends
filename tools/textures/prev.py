from PIL import Image
S=8
def faces(u,v,w,h,d):
    return {'right':(u,v+d,d,h),'front':(u+d,v+d,w,h),'left':(u+d+w,v+d,d,h),'back':(u+2*d+w,v+d,w,h)}
def crop(img,u,v,w,h,d,f,sc):
    x,y,fw,fh=faces(u,v,w,h,d)[f]; s=img.width//64
    return img.crop((x*s,y*s,(x+fw)*s,(y+fh)*s)).resize((fw*sc,fh*sc),Image.NEAREST)
def view(skin,l1,l2,side):
    sc=S; W=Image.new("RGBA",(16*sc+20,32*sc+20),(120,170,120,255))
    parts=[((0,0,8,8,8),(4,0),'h'),((16,16,8,12,4),(4,8),'b'),((40,16,4,12,4),(0,8) if side=='front' else (12,8),'a'),((32,48,4,12,4),(12,8) if side=='front' else (0,8),'a'),
           ((0,16,4,12,4),(4,20) if side=='front' else (8,20),'l'),((16,48,4,12,4),(8,20) if side=='front' else (4,20),'l')]
    for (uv,pos,kind) in parts:
        im=crop(skin,*uv,side,sc); W.alpha_composite(im,(10+pos[0]*sc,10+pos[1]*sc))
        if kind in 'bal':
            u,v,w,h,d=uv
            if (u,v)==(32,48): u,v=40,16
            if (u,v)==(16,48): u,v=0,16
            for L in ([l2] if kind=='l' else [])+[l1]+([l2] if kind=='b' else []):
                W.alpha_composite(crop(L,u,v,w,h,d,side,sc),(10+pos[0]*sc,10+pos[1]*sc))
    return W
out=Image.new("RGBA",(4*(16*8+20)+30,32*8+20),(60,60,60,255))
x=0
for n in ('ronaldo','messi'):
    sk=Image.open(f'entity/{n}.png').convert('RGBA')
    l1=Image.open(f'models/armor/{n}_layer_1.png'); l2=Image.open(f'models/armor/{n}_layer_2.png')
    for side in ('front','back'):
        out.alpha_composite(view(sk,l1,l2,side),(x,0)); x+=16*8+30
import sys; out.save(sys.argv[1] if len(sys.argv)>1 else 'kits_preview.png')
