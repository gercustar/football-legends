package ru.footballmod.item;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.SpawnEggItem;

/**
 * Яйцо призыва особой версии игрока (NBT {Version:N} лежит в компоненте ENTITY_DATA).
 * Такие яйца регистрируются РАНЬШЕ обычных: игра запоминает «яйцо моба» по последнему,
 * поэтому колесо мыши по футболисту даёт обычное яйцо.
 */
public class VersionSpawnEggItem extends SpawnEggItem {
    public VersionSpawnEggItem(EntityType<? extends MobEntity> type, int primary, int secondary, Settings settings) {
        super(type, primary, secondary, settings);
    }
}
