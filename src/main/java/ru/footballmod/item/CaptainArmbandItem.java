package ru.footballmod.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import ru.footballmod.config.FootballConfig;

import java.util.List;

/** Капитанская повязка: работает во второй руке. Капитан крепче, команда рядом — быстрее. */
public class CaptainArmbandItem extends Item {
    public CaptainArmbandItem(Settings settings) {
        super(settings);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isClient || world.getTime() % 40 != 0) return;
        if (!(entity instanceof PlayerEntity captain) || captain.getOffHandStack() != stack) return;
        captain.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 60, 0, true, false, true));
        captain.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 60, 0, true, false, true));
        double r = FootballConfig.get().captainAuraRadius;
        for (PlayerEntity mate : world.getEntitiesByClass(PlayerEntity.class, captain.getBoundingBox().expand(r),
                p -> p != captain && p.isAlive() && !p.isSpectator())) {
            mate.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 60, 0, true, false, true));
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("tooltip.ronaldomessi.captain_armband").formatted(Formatting.GOLD));
        tooltip.add(Text.translatable("tooltip.ronaldomessi.captain_armband2").formatted(Formatting.GRAY));
    }
}
