package ru.footballmod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

    // --- Мяч ---
    /** Сила обычного удара игрока по мячу. */
    public double playerKickPower = 1.0;
    /** Множитель силы удара с разбега. */
    public double sprintKickMultiplier = 1.5;

    // --- Футболисты-мобы ---
    public double ronaldoKickPower = 1.6;
    public double messiKickPower = 1.3;
    /** Минимальная перезарядка способности моба (тики, 20 тиков = 1 сек). */
    public int mobAbilityCooldownMin = 100;
    /** Случайная добавка к перезарядке (тики). */
    public int mobAbilityCooldownRandom = 80;
    /** Шанс Месси увернуться от удара (0..1). */
    public float messiDodgeChance = 0.25f;
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

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                try (Reader r = Files.newBufferedReader(FILE)) {
                    FootballConfig loaded = GSON.fromJson(r, FootballConfig.class);
                    if (loaded != null) INSTANCE = loaded;
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
