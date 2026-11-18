package ru.footballmod;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import ru.footballmod.block.GoalNetBlock;

public class ModBlocks {
    public static final Block GOAL_POST = register("goal_post", new Block(AbstractBlock.Settings.create()
            .mapColor(MapColor.WHITE).strength(1.0f).sounds(BlockSoundGroup.METAL)));

    public static final Block GOAL_NET = register("goal_net", new GoalNetBlock(AbstractBlock.Settings.create()
            .mapColor(MapColor.WHITE).strength(0.4f).sounds(BlockSoundGroup.WOOL).nonOpaque()
            .allowsSpawning((s, w, p, t) -> false).solidBlock((s, w, p) -> false)
            .suffocates((s, w, p) -> false).blockVision((s, w, p) -> false)));

    public static final Block TRAINING_CONE = register("training_cone", new ru.footballmod.block.TrainingConeBlock(
            AbstractBlock.Settings.create().mapColor(MapColor.ORANGE).strength(0.2f).sounds(BlockSoundGroup.WOOL)
                    .nonOpaque().noCollision()));

    private static Block register(String name, Block block) {
        Registry.register(Registries.BLOCK, RonaldoMessiMod.id(name), block);
        Registry.register(Registries.ITEM, RonaldoMessiMod.id(name), new BlockItem(block, new Item.Settings()));
        return block;
    }

    public static void init() {}
}
