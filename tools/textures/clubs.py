# Клубные формы (Этап 8). Подключается через exec() после kits.py и части legends.py
# (нужны C, sh, fabric, solid). Используется в stage8.py (форма-броня) и realistic.py (CR7 2008/2017, LM10 2012).


def stripes(c1, c2, n=5):
    """Вертикальные полосы, как у «Барселоны»."""
    def f(face, X, Y, fw, fh):
        if face in ('front', 'back'): c = c1 if (X * n // fw) % 2 == 0 else c2
        elif face in ('left', 'right'): c = c1 if (X * 2 // fw) % 2 == 0 else c2
        else: c = c1
        return fabric(c, X, Y, fw)
    return f


GOLD_R = C('c9a24a'); PURPLE = C('3d1a5a'); RM_WHITE = C('f4f4f4')
BLAU = C('004d98'); GRANA = C('a50044'); NAVY_B = C('0c1b3a'); YEL_B = C('edbb00')
MU_RED = C('da020e'); BLK = C('121212')

CLUBS = dict(
    real=dict(shirt=solid(RM_WHITE), collar=GOLD_R, collar2=PURPLE, cuff=GOLD_R, cuff2=RM_WHITE, shorts=RM_WHITE,
              shorts_stripe=GOLD_R, sock=RM_WHITE, sock_band=GOLD_R, boot=C('1a1a1a'), boot2=GOLD_R,
              name='MADRID', num='7', numcol=C('20223a'), numout=GOLD_R, stars=0),
    barca=dict(shirt=stripes(BLAU, GRANA), collar=NAVY_B, collar2=YEL_B, cuff=NAVY_B, cuff2=GRANA, shorts=NAVY_B,
               shorts_stripe=GRANA, sock=NAVY_B, sock_band=GRANA, boot=C('ff6a10'), boot2=C('a83a00'),
               name='BARCA', num='10', numcol=YEL_B, numout=NAVY_B, stars=0),
    united=dict(shirt=solid(MU_RED), collar=BLK, collar2=C('ffffff'), cuff=C('ffffff'), cuff2=BLK, shorts=C('ffffff'),
                shorts_stripe=BLK, sock=BLK, sock_band=MU_RED, boot=C('d9b23a'), boot2=C('6a5210'),
                name='UNITED', num='7', numcol=C('ffffff'), numout=BLK, stars=0),
)
