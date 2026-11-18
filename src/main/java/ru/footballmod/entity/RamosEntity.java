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
 * Серхио Рамос (Реал). Непробиваемый защитник.
 * Способность «Подкат»: низкий рывок в ноги, соперник отлетает и замедляется. Иногда — красная карточка самому себе!
 * Пассивно: больше всех здоровья и брони.
 */
public class RamosEntity extends FootballerEntity {
    public RamosEntity(EntityType<? extends RamosEntity> type, World world) {
        super(type, world);
    }

    @Override public Team team() { return Team.REAL_MADRID; }
    @Override protected double kickPower() { return 1.3; }

    @Override
    protected void useSpecialAbility(LivingEntity target) {
        Vec3d dir = horizontalDirTo(target);
        setVelocity(dir.x * 1.2, 0.05, dir.z * 1.2);
        velocityModified = true;
        if (squaredDistanceTo(target) < 16) {
            target.takeKnockback(1.2, -dir.x, -dir.z);
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 50, 2));
            tryAttack(target);
        }
        if (getWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.CLOUD, getX(), getY() + 0.1, getZ(), 20, 0.5, 0.05, 0.5, 0.05);
        }
        playSound(SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.2f, 0.7f);
        if (random.nextFloat() < ru.footballmod.config.FootballConfig.get().ramosRedCardChance) {
            addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 200, 1));
            addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 200, 0));
            shout(Text.translatable("msg.ronaldomessi.ramos_red_card"));
        } else {
            shout(Text.translatable("msg.ronaldomessi.ramos_tackle"));
        }
    }
}
