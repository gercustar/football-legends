package ru.footballmod.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;

/** Сетка ворот. Если мяч касается сетки со scoring=true (задняя стенка) — это гол. */
public class GoalNetBlock extends Block {
    public static final BooleanProperty SCORING = BooleanProperty.of("scoring");

    public GoalNetBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(SCORING, true));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(SCORING);
    }
}
