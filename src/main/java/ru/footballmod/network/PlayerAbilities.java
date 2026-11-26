package ru.footballmod.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import ru.footballmod.config.FootballConfig;
import ru.footballmod.entity.FootballerEntity;
import ru.footballmod.entity.SoccerBallEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Способности игрока (Этап 2): R — финт, V — рывок, G — пушка. Перезарядки считает только сервер. */
public final class PlayerAbilities {
    private static final class State {
        final int[] readyAt = new int[AbilityPayload.COUNT];
        int cannonUntil = -1;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    private PlayerAbilities() {}

    public static void init() {
        PayloadTypeRegistry.playC2S().register(AbilityPayload.ID, AbilityPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AbilityStatePayload.ID, AbilityStatePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(AbilityPayload.ID, (payload, ctx) -> handle(ctx.player(), payload));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> STATES.remove(handler.player.getUuid()));
    }

    private static State state(ServerPlayerEntity p) {
        return STATES.computeIfAbsent(p.getUuid(), id -> new State());
    }

    private static int now(ServerPlayerEntity p) {
        return p.getServer().getTicks();
    }

    /** Вызывается в потоке сервера. Возвращает true, если способность сработала. */
    public static boolean handle(ServerPlayerEntity player, AbilityPayload payload) {
        int a = payload.ability();
        if (a < 0 || a >= AbilityPayload.COUNT || !player.isAlive() || player.isSpectator()) return false;
        State s = state(player);
        int now = now(player);
        if (now < s.readyAt[a]) return false;
        FootballConfig cfg = FootballConfig.get();
        boolean ok = switch (a) {
            case AbilityPayload.FEINT -> feint(player, Integer.signum(payload.side()));
            case AbilityPayload.DASH -> dash(player);
            default -> {
                s.cannonUntil = now + cfg.cannonDurationTicks;
                player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_FIRECHARGE_USE,
                        SoundCategory.PLAYERS, 0.8f, 1.2f);
                ((ServerWorld) player.getWorld()).spawnParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 0.3, player.getZ(),
                        15, 0.3, 0.1, 0.3, 0.02);
                yield true;
            }
        };
        if (!ok) return false;
        s.readyAt[a] = now + switch (a) {
            case AbilityPayload.FEINT -> cfg.feintCooldownTicks;
            case AbilityPayload.DASH -> cfg.dashCooldownTicks;
            default -> cfg.cannonCooldownTicks;
        };
        sync(player);
        return true;
    }

    /** Множитель силы для удара с зарядкой: пушка тратится на первый же удар. */
    public static double consumeCannon(ServerPlayerEntity player) {
        State s = STATES.get(player.getUuid());
        if (s == null || now(player) > s.cannonUntil) return 1.0;
        s.cannonUntil = -1;
        sync(player);
        return FootballConfig.get().cannonMultiplier;
    }

    public static boolean cannonActive(ServerPlayerEntity player) {
        State s = STATES.get(player.getUuid());
        return s != null && now(player) <= s.cannonUntil;
    }

    /** Отправляет клиенту остаток перезарядок (для значков). */
    public static void sync(ServerPlayerEntity player) {
        State s = state(player);
        int now = now(player);
        AbilityStatePayload p = new AbilityStatePayload(Math.max(0, s.readyAt[0] - now), Math.max(0, s.readyAt[1] - now),
                Math.max(0, s.readyAt[2] - now), Math.max(0, s.cannonUntil - now));
        if (ServerPlayNetworking.canSend(player, AbilityStatePayload.ID)) ServerPlayNetworking.send(player, p);
    }

    private static Vec3d look(PlayerEntity p) {
        return Vec3d.fromPolar(0, p.getYaw());
    }

    /** Толкает игрока (движение игрока считает клиент, поэтому шлём ему скорость). */
    private static void push(ServerPlayerEntity p, Vec3d v) {
        p.setVelocity(v);
        p.velocityModified = true;
        p.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(p));
    }

    private static boolean canMove(ServerPlayerEntity p) {
        return !p.getAbilities().flying && !p.hasVehicle() && !p.isFallFlying() && (p.isOnGround() || p.isTouchingWater());
    }

    /** Мяч у ног игрока (в пределах 2 блоков, на земле) или null. */
    @Nullable
    private static SoccerBallEntity ballAtFeet(ServerPlayerEntity p) {
        SoccerBallEntity best = null;
        double bestD = 4.0;
        for (SoccerBallEntity b : p.getWorld().getEntitiesByClass(SoccerBallEntity.class, p.getBoundingBox().expand(2), SoccerBallEntity::isAlive)) {
            double d = b.squaredDistanceTo(p);
            if (d < bestD && b.getY() < p.getY() + 1) { bestD = d; best = b; }
        }
        return best;
    }

    private static boolean feint(ServerPlayerEntity p, int side) {
        if (!canMove(p)) return false;
        FootballConfig cfg = FootballConfig.get();
        if (side == 0) side = p.getRandom().nextBoolean() ? 1 : -1;
        Vec3d f = look(p);
        Vec3d left = new Vec3d(f.z, 0, -f.x);
        Vec3d v = left.multiply(side * cfg.feintPower).add(f.multiply(0.25)).add(0, 0.12, 0);
        SoccerBallEntity ball = ballAtFeet(p);
        push(p, v);
        if (ball != null) ball.launch(new Vec3d(v.x * 1.05, 0.05, v.z * 1.05), 0, p);
        ServerWorld w = (ServerWorld) p.getWorld();
        for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(cfg.feintRadius),
                e -> e != p && e.isAlive() && (e instanceof FootballerEntity || (e instanceof PlayerEntity pl && !pl.isSpectator())))) {
            e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, cfg.feintSlowTicks, 2), p);
        }
        w.spawnParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.2, p.getZ(), 12, 0.3, 0.05, 0.3, 0.03);
        w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_BREEZE_JUMP, SoundCategory.PLAYERS, 0.9f, 1.5f);
        return true;
    }

    private static boolean dash(ServerPlayerEntity p) {
        if (!canMove(p)) return false;
        FootballConfig cfg = FootballConfig.get();
        Vec3d f = look(p);
        SoccerBallEntity ball = ballAtFeet(p);
        push(p, f.multiply(cfg.dashPower).add(0, 0.08, 0));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, cfg.dashSpeedTicks, 2, false, false, true));
        if (ball != null) ball.launch(f.multiply(cfg.dashPower * 1.1).add(0, 0.05, 0), 0, p);
        ServerWorld w = (ServerWorld) p.getWorld();
        w.spawnParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.1, p.getZ(), 20, 0.3, 0.05, 0.3, 0.05);
        w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.8f, 1.4f);
        return true;
    }
}
