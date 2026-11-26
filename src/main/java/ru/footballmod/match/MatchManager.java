package ru.footballmod.match;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import ru.footballmod.ModBlocks;
import ru.footballmod.block.GoalNetBlock;
import ru.footballmod.config.FootballConfig;
import ru.footballmod.entity.FootballerEntity.Team;
import ru.footballmod.entity.RefereeEntity;
import ru.footballmod.entity.SoccerBallEntity;
import ru.footballmod.network.BoardActionPayload;
import ru.footballmod.network.BoardStatePayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Все матчи на сервере: табло, команды до начала матча, события (голы, фолы). */
public final class MatchManager {
    public static final String FAN_TAG = "ronaldomessi_fan";
    private static final List<Match> MATCHES = new ArrayList<>();
    private static final List<PenaltyShootout> SHOOTOUTS = new ArrayList<>();
    /** Состав команд до начала матча: табло → (игрок → команда). */
    private static final Map<BlockPos, Map<UUID, Team>> LOBBY = new HashMap<>();

    private MatchManager() {}

    public static void init() {
        PayloadTypeRegistry.playC2S().register(BoardActionPayload.ID, BoardActionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(BoardStatePayload.ID, BoardStatePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(BoardActionPayload.ID, (payload, ctx) -> handleAction(ctx.player(), payload));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            List<Match> ended = new ArrayList<>();
            MATCHES.removeIf(m -> { if (m.tick()) return false; ended.add(m); return true; });
            // Ничья — серия пенальти теми же командами
            for (Match m : ended) {
                if (m.penaltiesAfter) {
                    PenaltyShootout s = new PenaltyShootout(m.world, m.board, m.goalA, m.goalB.subtract(m.goalA),
                            new ArrayList<>(m.home), new ArrayList<>(m.away), null, null);
                    SHOOTOUTS.add(s);
                    s.start();
                }
            }
            SHOOTOUTS.removeIf(s -> !s.tick());
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(MatchManager::allowDamage);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> { MATCHES.clear(); SHOOTOUTS.clear(); LOBBY.clear(); });
    }

    @Nullable
    public static Match matchFor(Entity e) {
        if (MATCHES.isEmpty() || e.getWorld().isClient) return null;
        for (Match m : MATCHES) {
            if (m.world == e.getWorld() && (m.zone.contains(e.getPos()) || m.home.contains(e.getUuid()) || m.away.contains(e.getUuid()))) return m;
        }
        return null;
    }

    /** Матч, в котором игрок участвует или рядом с которым стоит (для Золотого свистка). */
    @Nullable
    public static Match matchNear(Entity e) {
        Match m = matchFor(e);
        if (m != null) return m;
        for (Match x : MATCHES) if (x.world == e.getWorld() && x.zone.expand(16).contains(e.getPos())) return x;
        return null;
    }

    @Nullable
    public static Match matchAt(ServerWorld w, BlockPos board) {
        for (Match m : MATCHES) if (m.world == w && m.board.equals(board)) return m;
        return null;
    }

    public static List<Match> matches() {
        return List.copyOf(MATCHES);
    }

    @Nullable
    static PenaltyShootout shootoutFor(Entity e) {
        if (SHOOTOUTS.isEmpty() || e.getWorld().isClient) return null;
        for (PenaltyShootout s : SHOOTOUTS) if (s.world == e.getWorld() && s.zone.contains(e.getPos())) return s;
        return null;
    }

    /** Рядом идёт серия пенальти (вратарь-бот живёт только пока она идёт). */
    public static boolean shootoutNear(Entity e) {
        return shootoutFor(e) != null;
    }

    /** Может ли моб сам бегать за мячом (на серии пенальти — нет). */
    public static boolean mayChaseBall(Entity mob) {
        if (SHOOTOUTS.isEmpty() || mob.getWorld().isClient) return true;
        for (PenaltyShootout s : SHOOTOUTS) if (s.world == mob.getWorld() && s.zone.expand(24).contains(mob.getPos())) return false;
        return true;
    }

    /** Для проверок: состояние серии одной строкой. */
    public static String debugShootout(PenaltyShootout s) {
        var b = s.ball();
        return s.stage + " kicks=" + s.home.size() + " last=" + s.lastResultKey
                + (b == null ? "" : String.format(" ball=(%.1f %.1f %.1f) v=%.2f along=%.1f", b.getX(), b.getY(), b.getZ(), b.getVelocity().length(),
                (b.getX() - s.spot.x) * -s.out.x + (b.getZ() - s.spot.z) * -s.out.z));
    }

    public static List<PenaltyShootout> shootouts() {
        return List.copyOf(SHOOTOUTS);
    }

    /** Тренировка пенальти (академия): игрок бьёт 5 раз во вратаря-бота. */
    public static PenaltyShootout startTraining(ServerWorld w, ServerPlayerEntity player, Vec3d netCenter, Vec3d toField, java.util.function.IntConsumer done) {
        PenaltyShootout s = new PenaltyShootout(w, null, netCenter, toField, List.of(player.getUuid()), List.of(), player.getUuid(), done);
        SHOOTOUTS.add(s);
        s.start();
        return s;
    }

    public static void stopShootout(PenaltyShootout s) {
        s.cleanup();
        SHOOTOUTS.remove(s);
    }

    /** Мяч заморожен (до начального удара, после гола, в перерыве, между пенальти). */
    public static boolean isFrozen(SoccerBallEntity ball) {
        PenaltyShootout s = shootoutFor(ball);
        if (s != null && s.isBall(ball)) return s.ballFrozen();
        Match m = matchFor(ball);
        return m != null && m.zone.contains(ball.getPos()) && m.ballFrozen();
    }

    /** Может ли этот игрок/моб касаться мяча (не удалён и мяч не заморожен). */
    public static boolean canTouch(SoccerBallEntity ball, Entity who) {
        PenaltyShootout s = shootoutFor(ball);
        if (s != null && s.isBall(ball)) return !s.ballFrozen() && s.canTouch(who);
        if (isFrozen(ball)) return false;
        Match m = matchFor(who);
        return m == null || !m.isSentOff(who);
    }

    /** Гол в сетку у net: true — посчитан матчем (обычное сообщение о голе не нужно). */
    public static boolean onGoal(SoccerBallEntity ball, BlockPos net) {
        for (PenaltyShootout s : SHOOTOUTS) if (s.world == ball.getWorld() && s.onGoal(ball, net)) return true;
        Match m = matchFor(ball);
        return m != null && m.onGoal(ball, net);
    }

    /** Центр ворот, которые атакует моб в матче, или null вне матча. */
    @Nullable
    public static Vec3d attackGoalFor(Entity e, Team team) {
        Match m = matchFor(e);
        return m == null || !m.zone.contains(e.getPos()) ? null : m.attackGoal(team);
    }

    /** Ворота, которые защищает моб, если он вратарь в матче; иначе null. */
    @Nullable
    public static Vec3d keeperGoal(ru.footballmod.entity.FootballerEntity e) {
        Match m = matchFor(e);
        return m != null && m.isKeeper(e) ? m.defendGoal(e.team()) : null;
    }

    /** На поле во время матча футболисты не дерутся. */
    public static boolean noFighting(Entity e) {
        if (!SHOOTOUTS.isEmpty() && !e.getWorld().isClient) {
            for (PenaltyShootout s : SHOOTOUTS) if (s.world == e.getWorld() && s.zone.expand(24).contains(e.getPos())) return true;
        }
        Match m = matchFor(e);
        return m != null && m.zone.contains(e.getPos()) && m.phase != Match.Phase.FINISHED;
    }

    private static boolean allowDamage(LivingEntity victim, DamageSource source, float amount) {
        Entity attacker = source.getAttacker();
        if (attacker == null || attacker == victim) return true;
        Match m = matchFor(victim);
        if (shootoutFor(victim) != null && attacker instanceof net.minecraft.entity.player.PlayerEntity) return false;   // на серии пенальти не дерёмся
        if (m == null || !m.zone.contains(victim.getPos())) return true;
        if (victim instanceof RefereeEntity) return false;
        Team a = m.teamOf(attacker), v = m.teamOf(victim);
        if (a == null || v == null) return true;           // не участники — обычные правила
        if (a != v) m.foul(attacker);                       // удар сопернику — фол
        return false;                                      // в матче не дерёмся
    }

    // ---------------- табло ----------------

    public static void openBoard(ServerPlayerEntity player, BlockPos board) {
        ServerWorld w = player.getServerWorld();
        Match m = matchAt(w, board);
        Map<UUID, Team> lobby = LOBBY.getOrDefault(board, Map.of());
        int home, away;
        Team mine;
        String status;
        if (m != null) {
            home = m.home.size();
            away = m.away.size();
            mine = m.teamOf(player);
            status = m.scoreText().getString();
        } else {
            home = (int) lobby.values().stream().filter(t -> t == Team.REAL_MADRID).count();
            away = lobby.size() - home;
            mine = lobby.get(player.getUuid());   // команда, выбранная раньше, сохраняется
            status = "";
        }
        boolean shootout = SHOOTOUTS.stream().anyMatch(s -> board.equals(s.board));
        if (m == null && shootout) status = SHOOTOUTS.stream().filter(s -> board.equals(s.board)).findFirst().get().scoreLine().getString();
        ServerPlayNetworking.send(player, new BoardStatePayload(board, home, away, mine == null ? 0 : mine == Team.REAL_MADRID ? 1 : 2, m != null || shootout, status));
    }

    static void handleAction(ServerPlayerEntity player, BoardActionPayload p) {
        ServerWorld w = player.getServerWorld();
        BlockPos board = p.pos();
        if (player.squaredDistanceTo(Vec3d.ofCenter(board)) > 64 || !w.getBlockState(board).isOf(ModBlocks.MATCH_BOARD)) return;
        Match m = matchAt(w, board);
        UUID id = player.getUuid();
        switch (p.action()) {
            case BoardActionPayload.JOIN_HOME, BoardActionPayload.JOIN_AWAY, BoardActionPayload.SPECTATE -> {
                Team t = p.action() == BoardActionPayload.JOIN_HOME ? Team.REAL_MADRID : p.action() == BoardActionPayload.JOIN_AWAY ? Team.BARCELONA : null;
                if (m != null) {
                    m.home.remove(id);
                    m.away.remove(id);
                    if (t == Team.REAL_MADRID) m.home.add(id);
                    if (t == Team.BARCELONA) m.away.add(id);
                }
                // Выбор команды запоминается у табло и для следующих матчей и серий пенальти
                Map<UUID, Team> lobby = LOBBY.computeIfAbsent(board.toImmutable(), b -> new HashMap<>());
                if (t == null) lobby.remove(id); else lobby.put(id, t);
                player.sendMessage(t == null ? Text.translatable("msg.ronaldomessi.joined_spectators")
                        : Text.translatable("msg.ronaldomessi.joined_team", Match.teamName(t)), false);
            }
            case BoardActionPayload.START_3, BoardActionPayload.START_5, BoardActionPayload.START_10 -> {
                if (m != null) {
                    player.sendMessage(Text.translatable("msg.ronaldomessi.match_already").formatted(Formatting.RED), false);
                } else {
                    int minutes = p.action() == BoardActionPayload.START_3 ? 3 : p.action() == BoardActionPayload.START_10 ? 10 : FootballConfig.get().matchMinutes;
                    String error = start(w, board.toImmutable(), minutes);
                    if (error != null) player.sendMessage(Text.translatable(error).formatted(Formatting.RED), false);
                }
            }
            case BoardActionPayload.PENALTIES -> {
                if (m != null) {
                    player.sendMessage(Text.translatable("msg.ronaldomessi.match_already").formatted(Formatting.RED), false);
                } else {
                    String error = startShootout(w, board.toImmutable());
                    if (error != null) player.sendMessage(Text.translatable(error).formatted(Formatting.RED), false);
                }
            }
            case BoardActionPayload.STOP -> {
                for (PenaltyShootout s : List.copyOf(SHOOTOUTS)) {
                    if (board.equals(s.board)) {
                        s.broadcast(Text.translatable("msg.ronaldomessi.match_stopped", player.getDisplayName()), true);
                        stopShootout(s);
                    }
                }
                if (m != null) {
                    m.broadcast(Text.translatable("msg.ronaldomessi.match_stopped", player.getDisplayName()), true);
                    m.cleanup();
                    MATCHES.remove(m);
                }
            }
            default -> { return; }
        }
        openBoard(player, board);
    }

    /** Начинает матч у табло. Возвращает ключ ошибки или null. */
    @Nullable
    public static String start(ServerWorld w, BlockPos board, int minutes) {
        if (matchAt(w, board) != null) return "msg.ronaldomessi.match_already";
        Vec3d[] goals = findGoals(w, board);
        if (goals == null) return "msg.ronaldomessi.match_no_goals";
        for (Match other : MATCHES) {
            if (other.world == w && (other.zone.contains(goals[0]) || other.zone.contains(goals[1]))) return "msg.ronaldomessi.match_already";
        }
        Set<UUID> home = new HashSet<>(), away = new HashSet<>();
        LOBBY.getOrDefault(board, Map.of()).forEach((id, t) -> (t == Team.REAL_MADRID ? home : away).add(id));
        Match m = new Match(w, board, goals[0], goals[1], minutes, home, away);
        MATCHES.add(m);
        m.start();
        return null;
    }

    /** Серия пенальти у табло: в ближние к табло ворота, команды — из меню табло. */
    @Nullable
    public static String startShootout(ServerWorld w, BlockPos board) {
        for (PenaltyShootout s : SHOOTOUTS) if (board.equals(s.board)) return "msg.ronaldomessi.match_already";
        Vec3d[] goals = findGoals(w, board);
        if (goals == null) return "msg.ronaldomessi.match_no_goals";
        List<UUID> home = new ArrayList<>(), away = new ArrayList<>();
        LOBBY.getOrDefault(board, Map.of()).forEach((id, t) -> (t == Team.REAL_MADRID ? home : away).add(id));
        PenaltyShootout s = new PenaltyShootout(w, board, goals[0], goals[1].subtract(goals[0]), home, away, null, null);
        SHOOTOUTS.add(s);
        s.start();
        return null;
    }

    /** Двое ворот рядом с табло: центры задних сеток (сначала ближние к табло). */
    @Nullable
    static Vec3d[] findGoals(ServerWorld w, BlockPos board) {
        int r = FootballConfig.get().matchSearchRadius;
        List<BlockPos> nets = new ArrayList<>();
        for (BlockPos p : BlockPos.iterate(board.add(-r, -8, -r), board.add(r, 8, r))) {
            BlockState s = w.getBlockState(p);
            if (s.isOf(ModBlocks.GOAL_NET) && s.get(GoalNetBlock.SCORING)) nets.add(p.toImmutable());
        }
        if (nets.size() < 2) return null;
        BlockPos a = nets.get(0), b = a;
        for (BlockPos p : nets) if (p.getSquaredDistance(a) > b.getSquaredDistance(a)) b = p;
        if (a.getSquaredDistance(b) < 15 * 15) return null;
        Vec3d sumA = Vec3d.ZERO, sumB = Vec3d.ZERO;
        int na = 0, nb = 0;
        for (BlockPos p : nets) {
            if (p.getSquaredDistance(a) <= p.getSquaredDistance(b)) { sumA = sumA.add(Vec3d.ofCenter(p)); na++; }
            else { sumB = sumB.add(Vec3d.ofCenter(p)); nb++; }
        }
        Vec3d ga = sumA.multiply(1.0 / na), gb = sumB.multiply(1.0 / nb);
        Vec3d bc = Vec3d.ofCenter(board);
        return ga.squaredDistanceTo(bc) <= gb.squaredDistanceTo(bc) ? new Vec3d[]{ga, gb} : new Vec3d[]{gb, ga};
    }
}
