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
import ru.footballmod.config.FootballConfig;

/**
 * Андрес Иньеста (Барселона). Неуловимый.
 * Способность «Ла Крокета»: рывок вбок и мимо соперника, соперник замедляется и получает удар, если он рядом.
 * Пассивно: иногда уходит от удара.
 */
public class IniestaEntity extends FootballerEntity {
    public IniestaEntity(EntityType<? extends IniestaEntity> type, World world) {
        super(type, world);
    }

    @Override public Team team() { return Team.BARCELONA; }
    @Override protected double kickPower() { return FootballConfig.get().iniestaKickPower; }

    @Override
    protected void useSpecialAbility(LivingEntity target) {
        Vec3d dir = horizontalDirTo(target);
        Vec3d side = new Vec3d(-dir.z, 0, dir.x).multiply(random.nextBoolean() ? 0.5 : -0.5);
        if (squaredDistanceTo(target) < 9) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1));
            tryAttack(target);
        }
        setVelocity(dir.x + side.x, 0.12, dir.z + side.z);
        velocityModified = true;
        addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 60, 1));
        if (getWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.ENCHANTED_HIT, getX(), getY() + 0.4, getZ(), 20, 0.4, 0.2, 0.4, 0.1);
        }
        playSound(SoundEvents.ENTITY_BREEZE_JUMP, 1.0f, 1.6f);
        shout(Text.translatable("msg.ronaldomessi.iniesta_croqueta"));
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (!getWorld().isClient && source.getAttacker() != null
                && random.nextFloat() < FootballConfig.get().iniestaDodgeChance) {
            Vec3d to = horizontalDirTo(source.getAttacker());
            double k = random.nextBoolean() ? 0.7 : -0.7;
            setVelocity(-to.z * k, 0.15, to.x * k);
            velocityModified = true;
            if (getWorld() instanceof ServerWorld sw) {
                sw.spawnParticles(ParticleTypes.CLOUD, getX(), getY() + 0.3, getZ(), 10, 0.3, 0.1, 0.3, 0.03);
            }
            playSound(SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE, 1.0f, 1.5f);
            return false;
        }
        return super.damage(source, amount);
    }
}
