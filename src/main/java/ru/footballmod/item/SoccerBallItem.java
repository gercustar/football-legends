package ru.footballmod.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import ru.footballmod.ModEntities;
import ru.footballmod.entity.SoccerBallEntity;

public class SoccerBallItem extends Item {
    public SoccerBallItem(Settings settings) {
        super(settings);
    }

    /** ПКМ по блоку — положить мяч. */
    @Override
    public ActionResult useOnBlock(ItemUsageContext ctx) {
        World world = ctx.getWorld();
        if (!world.isClient) {
            BlockPos pos = ctx.getBlockPos().offset(ctx.getSide());
            SoccerBallEntity ball = ModEntities.SOCCER_BALL.create(world);
            if (ball == null) return ActionResult.FAIL;
            ball.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
            world.spawnEntity(ball);
            ctx.getStack().decrementUnlessCreative(1, ctx.getPlayer());
        }
        return ActionResult.success(world.isClient);
    }

    /** ПКМ в воздух — бросить мяч вперёд. */
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient) {
            SoccerBallEntity ball = ModEntities.SOCCER_BALL.create(world);
            if (ball == null) return TypedActionResult.fail(stack);
            Vec3d look = user.getRotationVector();
            ball.refreshPositionAndAngles(user.getX() + look.x, user.getEyeY() - 0.4, user.getZ() + look.z, 0, 0);
            ball.setVelocity(look.multiply(0.9));
            world.spawnEntity(ball);
            stack.decrementUnlessCreative(1, user);
        }
        return TypedActionResult.success(stack, world.isClient);
    }
}
