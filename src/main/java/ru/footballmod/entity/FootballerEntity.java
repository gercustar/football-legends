package ru.footballmod.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.MathHelper;
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

/** Общая логика всех футболистов-легенд. */
public abstract class FootballerEntity extends PathAwareEntity {
    /** Команда: футболисты дерутся с чужой командой и помогают своей. */
    public enum Team { REAL_MADRID, BARCELONA, /** судья */ NEUTRAL }

    /** Версия игрока (0 — обычная, например CR7 2008 / 2017). Хранится в NBT как «Version». */
    private static final TrackedData<Integer> VARIANT =
            DataTracker.registerData(FootballerEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private int abilityCooldown = 60;

    protected FootballerEntity(EntityType<? extends FootballerEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 20;
    }

    public static DefaultAttributeContainer.Builder createFootballerAttributes() {
        return attributes(60.0, 0.3, 5.0, 0.0);
    }

    /** Характеристики футболиста: здоровье, скорость, урон, броня. */
    public static DefaultAttributeContainer.Builder attributes(double health, double speed, double damage, double armor) {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, health)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, speed)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, damage)
                .add(EntityAttributes.GENERIC_ARMOR, armor)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.3);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(VARIANT, 0);
    }

    /** Сколько версий у этого футболиста (1 — только обычная). */
    public int variantCount() { return 1; }

    /** Ключ версии для названия: entity.ronaldomessi.ronaldo_2008 и т.п. */
    protected String variantSuffix(int variant) { return ""; }

    public int getVariant() { return dataTracker.get(VARIANT); }

    public void setVariant(int variant) {
        variant = MathHelper.clamp(variant, 0, variantCount() - 1);
        dataTracker.set(VARIANT, variant);
        if (!getWorld().isClient) applyVariantStats(variant);
    }

    /** Характеристики версии; по умолчанию не меняются. */
    protected void applyVariantStats(int variant) {}

    protected void setBase(net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.attribute.EntityAttribute> a, double v) {
        var inst = getAttributeInstance(a);
        if (inst != null) inst.setBaseValue(v);
    }

    @Override
    protected Text getDefaultName() {
        int v = getVariant();
        return v == 0 ? super.getDefaultName() : Text.translatable(getType().getTranslationKey() + "_" + variantSuffix(v));
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (getVariant() != 0) nbt.putInt("Version", getVariant());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("Version")) setVariant(nbt.getInt("Version"));
    }

    /** Команда этого футболиста. */
    public abstract Team team();

    public boolean isOpponent(Entity other) {
        return other instanceof FootballerEntity f && f.team() != team() && f.isAlive()
                && f.team() != Team.NEUTRAL && team() != Team.NEUTRAL && !ru.footballmod.match.MatchManager.noFighting(this);
    }

    public boolean isTeammate(Entity other) {
        return other != this && other instanceof FootballerEntity f && f.team() == team() && f.isAlive() && team() != Team.NEUTRAL;
    }

    /** Ближайшие живые футболисты чужой (opponents=true) или своей команды. */
    protected List<FootballerEntity> nearbyFootballers(double radius, boolean opponents) {
        return getWorld().getEntitiesByClass(FootballerEntity.class, getBoundingBox().expand(radius),
                e -> opponents ? isOpponent(e) : isTeammate(e));
    }

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
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, FootballerEntity.class, 10, true, false, this::isOpponent));
    }

    /** Без этого анимация взмаха (у нас — удар ногой) у мобов не проигрывается. */
    @Override
    public void tickMovement() {
        tickHandSwing();
        super.tickMovement();
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (abilityCooldown > 0) abilityCooldown--;
        if (ru.footballmod.match.MatchManager.noFighting(this)) {
            // Матч: не дерёмся, а играем; способность — рывок к мячу
            if (getTarget() != null) setTarget(null);
            Vec3d keep = ru.footballmod.match.MatchManager.keeperGoal(this);
            if (keep != null) { keeperTick(keep); return; }
            if (abilityCooldown <= 0 && !getNavigation().isIdle() && team() != Team.NEUTRAL) {
                addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.SPEED, 40, 1));
                if (getWorld() instanceof ServerWorld sw) {
                    sw.spawnParticles(net.minecraft.particle.ParticleTypes.CLOUD, getX(), getY() + 0.1, getZ(), 8, 0.2, 0.05, 0.2, 0.03);
                }
                abilityCooldown = 100 + random.nextInt(80);
            }
            return;
        }
        LivingEntity target = getTarget();
        if (abilityCooldown <= 0 && target != null && target.isAlive() && squaredDistanceTo(target) < 100) {
            useSpecialAbility(target);
            var cfg = ru.footballmod.config.FootballConfig.get();
            abilityCooldown = cfg.mobAbilityCooldownMin + random.nextInt(Math.max(1, cfg.mobAbilityCooldownRandom));
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

    /** Вратарь: стоит на линии ворот напротив мяча; если мяч рядом — выходит к нему. */
    /** Вратарь пробует поймать каждый удар один раз. */
    private java.util.UUID keeperTriedBall;
    private int keeperTriedAge;

    private void keeperTick(Vec3d goal) {
        var cfg = ru.footballmod.config.FootballConfig.get();
        // Сейв: мяч рядом и летит в сторону ворот — ловим (не всегда)
        for (SoccerBallEntity b : getWorld().getEntitiesByClass(SoccerBallEntity.class, getBoundingBox().expand(cfg.keeperReach, 1.5, cfg.keeperReach), Entity::isAlive)) {
            Vec3d v = b.getVelocity();
            Vec3d toGoal = goal.subtract(b.getPos());
            if (v.horizontalLengthSquared() > 0.09 && v.x * toGoal.x + v.z * toGoal.z > 0 && b.getLastKicker() != this
                    && !(b.getUuid().equals(keeperTriedBall) && age - keeperTriedAge < 15)) {
                keeperTriedBall = b.getUuid();
                keeperTriedAge = age;
                if (random.nextFloat() < cfg.keeperSaveChance) {
                    b.launch(Vec3d.ZERO, 0, this);
                    swingHand(net.minecraft.util.Hand.MAIN_HAND);
                    if (getWorld() instanceof ServerWorld sw) {
                        sw.spawnParticles(net.minecraft.particle.ParticleTypes.CRIT, b.getX(), b.getY() + 0.3, b.getZ(), 10, 0.2, 0.2, 0.2, 0.1);
                    }
                    playSound(net.minecraft.sound.SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE, 1.0f, 0.8f);
                }
            }
        }
        if (age % 10 != 0) return;
        Vec3d center = ru.footballmod.match.MatchManager.attackGoalFor(this, team());
        if (center == null) return;
        Vec3d out = new Vec3d(center.x - goal.x, 0, center.z - goal.z).normalize();
        Vec3d side = new Vec3d(-out.z, 0, out.x);
        SoccerBallEntity ball = null;
        for (SoccerBallEntity b : getWorld().getEntitiesByClass(SoccerBallEntity.class, getBoundingBox().expand(40), Entity::isAlive)) {
            if (ball == null || b.squaredDistanceTo(goal) < ball.squaredDistanceTo(goal)) ball = b;
        }
        double lateral = 0;
        if (ball != null) {
            Vec3d rel = ball.getPos().subtract(goal);
            lateral = MathHelper.clamp(rel.x * side.x + rel.z * side.z, -2.5, 2.5);
            if (ball.squaredDistanceTo(goal) < cfg.keeperRushDistance * cfg.keeperRushDistance) {
                getNavigation().startMovingTo(ball, 1.25);
                return;
            }
        }
        Vec3d spot = goal.add(out.multiply(2.5)).add(side.multiply(lateral));
        getNavigation().startMovingTo(spot.x, getY(), spot.z, 1.1);
    }

    /** Удар низом (в матче): мяч не улетает за трибуны. */
    private void lowKick(SoccerBallEntity ball, Vec3d dir, double power, double lift) {
        ball.launch(new Vec3d(dir.x * power, lift, dir.z * power), 0, this);
        getWorld().playSound(null, ball.getX(), ball.getY(), ball.getZ(), net.minecraft.sound.SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK,
                net.minecraft.sound.SoundCategory.NEUTRAL, 1.0f, 1.2f);
    }

    public void kickBall(SoccerBallEntity ball) {
        Vec3d dir;
        Vec3d matchGoal = ru.footballmod.match.MatchManager.attackGoalFor(this, team());
        if (matchGoal != null && ru.footballmod.match.MatchManager.keeperGoal(this) != null) {
            // Вратарь ловит и выбивает мяч далеко вперёд
            Vec3d to = new Vec3d(matchGoal.x - getX(), 0, matchGoal.z - getZ()).normalize();
            swingHand(net.minecraft.util.Hand.MAIN_HAND);
            lowKick(ball, SoccerBallEntity.turnLeft(to, (random.nextFloat() - 0.5f) * 0.7f), ru.footballmod.config.FootballConfig.get().keeperClearPower, 0.22);
            return;
        }
        if (matchGoal != null) {
            // Матч: издалека ведём мяч к воротам, бьём только вблизи и не идеально точно
            var cfg = ru.footballmod.config.FootballConfig.get();
            Vec3d d = matchGoal.subtract(getPos());
            Vec3d to = new Vec3d(d.x, 0, d.z);
            double dist = to.length();
            to = dist < 1.0e-3 ? Vec3d.ZERO : to.multiply(1 / dist);
            swingHand(net.minecraft.util.Hand.MAIN_HAND);
            if (dist > cfg.botShotDistance) {
                lowKick(ball, SoccerBallEntity.turnLeft(to, (random.nextFloat() - 0.5f) * 0.5f), cfg.botDribblePower, 0.04);
            } else {
                float spread = (float) Math.toRadians(cfg.botShotSpreadDegrees);
                lowKick(ball, SoccerBallEntity.turnLeft(to, (random.nextFloat() * 2 - 1) * spread), Math.min(kickPower(), cfg.botShotMaxPower), 0.08 + random.nextFloat() * 0.1);
            }
            return;
        }
        BlockPos goal = findNearestGoal();
        List<FootballerEntity> rivals = nearbyFootballers(32, true);
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

    /** Точный пас ближайшему партнёру (3–20 блоков). Возвращает false, если пасовать некому. */
    protected boolean passToNearestTeammate(SoccerBallEntity ball) {
        FootballerEntity best = null;
        double bestD = 20 * 20;
        for (FootballerEntity mate : nearbyFootballers(20, false)) {
            double d = mate.squaredDistanceTo(this);
            if (d > 9 && d < bestD) { bestD = d; best = mate; }
        }
        if (best == null) return false;
        // Сила зависит от расстояния до партнёра
        double power = Math.min(1.4, 0.45 + Math.sqrt(bestD) * 0.06);
        swingHand(net.minecraft.util.Hand.MAIN_HAND);
        ball.kick(horizontalDirTo(best), power, this);
        return true;
    }

    /** Направление к ближайшим воротам или null, если их нет рядом. */
    @org.jetbrains.annotations.Nullable
    protected Vec3d dirToGoal() {
        BlockPos goal = findNearestGoal();
        if (goal == null) return null;
        Vec3d d = Vec3d.ofCenter(goal).subtract(getPos());
        return new Vec3d(d.x, 0, d.z).normalize();
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
        if (rivalKill || random.nextFloat() < ru.footballmod.config.FootballConfig.get().goldenBallDropChance) {
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
            boolean match = ru.footballmod.match.MatchManager.noFighting(mob);
            Vec3d keep = match ? ru.footballmod.match.MatchManager.keeperGoal(mob) : null;
            if (mob.getTarget() != null || mob.team() == Team.NEUTRAL || !ru.footballmod.match.MatchManager.mayChaseBall(mob)
                    || mob.getRandom().nextInt(match ? 2 : 10) != 0) return false;
            List<SoccerBallEntity> balls = mob.getWorld().getEntitiesByClass(SoccerBallEntity.class,
                    mob.getBoundingBox().expand(ru.footballmod.match.MatchManager.noFighting(mob) ? 28 : 16), Entity::isAlive);
            ball = null;
            double best = Double.MAX_VALUE;
            for (SoccerBallEntity b : balls) {
                double d = mob.squaredDistanceTo(b);
                if (d < best) { best = d; ball = b; }
            }
            if (ball != null && keep != null) {
                double r = ru.footballmod.config.FootballConfig.get().keeperRushDistance;
                if (ball.squaredDistanceTo(keep) > r * r) ball = null;   // вратарь не уходит далеко от ворот
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
