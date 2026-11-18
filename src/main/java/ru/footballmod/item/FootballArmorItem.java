package ru.footballmod.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import ru.footballmod.ModArmorMaterials;

import java.util.List;

/** Форма футболиста. Полный комплект (футболка + шорты + бутсы) даёт бонусы. */
public class FootballArmorItem extends ArmorItem {
    public FootballArmorItem(RegistryEntry<ArmorMaterial> material, Type type, Settings settings) {
        super(material, type, settings);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isClient || getType() != Type.CHESTPLATE || world.getTime() % 20 != 0) return;
        if (!(entity instanceof PlayerEntity player) || player.getEquippedStack(EquipmentSlot.CHEST) != stack) return;
        if (!isPart(player.getEquippedStack(EquipmentSlot.LEGS)) || !isPart(player.getEquippedStack(EquipmentSlot.FEET))) return;

        if (getMaterial() == ModArmorMaterials.RONALDO) {
            effect(player, new StatusEffectInstance(StatusEffects.JUMP_BOOST, 60, 1, true, false, true));
            effect(player, new StatusEffectInstance(StatusEffects.STRENGTH, 60, 0, true, false, true));
            effect(player, new StatusEffectInstance(StatusEffects.SPEED, 60, 0, true, false, true));
        } else {
            effect(player, new StatusEffectInstance(StatusEffects.SPEED, 60, 1, true, false, true));
            effect(player, new StatusEffectInstance(StatusEffects.HASTE, 60, 0, true, false, true));
            effect(player, new StatusEffectInstance(StatusEffects.LUCK, 60, 0, true, false, true));
        }
    }

    private static void effect(PlayerEntity p, StatusEffectInstance e) {
        p.addStatusEffect(e);
    }

    private boolean isPart(ItemStack s) {
        return s.getItem() instanceof FootballArmorItem a && a.getMaterial() == getMaterial();
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        String key = getMaterial() == ModArmorMaterials.RONALDO ? "tooltip.ronaldomessi.ronaldo_set" : "tooltip.ronaldomessi.messi_set";
        tooltip.add(Text.translatable(key).formatted(Formatting.GRAY));
    }
}
