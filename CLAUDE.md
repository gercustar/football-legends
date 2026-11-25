# Minecraft Football Legends — памятка для Claude

Футбольный мод для Minecraft Java (Fabric). Этот файл — передача проекта между чатами:
прочитай его целиком перед любой работой.

## Пользователь и как с ним работать
- Пишет по-русски; отвечай по-русски, просто и по шагам (он школьник/начинающий, но
  мод — его проект и его решения). Иногда пишет на английской раскладке:
  `ghjdthm` = «проверь», `f gjxtve` = «а почему» — расшифровывай.
- Играет через **TLauncher**, профиль **Fabric 1.21.1**, Windows, ник ПК `LENOVO`.
- Хочет работать **поэтапно**: перед большим этапом — план и согласие. Договорённость:
  «делай лёгкое, а когда начнётся сложное (🔴) — остановись и скажи».
- Не начинай новый этап без его команды. Сейчас ждём его решения (см. «Что дальше»).
- Он **принял Minecraft EULA** (написал «принимаю Minecraft EULA») — можно запускать
  тестовый сервер (`run/eula.txt` с `eula=true`).

## Версии (не менять без причины)
| | |
|---|---|
| Minecraft | 1.21.1 |
| Yarn | 1.21.1+build.3 |
| Fabric Loader | 0.16.14 |
| Fabric API | 0.116.6+1.21.1 |
| Loom | 1.11.8 (Gradle 8.14 из системы, wrapper нет — команда `gradle`) |
| Java | 21 |
| Mod ID | `ronaldomessi` — **НЕ переименовывать**, иначе пропадут предметы в мирах |
| Отображаемое имя | Minecraft Football Legends |
| Текущая версия | 1.3.0 (`gradle.properties` → `mod_version`) |

Для сборки окружению нужен доступ к хостам: maven.fabricmc.net, meta.fabricmc.net,
libraries.minecraft.net, piston-meta.mojang.com, piston-data.mojang.com,
launchermeta.mojang.com, resources.download.minecraft.net, plugins.gradle.org
(уже добавлены в его облачное окружение Default).

## Структура
```
src/main/java/ru/footballmod/
  RonaldoMessiMod.java      — точка входа: FootballConfig.load → материалы → блоки → сущности → предметы
  ModArmorMaterials / ModBlocks / ModEntities / ModItems — регистрация
  config/FootballConfig.java — ВСЕ числа баланса; файл .minecraft/config/football_legends.json
  entity/FootballerEntity    — база футболистов: команды (Team), ИИ, KickBallGoal, способности мобов
  entity/{Ronaldo,Messi,Zidane,Ronaldinho,Neymar,Mbappe,Modric,Ramos}Entity
  entity/SoccerBallEntity    — мяч: физика, отскоки, пинок, дриблинг, ГОЛ + табло (scoreboard)
  block/GoalNetBlock (свойство scoring), block/TrainingConeBlock
  item/  SoccerBall, GoldenBall, Goal (ставит ворота 7×4), FootballArmor (комплекты CR7/LM10),
         FootballBoots (Mercurial/F50/Predator), CaptainArmband, ShinGuards, EnergyDrink
  client/ RonaldoMessiClient, FootballerRenderer (PlayerEntityModel), SoccerBallRenderer
src/main/resources/assets/ronaldomessi/ — текстуры, модели, lang (ru_ru + en_us, всегда оба)
src/main/resources/data/ronaldomessi/   — рецепты, лут-таблицы блоков
tools/textures/ — Python-генераторы всех текстур (см. ниже)
```

## Что уже сделано (по коммитам)
1. **v1.0.0** — Роналду и Месси (мобы, способности SIUUU/дриблинг, драка), мяч, форма
   (футболка/шорты/бутсы, бонус за комплект), Золотой мяч, ворота + голы + табло «Голы».
2. **Этап 0 (v1.1.0)** — Git/GitHub, имя «Football Legends», FootballConfig.
3. **Этап 3a (v1.2.0)** — бутсы Mercurial (+15% скорость, атрибут), F50 (мяч липнет при
   ведении), Predator (удар ×1.3 по взгляду); капитанская повязка (во 2-й руке);
   щитки (ПКМ — вставляются в надетые штаны через компонент ATTRIBUTE_MODIFIERS);
   энергетик; конусы (без коллизии).
4. **v1.3.0** — система команд `FootballerEntity.Team` (REAL_MADRID / BARCELONA) вместо
   одного соперника; 6 легенд: Зидан (рулетка), Роналдиньо (финт + чеканка), Неймар
   (эластико + «падение» с жёлтой карточкой), Мбаппе (турбо), Модрич (дирижёр + пасы
   партнёрам), Рамос (подкат + красная карточка). HD-скины сборных с именами на спине.

Все этапы проверены сборкой и тестовым сервером (без вылетов). В игре пользователь
проверил v1.0.0 (включая HD-форму и ворота, был исправлен краш в KickBallGoal);
v1.2.0 и v1.3.0 отправлены ему на установку — спроси, поставил ли и есть ли баги.

## Известные проблемы / идеи
- **Дисбаланс**: «Реал» 5 игроков, «Барса» 3 — на тесте Реал всегда побеждает.
  Предложено: добавить Хави/Иньесту/Суареса или усилить Барсу. Ждёт решения.
- Мяч считается и на клиенте, и на сервере → в мультиплеере может дёргаться (чинится в Мяче 2.0).
- `findNearestGoal()` перебирает ~13 тыс. блоков при каждом ударе моба — норм, но не идеально.
- Своих звуков нет (используются ванильные). Голос «SIUUU» Claude сделать не может —
  нужен .ogg от пользователя.
- Названия Nike/Adidas и клубов — товарные знаки: для себя ок, при публикации на
  Modrinth/CurseForge переименовать (они только в lang-файлах; id предметов нейтральные).

## Дорожная карта (согласована)
| # | Этап | Сложность | Статус |
|---|---|---|---|
| 0 | Фундамент | 🟢 | ✅ |
| 1 | **Мяч 2.0** | 🔴 | ⏸ ждёт команды «начинай Этап 1» |
| 2 | Способности игрока | 🔴 | — |
| 3 | Экипировка | 🟡 | ✅ кроме **Золотого свистка** (нужен матч) |
| 4 | Матч (команды, таймер, табло-блок, судья, болельщики) | 🔴 | — |
| 5 | Пенальти | 🟡 | — |
| 6 | Академия (тренер, задания) | 🟡 | — |
| 7 | Карьера (профиль, XP, характеристики, достижения) | 🔴 | — |
| 8 | Версии игроков (CR7 2008/2017, LM10 2012/2022) и клубные формы, Galáctico | 🟡 | — |
| 9 | Новые легенды | 🟡 | ✅ 6 штук |
| 10 | Стадионы | 🟡 | — |

### Согласованное управление (Этап 1 и 2)
- ЛКМ по мячу — обычный удар (уже есть).
- **X** зажать/отпустить — заряженный удар (полоска силы над хотбаром).
- **A/D** во время удара — закрутка влево/вправо.
- **Z** — пас (низкий, по взгляду). **Shift** у мяча — остановить мяч.
- Прыжок в летящий мяч — удар головой.
- **R, V, G** — три способности игрока (отдельные клавиши, выбор пользователя).
- Все клавиши — через KeyBindingHelper (категория «Football Legends»), удары — C2S-пакет
  (PayloadTypeRegistry), сервер проверяет дистанцию до мяча. Физику мяча — только на
  сервере, клиент интерполирует. Мяч помнит 2 последних касания (для ассистов).

## Сборка и тесты
```bash
gradle build --no-daemon -q          # jar → build/libs/ronaldo-messi-<версия>.jar
```
Смоук-тест сервером (EULA принята; мир flat; команды подаются в stdin):
```bash
mkdir -p run && echo "eula=true" > run/eula.txt
printf "online-mode=false\nlevel-type=minecraft\\:flat\nspawn-protection=0\n" > run/server.properties
( sleep 110; echo "forceload add -16 -16 16 16"; echo "summon ronaldomessi:ronaldo 0 -60 0"; sleep 60; echo stop ) \
  | timeout 400 gradle runServer --no-daemon -q > run/test.log 2>&1
grep -E "Exception|Done \(|ronaldomessi" run/test.log
```
Ошибки «No data fixer registered», «yggdrasil», «No key layers» в логе — нормальные.
Проверка атрибутов: `summon zombie ... {ArmorItems:[{id:"ronaldomessi:mercurial_boots",count:1},{},{},{}]}`
и `attribute @e[...] minecraft:generic.movement_speed get`.
Перед использованием API Minecraft сверяйся с javap по
`~/.gradle/caches/fabric-loom/minecraftMaven/.../minecraft-merged-1.21.1-...yarn...jar`.

## Текстуры
Генерируются кодом (Pillow), не рисуются вручную. Скрипты в `tools/textures/`,
запуск из корня репо с аргументом `src/main/resources`:
- `textures.py` — v1: иконки предметов, мяч, Золотой мяч, иконка мода. ⚠ Также
  перезаписывает скины/броню Роналду и Месси НИЗКИМ разрешением — после него обязательно `kits.py`.
- `kits.py` — HD (×8) форма и скины Роналду/Месси, текстуры ворот. Содержит шрифт 5×7,
  функции `shirt_details`, `sleeve`, `sock_boot` и т.д.
- `stage3.py` — бутсы (иконки + HD на ногах), повязка, щитки, энергетик, конус.
- `legends.py` — HD-скины 6 легенд (импортирует помощники из kits.py).
- `compress.py` — сжатие HD-текстур (обязательно после генерации, иначе jar раздувается).
- `prev.py` — превью формы спереди/сзади (запускать из папки textures).
HD-текстуры: 64×64 → 512×512 (S=8), броня 64×32 → 512×256 — Minecraft масштабирует UV сам.

## Как доставлять мод пользователю (важно!)
Скачать .jar из чата он **не может** (просмотрщик файлов показывает «двоичные данные»,
кнопки «Скачать» нет). Remote Control к его ПК **не работает** для его аккаунта
(папки видны, но `not_available`; пробовали AppData, Рабочий стол, латиницу, обновление).
Рабочий способ:
1. `base64 -w0 build/libs/ronaldo-messi-X.jar > ronaldo-messi-mod.txt`, отправить файл (SendUserFile).
2. Он открывает .txt в чате → клик в текст → Ctrl+A, Ctrl+C.
3. В PowerShell выполняет команду (подставь размер jar в байтах и версию):
```powershell
$d="$env:APPDATA\.minecraft\mods"; Read-Host "Skopiruy tekst moda (Ctrl+A, Ctrl+C) i nazhmi Enter"; $c=(Get-Clipboard -Raw) -replace '\r?\n',''; $m=[regex]::Matches($c,'[A-Za-z0-9+/]{1000,}={0,2}') | Sort-Object Length -Descending | Select-Object -First 1; try { $b=[Convert]::FromBase64String($m.Value); if ($b.Length -ne РАЗМЕР) { throw "size" }; Remove-Item "$d\ronaldo-messi*.jar" -ErrorAction SilentlyContinue; [IO.File]::WriteAllBytes("$d\ronaldo-messi-ВЕРСИЯ.jar",$b); Write-Host "MOD ВЕРСИЯ USTANOVLEN!" -ForegroundColor Green } catch { Write-Host ("Ne poluchilos. V bufere: " + $c.Length + " simvolov, naydeno: " + $m.Value.Length) -ForegroundColor Red }
```
Сообщения в команде — латиницей (кириллица в его консоли ок, но так надёжнее).
Сначала Read-Host, потом копирование текста — иначе в буфере окажется сама команда.
Fabric API 0.116.6+1.21.1 у него уже стоит.
Краши: просить вывод последнего файла из `%APPDATA%\.minecraft\crash-reports`.

## Git
- Репозиторий: https://github.com/gercustar/football-legends, ветка `main`, пушить сразу после этапа.
- Один коммит на этап, сообщение на английском с перечнем изменений.
- Собранные jar, run/, build/ — в .gitignore.
