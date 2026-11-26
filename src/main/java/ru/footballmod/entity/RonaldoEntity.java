package ru.footballmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
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
    public Team team() {
        return Team.REAL_MADRID;
    }

    @Override
    protected double kickPower() {
        return ru.footballmod.config.FootballConfig.get().ronaldoKickPower;
    }

    public static final int PORTUGAL = 0, UNITED_2008 = 1, REAL_2017 = 2;

    @Override public int variantCount() { return 3; }
    @Override protected String variantSuffix(int v) { return v == UNITED_2008 ? "2008" : "2017"; }

    @Override
    protected void applyVariantStats(int v) {
        // 2008: быстрый и вёрткий; 2017: мощный и высокий в прыжке
        setBase(EntityAttributes.GENERIC_MOVEMENT_SPEED, v == UNITED_2008 ? 0.33 : 0.3);
        setBase(EntityAttributes.GENERIC_ATTACK_DAMAGE, v == REAL_2017 ? 6.0 : 5.0);
        setBase(EntityAttributes.GENERIC_MAX_HEALTH, v == REAL_2017 ? 65.0 : 60.0);
    }

    @Override
    protected void useSpecialAbility(LivingEntity target) {
        if (getVariant() == UNITED_2008) {
            stepOvers(target);
            return;
        }
        if (!isOnGround()) return;
        boolean prime = getVariant() == REAL_2017;
        Vec3d dir = horizontalDirTo(target).multiply(0.7);
        setVelocity(dir.x, prime ? 1.2 : 1.0, dir.z);
        velocityModified = true;
        addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, prime ? 160 : 120, 1));
        addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 30, 0));
        playSound(SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.5f, 0.8f);
        if (getWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.FIREWORK, getX(), getY() + 1, getZ(), 40, 0.4, 0.6, 0.4, 0.15);
        }
        shout(Text.translatable("msg.ronaldomessi.siuuu"));
    }

    /** CR7 2008: финты-«ножницы» — рывок зигзагом, соперник замедлен. */
    private void stepOvers(LivingEntity target) {
        Vec3d dir = horizontalDirTo(target);
        Vec3d side = new Vec3d(-dir.z, 0, dir.x).multiply(random.nextBoolean() ? 0.55 : -0.55);
        setVelocity(dir.x * 0.9 + side.x, 0.1, dir.z * 0.9 + side.z);
        velocityModified = true;
        addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 80, 2));
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 50, 1));
        if (getWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY() + 0.4, getZ(), 4, 0.4, 0.1, 0.4, 0);
        }
        playSound(SoundEvents.ENTITY_BREEZE_JUMP, 1.0f, 1.3f);
        shout(Text.translatable("msg.ronaldomessi.ronaldo_stepovers"));
    }
}
