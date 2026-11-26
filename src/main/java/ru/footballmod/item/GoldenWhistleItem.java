package ru.footballmod.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import ru.footballmod.match.Match;
import ru.footballmod.match.MatchManager;

import java.util.List;

/**
 * Золотой свисток (Этап 3): вы сами судите матч рядом.
 * ПКМ — пауза / продолжить, Shift + ПКМ — мяч в центр, ПКМ по игроку или футболисту — карточка.
 */
public class GoldenWhistleItem extends Item {
    public GoldenWhistleItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient) return TypedActionResult.success(stack);
        Match m = MatchManager.matchNear(user);
        if (m == null) {
            user.sendMessage(Text.translatable("msg.ronaldomessi.whistle_no_match").formatted(Formatting.GRAY), true);
            return TypedActionResult.fail(stack);
        }
        blow(user);
        if (user.isSneaking()) m.restartFromCentre(user);
        else m.togglePause(user);
        user.getItemCooldownManager().set(this, 10);
        return TypedActionResult.success(stack);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (user.getWorld().isClient) return ActionResult.SUCCESS;
        Match m = MatchManager.matchNear(user);
        if (m == null || m.teamOf(entity) == null) return ActionResult.PASS;
        blow(user);
        m.cardFromWhistle(entity);
        user.getItemCooldownManager().set(this, 10);
        return ActionResult.SUCCESS;
    }

    private static void blow(PlayerEntity user) {
        user.getWorld().playSound(null, user.getX(), user.getY() + 1, user.getZ(), SoundEvents.BLOCK_NOTE_BLOCK_FLUTE.value(),
                SoundCategory.PLAYERS, 2.0f, 2.0f);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("tooltip.ronaldomessi.whistle1").formatted(Formatting.GOLD));
        tooltip.add(Text.translatable("tooltip.ronaldomessi.whistle2").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.ronaldomessi.whistle3").formatted(Formatting.GRAY));
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }
}
