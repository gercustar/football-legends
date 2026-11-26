package ru.footballmod.match;

import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import org.jetbrains.annotations.Nullable;
import ru.footballmod.ModEntities;
import ru.footballmod.config.FootballConfig;
import ru.footballmod.entity.FootballerEntity;
import ru.footballmod.entity.FootballerEntity.Team;
import ru.footballmod.entity.GoalkeeperEntity;
import ru.footballmod.entity.SoccerBallEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.IntConsumer;

/**
 * Серия пенальти (Этап 5): по 5 ударов, потом до первого промаха. Бьют игроки команды, а если их нет —
 * футболисты-мобы. В воротах — игрок защищающейся команды (Shift — поймать) или вратарь-бот, который прыгает в угол.
 * В тренировочном режиме (академия) бьёт один игрок 5 раз.
 */
public class PenaltyShootout {
    public enum Stage { SETUP, WAIT, AIM, FLIGHT, RESULT, FINISHED }
    private static final int REGULATION = 5;

    final ServerWorld world;
    @Nullable final BlockPos board;
    /** Центр задней сетки, направление из ворот в поле, поперёк, середина линии ворот, точка. */
    final Vec3d net, out, side, line, spot;
    final double ground;
    final Box zone;
    final List<UUID> homePlayers, awayPlayers;
    @Nullable final UUID trainee;
    @Nullable final IntConsumer onTrainingDone;
    final List<Boolean> home = new ArrayList<>(), away = new ArrayList<>();
    Team kicking = Team.REAL_MADRID;
    Stage stage = Stage.SETUP;
    int timer, flightTicks, homeTurn, awayTurn, diveSide;
    @Nullable UUID ballId, kickerId, keeperId;
    /** Единственный вратарь-бот этой серии (раньше мог создаваться заново на каждый удар). */
    @Nullable UUID aiKeeperId;
    boolean kickerIsPlayer, keeperIsAi;
    @Nullable Boolean lastResult;
    String lastResultKey = "";
    final List<UUID> tempMobs = new ArrayList<>();
    /** Боты на время серии стоят в шеренгах за точкой: где стоит каждый. */
    final java.util.Map<UUID, Vec3d> lineup = new java.util.LinkedHashMap<>();
    final ServerBossBar bar = new ServerBossBar(Text.empty(), BossBar.Color.YELLOW, BossBar.Style.NOTCHED_10);

    PenaltyShootout(ServerWorld world, @Nullable BlockPos board, Vec3d net, Vec3d toField, List<UUID> homePlayers, List<UUID> awayPlayers,
                    @Nullable UUID trainee, @Nullable IntConsumer onTrainingDone) {
        this.world = world;
        this.board = board;
        this.net = net;
        this.out = new Vec3d(toField.x, 0, toField.z).normalize();
        this.side = new Vec3d(-out.z, 0, out.x);
        Vec3d l = net.add(out.multiply(2));
        Vec3d s = l.add(out.multiply(9));
        this.ground = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, MathHelper.floor(s.x), MathHelper.floor(s.z));
        this.line = new Vec3d(l.x, ground, l.z);
        this.spot = new Vec3d(MathHelper.floor(s.x) + 0.5, ground, MathHelper.floor(s.z) + 0.5);
        this.zone = new Box(net, spot).expand(12, 6, 12);
        this.homePlayers = homePlayers;
        this.awayPlayers = awayPlayers;
        this.trainee = trainee;
        this.onTrainingDone = onTrainingDone;
    }

    boolean training() {
        return trainee != null;
    }

    // ---------------- ход серии ----------------

    void start() {
        broadcast(Text.translatable(training() ? "msg.ronaldomessi.pen_training_start" : "msg.ronaldomessi.pen_start"), true);
        if (!training()) {
            lineUp();
            for (ServerPlayerEntity p : viewers()) {
                if (!homePlayers.contains(p.getUuid()) && !awayPlayers.contains(p.getUuid())) {
                    p.sendMessage(Text.translatable("msg.ronaldomessi.pen_spectator").formatted(Formatting.GRAY), false);
                }
            }
        }
        setup();
    }

    /** Шеренги за точкой: Реал слева, Барса справа; все стоят и смотрят на ворота. */
    private void lineUp() {
        int h = 0, a = 0;
        List<FootballerEntity> mobs = world.getEntitiesByClass(FootballerEntity.class, zone.expand(40), e -> e.isAlive() && e.team() != Team.NEUTRAL);
        mobs.sort(java.util.Comparator.comparing(Entity::getUuid));
        for (FootballerEntity f : mobs) {
            boolean isHome = f.team() == Team.REAL_MADRID;
            int i = isHome ? h++ : a++;
            Vec3d at = spot.add(out.multiply(9 + (i / 6) * 2)).add(side.multiply((isHome ? -1 : 1) * (2.5 + (i % 6) * 1.5)));
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, MathHelper.floor(at.x), MathHelper.floor(at.z));
            Vec3d pos = new Vec3d(at.x, y, at.z);
            lineup.put(f.getUuid(), pos);
            standAt(f, pos);
        }
    }

    private void standAt(Entity e, Vec3d pos) {
        float face = yawTo(pos, line);
        e.refreshPositionAndAngles(pos.x, pos.y, pos.z, face, 0);
        e.setVelocity(Vec3d.ZERO);
        e.setHeadYaw(face);
        if (e instanceof FootballerEntity f) {
            f.setBodyYaw(face);
            if (!(f instanceof GoalkeeperEntity)) f.setAiDisabled(true);   // вратарю нужен ИИ для прыжка
            f.getNavigation().stop();
        }
    }

    /** Ударивший бот возвращается в свою шеренгу. */
    private void backToLine(@Nullable UUID id) {
        if (id == null) return;
        Vec3d pos = lineup.get(id);
        Entity e = world.getEntity(id);
        if (pos != null && e != null && !(e instanceof PlayerEntity)) standAt(e, pos);
    }

    /** false — серию можно убрать. */
    boolean tick() {
        SoccerBallEntity ball = ball();
        switch (stage) {
            case SETUP -> setup();
            case WAIT -> {
                if (--timer <= 0) {
                    whistle();
                    stage = Stage.AIM;
                    timer = kickerIsPlayer ? FootballConfig.get().penaltyPlayerTimeTicks : 15;
                }
            }
            case AIM -> {
                if (ball == null) { result(false, "msg.ronaldomessi.pen_miss"); break; }
                if (ball.getVelocity().horizontalLengthSquared() > 0.04 && ball.getLastKicker() != null && ball.getLastKicker().getUuid().equals(kickerId)) {
                    startFlight(ball);
                } else if (--timer <= 0) {
                    if (kickerIsPlayer) result(false, "msg.ronaldomessi.pen_timeout");
                    else { botKick(ball); startFlight(ball); }
                }
            }
            case FLIGHT -> flight(ball);
            case RESULT -> {
                if (--timer <= 0) next();
            }
            case FINISHED -> {
                if (--timer <= 0) {
                    cleanup();
                    return false;
                }
            }
        }
        if (world.getTime() % 10 == 0) updateBar();
        return true;
    }

    private void setup() {
        FootballConfig cfg = FootballConfig.get();
        SoccerBallEntity ball = ball();
        if (ball == null) {
            ball = new SoccerBallEntity(ModEntities.SOCCER_BALL, world);
            ball.refreshPositionAndAngles(spot.x, spot.y + 0.3, spot.z, 0, 0);
            world.spawnEntity(ball);
            ballId = ball.getUuid();
        }
        place(ball, spot.add(0, 0.3, 0));
        // кто бьёт
        Entity kicker = chooseKicker();
        kickerId = kicker.getUuid();
        kickerIsPlayer = kicker instanceof PlayerEntity;
        Vec3d behind = spot.add(out.multiply(2.2));
        float faceGoal = yawTo(behind, line);
        if (kicker instanceof ServerPlayerEntity p) {
            p.teleport(world, behind.x, behind.y, behind.z, faceGoal, 10f);
            p.sendMessage(Text.translatable("msg.ronaldomessi.pen_your_kick").formatted(Formatting.GOLD), true);
        } else {
            standAt(kicker, behind);
            if (!lineup.containsKey(kicker.getUuid())) lineup.put(kicker.getUuid(), behind.add(out.multiply(9)));
        }
        // кто в воротах
        Entity keeper = chooseKeeper();
        keeperId = keeper.getUuid();
        keeperIsAi = keeper instanceof GoalkeeperEntity;
        Vec3d keep = line.add(out.multiply(0.5));
        float faceField = yawTo(keep, spot);
        if (keeper instanceof ServerPlayerEntity p) {
            p.teleport(world, keep.x, keep.y, keep.z, faceField, 0f);
            p.sendMessage(Text.translatable("msg.ronaldomessi.pen_you_keep").formatted(Formatting.AQUA), true);
        } else {
            keeper.refreshPositionAndAngles(keep.x, keep.y, keep.z, faceField, 0);
            keeper.setVelocity(Vec3d.ZERO);
            keeper.setHeadYaw(faceField);
        }
        broadcast(Text.translatable("msg.ronaldomessi.pen_kicker", kicker.getDisplayName(), Match.teamName(kicking)), false);
        stage = Stage.WAIT;
        timer = cfg.penaltyWaitTicks;
        flightTicks = 0;
        diveSide = 0;
    }

    private void startFlight(SoccerBallEntity ball) {
        stage = Stage.FLIGHT;
        flightTicks = 0;
        // Вратарь-бот угадывает угол (не всегда)
        double towards = Math.signum(ball.getVelocity().x * side.x + ball.getVelocity().z * side.z);
        float r = world.random.nextFloat();
        FootballConfig cfg = FootballConfig.get();
        if (r < 0.15f) diveSide = 0;
        else if (r < 0.15f + cfg.keeperReadChance) diveSide = (int) towards;
        else diveSide = -(int) towards;
        if (diveSide == 0 && towards != 0 && r >= 0.15f) diveSide = (int) towards;
    }

    private void flight(@Nullable SoccerBallEntity ball) {
        flightTicks++;
        if (ball == null) { result(false, "msg.ronaldomessi.pen_miss"); return; }
        Entity keeper = keeperId == null ? null : world.getEntity(keeperId);
        if (keeper instanceof GoalkeeperEntity gk) {
            if (flightTicks == 2) gk.dive(side.multiply(diveSide), diveSide == 0);
            Box reach = gk.getBoundingBox().expand(0.45, 0.25, 0.45);
            Box path = ball.getBoundingBox().stretch(ball.getVelocity().multiply(-1));
            if (flightTicks >= 2 && reach.intersects(path)) {
                ball.launch(out.multiply(0.35).add(side.multiply(diveSide * 0.2)).add(0, 0.2, 0), 0, gk);
                world.playSound(null, ball.getX(), ball.getY(), ball.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE, SoundCategory.NEUTRAL, 1.2f, 0.7f);
                result(false, "msg.ronaldomessi.pen_saved");
                return;
            }
        }
        Entity last = ball.getLastKicker();
        boolean keeperTouched = last != null && last.getUuid().equals(keeperId);
        if (keeperTouched) { result(false, "msg.ronaldomessi.pen_saved"); return; }
        if (flightTicks > 60 || (flightTicks > 4 && ball.getVelocity().lengthSquared() < 0.003) || ball.squaredDistanceTo(spot) > 30 * 30) {
            result(false, "msg.ronaldomessi.pen_miss");
        }
    }

    /** Гол в сетку серии. */
    boolean onGoal(SoccerBallEntity ball, BlockPos netPos) {
        if (!ball.getUuid().equals(ballId) || Vec3d.ofCenter(netPos).squaredDistanceTo(net) > 25) return false;
        if (stage == Stage.FLIGHT || stage == Stage.AIM) result(true, "msg.ronaldomessi.pen_goal");
        return true;
    }

    private void result(boolean goal, String key) {
        (kicking == Team.REAL_MADRID ? home : away).add(goal);
        lastResult = goal;
        lastResultKey = key;
        stage = Stage.RESULT;
        timer = 40;
        Text title = Text.translatable(key).formatted(goal ? Formatting.GREEN : Formatting.RED);
        for (ServerPlayerEntity p : viewers()) {
            p.networkHandler.sendPacket(new SubtitleS2CPacket(scoreLine()));
            p.networkHandler.sendPacket(new TitleS2CPacket(title));
        }
        if (goal) world.playSound(null, net.x, net.y, net.z, SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, SoundCategory.NEUTRAL, 1.5f, 1.0f);
        updateBar();
    }

    private void next() {
        backToLine(kickerId);
        Team winner = winner();
        if (winner != null || (training() && home.size() >= REGULATION)) {
            finish(winner);
            return;
        }
        if (!training()) kicking = kicking == Team.REAL_MADRID ? Team.BARCELONA : Team.REAL_MADRID;
        stage = Stage.SETUP;
    }

    /** Победитель по правилам: 5 ударов, досрочно, если догнать нельзя; потом — до первого промаха. */
    @Nullable
    Team winner() {
        if (training()) return null;
        int gh = goals(home), ga = goals(away), kh = home.size(), ka = away.size();
        if (kh <= REGULATION && ka <= REGULATION) {
            if (gh > ga + (REGULATION - ka)) return Team.REAL_MADRID;
            if (ga > gh + (REGULATION - kh)) return Team.BARCELONA;
            return null;
        }
        if (kh == ka && gh != ga) return gh > ga ? Team.REAL_MADRID : Team.BARCELONA;
        return null;
    }

    private void finish(@Nullable Team winner) {
        stage = Stage.FINISHED;
        timer = 100;
        whistle();
        whistle();
        Text title = training() ? Text.translatable("msg.ronaldomessi.pen_training_result", goals(home), home.size())
                : Text.translatable("msg.ronaldomessi.match_winner", Match.teamName(winner));
        for (ServerPlayerEntity p : viewers()) {
            p.networkHandler.sendPacket(new SubtitleS2CPacket(scoreLine()));
            p.networkHandler.sendPacket(new TitleS2CPacket(title));
        }
        if (!training()) broadcast(Text.translatable("msg.ronaldomessi.pen_end", scoreLine(), Match.teamName(winner)), false);
        if (onTrainingDone != null) onTrainingDone.accept(goals(home));
        updateBar();
    }

    void cleanup() {
        bar.clearPlayers();
        if (ballId != null) { Entity b = world.getEntity(ballId); if (b != null) b.discard(); }
        if (aiKeeperId != null) { Entity k = world.getEntity(aiKeeperId); if (k != null) k.discard(); }
        // на всякий случай: никаких вратарей-ботов у этих ворот после серии
        world.getEntitiesByClass(GoalkeeperEntity.class, zone.expand(8), e -> true).forEach(Entity::discard);
        for (UUID id : tempMobs) { Entity e = world.getEntity(id); if (e != null) e.discard(); }
        for (UUID id : lineup.keySet()) {
            if (world.getEntity(id) instanceof FootballerEntity f && !tempMobs.contains(id)) f.setAiDisabled(false);
        }
    }

    // ---------------- кто бьёт и кто в воротах ----------------

    private Entity chooseKicker() {
        if (training()) {
            ServerPlayerEntity p = world.getServer().getPlayerManager().getPlayer(trainee);
            if (p != null) return p;
        }
        List<UUID> players = kicking == Team.REAL_MADRID ? homePlayers : awayPlayers;
        List<ServerPlayerEntity> online = new ArrayList<>();
        for (UUID id : players) {
            ServerPlayerEntity p = world.getServer().getPlayerManager().getPlayer(id);
            if (p != null && p.getWorld() == world && p.isAlive()) online.add(p);
        }
        int turn = kicking == Team.REAL_MADRID ? homeTurn++ : awayTurn++;
        if (!online.isEmpty()) return online.get(turn % online.size());
        List<Entity> mobs = new ArrayList<>();
        for (UUID id : lineup.keySet()) {
            if (world.getEntity(id) instanceof FootballerEntity f && f.isAlive() && f.team() == kicking && !tempMobs.contains(id)) mobs.add(f);
        }
        if (mobs.isEmpty()) {
            for (FootballerEntity f : world.getEntitiesByClass(FootballerEntity.class, zone.expand(40), e -> e.isAlive() && e.team() == kicking)) mobs.add(f);
            mobs.sort(java.util.Comparator.comparing(Entity::getUuid));
        }
        if (!mobs.isEmpty()) return mobs.get(turn % mobs.size());
        // В команде никого нет — выходит запасной
        FootballerEntity sub = kicking == Team.REAL_MADRID ? ModEntities.RONALDO.create(world) : ModEntities.MESSI.create(world);
        sub.refreshPositionAndAngles(spot.x, spot.y, spot.z, 0, 0);
        sub.setPersistent();
        world.spawnEntity(sub);
        tempMobs.add(sub.getUuid());
        return sub;
    }

    private Entity chooseKeeper() {
        if (!training() && FootballConfig.get().penaltyPlayersKeepGoal) {
            List<UUID> players = kicking == Team.REAL_MADRID ? awayPlayers : homePlayers;
            for (UUID id : players) {
                ServerPlayerEntity p = world.getServer().getPlayerManager().getPlayer(id);
                if (p != null && p.getWorld() == world && p.isAlive()) {
                    // вратарь-бот отходит в сторону, пока в воротах игрок
                    Entity bot = aiKeeperId == null ? null : world.getEntity(aiKeeperId);
                    if (bot != null) standAt(bot, line.add(out.multiply(1)).add(side.multiply(6)));
                    return p;
                }
            }
        }
        if (aiKeeperId != null && world.getEntity(aiKeeperId) instanceof GoalkeeperEntity gk && gk.isAlive()) return gk;
        GoalkeeperEntity gk = new GoalkeeperEntity(ModEntities.GOALKEEPER, world);
        Vec3d keep = line.add(out.multiply(0.5));
        gk.refreshPositionAndAngles(keep.x, keep.y, keep.z, 0, 0);
        gk.setInvulnerable(true);
        gk.setPersistent();
        world.spawnEntity(gk);
        aiKeeperId = gk.getUuid();
        return gk;
    }

    private void botKick(SoccerBallEntity ball) {
        Entity kicker = world.getEntity(kickerId);
        if (kicker == null) return;
        double lat = (world.random.nextDouble() * 2 - 1) * 2.3;
        Vec3d target = line.add(side.multiply(lat));
        Vec3d dir = new Vec3d(target.x - ball.getX(), 0, target.z - ball.getZ()).normalize();
        double power = 1.3 + world.random.nextDouble() * 0.3, lift = 0.06 + world.random.nextDouble() * 0.2;
        ball.launch(new Vec3d(dir.x * power, lift, dir.z * power), 0, kicker);
        if (kicker instanceof FootballerEntity f) f.swingHand(Hand.MAIN_HAND);
        world.playSound(null, ball.getX(), ball.getY(), ball.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.NEUTRAL, 1.0f, 1.0f);
    }

    // ---------------- для MatchManager ----------------

    @Nullable
    SoccerBallEntity ball() {
        return ballId != null && world.getEntity(ballId) instanceof SoccerBallEntity b && b.isAlive() ? b : null;
    }

    boolean isBall(Entity e) {
        return e.getUuid().equals(ballId);
    }

    boolean ballFrozen() {
        return stage != Stage.AIM && stage != Stage.FLIGHT;
    }

    /** Трогать мяч серии можно только бьющему (до удара) и вратарю. */
    boolean canTouch(Entity who) {
        if (who.getUuid().equals(keeperId)) return true;
        return stage == Stage.AIM && who.getUuid().equals(kickerId);
    }

    boolean isKeeperOrKicker(Entity e) {
        return e.getUuid().equals(keeperId) || e.getUuid().equals(kickerId);
    }

    private void place(SoccerBallEntity ball, Vec3d at) {
        ball.refreshPositionAndAngles(at.x, at.y, at.z, 0, 0);
        ball.setVelocity(Vec3d.ZERO);
        ball.velocityModified = true;
        world.getChunkManager().sendToOtherNearbyPlayers(ball, new EntityPositionS2CPacket(ball));
    }

    private static float yawTo(Vec3d from, Vec3d to) {
        return (float) (MathHelper.atan2(to.z - from.z, to.x - from.x) * 180 / Math.PI) - 90f;
    }

    private static int goals(List<Boolean> kicks) {
        int n = 0;
        for (boolean b : kicks) if (b) n++;
        return n;
    }

    private void whistle() {
        world.playSound(null, spot.x, spot.y + 1, spot.z, SoundEvents.BLOCK_NOTE_BLOCK_FLUTE.value(), SoundCategory.NEUTRAL, 3.0f, 2.0f);
    }

    Text scoreLine() {
        if (training()) return Text.translatable("msg.ronaldomessi.pen_training_line", goals(home), home.size());
        return Text.empty().append(Match.teamName(Team.REAL_MADRID)).append(" ").append(marks(home))
                .append("  " + goals(home) + " : " + goals(away) + "  ").append(marks(away)).append(" ").append(Match.teamName(Team.BARCELONA));
    }

    private static MutableText marks(List<Boolean> kicks) {
        MutableText t = Text.empty();
        for (boolean b : kicks) t.append(Text.literal(b ? "●" : "✖").formatted(b ? Formatting.GREEN : Formatting.RED));
        for (int i = kicks.size(); i < REGULATION; i++) t.append(Text.literal("○").formatted(Formatting.GRAY));
        return t;
    }

    List<ServerPlayerEntity> viewers() {
        Box view = zone.expand(24);
        List<ServerPlayerEntity> out = new ArrayList<>();
        for (ServerPlayerEntity p : world.getPlayers()) {
            if (view.contains(p.getPos()) || homePlayers.contains(p.getUuid()) || awayPlayers.contains(p.getUuid()) || p.getUuid().equals(trainee)) out.add(p);
        }
        return out;
    }

    void updateBar() {
        bar.setName(Text.translatable("msg.ronaldomessi.pen_bar").append("  ").append(scoreLine()));
        int kicks = home.size() + away.size();
        bar.setPercent(training() ? Math.min(1f, home.size() / 5f) : Math.min(1f, kicks / 10f));
        for (ServerPlayerEntity p : List.copyOf(bar.getPlayers())) if (!viewers().contains(p)) bar.removePlayer(p);
        for (ServerPlayerEntity p : viewers()) bar.addPlayer(p);
    }

    void broadcast(Text text, boolean overlay) {
        for (ServerPlayerEntity p : viewers()) {
            p.sendMessage(text, false);
            if (overlay) p.sendMessage(text, true);
        }
    }
}
