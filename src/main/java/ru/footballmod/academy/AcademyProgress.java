package ru.footballmod.academy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Лучшие результаты игрока в академии (хранятся в самом игроке, переживают выход из мира и смерть).
 * shooting — попаданий из 3; dribbleTicks — лучшее время слалома (0 — ещё не прошёл); penalties — голов из 5;
 * rewards — уже выданные награды (биты: 1 — Predator, 2 — F50, 4 — Золотой свисток).
 */
public record AcademyProgress(int shooting, int dribbleTicks, int penalties, int rewards) {
    public static final AcademyProgress EMPTY = new AcademyProgress(0, 0, 0, 0);

    public static final Codec<AcademyProgress> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("shooting", 0).forGetter(AcademyProgress::shooting),
            Codec.INT.optionalFieldOf("dribble_ticks", 0).forGetter(AcademyProgress::dribbleTicks),
            Codec.INT.optionalFieldOf("penalties", 0).forGetter(AcademyProgress::penalties),
            Codec.INT.optionalFieldOf("rewards", 0).forGetter(AcademyProgress::rewards)
    ).apply(i, AcademyProgress::new));

    public AcademyProgress withShooting(int hits) {
        return new AcademyProgress(Math.max(shooting, hits), dribbleTicks, penalties, rewards);
    }

    public AcademyProgress withDribble(int ticks) {
        return new AcademyProgress(shooting, dribbleTicks == 0 ? ticks : Math.min(dribbleTicks, ticks), penalties, rewards);
    }

    public AcademyProgress withPenalties(int goals) {
        return new AcademyProgress(shooting, dribbleTicks, Math.max(penalties, goals), rewards);
    }

    public AcademyProgress withReward(int bit) {
        return new AcademyProgress(shooting, dribbleTicks, penalties, rewards | bit);
    }

    public boolean hasReward(int bit) {
        return (rewards & bit) != 0;
    }
}
