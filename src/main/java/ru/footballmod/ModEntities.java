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
import ru.footballmod.entity.FootballerEntity;

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

    public static void init() {
        FabricDefaultAttributeRegistry.register(RONALDO, FootballerEntity.createFootballerAttributes());
        FabricDefaultAttributeRegistry.register(MESSI, FootballerEntity.createFootballerAttributes());

        SpawnRestriction.register(RONALDO, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, MobEntity::canMobSpawn);
        SpawnRestriction.register(MESSI, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, MobEntity::canMobSpawn);

        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(BiomeKeys.PLAINS, BiomeKeys.SUNFLOWER_PLAINS,
                BiomeKeys.MEADOW, BiomeKeys.SAVANNA), SpawnGroup.CREATURE, RONALDO, 4, 1, 1);
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(BiomeKeys.PLAINS, BiomeKeys.SUNFLOWER_PLAINS,
                BiomeKeys.MEADOW, BiomeKeys.SAVANNA), SpawnGroup.CREATURE, MESSI, 4, 1, 1);
    }
}
