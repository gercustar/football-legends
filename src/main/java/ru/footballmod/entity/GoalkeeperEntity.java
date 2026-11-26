package ru.footballmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import ru.footballmod.match.MatchManager;

/** Вратарь серии пенальти: стоит на линии ворот и прыгает в угол. Появляется только на серию. */
public class GoalkeeperEntity extends FootballerEntity {
    private int noShootoutTicks;

    public GoalkeeperEntity(EntityType<? extends GoalkeeperEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 0;
    }

    @Override public Team team() { return Team.NEUTRAL; }
    @Override protected double kickPower() { return 0; }
    @Override protected void useSpecialAbility(LivingEntity target) {}

    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 16.0f));
    }

    /** Прыжок в сторону (или на месте, если остался в центре). */
    public void dive(Vec3d sideways, boolean stay) {
        swingHand(Hand.MAIN_HAND);
        if (stay) {
            setVelocity(0, 0.42, 0);
        } else {
            setVelocity(sideways.x * 0.55, 0.32, sideways.z * 0.55);
            if (getWorld() instanceof ServerWorld sw) {
                sw.spawnParticles(ParticleTypes.CLOUD, getX(), getY() + 0.2, getZ(), 8, 0.3, 0.05, 0.3, 0.03);
            }
        }
        velocityModified = true;
        playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.3f);
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (!MatchManager.shootoutNear(this)) {
            if (++noShootoutTicks > 100) discard();
        } else {
            noShootoutTicks = 0;
        }
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        return source.isSourceCreativePlayer() && super.damage(source, amount);
    }

    @Override
    protected void dropLoot(DamageSource source, boolean causedByPlayer) {}

    @Override
    public boolean cannotDespawn() {
        return true;
    }
}
