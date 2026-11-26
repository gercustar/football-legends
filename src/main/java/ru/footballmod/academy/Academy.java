package ru.footballmod.academy;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.footballmod.ModItems;
import ru.footballmod.RonaldoMessiMod;
import ru.footballmod.entity.CoachEntity;
import ru.footballmod.network.AcademyOpenPayload;
import ru.footballmod.network.AcademyStartPayload;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Академия (Этап 6): тренер даёт задания, лучшие результаты хранятся в игроке, за успехи — награды. */
public final class Academy {
    public static final int SHOOTING = 0, DRIBBLE = 1, PENALTIES = 2;
    public static final int REWARD_PREDATOR = 1, REWARD_F50 = 2, REWARD_WHISTLE = 4;

    public static final AttachmentType<AcademyProgress> PROGRESS = AttachmentRegistry.<AcademyProgress>builder()
            .persistent(AcademyProgress.CODEC)
            .copyOnDeath()
            .initializer(() -> AcademyProgress.EMPTY)
            .buildAndRegister(RonaldoMessiMod.id("academy"));

    private static final Map<UUID, Drill> DRILLS = new HashMap<>();

    private Academy() {}

    public static void init() {
        PayloadTypeRegistry.playS2C().register(AcademyOpenPayload.ID, AcademyOpenPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(AcademyStartPayload.ID, AcademyStartPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(AcademyStartPayload.ID, (p, ctx) -> start(ctx.player(), p.coachId(), p.drill()));
        ServerTickEvents.END_SERVER_TICK.register(Academy::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            Drill d = DRILLS.remove(handler.player.getUuid());
            if (d != null) d.cleanup();
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> { DRILLS.values().forEach(Drill::cleanup); DRILLS.clear(); });
    }

    public static AcademyProgress progress(ServerPlayerEntity p) {
        return p.getAttachedOrCreate(PROGRESS);
    }

    /** ПКМ по тренеру: меню с заданиями и лучшими результатами. */
    public static void open(ServerPlayerEntity player, CoachEntity coach) {
        AcademyProgress pr = progress(player);
        ServerPlayNetworking.send(player, new AcademyOpenPayload(coach.getId(), pr.shooting(), pr.dribbleTicks(), pr.penalties(), DRILLS.containsKey(player.getUuid())));
    }

    /** Текущее задание игрока (для проверок). */
    static Drill drill(UUID player) {
        return DRILLS.get(player);
    }

    public static boolean active(ServerPlayerEntity p) {
        return DRILLS.containsKey(p.getUuid());
    }

    public static void start(ServerPlayerEntity player, int coachId, int drill) {
        Entity coach = player.getServerWorld().getEntityById(coachId);
        if (!(coach instanceof CoachEntity) || player.squaredDistanceTo(coach) > 64) return;
        Drill old = DRILLS.remove(player.getUuid());
        if (old != null) {   // повторное нажатие — отменить текущее задание
            old.cleanup();
            player.sendMessage(Text.translatable("msg.ronaldomessi.drill_cancelled"), false);
            return;
        }
        Drill d = switch (drill) {
            case SHOOTING -> new Drills.Shooting(player.getServerWorld(), player);
            case DRIBBLE -> new Drills.Dribble(player.getServerWorld(), player);
            case PENALTIES -> new Drills.Penalties(player.getServerWorld(), player);
            default -> null;
        };
        if (d == null) return;
        if (!d.setup()) {
            d.cleanup();
            player.sendMessage(Text.translatable("msg.ronaldomessi.drill_no_space").formatted(Formatting.RED), false);
            return;
        }
        DRILLS.put(player.getUuid(), d);
        coachSays(player, "msg.ronaldomessi.coach_start_" + drill);
    }

    private static void tick(MinecraftServer server) {
        DRILLS.entrySet().removeIf(e -> {
            Drill d = e.getValue();
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(e.getKey());
            if (p == null || p.getServerWorld() != d.world) { d.cleanup(); return true; }
            d.tick(p);
            if (d.ticks % 5 == 0) d.bar.setName(d.barText());
            if (!d.done) return false;
            finish(p, d);
            d.cleanup();
            return true;
        });
    }

    private static void finish(ServerPlayerEntity p, Drill d) {
        AcademyProgress pr = progress(p);
        if (d instanceof Drills.Shooting s) {
            pr = pr.withShooting(s.hits);
            p.addExperienceLevels(s.hits * 2);
            coachSays(p, s.hits == 3 ? "msg.ronaldomessi.coach_shooting_perfect" : "msg.ronaldomessi.coach_shooting", s.hits, s.shots);
            if (s.hits == 3 && !pr.hasReward(REWARD_PREDATOR)) pr = reward(p, pr, REWARD_PREDATOR, new ItemStack(ModItems.PREDATOR_BOOTS));
        } else if (d instanceof Drills.Dribble dr) {
            if (dr.success) {
                pr = pr.withDribble(dr.ticks);
                p.addExperienceLevels(5);
                coachSays(p, "msg.ronaldomessi.coach_dribble_done", Drill.clock(dr.ticks));
                if (!pr.hasReward(REWARD_F50)) pr = reward(p, pr, REWARD_F50, new ItemStack(ModItems.F50_BOOTS));
            } else {
                coachSays(p, "msg.ronaldomessi.coach_dribble_fail", dr.next, Drills.Dribble.CONES);
            }
        } else if (d instanceof Drills.Penalties pen) {
            int g = Math.max(0, pen.goals);
            pr = pr.withPenalties(g);
            p.addExperienceLevels(g * 2);
            coachSays(p, g == 5 ? "msg.ronaldomessi.coach_pen_perfect" : "msg.ronaldomessi.coach_pen", g);
            if (g == 5 && !pr.hasReward(REWARD_WHISTLE)) pr = reward(p, pr, REWARD_WHISTLE, new ItemStack(ModItems.GOLDEN_WHISTLE));
        }
        p.setAttached(PROGRESS, pr);
    }

    private static AcademyProgress reward(ServerPlayerEntity p, AcademyProgress pr, int bit, ItemStack item) {
        p.getInventory().offerOrDrop(item);
        p.sendMessage(Text.translatable("msg.ronaldomessi.coach_reward", item.getName()).formatted(Formatting.GOLD), false);
        return pr.withReward(bit);
    }

    static void coachSays(ServerPlayerEntity p, String key, Object... args) {
        p.sendMessage(Text.translatable("msg.ronaldomessi.coach_prefix").append(Text.translatable(key, args)), false);
    }
}
