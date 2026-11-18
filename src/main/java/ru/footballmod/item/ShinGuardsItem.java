package ru.footballmod.item;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import ru.footballmod.RonaldoMessiMod;
import ru.footballmod.config.FootballConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Защитные щитки. ПКМ — вставить в надетые шорты/штаны.
 * Шорты получают сопротивление отбрасыванию и броню (стандартный механизм атрибутов предмета,
 * поэтому сохраняется в мире и работает на сервере).
 */
public class ShinGuardsItem extends Item {
    private static final Identifier KB_ID = RonaldoMessiMod.id("shin_guards_knockback");
    private static final Identifier ARMOR_ID = RonaldoMessiMod.id("shin_guards_armor");

    public ShinGuardsItem(Settings settings) {
        super(settings);
    }

    public static boolean hasGuards(ItemStack legs) {
        AttributeModifiersComponent c = legs.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        return c != null && c.modifiers().stream().anyMatch(e -> e.modifier().id().equals(KB_ID));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack guards = user.getStackInHand(hand);
        ItemStack legs = user.getEquippedStack(EquipmentSlot.LEGS);
        if (legs.isEmpty()) {
            if (!world.isClient) user.sendMessage(Text.translatable("msg.ronaldomessi.shin_guards_no_legs").formatted(Formatting.RED), true);
            return TypedActionResult.fail(guards);
        }
        if (hasGuards(legs)) {
            if (!world.isClient) user.sendMessage(Text.translatable("msg.ronaldomessi.shin_guards_already").formatted(Formatting.YELLOW), true);
            return TypedActionResult.fail(guards);
        }
        if (!world.isClient) {
            AttributeModifiersComponent base = legs.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
            if (base == null || base.modifiers().isEmpty()) base = legs.getItem().getAttributeModifiers();
            var cfg = FootballConfig.get();
            legs.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, base
                    .with(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,
                            new EntityAttributeModifier(KB_ID, cfg.shinGuardsKnockbackResistance, EntityAttributeModifier.Operation.ADD_VALUE),
                            AttributeModifierSlot.LEGS)
                    .with(EntityAttributes.GENERIC_ARMOR,
                            new EntityAttributeModifier(ARMOR_ID, cfg.shinGuardsArmor, EntityAttributeModifier.Operation.ADD_VALUE),
                            AttributeModifierSlot.LEGS));
            LoreComponent lore = legs.getOrDefault(DataComponentTypes.LORE, LoreComponent.DEFAULT);
            List<Text> lines = new ArrayList<>(lore.lines());
            lines.add(Text.translatable("tooltip.ronaldomessi.with_shin_guards").formatted(Formatting.BLUE));
            legs.set(DataComponentTypes.LORE, new LoreComponent(lines));
            guards.decrementUnlessCreative(1, user);
            user.sendMessage(Text.translatable("msg.ronaldomessi.shin_guards_done").formatted(Formatting.GREEN), true);
        }
        user.playSound(SoundEvents.ITEM_ARMOR_EQUIP_IRON.value(), 1.0f, 1.0f);
        return TypedActionResult.success(guards, world.isClient);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("tooltip.ronaldomessi.shin_guards").formatted(Formatting.GRAY));
    }
}
