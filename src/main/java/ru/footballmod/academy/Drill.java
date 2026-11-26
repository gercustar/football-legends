package ru.footballmod.academy;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import ru.footballmod.ModEntities;
import ru.footballmod.entity.SoccerBallEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Одно задание академии. Ставит свои блоки только в пустые места и убирает их после задания. */
abstract class Drill {
    final ServerWorld world;
    final UUID player;
    /** Блок, на котором стоял игрок (земля — на уровень ниже), и направление «вперёд». */
    final BlockPos origin;
    final Direction forward, side;
    final List<BlockPos> placed = new ArrayList<>();
    final ServerBossBar bar = new ServerBossBar(Text.empty(), BossBar.Color.BLUE, BossBar.Style.PROGRESS);
    @Nullable UUID ballId;
    int ticks;
    boolean done;

    Drill(ServerWorld world, ServerPlayerEntity player) {
        this.world = world;
        this.player = player.getUuid();
        this.origin = player.getBlockPos();
        this.forward = player.getHorizontalFacing();
        this.side = forward.rotateYClockwise();
        bar.addPlayer(player);
    }

    BlockPos at(int ahead, int lateral, int up) {
        return origin.offset(forward, ahead).offset(side, lateral).up(up);
    }

    /** Ставит блок, если место свободно. false — место занято. */
    boolean put(BlockPos p, BlockState s) {
        BlockState cur = world.getBlockState(p);
        if (!cur.isReplaceable() || cur.hasBlockEntity()) return false;
        if (!s.canPlaceAt(world, p)) return false;
        world.setBlockState(p, s, 3);
        placed.add(p.toImmutable());
        return true;
    }

    /** Подготовка задания. false — не получилось (нет места). */
    abstract boolean setup();

    /** Ход задания; done = true — закончено. */
    abstract void tick(ServerPlayerEntity p);

    abstract Text barText();

    SoccerBallEntity spawnBall(Vec3d at) {
        SoccerBallEntity b = new SoccerBallEntity(ModEntities.SOCCER_BALL, world);
        b.refreshPositionAndAngles(at.x, at.y, at.z, 0, 0);
        world.spawnEntity(b);
        ballId = b.getUuid();
        return b;
    }

    @Nullable
    SoccerBallEntity ball() {
        return ballId != null && world.getEntity(ballId) instanceof SoccerBallEntity b && b.isAlive() ? b : null;
    }

    void resetBall(Vec3d at) {
        SoccerBallEntity b = ball();
        if (b == null) { spawnBall(at); return; }
        b.refreshPositionAndAngles(at.x, at.y, at.z, 0, 0);
        b.setVelocity(Vec3d.ZERO);
        b.velocityModified = true;
        world.getChunkManager().sendToOtherNearbyPlayers(b, new EntityPositionS2CPacket(b));
    }

    Vec3d center(BlockPos p) {
        return Vec3d.ofBottomCenter(p);
    }

    void cleanup() {
        bar.clearPlayers();
        SoccerBallEntity b = ball();
        if (b != null) b.discard();
        for (BlockPos p : placed) {
            Block block = world.getBlockState(p).getBlock();
            if (block != Blocks.AIR) world.setBlockState(p, Blocks.AIR.getDefaultState(), 3);
        }
    }

    static String clock(int ticks) {
        int s = Math.max(0, ticks) / 20;
        return String.format("%d:%02d", s / 60, s % 60);
    }

    boolean ballTouchedBy(SoccerBallEntity b, Entity who) {
        return b.getLastKicker() != null && b.getLastKicker().getUuid().equals(who.getUuid());
    }
}
