package ru.footballmod.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;
import ru.footballmod.match.MatchManager;

import java.util.EnumSet;
import java.util.List;

/** Судья: бегает рядом с мячом, свистит и показывает карточки. Появляется с матчем и уходит после него. */
public class RefereeEntity extends FootballerEntity {
    private int noMatchTicks;

    public RefereeEntity(EntityType<? extends RefereeEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 0;
    }

    @Override public Team team() { return Team.NEUTRAL; }
    @Override protected double kickPower() { return 0; }
    @Override protected void useSpecialAbility(LivingEntity target) {}

    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new FollowBallGoal(this));
        goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 10.0f));
        goalSelector.add(7, new LookAroundGoal(this));
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        // Матч закончился или мир перезагрузили посреди матча — судья уходит
        if (MatchManager.matchFor(this) == null) {
            if (++noMatchTicks > 100) discard();
        } else {
            noMatchTicks = 0;
        }
    }

    /** Жёлтая или красная карточка: взмах рукой и цветные искры над головой нарушителя. */
    public void showCard(Entity who, boolean red) {
        lookAtEntity(who, 360, 360);
        swingHand(Hand.MAIN_HAND);
        if (getWorld() instanceof ServerWorld sw) {
            Vector3f color = red ? new Vector3f(0.9f, 0.1f, 0.1f) : new Vector3f(1.0f, 0.85f, 0.1f);
            sw.spawnParticles(new DustParticleEffect(color, 1.6f), who.getX(), who.getY() + who.getHeight() + 0.6, who.getZ(), 20, 0.15, 0.25, 0.15, 0);
            sw.spawnParticles(new DustParticleEffect(color, 1.6f), getX(), getY() + getHeight() + 0.4, getZ(), 12, 0.1, 0.2, 0.1, 0);
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

    /** Держится в 5 блоках сбоку от мяча. */
    static class FollowBallGoal extends Goal {
        private final RefereeEntity ref;
        private int cooldown;

        FollowBallGoal(RefereeEntity ref) {
            this.ref = ref;
            setControls(EnumSet.of(Control.MOVE));
        }

        @Override public boolean canStart() { return true; }
        @Override public boolean shouldContinue() { return true; }

        @Override
        public void tick() {
            if (--cooldown > 0) return;
            cooldown = 10;
            List<SoccerBallEntity> balls = ref.getWorld().getEntitiesByClass(SoccerBallEntity.class, ref.getBoundingBox().expand(48), Entity::isAlive);
            if (balls.isEmpty()) return;
            SoccerBallEntity ball = balls.get(0);
            for (SoccerBallEntity b : balls) if (b.squaredDistanceTo(ref) < ball.squaredDistanceTo(ref)) ball = b;
            Vec3d toRef = ref.getPos().subtract(ball.getPos());
            Vec3d dir = new Vec3d(toRef.x, 0, toRef.z);
            dir = dir.lengthSquared() < 1.0e-3 ? new Vec3d(1, 0, 0) : dir.normalize();
            Vec3d spot = ball.getPos().add(dir.multiply(5));
            if (ref.squaredDistanceTo(spot) > 4) ref.getNavigation().startMovingTo(spot.x, spot.y, spot.z, 1.1);
        }
    }
}
