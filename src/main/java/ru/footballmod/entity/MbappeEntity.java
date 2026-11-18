package ru.footballmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Килиан Мбаппе (Реал). Самый быстрый футболист мода.
 * Способность «Турбо»: огромное ускорение с огненным следом.
 * Пассивно: самый быстрый бег и очень сильный удар.
 */
public class MbappeEntity extends FootballerEntity {
    private int turboTicks;

    public MbappeEntity(EntityType<? extends MbappeEntity> type, World world) {
        super(type, world);
    }

    @Override public Team team() { return Team.REAL_MADRID; }
    @Override protected double kickPower() { return 1.7; }

    @Override
    protected void useSpecialAbility(LivingEntity target) {
        turboTicks = 60;
        addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 60, 2));
        Vec3d dir = horizontalDirTo(target).multiply(1.4);
        setVelocity(dir.x, 0.1, dir.z);
        velocityModified = true;
        playSound(SoundEvents.ENTITY_BLAZE_SHOOT, 1.0f, 1.2f);
        shout(Text.translatable("msg.ronaldomessi.mbappe_turbo"));
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (turboTicks > 0) {
            turboTicks--;
            if (getWorld() instanceof ServerWorld sw) {
                sw.spawnParticles(ParticleTypes.FLAME, getX(), getY() + 0.1, getZ(), 3, 0.2, 0.05, 0.2, 0.01);
            }
        }
    }
}
