package ru.footballmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Роналду: прыжок «СИУУУ» — взлетает высоко, приземляется рядом с соперником и становится сильнее. */
public class RonaldoEntity extends FootballerEntity {
    public RonaldoEntity(EntityType<? extends RonaldoEntity> type, World world) {
        super(type, world);
    }

    @Override
    protected Class<? extends FootballerEntity> rivalClass() {
        return MessiEntity.class;
    }

    @Override
    protected double kickPower() {
        return ru.footballmod.config.FootballConfig.get().ronaldoKickPower;
    }

    @Override
    protected void useSpecialAbility(LivingEntity target) {
        if (!isOnGround()) return;
        Vec3d dir = horizontalDirTo(target).multiply(0.7);
        setVelocity(dir.x, 1.0, dir.z);
        velocityModified = true;
        addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 120, 1));
        addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 30, 0));
        playSound(SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.5f, 0.8f);
        if (getWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.FIREWORK, getX(), getY() + 1, getZ(), 40, 0.4, 0.6, 0.4, 0.15);
        }
        shout(Text.translatable("msg.ronaldomessi.siuuu"));
    }
}
