# Сжатие HD-текстур: 48-64 цвета, сохранить как RGBA (Minecraft читает их без проблем).
# Запуск из корня репозитория:  python3 tools/textures/compress.py
import glob, os
from PIL import Image
base = "src/main/resources/assets/ronaldomessi/textures/"
for f in glob.glob(base + "entity/*.png") + glob.glob(base + "models/armor/*.png"):
    im = Image.open(f).convert("RGBA")
    im.quantize(colors=64, method=Image.Quantize.FASTOCTREE).convert("RGBA").save(f, optimize=True)
    print(f, os.path.getsize(f))
