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
 * Зинедин Зидан (Реал). Мастер контроля мяча.
 * Способность «Марсельская рулетка»: обкручивает соперника и оказывается у него за спиной, сразу нанося удар.
 * Пассивно: бьёт по воротам точно и низом.
 */
public class ZidaneEntity extends FootballerEntity {
    public ZidaneEntity(EntityType<? extends ZidaneEntity> type, World world) {
        super(type, world);
    }

    @Override public Team team() { return Team.REAL_MADRID; }
    @Override protected double kickPower() { return 1.4; }

    @Override
    protected void useSpecialAbility(LivingEntity target) {
        // Встаём с противоположной стороны от соперника
        Vec3d toMe = getPos().subtract(target.getPos());
        Vec3d behind = target.getPos().subtract(new Vec3d(toMe.x, 0, toMe.z).normalize().multiply(1.3));
        if (getWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY() + 1, getZ(), 3, 0.3, 0.3, 0.3, 0);
            if (sw.isSpaceEmpty(this, getBoundingBox().offset(behind.subtract(getPos())))) {
                requestTeleport(behind.x, target.getY(), behind.z);
            }
            sw.spawnParticles(ParticleTypes.SWEEP_ATTACK, behind.x, target.getY() + 1, behind.z, 3, 0.3, 0.3, 0.3, 0);
        }
        lookAtEntity(target, 360, 360);
        tryAttack(target);
        playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.9f);
        shout(Text.translatable("msg.ronaldomessi.zidane_roulette"));
    }
}
