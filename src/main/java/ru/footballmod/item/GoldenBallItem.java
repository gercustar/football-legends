package ru.footballmod.item;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/** Золотой мяч (Ballon d'Or): даёт силу чемпиона. */
public class GoldenBallItem extends Item {
    public GoldenBallItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient) {
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 200, 1));
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 400, 1));
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 400, 1));
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 600, 2));
            user.sendMessage(Text.translatable("msg.ronaldomessi.golden_ball").formatted(Formatting.GOLD), true);
        }
        user.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.0f);
        user.getItemCooldownManager().set(this, 1200);
        return TypedActionResult.success(stack, world.isClient);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("tooltip.ronaldomessi.golden_ball").formatted(Formatting.YELLOW));
    }
}
