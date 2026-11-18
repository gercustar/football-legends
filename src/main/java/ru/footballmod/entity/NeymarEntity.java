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
 * Неймар (Барселона). Дриблинг и… артистизм.
 * Способность «Эластико»: резкий рывок мимо соперника зигзагом.
 * Пассивно «Нейма-а-ар!»: иногда падает от удара, не получая урона, а обидчик получает «жёлтую карточку» (слабость).
 */
public class NeymarEntity extends FootballerEntity {
    public NeymarEntity(EntityType<? extends NeymarEntity> type, World world) {
        super(type, world);
    }

    @Override public Team team() { return Team.BARCELONA; }
    @Override protected double kickPower() { return 1.35; }

    @Override
    protected void useSpecialAbility(LivingEntity target) {
        Vec3d dir = horizontalDirTo(target);
        Vec3d side = new Vec3d(-dir.z, 0, dir.x);
        Vec3d v = dir.multiply(1.1).add(side.multiply(random.nextBoolean() ? 0.6 : -0.6));
        setVelocity(v.x, 0.15, v.z);
        velocityModified = true;
        addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 60, 1));
        if (getWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.CRIT, getX(), getY() + 0.3, getZ(), 20, 0.4, 0.1, 0.4, 0.2);
        }
        playSound(SoundEvents.ENTITY_BREEZE_JUMP, 1.0f, 1.4f);
        shout(Text.translatable("msg.ronaldomessi.neymar_elastico"));
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (!getWorld().isClient && source.getAttacker() instanceof LivingEntity attacker
                && random.nextFloat() < ru.footballmod.config.FootballConfig.get().neymarDiveChance) {
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100, 0));
            if (getWorld() instanceof ServerWorld sw) {
                sw.spawnParticles(ParticleTypes.POOF, getX(), getY() + 0.2, getZ(), 15, 0.5, 0.1, 0.5, 0.02);
            }
            playSound(SoundEvents.ENTITY_PLAYER_HURT, 1.0f, 1.5f);
            shout(Text.translatable("msg.ronaldomessi.neymar_dive", attacker.getDisplayName()));
            return false;
        }
        return super.damage(source, amount);
    }
}
