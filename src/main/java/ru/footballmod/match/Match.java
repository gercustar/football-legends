package ru.footballmod.match;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.block.BlockState;
import net.minecraft.block.StairsBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import org.jetbrains.annotations.Nullable;
import ru.footballmod.ModEntities;
import ru.footballmod.ModItems;
import ru.footballmod.config.FootballConfig;
import ru.footballmod.entity.FootballerEntity;
import ru.footballmod.entity.FootballerEntity.Team;
import ru.footballmod.entity.RefereeEntity;
import ru.footballmod.entity.SoccerBallEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Один матч у табло: «Реал» (хозяева) против «Барсы» (гости).
 * Игроки выбирают команду на табло, футболисты-мобы на поле играют за свою команду.
 */
public class Match {
    public enum Phase { KICKOFF, PLAYING, GOAL, HALFTIME, FINISHED, PAUSED }

    final ServerWorld world;
    final BlockPos board;
    final Vec3d goalA, goalB, center;
    final Box zone;
    final Set<UUID> home, away;
    final int totalTicks;
    int elapsed, half = 1, homeScore, awayScore, phaseTicks;
    boolean homeDefendsA = true;
    Phase phase = Phase.KICKOFF;
    final ServerBossBar bar = new ServerBossBar(Text.empty(), BossBar.Color.GREEN, BossBar.Style.PROGRESS);
    final Map<UUID, Integer> goals = new HashMap<>();
    final Map<UUID, Text> names = new HashMap<>();
    final Map<UUID, Integer> yellows = new HashMap<>();
    /** Кто удалён и до какого времени мира. */
    final Map<UUID, Long> sentOff = new HashMap<>();
    final List<UUID> homeFans = new ArrayList<>(), awayFans = new ArrayList<>();
    @Nullable UUID referee;
    /** Вратари: ближайший к своим воротам футболист-моб каждой команды. */
    @Nullable UUID homeKeeper, awayKeeper;
    /** Ось поля от ворот A к воротам B, поперечная ось, расстояние между сетками. */
    final Vec3d axis, side;
    final double length;
    /** Счётчики для проверки: отскоки от бортов и потерянные мячи. */
    int bounces, lost;
    /** Ничья — после матча серия пенальти. */
    boolean penaltiesAfter;
    /** Пауза Золотым свистком: к какой фазе вернуться. */
    Phase beforePause = Phase.PLAYING;

    Match(ServerWorld world, BlockPos board, Vec3d goalA, Vec3d goalB, int minutes, Set<UUID> home, Set<UUID> away) {
        this.world = world;
        this.board = board;
        this.goalA = goalA;
        this.goalB = goalB;
        Vec3d mid = goalA.add(goalB).multiply(0.5);
        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, MathHelper.floor(mid.x), MathHelper.floor(mid.z));
        this.center = new Vec3d(MathHelper.floor(mid.x) + 0.5, y, MathHelper.floor(mid.z) + 0.5);
        this.zone = new Box(goalA, goalB).expand(22, 0, 22).expand(0, 10, 0).union(new Box(center, center).expand(22, 10, 22));
        this.home = home;
        this.away = away;
        this.totalTicks = Math.max(2, minutes) * 1200;
        Vec3d d = new Vec3d(goalB.x - goalA.x, 0, goalB.z - goalA.z);
        this.length = d.length();
        this.axis = d.multiply(1 / length);
        this.side = new Vec3d(-axis.z, 0, axis.x);
    }

    /** Положение точки вдоль поля (от сетки A) и поперёк (от середины). */
    double along(Vec3d p) {
        return (p.x - goalA.x) * axis.x + (p.z - goalA.z) * axis.z;
    }

    double lateral(Vec3d p) {
        return (p.x - center.x) * side.x + (p.z - center.z) * side.z;
    }

    /**
     * Невидимые борта по краю поля: мяч отскакивает от боковых линий и линий ворот,
     * а в сами ворота (между штангами, ниже перекладины) залетает как обычно.
     */
    private void keepBallsOnPitch() {
        double hw = FootballConfig.get().pitchHalfWidth, lineA = 2, lineB = length - 2;
        for (SoccerBallEntity b : world.getEntitiesByClass(SoccerBallEntity.class, zone, Entity::isAlive)) {
            Vec3d p = b.getPos(), v = b.getVelocity();
            double a = along(p), l = lateral(p);
            double va = v.x * axis.x + v.z * axis.z, vl = v.x * side.x + v.z * side.z;
            double na = a, nl = l;
            boolean hit = false;
            if (Math.abs(l) > hw) {
                nl = Math.signum(l) * hw;
                if (vl * Math.signum(l) > 0) vl = -vl * 0.6;
                hit = true;
            }
            boolean mouth = Math.abs(nl) <= 3.4 && p.y < center.y + 3.8;
            if (!mouth && (a < lineA || a > lineB)) {
                na = a < lineA ? lineA : lineB;
                if (va * (a < lineA ? -1 : 1) > 0) va = -va * 0.6;
                hit = true;
            }
            if (!hit) continue;
            bounces++;
            double dx = (na - a) * axis.x + (nl - l) * side.x, dz = (na - a) * axis.z + (nl - l) * side.z;
            b.setPosition(p.x + dx, p.y, p.z + dz);
            b.setVelocity(axis.x * va + side.x * vl, v.y, axis.z * va + side.z * vl);
            b.velocityModified = true;
        }
    }

    // ---------------- кто за кого ----------------

    @Nullable
    public Team teamOf(Entity e) {
        if (e instanceof PlayerEntity) {
            if (home.contains(e.getUuid())) return Team.REAL_MADRID;
            if (away.contains(e.getUuid())) return Team.BARCELONA;
            return null;
        }
        if (e instanceof FootballerEntity f && f.team() != Team.NEUTRAL && zone.contains(e.getPos())) return f.team();
        return null;
    }

    /** Центр ворот, которые защищает команда. */
    public Vec3d defendGoal(Team team) {
        return attackGoal(team) == goalA ? goalB : goalA;
    }

    public boolean isKeeper(Entity e) {
        return e.getUuid().equals(homeKeeper) || e.getUuid().equals(awayKeeper);
    }

    private void pickKeepers() {
        homeKeeper = nearestTo(defendGoal(Team.REAL_MADRID), Team.REAL_MADRID);
        awayKeeper = nearestTo(defendGoal(Team.BARCELONA), Team.BARCELONA);
    }

    @Nullable
    private UUID nearestTo(Vec3d goal, Team team) {
        FootballerEntity best = null;
        for (FootballerEntity f : world.getEntitiesByClass(FootballerEntity.class, zone, e -> e.isAlive() && e.team() == team && !isSentOff(e))) {
            if (best == null || f.squaredDistanceTo(goal) < best.squaredDistanceTo(goal)) best = f;
        }
        return best == null ? null : best.getUuid();
    }

    /** Центр ворот, которые атакует команда. */
    public Vec3d attackGoal(Team team) {
        boolean homeAttacksB = homeDefendsA;
        boolean attacksB = team == Team.REAL_MADRID ? homeAttacksB : !homeAttacksB;
        return attacksB ? goalB : goalA;
    }

    public boolean isSentOff(Entity e) {
        Long until = sentOff.get(e.getUuid());
        return until != null && world.getTime() < until;
    }

    public boolean ballFrozen() {
        return phase != Phase.PLAYING;
    }

    // ---------------- ход матча ----------------

    void start() {
        world.getEntitiesByClass(ArmorStandEntity.class, zone.expand(12), e -> e.getCommandTags().contains(MatchManager.FAN_TAG)).forEach(Entity::discard);
        spawnReferee();
        spawnFans();
        resetBall();
        broadcast(Text.translatable("msg.ronaldomessi.match_start", totalTicks / 1200), true);
        updateBar();
    }

    /** Возвращает false, когда матч можно убрать. */
    boolean tick() {
        FootballConfig cfg = FootballConfig.get();
        keepBallsOnPitch();
        switch (phase) {
            case KICKOFF -> {
                if (--phaseTicks <= 0) {
                    phase = Phase.PLAYING;
                    whistle(1);
                }
            }
            case PLAYING -> {
                elapsed++;
                if (elapsed % 10 == 0 && world.getEntitiesByClass(SoccerBallEntity.class, zone, Entity::isAlive).isEmpty()) {
                    // Мяч совсем улетел (или пропал) — в центр
                    lost++;
                    whistle(1);
                    broadcast(Text.translatable("msg.ronaldomessi.ball_out"), true);
                    resetBall();
                    return true;
                }
                if (half == 1 && elapsed >= totalTicks / 2) {
                    phase = Phase.HALFTIME;
                    phaseTicks = cfg.halftimePauseTicks;
                    whistle(2);
                    broadcast(Text.translatable("msg.ronaldomessi.match_halftime", homeScore, awayScore), true);
                } else if (elapsed >= totalTicks) {
                    finish();
                }
            }
            case GOAL -> {
                if (--phaseTicks <= 0) resetBall();
            }
            case HALFTIME -> {
                if (--phaseTicks <= 0) {
                    half = 2;
                    homeDefendsA = !homeDefendsA;
                    pickKeepers();
                    broadcast(Text.translatable("msg.ronaldomessi.match_second_half"), false);
                    resetBall();
                }
            }
            case FINISHED -> {
                if (--phaseTicks <= 0) {
                    cleanup();
                    return false;
                }
            }
            case PAUSED -> { }
        }
        returnSentOff();
        if (world.getTime() % 100 == 0 || homeKeeper == null && awayKeeper == null && world.getTime() % 20 == 0) pickKeepers();
        if (world.getTime() % 10 == 0) updateBar();
        return true;
    }

    private void resetBall() {
        SoccerBallEntity ball = null;
        double best = Double.MAX_VALUE;
        for (SoccerBallEntity b : world.getEntitiesByClass(SoccerBallEntity.class, zone, Entity::isAlive)) {
            double d = b.squaredDistanceTo(center);
            if (d < best) { best = d; ball = b; }
        }
        if (ball == null) {
            // Новый мяч: сначала место, потом в мир (иначе он появлялся в 0,0,0 и мог быть не виден)
            ball = new SoccerBallEntity(ModEntities.SOCCER_BALL, world);
            ball.refreshPositionAndAngles(center.x, center.y + 0.5, center.z, 0, 0);
            world.spawnEntity(ball);
        }
        placeBall(ball, new Vec3d(center.x, center.y + 0.5, center.z), FootballConfig.get().kickoffPauseTicks);
    }

    /** Ставит мяч в точку и замораживает игру на pause тиков. */
    private void placeBall(SoccerBallEntity ball, Vec3d at, int pause) {
        ball.refreshPositionAndAngles(at.x, at.y, at.z, 0, 0);
        ball.setVelocity(Vec3d.ZERO);
        ball.velocityModified = true;
        // Сразу сообщаем игрокам точное место мяча
        world.getChunkManager().sendToOtherNearbyPlayers(ball, new net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket(ball));
        phase = Phase.KICKOFF;
        phaseTicks = pause;
    }

    /** Гол в сетку у net. true — гол обработан матчем. */
    boolean onGoal(SoccerBallEntity ball, BlockPos net) {
        Vec3d n = Vec3d.ofCenter(net);
        boolean inA = n.squaredDistanceTo(goalA) < 64, inB = n.squaredDistanceTo(goalB) < 64;
        if (!inA && !inB) return false;
        if (phase != Phase.PLAYING) return true;
        boolean homeDefends = inA == homeDefendsA;
        Team scoring = homeDefends ? Team.BARCELONA : Team.REAL_MADRID;
        if (scoring == Team.REAL_MADRID) homeScore++; else awayScore++;
        Entity scorer = ball.getLastKicker();
        Text who;
        if (scorer != null && teamOf(scorer) == scoring) {
            goals.merge(scorer.getUuid(), 1, Integer::sum);
            names.put(scorer.getUuid(), scorer.getDisplayName());
            who = scorer.getDisplayName();
        } else {
            who = Text.translatable("msg.ronaldomessi.own_goal", scorer != null ? scorer.getDisplayName() : Text.literal("?"));
        }
        Text score = scoreText();
        for (ServerPlayerEntity p : viewers()) {
            p.networkHandler.sendPacket(new SubtitleS2CPacket(score));
            p.networkHandler.sendPacket(new TitleS2CPacket(Text.translatable("msg.ronaldomessi.goal_title")));
        }
        broadcast(Text.translatable("msg.ronaldomessi.match_goal", teamName(scoring), who, homeScore, awayScore), false);
        cheer(scoring);
        world.playSound(null, ball.getX(), ball.getY(), ball.getZ(), SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, SoundCategory.NEUTRAL, 2.0f, 1.0f);
        phase = Phase.GOAL;
        phaseTicks = 40;
        updateBar();
        return true;
    }

    void finish() {
        phase = Phase.FINISHED;
        phaseTicks = 200;
        whistle(3);
        Text result = homeScore == awayScore ? Text.translatable("msg.ronaldomessi.match_draw")
                : Text.translatable("msg.ronaldomessi.match_winner", teamName(homeScore > awayScore ? Team.REAL_MADRID : Team.BARCELONA));
        for (ServerPlayerEntity p : viewers()) {
            p.networkHandler.sendPacket(new SubtitleS2CPacket(scoreText()));
            p.networkHandler.sendPacket(new TitleS2CPacket(result));
        }
        broadcast(Text.translatable("msg.ronaldomessi.match_end", homeScore, awayScore).append(" ").append(result), true);
        // Лучший бомбардир-игрок получает Золотой мяч
        UUID best = null;
        int max = 0;
        for (var e : goals.entrySet()) if (e.getValue() > max) { max = e.getValue(); best = e.getKey(); }
        if (best != null) {
            broadcast(Text.translatable("msg.ronaldomessi.match_top_scorer", names.get(best), max), false);
            ServerPlayerEntity p = world.getServer().getPlayerManager().getPlayer(best);
            if (p != null) p.getInventory().offerOrDrop(new ItemStack(ModItems.GOLDEN_BALL));
        }
        if (homeScore != awayScore) cheer(homeScore > awayScore ? Team.REAL_MADRID : Team.BARCELONA);
        else {
            penaltiesAfter = true;
            broadcast(Text.translatable("msg.ronaldomessi.draw_penalties"), false);
        }
        updateBar();
    }

    void cleanup() {
        bar.clearPlayers();
        if (referee != null) {
            Entity r = world.getEntity(referee);
            if (r != null) r.discard();
        }
        for (UUID id : homeFans) { Entity e = world.getEntity(id); if (e != null) e.discard(); }
        for (UUID id : awayFans) { Entity e = world.getEntity(id); if (e != null) e.discard(); }
        for (UUID id : sentOff.keySet()) {
            Entity e = world.getEntity(id);
            if (e instanceof FootballerEntity f) f.setAiDisabled(false);
        }
    }

    // ---------------- Золотой свисток ----------------

    /** Пауза/продолжение. true — теперь пауза. */
    public boolean togglePause(Entity by) {
        if (phase == Phase.FINISHED) return false;
        if (phase == Phase.PAUSED) {
            phase = beforePause;
            whistle(1);
            broadcast(Text.translatable("msg.ronaldomessi.whistle_resume", by.getDisplayName()), true);
            return false;
        }
        beforePause = phase;
        phase = Phase.PAUSED;
        whistle(2);
        broadcast(Text.translatable("msg.ronaldomessi.whistle_pause", by.getDisplayName()), true);
        updateBar();
        return true;
    }

    /** Мяч в центр, пауза перед свистком. */
    public void restartFromCentre(Entity by) {
        if (phase == Phase.FINISHED) return;
        broadcast(Text.translatable("msg.ronaldomessi.whistle_restart", by.getDisplayName()), true);
        resetBall();
    }

    /** Карточка от игрока со свистком. */
    public void cardFromWhistle(Entity target) {
        if (teamOf(target) != null) foul(target);
    }

    // ---------------- фолы и карточки ----------------

    /** Фол: attacker ударил соперника. Жёлтая; вторая жёлтая — красная и удаление. */
    void foul(Entity attacker) {
        if (phase == Phase.FINISHED) return;
        int y = yellows.merge(attacker.getUuid(), 1, Integer::sum);
        RefereeEntity ref = refereeEntity();
        if (ref != null) ref.showCard(attacker, y >= 2);
        whistle(1);
        if (y < 2) {
            broadcast(Text.translatable("msg.ronaldomessi.yellow_card", attacker.getDisplayName()), false);
            return;
        }
        yellows.put(attacker.getUuid(), 0);
        long ticks = FootballConfig.get().sentOffTicks;
        sentOff.put(attacker.getUuid(), world.getTime() + ticks);
        broadcast(Text.translatable("msg.ronaldomessi.red_card", attacker.getDisplayName(), ticks / 20), false);
        Vec3d bench = Vec3d.ofBottomCenter(board.up());
        if (attacker instanceof ServerPlayerEntity p) p.requestTeleport(bench.x, bench.y, bench.z);
        else {
            attacker.refreshPositionAndAngles(bench.x, bench.y, bench.z, attacker.getYaw(), 0);
            if (attacker instanceof FootballerEntity f) f.setAiDisabled(true);
        }
    }

    private void returnSentOff() {
        if (sentOff.isEmpty() || world.getTime() % 20 != 0) return;
        sentOff.entrySet().removeIf(e -> {
            if (world.getTime() < e.getValue()) return false;
            Entity ent = world.getEntity(e.getKey());
            if (ent instanceof FootballerEntity f) f.setAiDisabled(false);
            if (ent != null) broadcast(Text.translatable("msg.ronaldomessi.back_from_red", ent.getDisplayName()), false);
            return true;
        });
    }

    // ---------------- судья, болельщики, салют ----------------

    @Nullable
    RefereeEntity refereeEntity() {
        return referee != null && world.getEntity(referee) instanceof RefereeEntity r ? r : null;
    }

    private void spawnReferee() {
        RefereeEntity r = new RefereeEntity(ModEntities.REFEREE, world);
        r.refreshPositionAndAngles(center.x + 3, center.y, center.z, 0, 0);
        r.setInvulnerable(true);
        r.setPersistent();
        world.spawnEntity(r);
        referee = r.getUuid();
    }

    /** Болельщики поровну на обеих трибунах и равномерно по всей длине (раньше при поле «восток–запад» все садились на одну). */
    private void spawnFans() {
        int max = FootballConfig.get().maxFans;
        Vec3d axisDir = goalB.subtract(goalA);
        List<BlockPos> homeSeats = new ArrayList<>(), awaySeats = new ArrayList<>();
        for (BlockPos p : BlockPos.iterate(BlockPos.ofFloored(zone.minX - 8, center.y, zone.minZ - 8), BlockPos.ofFloored(zone.maxX + 8, center.y + 6, zone.maxZ + 8))) {
            if (!(world.getBlockState(p).getBlock() instanceof StairsBlock) || !world.getBlockState(p.up()).isAir()) continue;
            Vec3d rel = Vec3d.ofBottomCenter(p).subtract(center);
            (axisDir.x * rel.z - axisDir.z * rel.x > 0 ? homeSeats : awaySeats).add(p.toImmutable());
        }
        seat(homeSeats, max / 2, true);
        seat(awaySeats, max - max / 2, false);
    }

    private void seat(List<BlockPos> seats, int count, boolean homeSide) {
        if (seats.isEmpty() || count <= 0) return;
        seats.sort(java.util.Comparator.comparingDouble(p -> along(Vec3d.ofBottomCenter(p)) * 1000 + Math.abs(lateral(Vec3d.ofBottomCenter(p)))));
        double step = Math.max(1.0, seats.size() / (double) count);
        for (int k = 0; k < count && (int) (k * step) < seats.size(); k++) {
            BlockPos p = seats.get((int) (k * step));
            Vec3d at = Vec3d.ofBottomCenter(p).add(0, 0.5, 0);
            ArmorStandEntity fan = new ArmorStandEntity(world, at.x, at.y, at.z);
            NbtCompound nbt = new NbtCompound();
            nbt.putBoolean("Marker", true);
            nbt.putBoolean("Invulnerable", true);
            nbt.putInt("DisabledSlots", 4144959);
            nbt.putBoolean("ShowArms", true);
            fan.readCustomDataFromNbt(nbt);
            fan.setInvulnerable(true);
            fan.addCommandTag(MatchManager.FAN_TAG);
            Vec3d look = center.subtract(at);
            fan.setYaw((float) (MathHelper.atan2(look.z, look.x) * 180 / Math.PI) - 90);
            int shirt = homeSide ? 0xF4F4F4 : 0x004D98, legs = homeSide ? 0xF4F4F4 : 0xA50044;
            fan.equipStack(EquipmentSlot.CHEST, dyed(Items.LEATHER_CHESTPLATE, shirt));
            fan.equipStack(EquipmentSlot.LEGS, dyed(Items.LEATHER_LEGGINGS, legs));
            fan.equipStack(EquipmentSlot.HEAD, dyed(Items.LEATHER_HELMET, homeSide ? 0xC9A24A : 0xEDBB00));
            world.spawnEntity(fan);
            (homeSide ? homeFans : awayFans).add(fan.getUuid());
        }
    }

    private static ItemStack dyed(net.minecraft.item.Item item, int rgb) {
        ItemStack s = new ItemStack(item);
        s.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(rgb, false));
        return s;
    }

    /** Болельщики команды подпрыгивают, над ними салют. */
    void cheer(Team team) {
        List<UUID> fans = team == Team.REAL_MADRID ? homeFans : awayFans;
        int colorA = team == Team.REAL_MADRID ? 0xFFFFFF : 0x004D98, colorB = team == Team.REAL_MADRID ? 0xC9A24A : 0xA50044;
        int rockets = 0;
        for (UUID id : fans) {
            Entity e = world.getEntity(id);
            if (e == null) continue;
            if (rockets < 3 && world.random.nextInt(4) == 0) {
                ItemStack fw = new ItemStack(Items.FIREWORK_ROCKET);
                fw.set(DataComponentTypes.FIREWORKS, new FireworksComponent(1, List.of(new FireworkExplosionComponent(
                        FireworkExplosionComponent.Type.LARGE_BALL, IntList.of(colorA, colorB), IntList.of(), true, true))));
                world.spawnEntity(new FireworkRocketEntity(world, e.getX(), e.getY() + 2, e.getZ(), fw));
                rockets++;
            }
        }
        if (!fans.isEmpty()) {
            Entity e = world.getEntity(fans.get(0));
            if (e != null) world.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.ENTITY_VILLAGER_CELEBRATE, SoundCategory.NEUTRAL, 2.0f, 1.0f);
        }
    }

    void whistle(int times) {
        for (int i = 0; i < times; i++) {
            world.playSound(null, center.x, center.y + 1, center.z, SoundEvents.BLOCK_NOTE_BLOCK_FLUTE.value(), SoundCategory.NEUTRAL, 3.0f, 2.0f - i * 0.05f);
        }
    }

    // ---------------- табло сверху экрана ----------------

    static Text teamName(Team t) {
        return Text.translatable(t == Team.REAL_MADRID ? "match.ronaldomessi.home" : "match.ronaldomessi.away");
    }

    Text scoreText() {
        return Text.empty().append(teamName(Team.REAL_MADRID)).append(" " + homeScore + " : " + awayScore + " ").append(teamName(Team.BARCELONA));
    }

    List<ServerPlayerEntity> viewers() {
        Box view = zone.expand(16);
        List<ServerPlayerEntity> out = new ArrayList<>();
        for (ServerPlayerEntity p : world.getPlayers()) {
            if (view.contains(p.getPos()) || home.contains(p.getUuid()) || away.contains(p.getUuid())) out.add(p);
        }
        return out;
    }

    void updateBar() {
        int left = Math.max(0, totalTicks - elapsed) / 20;
        String clock = String.format("%02d:%02d", left / 60, left % 60);
        String stage = switch (phase) {
            case HALFTIME -> "match.ronaldomessi.phase_halftime";
            case PAUSED -> "match.ronaldomessi.phase_paused";
            case FINISHED -> "match.ronaldomessi.phase_finished";
            default -> half == 1 ? "match.ronaldomessi.phase_first" : "match.ronaldomessi.phase_second";
        };
        bar.setName(scoreText().copy().append(Text.literal("  " + clock + "  ").formatted(Formatting.YELLOW)).append(Text.translatable(stage)));
        bar.setPercent(MathHelper.clamp(1f - elapsed / (float) totalTicks, 0f, 1f));
        bar.setColor(phase == Phase.FINISHED ? BossBar.Color.YELLOW : phase == Phase.PLAYING ? BossBar.Color.GREEN : BossBar.Color.WHITE);
        Set<ServerPlayerEntity> now = new HashSet<>(viewers());
        for (ServerPlayerEntity p : List.copyOf(bar.getPlayers())) if (!now.contains(p)) bar.removePlayer(p);
        for (ServerPlayerEntity p : now) bar.addPlayer(p);
    }

    void broadcast(Text text, boolean overlayToo) {
        for (ServerPlayerEntity p : viewers()) {
            p.sendMessage(text, false);
            if (overlayToo) p.sendMessage(text, true);
        }
    }
}
