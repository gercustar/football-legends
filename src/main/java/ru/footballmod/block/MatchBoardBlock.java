package ru.footballmod.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import ru.footballmod.match.MatchManager;

/** Табло: ПКМ открывает меню матча (выбор команды, старт, стоп). Ворота ищет само в 48 блоках. */
public class MatchBoardBlock extends Block {
    public MatchBoardBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (player instanceof ServerPlayerEntity sp) MatchManager.openBoard(sp, pos);
        return ActionResult.success(world.isClient);
    }
}
