package ru.footballmod.item;

import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.footballmod.RonaldoMessiMod;
import ru.footballmod.config.FootballConfig;

import java.util.List;

/**
 * Бутсы с особым свойством:
 * SPEED — Mercurial (скорость), DRIBBLE — F50 (мяч «прилипает» при ведении),
 * POWER — Predator (сильный и точный удар по взгляду).
 * Эффекты для мяча проверяются в SoccerBallEntity через {@link #typeOf(PlayerEntity)}.
 */
public class FootballBootsItem extends ArmorItem {
    public enum BootType { SPEED, DRIBBLE, POWER }

    private final BootType bootType;
    private AttributeModifiersComponent modifiers;

    public FootballBootsItem(RegistryEntry<ArmorMaterial> material, BootType bootType, Settings settings) {
        super(material, Type.BOOTS, settings);
        this.bootType = bootType;
    }

    public BootType getBootType() {
        return bootType;
    }

    /** Тип бутс, надетых на игрока, или null. */
    public static BootType typeOf(PlayerEntity player) {
        ItemStack feet = player.getEquippedStack(EquipmentSlot.FEET);
        return feet.getItem() instanceof FootballBootsItem b ? b.bootType : null;
    }

    @Override
    public AttributeModifiersComponent getAttributeModifiers() {
        if (modifiers == null) {
            var cfg = FootballConfig.get();
            double speed = switch (bootType) {
                case SPEED -> cfg.mercurialSpeedBonus;
                case DRIBBLE -> cfg.f50SpeedBonus;
                case POWER -> 0.0;
            };
            AttributeModifiersComponent base = super.getAttributeModifiers();
            modifiers = speed <= 0 ? base : base.with(EntityAttributes.GENERIC_MOVEMENT_SPEED,
                    new EntityAttributeModifier(RonaldoMessiMod.id("boots_speed"), speed,
                            EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                    AttributeModifierSlot.FEET);
        }
        return modifiers;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        String key = switch (bootType) {
            case SPEED -> "tooltip.ronaldomessi.mercurial";
            case DRIBBLE -> "tooltip.ronaldomessi.f50";
            case POWER -> "tooltip.ronaldomessi.predator";
        };
        tooltip.add(Text.translatable(key).formatted(Formatting.AQUA));
    }
}
