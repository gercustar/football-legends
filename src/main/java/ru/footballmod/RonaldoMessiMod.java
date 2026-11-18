package ru.footballmod;

import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;

public class RonaldoMessiMod implements ModInitializer {
    public static final String MOD_ID = "ronaldomessi";

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModArmorMaterials.init();
        ModBlocks.init();
        ModEntities.init();
        ModItems.init();
    }
}
