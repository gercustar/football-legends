package ru.footballmod;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Rarity;
import ru.footballmod.item.FootballArmorItem;
import ru.footballmod.item.GoldenBallItem;
import ru.footballmod.item.GoalItem;
import ru.footballmod.item.SoccerBallItem;

public class ModItems {
    public static final Item SOCCER_BALL = register("soccer_ball", new SoccerBallItem(new Item.Settings().maxCount(16)));
    public static final Item GOLDEN_BALL = register("golden_ball", new GoldenBallItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC)));

    public static final Item GOAL = register("goal", new GoalItem(new Item.Settings().maxCount(16)));

    public static final Item RONALDO_SPAWN_EGG = register("ronaldo_spawn_egg",
            new SpawnEggItem(ModEntities.RONALDO, 0xD4202A, 0x0B6B2E, new Item.Settings()));
    public static final Item MESSI_SPAWN_EGG = register("messi_spawn_egg",
            new SpawnEggItem(ModEntities.MESSI, 0x75AADB, 0xFFFFFF, new Item.Settings()));

    public static final Item RONALDO_JERSEY = armor("ronaldo_jersey", ModArmorMaterials.RONALDO, ArmorItem.Type.CHESTPLATE);
    public static final Item RONALDO_SHORTS = armor("ronaldo_shorts", ModArmorMaterials.RONALDO, ArmorItem.Type.LEGGINGS);
    public static final Item RONALDO_BOOTS = armor("ronaldo_boots", ModArmorMaterials.RONALDO, ArmorItem.Type.BOOTS);
    public static final Item MESSI_JERSEY = armor("messi_jersey", ModArmorMaterials.MESSI, ArmorItem.Type.CHESTPLATE);
    public static final Item MESSI_SHORTS = armor("messi_shorts", ModArmorMaterials.MESSI, ArmorItem.Type.LEGGINGS);
    public static final Item MESSI_BOOTS = armor("messi_boots", ModArmorMaterials.MESSI, ArmorItem.Type.BOOTS);

    public static final ItemGroup GROUP = Registry.register(Registries.ITEM_GROUP, RonaldoMessiMod.id("main"),
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(SOCCER_BALL))
                    .displayName(Text.translatable("itemGroup.ronaldomessi.main"))
                    .entries((ctx, e) -> {
                        e.add(SOCCER_BALL);
                        e.add(GOLDEN_BALL);
                        e.add(GOAL);
                        e.add(ModBlocks.GOAL_POST);
                        e.add(ModBlocks.GOAL_NET);
                        e.add(RONALDO_SPAWN_EGG);
                        e.add(MESSI_SPAWN_EGG);
                        e.add(RONALDO_JERSEY);
                        e.add(RONALDO_SHORTS);
                        e.add(RONALDO_BOOTS);
                        e.add(MESSI_JERSEY);
                        e.add(MESSI_SHORTS);
                        e.add(MESSI_BOOTS);
                    })
                    .build());

    private static Item armor(String name, net.minecraft.registry.entry.RegistryEntry<ArmorMaterial> mat, ArmorItem.Type type) {
        return register(name, new FootballArmorItem(mat, type,
                new Item.Settings().maxDamage(type.getMaxDamage(25))));
    }

    private static Item register(String name, Item item) {
        return Registry.register(Registries.ITEM, RonaldoMessiMod.id(name), item);
    }

    public static void init() {}
}
