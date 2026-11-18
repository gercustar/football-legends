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
