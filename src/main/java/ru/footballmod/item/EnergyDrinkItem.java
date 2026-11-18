package ru.footballmod.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import ru.footballmod.config.FootballConfig;

import java.util.List;

/** Энергетический напиток: скорость и прыгучесть на время, затем короткая усталость. */
public class EnergyDrinkItem extends Item {
    public EnergyDrinkItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return ItemUsage.consumeHeldItem(world, user, hand);
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.DRINK;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 24;
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        var cfg = FootballConfig.get();
        if (!world.isClient) {
            int d = cfg.energyDrinkDuration;
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, d, 1));
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, d, 0));
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, 1, 0));
        }
        if (user instanceof PlayerEntity p) {
            p.getItemCooldownManager().set(this, cfg.energyDrinkCooldown);
            stack.decrementUnlessCreative(1, p);
        } else {
            stack.decrement(1);
        }
        return stack;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("tooltip.ronaldomessi.energy_drink").formatted(Formatting.GREEN));
    }
}
