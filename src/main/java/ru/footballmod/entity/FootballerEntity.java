package ru.footballmod.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import ru.footballmod.ModItems;

import java.util.EnumSet;
import java.util.List;

/** Общая логика для Роналду и Месси. */
public abstract class FootballerEntity extends PathAwareEntity {
    private int abilityCooldown = 60;

    protected FootballerEntity(EntityType<? extends FootballerEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 20;
    }

    public static DefaultAttributeContainer.Builder createFootballerAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 60.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.3);
    }

    /** Класс соперника, с которым этот футболист будет драться. */
    protected abstract Class<? extends FootballerEntity> rivalClass();

    /** Суперспособность против цели. */
    protected abstract void useSpecialAbility(LivingEntity target);

    /** Сила удара по мячу. */
    protected abstract double kickPower();

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.add(2, new KickBallGoal(this));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, rivalClass(), true));
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (abilityCooldown > 0) abilityCooldown--;
        LivingEntity target = getTarget();
        if (abilityCooldown <= 0 && target != null && target.isAlive() && squaredDistanceTo(target) < 100) {
            useSpecialAbility(target);
            abilityCooldown = 100 + random.nextInt(80);
        }
    }

    /** Сообщение в чат всем игрокам поблизости. */
    protected void shout(Text message) {
        if (getWorld() instanceof ServerWorld sw) {
            for (ServerPlayerEntity p : sw.getPlayers(p -> p.squaredDistanceTo(this) < 48 * 48)) {
                p.sendMessage(message, false);
            }
        }
    }

    protected Vec3d horizontalDirTo(Entity target) {
        Vec3d d = target.getPos().subtract(getPos());
        d = new Vec3d(d.x, 0, d.z);
        return d.lengthSquared() < 1.0e-4 ? Vec3d.ZERO : d.normalize();
    }

    public void kickBall(SoccerBallEntity ball) {
        Vec3d dir;
        BlockPos goal = findNearestGoal();
        List<? extends FootballerEntity> rivals = getWorld().getEntitiesByClass(rivalClass(),
                getBoundingBox().expand(32), Entity::isAlive);
        if (goal != null) {
            Vec3d d = Vec3d.ofCenter(goal).subtract(getPos());
            dir = new Vec3d(d.x, 0, d.z).normalize();
        } else if (!rivals.isEmpty()) {
            dir = horizontalDirTo(rivals.get(0));
        } else {
            float a = random.nextFloat() * (float) (Math.PI * 2);
            dir = new Vec3d(Math.cos(a), 0, Math.sin(a));
        }
        swingHand(net.minecraft.util.Hand.MAIN_HAND);
        ball.kick(dir, kickPower(), this);
    }

    /** Ближайшая задняя сетка ворот в радиусе 20 блоков — туда футболист бьёт по мячу. */
    @org.jetbrains.annotations.Nullable
    private BlockPos findNearestGoal() {
        BlockPos center = getBlockPos();
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.iterate(center.add(-20, -3, -20), center.add(20, 4, 20))) {
            net.minecraft.block.BlockState st = getWorld().getBlockState(p);
            if (st.isOf(ru.footballmod.ModBlocks.GOAL_NET) && st.get(ru.footballmod.block.GoalNetBlock.SCORING)) {
                double d = p.getSquaredDistance(center);
                if (d < bestD) { bestD = d; best = p.toImmutable(); }
            }
        }
        return best;
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        if (!getWorld().isClient && getWorld() instanceof ServerWorld sw) {
            Entity killer = source.getAttacker();
            if (killer instanceof FootballerEntity || killer instanceof PlayerEntity) {
                Text msg = Text.translatable("msg.ronaldomessi.win", killer.getDisplayName(), getDisplayName());
                for (ServerPlayerEntity p : sw.getPlayers()) p.sendMessage(msg, false);
            }
        }
    }

    @Override
    protected void dropLoot(DamageSource source, boolean causedByPlayer) {
        super.dropLoot(source, causedByPlayer);
        dropStack(new ItemStack(ModItems.SOCCER_BALL));
        boolean rivalKill = source.getAttacker() instanceof FootballerEntity;
        if (rivalKill || random.nextFloat() < 0.4f) {
            dropStack(new ItemStack(ModItems.GOLDEN_BALL));
        }
    }

    /** Подбежать к мячу и пнуть его в сторону соперника. */
    static class KickBallGoal extends Goal {
        private final FootballerEntity mob;
        private SoccerBallEntity ball;
        private int timer;

        KickBallGoal(FootballerEntity mob) {
            this.mob = mob;
            setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            if (mob.getTarget() != null || mob.getRandom().nextInt(10) != 0) return false;
            List<SoccerBallEntity> balls = mob.getWorld().getEntitiesByClass(SoccerBallEntity.class,
                    mob.getBoundingBox().expand(16), Entity::isAlive);
            ball = null;
            double best = Double.MAX_VALUE;
            for (SoccerBallEntity b : balls) {
                double d = mob.squaredDistanceTo(b);
                if (d < best) { best = d; ball = b; }
            }
            return ball != null;
        }

        @Override
        public boolean shouldContinue() {
            return ball != null && ball.isAlive() && mob.getTarget() == null
                    && mob.squaredDistanceTo(ball) < 24 * 24 && timer < 300;
        }

        @Override
        public void start() {
            timer = 0;
            mob.getNavigation().startMovingTo(ball, 1.15);
        }

        @Override
        public void stop() {
            ball = null;
            mob.getNavigation().stop();
        }

        @Override
        public boolean shouldRunEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            // В нечётные тики игра вызывает tick() без проверки shouldContinue(),
            // поэтому мяч уже может быть пнут (null) или исчезнуть.
            if (ball == null || !ball.isAlive()) return;
            timer++;
            mob.getLookControl().lookAt(ball, 30f, 30f);
            if (mob.squaredDistanceTo(ball) < 2.5) {
                mob.kickBall(ball);
                ball = null;
            } else if (timer % 10 == 0) {
                mob.getNavigation().startMovingTo(ball, 1.15);
            }
        }
    }
}
