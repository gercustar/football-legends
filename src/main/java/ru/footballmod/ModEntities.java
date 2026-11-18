package ru.footballmod;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnLocationTypes;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.BiomeKeys;
import ru.footballmod.entity.MessiEntity;
import ru.footballmod.entity.RonaldoEntity;
import ru.footballmod.entity.SoccerBallEntity;
import ru.footballmod.entity.*;

public class ModEntities {
    public static final EntityType<RonaldoEntity> RONALDO = Registry.register(Registries.ENTITY_TYPE,
            RonaldoMessiMod.id("ronaldo"),
            EntityType.Builder.create(RonaldoEntity::new, SpawnGroup.CREATURE)
                    .dimensions(0.6f, 1.95f).eyeHeight(1.62f).maxTrackingRange(10).build("ronaldo"));

    public static final EntityType<MessiEntity> MESSI = Registry.register(Registries.ENTITY_TYPE,
            RonaldoMessiMod.id("messi"),
            EntityType.Builder.create(MessiEntity::new, SpawnGroup.CREATURE)
                    .dimensions(0.6f, 1.8f).eyeHeight(1.5f).maxTrackingRange(10).build("messi"));

    public static final EntityType<SoccerBallEntity> SOCCER_BALL = Registry.register(Registries.ENTITY_TYPE,
            RonaldoMessiMod.id("soccer_ball"),
            EntityType.Builder.<SoccerBallEntity>create(SoccerBallEntity::new, SpawnGroup.MISC)
                    .dimensions(0.5f, 0.5f).maxTrackingRange(8).trackingTickInterval(1).build("soccer_ball"));


    public static final EntityType<ZidaneEntity> ZIDANE = Registry.register(Registries.ENTITY_TYPE,
            RonaldoMessiMod.id("zidane"),
            EntityType.Builder.create(ZidaneEntity::new, SpawnGroup.CREATURE)
                    .dimensions(0.6f, 1.85f).eyeHeight(1.62f).maxTrackingRange(10).build("zidane"));

    public static final EntityType<RonaldinhoEntity> RONALDINHO = Registry.register(Registries.ENTITY_TYPE,
            RonaldoMessiMod.id("ronaldinho"),
            EntityType.Builder.create(RonaldinhoEntity::new, SpawnGroup.CREATURE)
                    .dimensions(0.6f, 1.85f).eyeHeight(1.62f).maxTrackingRange(10).build("ronaldinho"));

    public static final EntityType<NeymarEntity> NEYMAR = Registry.register(Registries.ENTITY_TYPE,
            RonaldoMessiMod.id("neymar"),
            EntityType.Builder.create(NeymarEntity::new, SpawnGroup.CREATURE)
                    .dimensions(0.6f, 1.8f).eyeHeight(1.55f).maxTrackingRange(10).build("neymar"));

    public static final EntityType<MbappeEntity> MBAPPE = Registry.register(Registries.ENTITY_TYPE,
            RonaldoMessiMod.id("mbappe"),
            EntityType.Builder.create(MbappeEntity::new, SpawnGroup.CREATURE)
                    .dimensions(0.6f, 1.85f).eyeHeight(1.62f).maxTrackingRange(10).build("mbappe"));

    public static final EntityType<ModricEntity> MODRIC = Registry.register(Registries.ENTITY_TYPE,
            RonaldoMessiMod.id("modric"),
            EntityType.Builder.create(ModricEntity::new, SpawnGroup.CREATURE)
                    .dimensions(0.6f, 1.75f).eyeHeight(1.5f).maxTrackingRange(10).build("modric"));

    public static final EntityType<RamosEntity> RAMOS = Registry.register(Registries.ENTITY_TYPE,
            RonaldoMessiMod.id("ramos"),
            EntityType.Builder.create(RamosEntity::new, SpawnGroup.CREATURE)
                    .dimensions(0.6f, 1.9f).eyeHeight(1.62f).maxTrackingRange(10).build("ramos"));

    public static void init() {
        FabricDefaultAttributeRegistry.register(RONALDO, FootballerEntity.createFootballerAttributes());
        FabricDefaultAttributeRegistry.register(MESSI, FootballerEntity.createFootballerAttributes());

        SpawnRestriction.register(RONALDO, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, MobEntity::canMobSpawn);
        SpawnRestriction.register(MESSI, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, MobEntity::canMobSpawn);

        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(BiomeKeys.PLAINS, BiomeKeys.SUNFLOWER_PLAINS,
                BiomeKeys.MEADOW, BiomeKeys.SAVANNA), SpawnGroup.CREATURE, RONALDO, 4, 1, 1);
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(BiomeKeys.PLAINS, BiomeKeys.SUNFLOWER_PLAINS,
                BiomeKeys.MEADOW, BiomeKeys.SAVANNA), SpawnGroup.CREATURE, MESSI, 4, 1, 1);

        legend(ZIDANE, FootballerEntity.attributes(60.0, 0.29, 6.0, 2.0));
        legend(RONALDINHO, FootballerEntity.attributes(55.0, 0.31, 4.0, 0.0));
        legend(NEYMAR, FootballerEntity.attributes(50.0, 0.32, 4.0, 0.0));
        legend(MBAPPE, FootballerEntity.attributes(55.0, 0.36, 5.0, 0.0));
        legend(MODRIC, FootballerEntity.attributes(55.0, 0.30, 4.0, 0.0));
        legend(RAMOS, FootballerEntity.attributes(80.0, 0.28, 7.0, 6.0));
    }

    private static <T extends FootballerEntity> void legend(EntityType<T> type,
            net.minecraft.entity.attribute.DefaultAttributeContainer.Builder attrs) {
        FabricDefaultAttributeRegistry.register(type, attrs);
        SpawnRestriction.register(type, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, MobEntity::canMobSpawn);
        int weight = ru.footballmod.config.FootballConfig.get().legendsSpawnWeight;
        if (weight > 0) {
            BiomeModifications.addSpawn(BiomeSelectors.includeByKey(BiomeKeys.PLAINS, BiomeKeys.SUNFLOWER_PLAINS,
                    BiomeKeys.MEADOW, BiomeKeys.SAVANNA), SpawnGroup.CREATURE, type, weight, 1, 1);
        }
    }
}
