package ru.footballmod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Все числа баланса мода в одном месте.
 * Файл: .minecraft/config/football_legends.json — создаётся при первом запуске,
 * его можно править без пересборки мода (нужен перезапуск игры/сервера).
 * Новые поля добавляются в файл автоматически со значениями по умолчанию.
 */
public class FootballConfig {
    private static final Logger LOG = LoggerFactory.getLogger("football_legends");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("football_legends.json");

    public static FootballConfig INSTANCE = new FootballConfig();

    private static final int VERSION = 4;
    /** Служебное: версия файла настроек, менять не нужно. */
    public int configVersion = VERSION;

    // --- Мяч ---
    /** Сила обычного удара игрока по мячу. */
    public double playerKickPower = 1.0;
    /** Множитель силы удара с разбега. */
    public double sprintKickMultiplier = 1.5;

    /** Дистанция от игрока до мяча, на которой работают удар с зарядкой и пас (блоки). */
    public double ballReach = 2.5;
    /** Минимум тиков между ударами и пасами одного игрока. */
    public int kickCooldownTicks = 6;
    /** Время полной зарядки удара (тики, 20 = 1 сек). */
    public int chargeTicks = 20;
    /** Сила удара без зарядки и с полной зарядкой. */
    public double shotMinPower = 0.7;
    public double shotMaxPower = 2.2;
    /** Закрутка: поворот мяча за тик в начале полёта (радианы). */
    public double curveStrength = 0.075;
    /** Во сколько раз закрутка слабеет за тик в воздухе. */
    public double curveDecay = 0.94;
    /** Дополнительное ослабление закрутки за тик на земле. */
    public double curveGroundDecay = 0.7;
    /** «Банан»: насколько закрученный мяч сначала уходит в другую сторону (градусы), потом загибается обратно. */
    public double curveStartAngle = 32.0;
    /** Насколько выше летит закрученный мяч. */
    public double curveLift = 0.06;
    /** Сила паса. */
    public double passPower = 1.0;
    /** Сила удара головой. */
    public double headerPower = 1.3;
    /** Насколько близко должен быть мяч для удара головой (блоки). */
    public double headerReach = 0.35;
    /** Насколько близко должен быть мяч, чтобы остановить его Shift (блоки). */
    public double ballStopReach = 0.9;

    // --- Способности игрока (Этап 2) ---
    /** R — финт: сила шага вбок, радиус и длительность замедления соперников (тики). */
    public double feintPower = 0.9;
    public double feintRadius = 3.0;
    public int feintSlowTicks = 30;
    public int feintCooldownTicks = 120;
    /** V — рывок: сила толчка вперёд и длительность ускорения (тики). */
    public double dashPower = 1.4;
    public int dashSpeedTicks = 30;
    public int dashCooldownTicks = 160;
    /** G — пушка: во сколько раз сильнее следующий удар B и сколько тиков действует заряд. */
    public double cannonMultiplier = 1.5;
    public int cannonDurationTicks = 200;
    public int cannonCooldownTicks = 300;

    // --- Матч (Этап 4) ---
    /** Длина матча по кнопке «5 минут» (минуты; два тайма пополам). */
    public int matchMinutes = 5;
    /** Пауза перед начальным ударом и после гола (тики). */
    public int kickoffPauseTicks = 60;
    /** Перерыв между таймами (тики). */
    public int halftimePauseTicks = 100;
    /** На сколько удаляют после красной карточки (тики). */
    public int sentOffTicks = 1200;
    /** Как далеко от табло искать ворота (блоки). */
    public int matchSearchRadius = 48;
    /** Половина ширины поля для невидимых бортов в матче (блоки от середины до боковой линии; у стадиона — 13). */
    public double pitchHalfWidth = 13.0;
    /** Сколько болельщиков сажать на трибуны. */
    public int maxFans = 40;
    /** Футболисты в матче бьют по воротам только с этого расстояния (блоки), а издалека ведут мяч. */
    public double botShotDistance = 16.0;
    /** Сила ведения мяча ботом в матче. */
    public double botDribblePower = 0.55;
    /** Вратарь-бот выходит к мячу, если тот ближе этого к воротам (блоки). */
    public double keeperRushDistance = 7.0;
    /** Вратарь-бот: шанс поймать удар и как далеко он дотягивается (блоки). */
    public float keeperSaveChance = 0.6f;
    public double keeperReach = 1.6;
    /** Сила выбивания мяча вратарём. */
    public double keeperClearPower = 1.25;
    /** Самый сильный удар бота в матче (чтобы мяч не улетал с поля). */
    public double botShotMaxPower = 1.35;
    /** Неточность удара бота в матче (± градусы). */
    public double botShotSpreadDegrees = 20.0;

    // --- Пенальти (Этап 5) ---
    /** Пауза перед свистком на каждый удар (тики). */
    public int penaltyWaitTicks = 50;
    /** Сколько тиков игрок может прицеливаться перед ударом. */
    public int penaltyPlayerTimeTicks = 200;
    /** Ставить ли игрока защищающейся команды в ворота (false — всегда вратарь-бот, игрок только бьёт). */
    public boolean penaltyPlayersKeepGoal = false;
    /** Шанс вратаря-бота угадать угол (0..1). */
    public float keeperReadChance = 0.45f;

    // --- Футболисты-мобы ---
    public double ronaldoKickPower = 1.6;
    public double messiKickPower = 1.3;
    /** Минимальная перезарядка способности моба (тики, 20 тиков = 1 сек). */
    public int mobAbilityCooldownMin = 100;
    /** Случайная добавка к перезарядке (тики). */
    public int mobAbilityCooldownRandom = 80;
    /** Шанс Месси увернуться от удара (0..1). */
    public float messiDodgeChance = 0.25f;
    /** Шанс Неймара «упасть» от удара без урона (0..1). */
    public float neymarDiveChance = 0.3f;
    /** Шанс, что Рамос получит красную карточку после подката (0..1). */
    public float ramosRedCardChance = 0.15f;
    public double xaviKickPower = 1.2;
    /** Сколько здоровья Хави возвращает себе и партнёрам способностью (2 = одно сердце). */
    public float xaviHeal = 4.0f;
    public double iniestaKickPower = 1.3;
    /** Шанс Иньесты уйти от удара (0..1). */
    public float iniestaDodgeChance = 0.2f;
    public double suarezKickPower = 1.6;
    /** Сколько здоровья Суарес получает за укус. */
    public float suarezBiteHeal = 4.0f;
    /** Шанс, что Суарес получит красную карточку за укус (0..1). */
    public float suarezRedCardChance = 0.2f;
    /** Вес естественного появления новых легенд в мире (0 — не появляются). */
    public int legendsSpawnWeight = 2;
    /** Шанс выпадения Золотого мяча, если футболиста победил не соперник. */
    public float goldenBallDropChance = 0.4f;

    // --- Бутсы ---
    /** Mercurial: прибавка к скорости бега (0.15 = +15%). */
    public double mercurialSpeedBonus = 0.15;
    /** F50: небольшая прибавка к скорости. */
    public double f50SpeedBonus = 0.05;
    /** F50: сила толчка мяча при ведении (меньше — мяч ближе к ногам). */
    public double f50DribblePush = 0.32;
    /** Predator: множитель силы удара. */
    public double predatorKickMultiplier = 1.3;

    // --- Экипировка ---
    /** Радиус, в котором капитан ускоряет игроков рядом. */
    public double captainAuraRadius = 10.0;
    public double shinGuardsKnockbackResistance = 0.4;
    public double shinGuardsArmor = 2.0;
    /** Длительность энергетика (тики). */
    public int energyDrinkDuration = 400;
    /** Перезарядка энергетика (тики). */
    public int energyDrinkCooldown = 900;

    // --- Предметы ---
    /** Перезарядка Золотого мяча (тики). */
    public int goldenBallCooldown = 1200;

    /** Закрутку меняли в 1.6.0, 1.7.0 и 1.8.1 (дуга). Меняем только нетронутые старые значения, настройки игрока сохраняем. */
    private void migrate(int from) {
        FootballConfig fresh = new FootballConfig();
        if ((from < 2 && curveStartAngle == 8.0) || (from < 4 && curveStartAngle == 4.0)) curveStartAngle = fresh.curveStartAngle;
        if ((from < 2 && curveStrength == 0.045) || (from < 3 && curveStrength == 0.06) || (from < 4 && curveStrength == 0.085)) curveStrength = fresh.curveStrength;
    }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                try (Reader r = Files.newBufferedReader(FILE)) {
                    JsonObject json = GSON.fromJson(r, JsonObject.class);
                    FootballConfig loaded = GSON.fromJson(json, FootballConfig.class);
                    if (loaded != null) {
                        int from = json.has("configVersion") ? json.get("configVersion").getAsInt() : 1;
                        if (from < VERSION) loaded.migrate(from);
                        loaded.configVersion = VERSION;
                        INSTANCE = loaded;
                    }
                }
            }
            try (Writer w = Files.newBufferedWriter(FILE)) {
                GSON.toJson(INSTANCE, w);   // дописывает новые поля
            }
        } catch (Exception e) {
            LOG.error("Не удалось прочитать {} — используются значения по умолчанию", FILE, e);
            INSTANCE = new FootballConfig();
        }
    }

    public static FootballConfig get() {
        return INSTANCE;
    }
}
