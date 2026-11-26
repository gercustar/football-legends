package ru.footballmod.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import ru.footballmod.ModBlocks;
import ru.footballmod.ModItems;
import ru.footballmod.block.GoalNetBlock;
import ru.footballmod.config.FootballConfig;
import ru.footballmod.item.FootballBootsItem;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.scoreboard.ScoreAccess;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Футбольный мяч: катится, отскакивает от стен и земли, его можно пинать.
 * Физику считает ТОЛЬКО сервер; клиент плавно двигает мяч к присланной позиции.
 */
public class SoccerBallEntity extends Entity {
    /** Сколько тиков после удара тот же игрок не может остановить мяч или ударить его головой. */
    private static final int KICKER_IMMUNITY_TICKS = 10;

    public float spin;
    public float prevSpin;
    /** Кто последним коснулся мяча — ему засчитается гол. */
    @Nullable private Entity lastKicker;
    /** Кто коснулся мяча перед ним (для голевых передач). */
    @Nullable private Entity previousKicker;
    private int lastKickAge = -1000;
    private boolean inGoal;
    /** Закрутка: поворот скорости за тик в радианах, плюс — влево. */
    private double curve;
    /** Огненный след после удара «пушкой» (тики). */
    private int fireTrail;

    // Плавное движение на клиенте
    private int lerpSteps;
    private double lerpX, lerpY, lerpZ;

    public SoccerBallEntity(EntityType<? extends SoccerBallEntity> type, World world) {
        super(type, world);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {}

    @Override
    public void tick() {
        super.tick();
        if (getWorld() instanceof ServerWorld sw) {
            tickPhysics(sw);
        } else {
            tickClient();
        }
    }

    // ---------------- сервер: физика ----------------

    private void tickPhysics(ServerWorld sw) {
        if (ru.footballmod.match.MatchManager.isFrozen(this)) {
            // До начального удара, после гола и в перерыве мяч стоит
            if (getVelocity().lengthSquared() > 0) { setVelocity(Vec3d.ZERO); velocityModified = true; }
            curve = 0;
            fireTrail = 0;
            return;
        }
        Vec3d start = getPos();
        Vec3d v = getVelocity();
        if (isTouchingWater()) {
            v = v.multiply(0.9).add(0, 0.03, 0); // мяч плавает
            curve = 0;
        } else {
            v = v.add(0, -0.04, 0);
        }
        if (curve != 0) {
            v = turnLeft(v, curve);
            curve *= FootballConfig.get().curveDecay;
            if (Math.abs(curve) < 0.001) curve = 0;
        }
        setVelocity(v);
        Vec3d before = v;
        move(MovementType.SELF, v);
        Vec3d after = getVelocity();

        double x = after.x, y = after.y, z = after.z;
        if (horizontalCollision) {
            if (Math.abs(before.x) > 1.0e-3 && Math.abs(after.x) < 1.0e-4) x = -before.x * 0.6;
            if (Math.abs(before.z) > 1.0e-3 && Math.abs(after.z) < 1.0e-4) z = -before.z * 0.6;
        }
        if (verticalCollision && before.y < -0.12) {
            y = -before.y * 0.55;
            if (before.y < -0.25) {
                getWorld().playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_WOOL_STEP,
                        SoundCategory.NEUTRAL, 0.6f, 1.4f);
            }
        }
        // Отскок от стены снимает закрутку, на земле она быстро гаснет
        if (horizontalCollision) curve = 0;
        else if (verticalCollision) curve *= FootballConfig.get().curveGroundDecay;

        double friction = isOnGround() ? 0.9 : 0.985;
        x *= friction;
        z *= friction;
        y *= 0.98;
        if (Math.abs(x) < 1.0e-3) x = 0;
        if (Math.abs(z) < 1.0e-3) z = 0;
        setVelocity(x, y, z);
        if (x * x + z * z > 1.0e-4) {
            setYaw((float) (MathHelper.atan2(z, x) * (180.0 / Math.PI)) - 90.0f);
        }

        BlockPos net = touchingScoringNet();
        if (net != null && !inGoal) scoreGoal(sw, net);
        inGoal = net != null;

        handlePlayers(sw, start);

        if (fireTrail > 0) {
            fireTrail--;
            if (getVelocity().lengthSquared() < 0.04) fireTrail = 0;
            else sw.spawnParticles(ParticleTypes.FLAME, getX(), getY() + 0.25, getZ(), 3, 0.08, 0.08, 0.08, 0.01);
        }
    }

    public void setFireTrail(int ticks) {
        fireTrail = ticks;
        getWorld().playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.NEUTRAL, 0.9f, 1.1f);
    }

    public int getFireTrail() { return fireTrail; }

    /** Остановка мяча (Shift) и удар головой (прыжок в летящий мяч). */
    private void handlePlayers(ServerWorld sw, Vec3d start) {
        Vec3d v = getVelocity();
        boolean rolling = v.lengthSquared() > 4.0e-4 && (v.horizontalLengthSquared() > 4.0e-4 || !isOnGround());
        if (!rolling) return;
        FootballConfig cfg = FootballConfig.get();
        // Весь путь мяча за тик, чтобы быстрый мяч не «проскочил» игрока
        Box path = getBoundingBox().union(getBoundingBox().offset(start.subtract(getPos())));
        for (ServerPlayerEntity p : sw.getPlayers(p -> p.isAlive() && !p.isSpectator() && !p.getAbilities().flying)) {
            if (p == lastKicker && age - lastKickAge < KICKER_IMMUNITY_TICKS) continue;
            if (p.isSneaking()) {
                if (path.intersects(p.getBoundingBox().expand(cfg.ballStopReach, 0.1, cfg.ballStopReach))) {
                    stopBall(p);
                    return;
                }
            } else if (!isOnGround() && !p.isOnGround() && !p.isTouchingWater() && !p.isClimbing()
                    && path.maxY >= p.getEyeY() - 0.35
                    && path.intersects(p.getBoundingBox().expand(cfg.headerReach, 0.45, cfg.headerReach))) {
                header(p);
                return;
            }
        }
    }

    private void stopBall(PlayerEntity player) {
        setVelocity(0, Math.min(0, getVelocity().y), 0);
        velocityModified = true;
        curve = 0;
        touch(player);
        getWorld().playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_WOOL_HIT,
                SoundCategory.NEUTRAL, 0.7f, 0.9f);
    }

    private void header(PlayerEntity player) {
        double power = FootballConfig.get().headerPower;
        Vec3d look = player.getRotationVector();
        launch(new Vec3d(look.x * power, look.y * power * 0.8 + 0.12, look.z * power), 0, player);
        getWorld().playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_STRONG,
                SoundCategory.NEUTRAL, 0.9f, 1.5f);
    }

    /** Поворот горизонтальной скорости влево (для отрицательного угла — вправо). */
    public static Vec3d turnLeft(Vec3d v, double angle) {
        double cos = Math.cos(angle), sin = Math.sin(angle);
        return new Vec3d(v.x * cos + v.z * sin, v.y, v.z * cos - v.x * sin);
    }

    // ---------------- клиент: плавный показ ----------------

    private void tickClient() {
        prevSpin = spin;
        // Игра поставила мяч напрямую (телепорт) — старое плавное движение больше не нужно
        if (lerpSteps > 0 && squaredDistanceTo(lerpX, lerpY, lerpZ) > 64) lerpSteps = 0;
        if (lerpSteps > 0) {
            double k = 1.0 / lerpSteps;
            double dx = (lerpX - getX()) * k, dy = (lerpY - getY()) * k, dz = (lerpZ - getZ()) * k;
            setPosition(getX() + dx, getY() + dy, getZ() + dz);
            lerpSteps--;
            double speed = Math.sqrt(dx * dx + dz * dz);
            spin += (float) (speed * 80.0);
            if (speed > 0.01) {
                setYaw((float) (MathHelper.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f);
            }
        }
    }

    @Override
    public void updateTrackedPositionAndAngles(double x, double y, double z, float yaw, float pitch, int interpolationSteps) {
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        if (squaredDistanceTo(x, y, z) > 64) {
            // Большой прыжок (мяч поставили в центр и т.п.) — сразу на место, без плавного движения
            setPosition(x, y, z);
            lerpSteps = 0;
            return;
        }
        // 2 шага вместо обычных 3 — меньше задержка, мяч остаётся плавным
        lerpSteps = Math.max(1, Math.min(2, interpolationSteps));
    }

    @Override
    public void onSpawnPacket(net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket packet) {
        super.onSpawnPacket(packet);
        lerpSteps = 0;
    }

    @Override public double getLerpTargetX() { return lerpSteps > 0 ? lerpX : getX(); }
    @Override public double getLerpTargetY() { return lerpSteps > 0 ? lerpY : getY(); }
    @Override public double getLerpTargetZ() { return lerpSteps > 0 ? lerpZ : getZ(); }

    // ---------------- голы ----------------

    @Nullable
    private BlockPos touchingScoringNet() {
        for (BlockPos p : BlockPos.iterate(BlockPos.ofFloored(getBoundingBox().expand(0.1).getMinPos()),
                BlockPos.ofFloored(getBoundingBox().expand(0.1).getMaxPos()))) {
            BlockState st = getWorld().getBlockState(p);
            if (st.isOf(ModBlocks.GOAL_NET) && st.get(GoalNetBlock.SCORING)) return p.toImmutable();
        }
        return null;
    }

    private void scoreGoal(ServerWorld sw, BlockPos net) {
        if (ru.footballmod.match.MatchManager.onGoal(this, net)) return;   // в матче — свой счёт и сообщения
        Text scorer = lastKicker != null ? lastKicker.getDisplayName() : Text.translatable("msg.ronaldomessi.nobody");
        Text chat = Text.translatable("msg.ronaldomessi.goal", scorer);
        for (ServerPlayerEntity p : sw.getPlayers(p -> p.squaredDistanceTo(this) < 96 * 96)) {
            p.sendMessage(chat, false);
            p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.translatable("msg.ronaldomessi.scored_by", scorer)));
            p.networkHandler.sendPacket(new TitleS2CPacket(Text.translatable("msg.ronaldomessi.goal_title")));
        }
        sw.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, getX(), getY() + 0.5, getZ(), 80, 0.6, 0.6, 0.6, 0.5);
        sw.spawnParticles(ParticleTypes.FIREWORK, getX(), getY() + 1.0, getZ(), 40, 0.8, 0.8, 0.8, 0.2);
        sw.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, SoundCategory.NEUTRAL, 2.0f, 1.0f);
        sw.playSound(null, getX(), getY(), getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.NEUTRAL, 1.0f, 1.0f);
        addToScoreboard(sw);
    }

    /** Табло «Голы» справа на экране. */
    private void addToScoreboard(ServerWorld sw) {
        if (lastKicker == null) return;
        Scoreboard board = sw.getScoreboard();
        ScoreboardObjective obj = board.getNullableObjective("ronaldomessi_goals");
        if (obj == null) {
            obj = board.addObjective("ronaldomessi_goals", ScoreboardCriterion.DUMMY,
                    Text.translatable("scoreboard.ronaldomessi.goals"), ScoreboardCriterion.RenderType.INTEGER, true, null);
        }
        board.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, obj);
        if (lastKicker instanceof PlayerEntity player) {
            board.getOrCreateScore(player, obj).incrementScore(1);
        } else {
            ScoreAccess score = board.getOrCreateScore(ScoreHolder.fromName(
                    net.minecraft.registry.Registries.ENTITY_TYPE.getId(lastKicker.getType()).getPath()), obj);
            score.incrementScore(1);
            score.setDisplayText(lastKicker.getType().getName());
        }
    }

    // ---------------- удары и касания ----------------

    /** Запоминает касание: последние два РАЗНЫХ игрока. */
    private void touch(Entity e) {
        if (lastKicker == null || !lastKicker.getUuid().equals(e.getUuid())) previousKicker = lastKicker;
        lastKicker = e;
    }

    @Nullable public Entity getLastKicker() { return lastKicker; }
    @Nullable public Entity getPreviousKicker() { return previousKicker; }
    public double getCurve() { return curve; }

    /** Отправляет мяч с точной скоростью и закруткой. */
    public void launch(Vec3d velocity, double curve, @Nullable Entity kicker) {
        if (kicker != null && !ru.footballmod.match.MatchManager.canTouch(this, kicker)) return;
        if (kicker != null) {
            touch(kicker);
            lastKickAge = age;
        }
        this.curve = curve;
        setVelocity(velocity);
        velocityModified = true;
    }

    public void kick(Vec3d dir, double power, @Nullable Entity kicker) {
        launch(new Vec3d(dir.x * power, 0.2 + power * 0.15, dir.z * power), 0, kicker);
        getWorld().playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK,
                SoundCategory.NEUTRAL, 1.0f, 1.2f);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) return false;
        if (!getWorld().isClient) {
            Entity attacker = source.getAttacker();
            if (attacker != null && !ru.footballmod.match.MatchManager.canTouch(this, attacker)) return true;
            if (attacker != null) {
                Vec3d look = attacker.getRotationVector();
                Vec3d dir = new Vec3d(look.x, 0, look.z).normalize();
                FootballConfig cfg = FootballConfig.get();
                double power = cfg.playerKickPower * (attacker.isSprinting() ? cfg.sprintKickMultiplier : 1.0);
                if (attacker instanceof PlayerEntity p && FootballBootsItem.typeOf(p) == FootballBootsItem.BootType.POWER) {
                    // Predator: удар сильнее и точно по взгляду (с учётом высоты)
                    power *= cfg.predatorKickMultiplier;
                    Vec3d aim = look.normalize();
                    launch(new Vec3d(aim.x * power, Math.max(0.15, aim.y * power + 0.15), aim.z * power), 0, attacker);
                    getWorld().playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_STRONG,
                            SoundCategory.NEUTRAL, 1.0f, 1.0f);
                    return true;
                }
                kick(dir, power, attacker);
            } else if (source.getPosition() != null) {
                Vec3d dir = getPos().subtract(source.getPosition());
                dir = new Vec3d(dir.x, 0, dir.z);
                if (dir.lengthSquared() > 1.0e-4) kick(dir.normalize(), 1.0, null);
            }
        }
        return true;
    }

    @Override
    public void onPlayerCollision(PlayerEntity player) {
        // Дриблинг: подходишь к мячу — он катится вперёд
        if (getWorld().isClient) return;
        if (player.isSneaking()) return; // Shift — держим мяч на месте
        if (!ru.footballmod.match.MatchManager.canTouch(this, player)) return;
        Vec3d v = getVelocity();
        if (FootballBootsItem.typeOf(player) == FootballBootsItem.BootType.DRIBBLE) {
            // F50: мяч «прилипает» — катится туда, куда смотрит игрок, чуть впереди него
            if (v.horizontalLengthSquared() > 0.16) return;
            Vec3d look = player.getRotationVector();
            Vec3d dir = new Vec3d(look.x, 0, look.z);
            if (dir.lengthSquared() < 1.0e-4) return;
            dir = dir.normalize().multiply(FootballConfig.get().f50DribblePush);
            setVelocity(dir.x, 0.02, dir.z);
            velocityModified = true;
            touch(player);
            return;
        }
        if (v.horizontalLengthSquared() > 0.04) return;
        Vec3d d = getPos().subtract(player.getPos());
        d = new Vec3d(d.x, 0, d.z);
        if (d.lengthSquared() > 1.2 || d.lengthSquared() < 1.0e-4) return;
        d = d.normalize().multiply(0.35);
        setVelocity(d.x, 0.05, d.z);
        velocityModified = true;
        touch(player);
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        if (!player.isSneaking()) return ActionResult.PASS;
        if (!getWorld().isClient) {
            player.getInventory().offerOrDrop(new ItemStack(ModItems.SOCCER_BALL));
            discard();
        }
        return ActionResult.success(getWorld().isClient);
    }

    @Override
    public boolean canHit() {
        return !isRemoved();
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {}

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {}
}
