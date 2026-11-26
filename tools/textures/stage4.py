# Этап 4: текстуры блока «Табло». Запуск из корня репозитория:  python tools/textures/stage4.py src/main/resources
import sys
from PIL import Image

A = sys.argv[1] + "/assets/ronaldomessi/textures/block/"


def C(h):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


SIDE = ["kkkkkkkkkkkkkkkk",
        "kddddddddddddddk",
        "kdwwwdddddrrrrdk",
        "kdwddddddddrrddk",
        "kddddddddddddddk",
        "kdoooddddddoooddk",
        "kdododdooddododdk",
        "kdododdddddododdk",
        "kdododdooddododdk",
        "kdoooddddddoooddk",
        "kddddddddddddddk",
        "kdyyyyyyyyyyyydk",
        "kdyyyyyyyddddddk",
        "kddddddddddddddk",
        "kddddddddddddddk",
        "kkkkkkkkkkkkkkkk"]
PAL = dict(k=C('5a5a5a'), d=C('111318'), w=C('f4f4f4'), r=C('a50044'), o=C('ff9a1f'), y=C('3fbf3a'))
im = Image.new("RGBA", (16, 16))
for y, row in enumerate(SIDE):
    for x, ch in enumerate(row[:16]):
        im.putpixel((x, y), PAL[ch])
im.save(A + "match_board.png")
top = Image.new("RGBA", (16, 16))
for y in range(16):
    for x in range(16):
        top.putpixel((x, y), C('5a5a5a') if x in (0, 15) or y in (0, 15) else C('2a2c33') if (x + y) % 5 else C('33363e'))
top.save(A + "match_board_top.png")
print("ok")
