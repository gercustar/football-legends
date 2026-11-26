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
import ru.footballmod.item.*;
import ru.footballmod.item.SoccerBallItem;

public class ModItems {
    public static final Item SOCCER_BALL = register("soccer_ball", new SoccerBallItem(new Item.Settings().maxCount(16)));
    public static final Item GOLDEN_BALL = register("golden_ball", new GoldenBallItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC)));

    public static final Item GOAL = register("goal", new GoalItem(new Item.Settings().maxCount(16)));

    // Версии игроков (Этап 8): яйцо с NBT {Version:N}
    public static final Item RONALDO_2008_SPAWN_EGG = versionEgg("ronaldo_2008_spawn_egg", ModEntities.RONALDO, 1, 0xDA020E, 0xFFE500);
    public static final Item RONALDO_2017_SPAWN_EGG = versionEgg("ronaldo_2017_spawn_egg", ModEntities.RONALDO, 2, 0xF5F5F5, 0xC9A24A);
    public static final Item MESSI_2012_SPAWN_EGG = versionEgg("messi_2012_spawn_egg", ModEntities.MESSI, 1, 0x004D98, 0xA50044);

    public static final Item RONALDO_SPAWN_EGG = register("ronaldo_spawn_egg",
            new SpawnEggItem(ModEntities.RONALDO, 0xD4202A, 0x0B6B2E, new Item.Settings()));
    public static final Item MESSI_SPAWN_EGG = register("messi_spawn_egg",
            new SpawnEggItem(ModEntities.MESSI, 0x75AADB, 0xFFFFFF, new Item.Settings()));

    public static final Item ZIDANE_SPAWN_EGG = register("zidane_spawn_egg",
            new SpawnEggItem(ModEntities.ZIDANE, 0x1F2D5A, 0xFFFFFF, new Item.Settings()));
    public static final Item RONALDINHO_SPAWN_EGG = register("ronaldinho_spawn_egg",
            new SpawnEggItem(ModEntities.RONALDINHO, 0xFFDC02, 0x00923F, new Item.Settings()));
    public static final Item NEYMAR_SPAWN_EGG = register("neymar_spawn_egg",
            new SpawnEggItem(ModEntities.NEYMAR, 0xFFDC02, 0x1F3A93, new Item.Settings()));
    public static final Item MBAPPE_SPAWN_EGG = register("mbappe_spawn_egg",
            new SpawnEggItem(ModEntities.MBAPPE, 0x1F2D5A, 0xE21B2C, new Item.Settings()));
    public static final Item MODRIC_SPAWN_EGG = register("modric_spawn_egg",
            new SpawnEggItem(ModEntities.MODRIC, 0xE21B2C, 0xFFFFFF, new Item.Settings()));
    public static final Item RAMOS_SPAWN_EGG = register("ramos_spawn_egg",
            new SpawnEggItem(ModEntities.RAMOS, 0xC60B1E, 0x1F3A93, new Item.Settings()));
    // Клубные формы (Этап 8)
    public static final Item REAL_JERSEY = armor("real_jersey", ModArmorMaterials.REAL, ArmorItem.Type.CHESTPLATE);
    public static final Item REAL_SHORTS = armor("real_shorts", ModArmorMaterials.REAL, ArmorItem.Type.LEGGINGS);
    public static final Item REAL_BOOTS = armor("real_boots", ModArmorMaterials.REAL, ArmorItem.Type.BOOTS);
    public static final Item BARCA_JERSEY = armor("barca_jersey", ModArmorMaterials.BARCA, ArmorItem.Type.CHESTPLATE);
    public static final Item BARCA_SHORTS = armor("barca_shorts", ModArmorMaterials.BARCA, ArmorItem.Type.LEGGINGS);
    public static final Item BARCA_BOOTS = armor("barca_boots", ModArmorMaterials.BARCA, ArmorItem.Type.BOOTS);
    public static final Item UNITED_JERSEY = armor("united_jersey", ModArmorMaterials.UNITED, ArmorItem.Type.CHESTPLATE);
    public static final Item UNITED_SHORTS = armor("united_shorts", ModArmorMaterials.UNITED, ArmorItem.Type.LEGGINGS);
    public static final Item UNITED_BOOTS = armor("united_boots", ModArmorMaterials.UNITED, ArmorItem.Type.BOOTS);

    // Золотой свисток (Этап 3) и тренер (Этап 6)
    public static final Item GOLDEN_WHISTLE = register("golden_whistle", new GoldenWhistleItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC)));
    public static final Item COACH_SPAWN_EGG = register("coach_spawn_egg",
            new SpawnEggItem(ModEntities.COACH, 0x1C2A4A, 0xF4F4F4, new Item.Settings()));

    // Стадион (Этап 10)
    public static final Item STADIUM_KIT = register("stadium_kit", new StadiumItem(new Item.Settings().maxCount(1).rarity(Rarity.RARE)));

    public static final Item XAVI_SPAWN_EGG = register("xavi_spawn_egg",
            new SpawnEggItem(ModEntities.XAVI, 0xC60B1E, 0xF1BF00, new Item.Settings()));
    public static final Item INIESTA_SPAWN_EGG = register("iniesta_spawn_egg",
            new SpawnEggItem(ModEntities.INIESTA, 0xC60B1E, 0xFFFFFF, new Item.Settings()));
    public static final Item SUAREZ_SPAWN_EGG = register("suarez_spawn_egg",
            new SpawnEggItem(ModEntities.SUAREZ, 0x5CBFEB, 0x111111, new Item.Settings()));

    public static final Item RONALDO_JERSEY = armor("ronaldo_jersey", ModArmorMaterials.RONALDO, ArmorItem.Type.CHESTPLATE);
    public static final Item RONALDO_SHORTS = armor("ronaldo_shorts", ModArmorMaterials.RONALDO, ArmorItem.Type.LEGGINGS);
    public static final Item RONALDO_BOOTS = armor("ronaldo_boots", ModArmorMaterials.RONALDO, ArmorItem.Type.BOOTS);
    public static final Item MESSI_JERSEY = armor("messi_jersey", ModArmorMaterials.MESSI, ArmorItem.Type.CHESTPLATE);
    public static final Item MESSI_SHORTS = armor("messi_shorts", ModArmorMaterials.MESSI, ArmorItem.Type.LEGGINGS);
    public static final Item MESSI_BOOTS = armor("messi_boots", ModArmorMaterials.MESSI, ArmorItem.Type.BOOTS);

    // --- Экипировка (Этап 3) ---
    public static final Item MERCURIAL_BOOTS = boots("mercurial_boots", ModArmorMaterials.MERCURIAL, FootballBootsItem.BootType.SPEED);
    public static final Item F50_BOOTS = boots("f50_boots", ModArmorMaterials.F50, FootballBootsItem.BootType.DRIBBLE);
    public static final Item PREDATOR_BOOTS = boots("predator_boots", ModArmorMaterials.PREDATOR, FootballBootsItem.BootType.POWER);
    public static final Item CAPTAIN_ARMBAND = register("captain_armband", new CaptainArmbandItem(new Item.Settings().maxCount(1).rarity(Rarity.RARE)));
    public static final Item SHIN_GUARDS = register("shin_guards", new ShinGuardsItem(new Item.Settings().maxCount(16)));
    public static final Item ENERGY_DRINK = register("energy_drink", new EnergyDrinkItem(new Item.Settings().maxCount(16)));

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
                        e.add(ZIDANE_SPAWN_EGG);
                        e.add(RONALDINHO_SPAWN_EGG);
                        e.add(NEYMAR_SPAWN_EGG);
                        e.add(MBAPPE_SPAWN_EGG);
                        e.add(MODRIC_SPAWN_EGG);
                        e.add(RAMOS_SPAWN_EGG);
                        e.add(XAVI_SPAWN_EGG);
                        e.add(INIESTA_SPAWN_EGG);
                        e.add(SUAREZ_SPAWN_EGG);
                        e.add(RONALDO_2008_SPAWN_EGG);
                        e.add(RONALDO_2017_SPAWN_EGG);
                        e.add(MESSI_2012_SPAWN_EGG);
                        e.add(RONALDO_JERSEY);
                        e.add(RONALDO_SHORTS);
                        e.add(RONALDO_BOOTS);
                        e.add(MESSI_JERSEY);
                        e.add(MESSI_SHORTS);
                        e.add(MESSI_BOOTS);
                        e.add(REAL_JERSEY);
                        e.add(REAL_SHORTS);
                        e.add(REAL_BOOTS);
                        e.add(BARCA_JERSEY);
                        e.add(BARCA_SHORTS);
                        e.add(BARCA_BOOTS);
                        e.add(UNITED_JERSEY);
                        e.add(UNITED_SHORTS);
                        e.add(UNITED_BOOTS);
                        e.add(MERCURIAL_BOOTS);
                        e.add(F50_BOOTS);
                        e.add(PREDATOR_BOOTS);
                        e.add(CAPTAIN_ARMBAND);
                        e.add(SHIN_GUARDS);
                        e.add(ENERGY_DRINK);
                        e.add(ModBlocks.TRAINING_CONE);
                        e.add(STADIUM_KIT);
                        e.add(ModBlocks.MATCH_BOARD);
                        e.add(GOLDEN_WHISTLE);
                        e.add(COACH_SPAWN_EGG);
                    })
                    .build());

    private static Item armor(String name, net.minecraft.registry.entry.RegistryEntry<ArmorMaterial> mat, ArmorItem.Type type) {
        return register(name, new FootballArmorItem(mat, type,
                new Item.Settings().maxDamage(type.getMaxDamage(25))));
    }

    private static Item boots(String name, net.minecraft.registry.entry.RegistryEntry<ArmorMaterial> mat, FootballBootsItem.BootType t) {
        return register(name, new FootballBootsItem(mat, t,
                new Item.Settings().maxDamage(ArmorItem.Type.BOOTS.getMaxDamage(25))));
    }

    private static Item versionEgg(String name, net.minecraft.entity.EntityType<? extends net.minecraft.entity.mob.MobEntity> type,
                                   int version, int primary, int secondary) {
        net.minecraft.nbt.NbtCompound nbt = new net.minecraft.nbt.NbtCompound();
        nbt.putString("id", net.minecraft.registry.Registries.ENTITY_TYPE.getId(type).toString());
        nbt.putInt("Version", version);
        return register(name, new VersionSpawnEggItem(type, primary, secondary, new Item.Settings()
                .component(net.minecraft.component.DataComponentTypes.ENTITY_DATA, net.minecraft.component.type.NbtComponent.of(nbt))));
    }

    private static Item register(String name, Item item) {
        return Registry.register(Registries.ITEM, RonaldoMessiMod.id(name), item);
    }

    public static void init() {}
}
