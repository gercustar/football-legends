package ru.footballmod.academy;

import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import ru.footballmod.ModBlocks;
import ru.footballmod.entity.SoccerBallEntity;
import ru.footballmod.item.GoalItem;
import ru.footballmod.match.MatchManager;
import ru.footballmod.match.PenaltyShootout;

import java.util.ArrayList;
import java.util.List;

/** Три задания академии: точность, дриблинг, пенальти. */
final class Drills {
    private Drills() {}

    static void ding(ServerWorld w, Vec3d at, float pitch) {
        w.playSound(null, at.x, at.y, at.z, SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.PLAYERS, 1.2f, pitch);
        w.spawnParticles(ParticleTypes.HAPPY_VILLAGER, at.x, at.y + 0.5, at.z, 12, 0.4, 0.4, 0.4, 0.05);
    }

    // ---------------- 1. Точность: 3 мишени, 8 ударов ----------------
    static final class Shooting extends Drill {
        static final int SHOTS = 8, TIME = 90 * 20;
        final List<BlockPos> targets = new ArrayList<>();
        int hits, shots, sinceKick = -1, resetIn = -1;
        Vec3d spot;

        Shooting(ServerWorld w, ServerPlayerEntity p) { super(w, p); }

        @Override
        boolean setup() {
            int[][] t = {{-3, 1}, {0, 2}, {3, 1}};
            for (int[] x : t) {
                BlockPos p = at(15, x[0], x[1]);
                // мишени на столбиках от земли
                for (int y = 0; y < x[1]; y++) if (!put(at(15, x[0], y), Blocks.OAK_FENCE.getDefaultState())) return false;
                if (!put(p, Blocks.TARGET.getDefaultState())) return false;
                targets.add(p);
            }
            spot = center(at(3, 0, 0)).add(0, 0.3, 0);
            spawnBall(spot);
            return true;
        }

        @Override
        void tick(ServerPlayerEntity p) {
            ticks++;
            SoccerBallEntity b = ball();
            if (b == null) { spawnBall(spot); return; }
            if (resetIn > 0 && --resetIn == 0) { resetBall(spot); sinceKick = -1; resetIn = -1; }
            if (sinceKick < 0 && b.getVelocity().horizontalLengthSquared() > 0.04 && ballTouchedBy(b, p)) sinceKick = 0;
            if (sinceKick >= 0 && resetIn < 0) {
                sinceKick++;
                Box box = b.getBoundingBox().expand(0.3);
                for (BlockPos t : List.copyOf(targets)) {
                    if (box.intersects(new Box(t))) {
                        targets.remove(t);
                        world.setBlockState(t, Blocks.LIME_WOOL.getDefaultState(), 3);
                        hits++;
                        ding(world, Vec3d.ofCenter(t), 1.0f + hits * 0.25f);
                        p.sendMessage(Text.translatable("msg.ronaldomessi.drill_hit", hits), true);
                        endShot();
                        return;
                    }
                }
                if (sinceKick > 70 || b.getVelocity().lengthSquared() < 0.002 || b.squaredDistanceTo(spot) > 30 * 30) endShot();
            }
            if (hits >= 3 || shots >= SHOTS || ticks >= TIME) done = true;
        }

        private void endShot() {
            shots++;
            resetIn = 15;
        }

        @Override
        Text barText() {
            return Text.translatable("msg.ronaldomessi.drill_shooting_bar", hits, shots, SHOTS, clock(TIME - ticks));
        }
    }

    // ---------------- 2. Дриблинг: слалом между 5 конусами на время ----------------
    static final class Dribble extends Drill {
        static final int CONES = 5, TIME = 45 * 20;
        final List<Vec3d> checkpoints = new ArrayList<>();
        Vec3d spot, finish;
        int next;
        boolean success;

        Dribble(ServerWorld w, ServerPlayerEntity p) { super(w, p); }

        @Override
        boolean setup() {
            Vec3d sideVec = Vec3d.of(side.getVector());
            for (int i = 0; i < CONES; i++) {
                BlockPos cone = at(5 + 3 * i, 0, 0);
                if (!put(cone, ModBlocks.TRAINING_CONE.getDefaultState())) return false;
                checkpoints.add(center(cone).add(sideVec.multiply(i % 2 == 0 ? 1.6 : -1.6)));
            }
            BlockPos fin = at(5 + 3 * CONES + 1, 0, 0);
            if (!put(fin, Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE.getDefaultState())) return false;
            finish = center(fin);
            spot = center(at(2, 0, 0)).add(0, 0.3, 0);
            spawnBall(spot);
            return true;
        }

        @Override
        void tick(ServerPlayerEntity p) {
            ticks++;
            SoccerBallEntity b = ball();
            if (b == null) { done = true; return; }
            Vec3d pos = b.getPos();
            if (next < checkpoints.size()) {
                Vec3d c = checkpoints.get(next);
                if (horiz(pos, c) < 1.4) {
                    next++;
                    ding(world, c, 0.8f + next * 0.15f);
                }
            } else if (horiz(pos, finish) < 1.6) {
                success = true;
                done = true;
                return;
            }
            if (ticks >= TIME || b.squaredDistanceTo(spot) > 35 * 35) done = true;
        }

        private static double horiz(Vec3d a, Vec3d b) {
            double dx = a.x - b.x, dz = a.z - b.z;
            return Math.sqrt(dx * dx + dz * dz);
        }

        @Override
        Text barText() {
            return Text.translatable("msg.ronaldomessi.drill_dribble_bar", next, CONES, clock(ticks));
        }
    }

    // ---------------- 3. Пенальти: 5 ударов во вратаря-бота ----------------
    static final class Penalties extends Drill {
        PenaltyShootout shootout;
        int goals = -1;

        Penalties(ServerWorld w, ServerPlayerEntity p) { super(w, p); }

        @Override
        boolean setup() {
            BlockPos base = at(15, 0, 0);   // ворота стоят на земле (уровень ног игрока)
            Direction f = forward;
            // сначала проверяем, что место под ворота свободно
            Direction s = f.rotateYClockwise();
            List<BlockPos> spots = new ArrayList<>();
            for (int lat = -3; lat <= 3; lat++) for (int y = 0; y <= 3; y++) for (int d = 0; d <= 2; d++) {
                boolean edge = Math.abs(lat) == 3;
                if (d == 0 && !(edge || y == 3)) continue;
                if (d == 1 && !(edge || y == 3)) continue;
                spots.add(base.offset(s, lat).up(y).offset(f, d));
            }
            for (BlockPos p : spots) if (!world.getBlockState(p).isReplaceable() || world.getBlockState(p).hasBlockEntity()) return false;
            GoalItem.placeGoal(world, base, f);
            for (BlockPos p : spots) if (world.getBlockState(p).isOf(ModBlocks.GOAL_POST) || world.getBlockState(p).isOf(ModBlocks.GOAL_NET)) placed.add(p);
            ServerPlayerEntity pl = world.getServer().getPlayerManager().getPlayer(player);
            Vec3d net = Vec3d.ofCenter(base.offset(f, 2).up(1));
            shootout = MatchManager.startTraining(world, pl, net, Vec3d.of(f.getOpposite().getVector()), g -> goals = g);
            return true;
        }

        @Override
        void tick(ServerPlayerEntity p) {
            ticks++;
            if (goals >= 0 && !MatchManager.shootouts().contains(shootout)) done = true;
            if (ticks > 20 * 120) done = true;
        }

        @Override
        void cleanup() {
            if (MatchManager.shootouts().contains(shootout)) MatchManager.stopShootout(shootout);
            super.cleanup();
        }

        @Override
        Text barText() {
            return Text.translatable("msg.ronaldomessi.drill_penalty_bar");
        }
    }
}
