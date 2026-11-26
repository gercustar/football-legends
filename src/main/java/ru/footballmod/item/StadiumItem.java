package ru.footballmod.item;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.StairsBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import ru.footballmod.ModBlocks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Комплект стадиона: поле 27×41 с разметкой, двое ворот, трибуны и прожекторы.
 * ПКМ по земле — показать границы, второй ПКМ на то же место в течение 10 секунд — построить.
 * Строит только поверх природных блоков: сундуки и постройки игрока не трогает (тогда отказывается).
 */
public class StadiumItem extends Item {
    public static final int HALF_WIDTH = 13;     // поле: боковые линии на ±13
    public static final int LENGTH = 41;         // линии ворот на 2 и 42 блоке вперёд
    private static final int START = 2;
    private static final int CONFIRM_TICKS = 200;

    private record Pending(BlockPos pos, Direction facing, long time) {}
    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    public StadiumItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext ctx) {
        if (!(ctx.getWorld() instanceof ServerWorld world) || ctx.getPlayer() == null) return ActionResult.SUCCESS;
        PlayerEntity player = ctx.getPlayer();
        BlockPos ground = ctx.getBlockPos();
        Direction facing = ctx.getHorizontalPlayerFacing();
        Pending p = PENDING.get(player.getUuid());
        if (p != null && p.pos.equals(ground) && p.facing == facing && world.getTime() - p.time <= CONFIRM_TICKS) {
            PENDING.remove(player.getUuid());
            int blocked = build(world, ground, facing, false);
            if (blocked > 0) {
                player.sendMessage(Text.translatable("msg.ronaldomessi.stadium_blocked", blocked).formatted(Formatting.RED), false);
                return ActionResult.FAIL;
            }
            build(world, ground, facing, true);
            world.playSound(null, ground, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.BLOCKS, 1.0f, 1.0f);
            player.sendMessage(Text.translatable("msg.ronaldomessi.stadium_built"), false);
            ctx.getStack().decrementUnlessCreative(1, player);
            return ActionResult.CONSUME;
        }
        PENDING.put(player.getUuid(), new Pending(ground, facing, world.getTime()));
        showOutline(world, ground, facing);
        player.sendMessage(Text.translatable("msg.ronaldomessi.stadium_confirm"), false);
        return ActionResult.CONSUME;
    }

    private static BlockPos at(BlockPos ground, Direction f, int lat, int along, int up) {
        return ground.offset(f, along).offset(f.rotateYClockwise(), lat).up(up);
    }

    private static void showOutline(ServerWorld w, BlockPos g, Direction f) {
        for (int along = -1; along <= LENGTH + 4; along += 2) {
            for (int lat : new int[]{-HALF_WIDTH - 6, HALF_WIDTH + 6}) {
                BlockPos p = at(g, f, lat, along, 1);
                w.spawnParticles(ParticleTypes.HAPPY_VILLAGER, p.getX() + 0.5, p.getY() + 0.2, p.getZ() + 0.5, 2, 0.2, 0.3, 0.2, 0);
            }
        }
        for (int lat = -HALF_WIDTH - 6; lat <= HALF_WIDTH + 6; lat += 2) {
            for (int along : new int[]{-1, LENGTH + 4}) {
                BlockPos p = at(g, f, lat, along, 1);
                w.spawnParticles(ParticleTypes.HAPPY_VILLAGER, p.getX() + 0.5, p.getY() + 0.2, p.getZ() + 0.5, 2, 0.2, 0.3, 0.2, 0);
            }
        }
    }

    /** Можно ли заменить этот блок: воздух, трава, земля, камень, листья, песок и т.п. Не трогаем сундуки и постройки. */
    private static boolean natural(ServerWorld w, BlockPos p) {
        BlockState s = w.getBlockState(p);
        if (s.hasBlockEntity()) return false;
        return s.isAir() || s.isReplaceable() || s.isIn(BlockTags.DIRT) || s.isIn(BlockTags.BASE_STONE_OVERWORLD)
                || s.isIn(BlockTags.LOGS) || s.isIn(BlockTags.LEAVES) || s.isIn(BlockTags.SAND) || s.isIn(BlockTags.FLOWERS)
                || s.isIn(BlockTags.SNOW) || s.isOf(Blocks.GRAVEL) || s.isOf(Blocks.CLAY) || s.isOf(Blocks.ICE)
                || s.isOf(Blocks.SANDSTONE) || s.isOf(Blocks.MOSS_BLOCK) || s.isOf(Blocks.SUGAR_CANE) || s.isOf(Blocks.CACTUS)
                || s.isOf(Blocks.PUMPKIN) || s.isOf(Blocks.MELON) || s.isOf(Blocks.BROWN_MUSHROOM_BLOCK) || s.isOf(Blocks.RED_MUSHROOM_BLOCK)
                || s.isOf(ModBlocks.GOAL_POST) || s.isOf(ModBlocks.GOAL_NET) || s.isOf(ModBlocks.TRAINING_CONE)
                || s.isOf(Blocks.WHITE_CONCRETE) || s.isOf(Blocks.IRON_BARS) || s.isOf(Blocks.SEA_LANTERN)
                || s.isOf(Blocks.STONE_BRICKS) || s.isOf(Blocks.RED_NETHER_BRICK_STAIRS) || s.isOf(Blocks.PRISMARINE_BRICK_STAIRS)
                || s.isOf(Blocks.RED_BANNER);
    }

    /** Ставит блок (или, если write=false, только считает мешающие). Возвращает 1, если место занято постройкой. */
    private static int put(ServerWorld w, BlockPos p, BlockState s, boolean write) {
        if (!natural(w, p)) return 1;
        if (write) w.setBlockState(p, s, 2);
        return 0;
    }

    /** Строит стадион (write=true) или проверяет место (write=false). Возвращает число мешающих блоков. */
    public static int build(ServerWorld w, BlockPos g, Direction f, boolean write) {
        int blocked = 0;
        BlockState air = Blocks.AIR.getDefaultState();
        // 1. Расчистка над всей площадкой
        for (int lat = -HALF_WIDTH - 6; lat <= HALF_WIDTH + 6; lat++)
            for (int along = -1; along <= LENGTH + 4; along++)
                for (int up = 1; up <= 8; up++) {
                    BlockPos p = at(g, f, lat, along, up);
                    if (!w.getBlockState(p).isAir()) blocked += put(w, p, air, write);
                }
        // 2. Газон полосами и разметка
        int end = START + LENGTH - 1;
        int mid = (START + end) / 2;
        for (int lat = -HALF_WIDTH - 2; lat <= HALF_WIDTH + 2; lat++)
            for (int along = START - 3; along <= end + 3; along++) {
                BlockPos p = at(g, f, lat, along, 0);
                BlockState s = ((along - START) / 4) % 2 == 0 ? Blocks.GRASS_BLOCK.getDefaultState() : Blocks.MOSS_BLOCK.getDefaultState();
                boolean inside = Math.abs(lat) <= HALF_WIDTH && along >= START && along <= end;
                if (inside && line(lat, along, START, end, mid)) s = Blocks.WHITE_CONCRETE.getDefaultState();
                blocked += put(w, p, s, write);
                BlockPos below = p.down();
                if (write && w.getBlockState(below).isReplaceable()) w.setBlockState(below, Blocks.DIRT.getDefaultState(), 2);
            }
        if (!write) return blocked;
        // 3. Ворота: сетка за линией ворот
        GoalItem.placeGoal(w, at(g, f, 0, end, 1), f);
        GoalItem.placeGoal(w, at(g, f, 0, START, 1), f.getOpposite());
        // 4. Трибуны вдоль боковых линий (4 ряда), сиденья смотрят на поле
        for (int side : new int[]{-1, 1}) {
            Direction away = side > 0 ? f.rotateYClockwise() : f.rotateYCounterclockwise();
            BlockState seat = (side < 0 ? Blocks.RED_NETHER_BRICK_STAIRS : Blocks.PRISMARINE_BRICK_STAIRS)
                    .getDefaultState().with(StairsBlock.FACING, away);
            for (int row = 0; row < 4; row++) {
                int lat = side * (HALF_WIDTH + 3 + row);
                for (int along = START + 2; along <= end - 2; along++) {
                    for (int up = 1; up <= row; up++) w.setBlockState(at(g, f, lat, along, up), Blocks.STONE_BRICKS.getDefaultState(), 2);
                    w.setBlockState(at(g, f, lat, along, row + 1), seat, 2);
                }
            }
        }
        // 5. Прожекторы по углам и угловые флажки
        for (int lat : new int[]{-HALF_WIDTH - 6, HALF_WIDTH + 6})
            for (int along : new int[]{-1, LENGTH + 4}) {
                for (int up = 1; up <= 9; up++) {
                    BlockPos p = at(g, f, lat, along, up);
                    if (natural(w, p)) w.setBlockState(p, Blocks.IRON_BARS.getDefaultState(), 3);
                }
                BlockPos top = at(g, f, lat, along, 10);
                if (natural(w, top)) w.setBlockState(top, Blocks.SEA_LANTERN.getDefaultState(), 3);
            }
        for (int lat : new int[]{-HALF_WIDTH, HALF_WIDTH})
            for (int along : new int[]{START, end})
                w.setBlockState(at(g, f, lat, along, 1), Blocks.RED_BANNER.getDefaultState(), 3);
        return 0;
    }

    /** Разметка: боковые линии, линии ворот, центр, круг, штрафные и вратарские, точки. */
    static boolean line(int lat, int along, int start, int end, int mid) {
        int a = Math.abs(lat);
        if (a == HALF_WIDTH || along == start || along == end || along == mid) return true;
        double r = Math.sqrt(lat * lat + (along - mid) * (along - mid));
        if (Math.abs(r - 5) < 0.5 || (lat == 0 && along == mid)) return true;
        for (int g : new int[]{start, end}) {
            int d = Math.abs(along - g);
            if ((d == 7 && a <= 8) || (d < 7 && a == 8)) return true;      // штрафная
            if ((d == 3 && a <= 5) || (d < 3 && a == 5)) return true;      // вратарская
            if (d == 9 && lat == 0) return true;                          // точка пенальти
        }
        return false;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("tooltip.ronaldomessi.stadium_kit").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.ronaldomessi.stadium_kit2").formatted(Formatting.DARK_GRAY));
    }
}
