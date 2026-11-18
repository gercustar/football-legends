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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import ru.footballmod.ModBlocks;
import ru.footballmod.ModItems;
import ru.footballmod.block.GoalNetBlock;
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

/** Футбольный мяч: катится, отскакивает от стен и земли, его можно пинать. */
public class SoccerBallEntity extends Entity {
    public float spin;
    public float prevSpin;
    /** Кто последним ударил по мячу — ему засчитается гол. */
    @Nullable private Entity lastKicker;
    private boolean inGoal;

    public SoccerBallEntity(EntityType<? extends SoccerBallEntity> type, World world) {
        super(type, world);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {}

    @Override
    public void tick() {
        super.tick();
        prevSpin = spin;

        Vec3d v = getVelocity();
        if (isTouchingWater()) {
            v = v.multiply(0.9).add(0, 0.03, 0); // мяч плавает
        } else {
            v = v.add(0, -0.04, 0);
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
        double friction = isOnGround() ? 0.9 : 0.985;
        x *= friction;
        z *= friction;
        y *= 0.98;
        if (Math.abs(x) < 1.0e-3) x = 0;
        if (Math.abs(z) < 1.0e-3) z = 0;
        setVelocity(x, y, z);

        double speed = Math.sqrt(x * x + z * z);
        spin += (float) (speed * 80.0);
        if (speed > 0.01) {
            setYaw((float) (MathHelper.atan2(z, x) * (180.0 / Math.PI)) - 90.0f);
        }

        if (getWorld() instanceof ServerWorld sw) {
            boolean touching = touchingScoringNet();
            if (touching && !inGoal) scoreGoal(sw);
            inGoal = touching;
        }
    }

    private boolean touchingScoringNet() {
        for (BlockPos p : BlockPos.iterate(BlockPos.ofFloored(getBoundingBox().expand(0.1).getMinPos()),
                BlockPos.ofFloored(getBoundingBox().expand(0.1).getMaxPos()))) {
            BlockState st = getWorld().getBlockState(p);
            if (st.isOf(ModBlocks.GOAL_NET) && st.get(GoalNetBlock.SCORING)) return true;
        }
        return false;
    }

    private void scoreGoal(ServerWorld sw) {
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

    public void kick(Vec3d dir, double power, @Nullable Entity kicker) {
        if (kicker != null) lastKicker = kicker;
        setVelocity(dir.x * power, 0.2 + power * 0.15, dir.z * power);
        velocityModified = true;
        getWorld().playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK,
                SoundCategory.NEUTRAL, 1.0f, 1.2f);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) return false;
        if (!getWorld().isClient) {
            Entity attacker = source.getAttacker();
            if (attacker != null) {
                Vec3d look = attacker.getRotationVector();
                Vec3d dir = new Vec3d(look.x, 0, look.z).normalize();
                double power = attacker.isSprinting() ? 1.5 : 1.0;
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
        Vec3d v = getVelocity();
        if (v.horizontalLengthSquared() > 0.04) return;
        Vec3d d = getPos().subtract(player.getPos());
        d = new Vec3d(d.x, 0, d.z);
        if (d.lengthSquared() > 1.2 || d.lengthSquared() < 1.0e-4) return;
        d = d.normalize().multiply(0.35);
        setVelocity(d.x, 0.05, d.z);
        velocityModified = true;
        lastKicker = player;
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
