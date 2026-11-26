package ru.footballmod;

import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import ru.footballmod.config.FootballConfig;
import ru.footballmod.network.BallControl;

public class RonaldoMessiMod implements ModInitializer {
    public static final String MOD_ID = "ronaldomessi";

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        FootballConfig.load();
        ModArmorMaterials.init();
        ModBlocks.init();
        ModEntities.init();
        ModItems.init();
        BallControl.init();
        ru.footballmod.network.PlayerAbilities.init();
        ru.footballmod.match.MatchManager.init();
        ru.footballmod.academy.Academy.init();
    }
}
