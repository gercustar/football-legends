package ru.footballmod.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import ru.footballmod.config.FootballConfig;
import ru.footballmod.entity.SoccerBallEntity;
import ru.footballmod.item.FootballBootsItem;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Серверная часть управления мячом: все проверки и расчёт ударов — здесь. */
public final class BallControl {
    private static final int NOT_CHARGING = Integer.MIN_VALUE;

    private static final class State {
        int chargeStart = NOT_CHARGING;
        int lastKick = -1000;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    private BallControl() {}

    public static void init() {
        PayloadTypeRegistry.playC2S().register(BallActionPayload.ID, BallActionPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(BallActionPayload.ID,
                (payload, context) -> handle(context.player(), payload));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> STATES.remove(handler.player.getUuid()));
    }

    /** Вызывается в потоке сервера. */
    public static void handle(ServerPlayerEntity player, BallActionPayload payload) {
        State state = STATES.computeIfAbsent(player.getUuid(), id -> new State());
        int now = player.getServer().getTicks();
        switch (payload.action()) {
            case BallActionPayload.CHARGE_START -> state.chargeStart = now;
            case BallActionPayload.CHARGE_CANCEL -> state.chargeStart = NOT_CHARGING;
            case BallActionPayload.SHOT -> {
                int start = state.chargeStart;
                state.chargeStart = NOT_CHARGING;
                // Отпускание без начала зарядки — не удар
                if (start == NOT_CHARGING) return;
                float charge = MathHelper.clamp((now - start) / (float) Math.max(1, FootballConfig.get().chargeTicks), 0f, 1f);
                SoccerBallEntity ball = readyBall(player, state, now);
                if (ball == null) return;
                state.lastKick = now;
                shoot(player, ball, charge, Integer.signum(payload.curve()));
            }
            case BallActionPayload.PASS -> {
                SoccerBallEntity ball = readyBall(player, state, now);
                if (ball == null) return;
                state.lastKick = now;
                pass(player, ball);
            }
            default -> { }
        }
    }

    /** Мяч, по которому игрок сейчас имеет право ударить, или null. */
    @Nullable
    private static SoccerBallEntity readyBall(ServerPlayerEntity player, State state, int now) {
        FootballConfig cfg = FootballConfig.get();
        if (!player.isAlive() || player.isSpectator()) return null;
        if (now - state.lastKick < cfg.kickCooldownTicks) return null;
        Vec3d look = horizontalLook(player);
        SoccerBallEntity best = null;
        double bestD = cfg.ballReach * cfg.ballReach;
        for (SoccerBallEntity ball : player.getWorld().getEntitiesByClass(SoccerBallEntity.class,
                player.getBoundingBox().expand(cfg.ballReach), SoccerBallEntity::isAlive)) {
            double d = player.getBoundingBox().squaredMagnitude(ball.getBoundingBox().getCenter());
            if (d > bestD) continue;
            // Мяч за спиной не считается (если он не под ногами)
            Vec3d to = new Vec3d(ball.getX() - player.getX(), 0, ball.getZ() - player.getZ());
            if (to.lengthSquared() > 0.64 && to.normalize().dotProduct(look) < -0.3) continue;
            if (!player.canSee(ball)) continue; // через стену нельзя
            bestD = d;
            best = ball;
        }
        return best;
    }

    private static Vec3d horizontalLook(ServerPlayerEntity player) {
        return Vec3d.fromPolar(0, player.getYaw());
    }

    private static void shoot(ServerPlayerEntity player, SoccerBallEntity ball, float charge, int curve) {
        FootballConfig cfg = FootballConfig.get();
        double power = MathHelper.lerp(charge, cfg.shotMinPower, cfg.shotMaxPower);
        if (FootballBootsItem.typeOf(player) == FootballBootsItem.BootType.POWER) power *= cfg.predatorKickMultiplier;
        double cannon = PlayerAbilities.consumeCannon(player);
        power *= cannon;

        // Взгляд вниз (на мяч под ногами) не ослабляет удар; взгляд вверх — навес
        double up = Math.max(0, player.getRotationVector().y);
        double flat = Math.sqrt(1 - up * up) * power;
        Vec3d dir = horizontalLook(player);
        double lift = 0.12 + 0.18 * charge + (curve != 0 ? cfg.curveLift : 0);
        Vec3d velocity = new Vec3d(dir.x * flat, up * power + lift, dir.z * flat);
        // Закрученный мяч сначала уходит чуть в другую сторону
        velocity = SoccerBallEntity.turnLeft(velocity, -curve * Math.toRadians(cfg.curveStartAngle));
        ball.launch(velocity, curve * cfg.curveStrength, player);
        if (cannon > 1.0) ball.setFireTrail(40);

        player.swingHand(Hand.MAIN_HAND, true);
        ball.getWorld().playSound(null, ball.getX(), ball.getY(), ball.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_STRONG,
                SoundCategory.NEUTRAL, 0.8f + 0.6f * charge, 1.3f - 0.4f * charge);
    }

    private static void pass(ServerPlayerEntity player, SoccerBallEntity ball) {
        Vec3d dir = horizontalLook(player).multiply(FootballConfig.get().passPower);
        ball.launch(new Vec3d(dir.x, 0.06, dir.z), 0, player);
        player.swingHand(Hand.MAIN_HAND, true);
        ball.getWorld().playSound(null, ball.getX(), ball.getY(), ball.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_WEAK,
                SoundCategory.NEUTRAL, 0.9f, 1.3f);
    }
}
