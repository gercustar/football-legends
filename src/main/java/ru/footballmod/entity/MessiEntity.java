package ru.footballmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Месси: дриблинг-рывок к сопернику и шанс увернуться от удара. */
public class MessiEntity extends FootballerEntity {
    public MessiEntity(EntityType<? extends MessiEntity> type, World world) {
        super(type, world);
    }

    @Override
    protected Class<? extends FootballerEntity> rivalClass() {
        return RonaldoEntity.class;
    }

    @Override
    protected double kickPower() {
        return ru.footballmod.config.FootballConfig.get().messiKickPower;
    }

    @Override
    protected void useSpecialAbility(LivingEntity target) {
        Vec3d dir = horizontalDirTo(target).multiply(1.3);
        setVelocity(dir.x, 0.25, dir.z);
        velocityModified = true;
        addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 80, 2));
        playSound(SoundEvents.ENTITY_BREEZE_JUMP, 1.2f, 1.2f);
        if (getWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.CLOUD, getX(), getY() + 0.2, getZ(), 25, 0.4, 0.1, 0.4, 0.05);
        }
        shout(Text.translatable("msg.ronaldomessi.dribble"));
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        // 25% шанс обвести удар — как дриблинг Месси
        if (!getWorld().isClient && source.getAttacker() != null && random.nextFloat() < ru.footballmod.config.FootballConfig.get().messiDodgeChance) {
            Vec3d away = horizontalDirTo(source.getAttacker());
            setVelocity(-away.z * 0.8, 0.2, away.x * 0.8); // шаг вбок
            velocityModified = true;
            if (getWorld() instanceof ServerWorld sw) {
                sw.spawnParticles(ParticleTypes.CRIT, getX(), getY() + 1, getZ(), 15, 0.3, 0.5, 0.3, 0.1);
            }
            playSound(SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE, 1.0f, 1.3f);
            return false;
        }
        return super.damage(source, amount);
    }
}
