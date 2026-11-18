package ru.footballmod;

import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvents;

import java.util.EnumMap;
import java.util.List;

public class ModArmorMaterials {
    public static final RegistryEntry<ArmorMaterial> RONALDO = register("ronaldo");
    public static final RegistryEntry<ArmorMaterial> MESSI = register("messi");
    // Бутсы — у каждой пары своя текстура на ногах
    public static final RegistryEntry<ArmorMaterial> MERCURIAL = register("mercurial");
    public static final RegistryEntry<ArmorMaterial> F50 = register("f50");
    public static final RegistryEntry<ArmorMaterial> PREDATOR = register("predator");

    private static RegistryEntry<ArmorMaterial> register(String name) {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.BOOTS, 3);
        defense.put(ArmorItem.Type.LEGGINGS, 5);
        defense.put(ArmorItem.Type.CHESTPLATE, 7);
        defense.put(ArmorItem.Type.HELMET, 3);
        defense.put(ArmorItem.Type.BODY, 7);
        return Registry.registerReference(Registries.ARMOR_MATERIAL, RonaldoMessiMod.id(name),
                new ArmorMaterial(defense, 25, SoundEvents.ITEM_ARMOR_EQUIP_LEATHER,
                        () -> Ingredient.ofItems(Items.WHITE_WOOL),
                        List.of(new ArmorMaterial.Layer(RonaldoMessiMod.id(name))),
                        1.5f, 0.0f));
    }

    public static void init() {}
}
