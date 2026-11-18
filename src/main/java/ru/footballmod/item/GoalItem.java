package ru.footballmod.item;

import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import ru.footballmod.ModBlocks;
import ru.footballmod.block.GoalNetBlock;

/** Ставит готовые ворота 7×4 перед игроком, открытой стороной к нему. */
public class GoalItem extends Item {
    public GoalItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext ctx) {
        World world = ctx.getWorld();
        if (world.isClient) return ActionResult.SUCCESS;

        Direction forward = ctx.getHorizontalPlayerFacing();
        Direction side = forward.rotateYClockwise();
        BlockPos origin = ctx.getBlockPos().up().offset(forward, 2);

        BlockState post = ModBlocks.GOAL_POST.getDefaultState();
        BlockState backNet = ModBlocks.GOAL_NET.getDefaultState().with(GoalNetBlock.SCORING, true);
        BlockState sideNet = ModBlocks.GOAL_NET.getDefaultState().with(GoalNetBlock.SCORING, false);

        for (int lat = -3; lat <= 3; lat++) {
            for (int y = 0; y <= 3; y++) {
                for (int depth = 0; depth <= 2; depth++) {
                    boolean edge = Math.abs(lat) == 3;
                    BlockState state = null;
                    if (depth == 0) {
                        if (edge || y == 3) state = post;           // штанги и перекладина
                    } else if (depth == 2) {
                        state = backNet;                            // задняя сетка — тут засчитывается гол
                    } else if (edge || y == 3) {
                        state = sideNet;                            // боковые и верхняя сетка
                    }
                    if (state == null) continue;
                    BlockPos p = origin.offset(side, lat).up(y).offset(forward, depth);
                    if (world.getBlockState(p).isReplaceable()) world.setBlockState(p, state);
                }
            }
        }
        world.playSound(null, origin, BlockSoundGroup.METAL.getPlaceSound(), SoundCategory.BLOCKS, 1.0f, 1.0f);
        if (ctx.getPlayer() != null) ctx.getStack().decrementUnlessCreative(1, ctx.getPlayer());
        return ActionResult.CONSUME;
    }
}
