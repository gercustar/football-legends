package ru.footballmod.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Identifier;
import ru.footballmod.ModBlocks;
import ru.footballmod.ModEntities;
import ru.footballmod.RonaldoMessiMod;
import ru.footballmod.entity.FootballerEntity;

public class RonaldoMessiClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        realistic(ModEntities.RONALDO, new String[]{"ronaldo", "ronaldo_2008", "ronaldo_2017"},
                new FootballerLook[]{FootballerLook.RONALDO, FootballerLook.RONALDO_2008, FootballerLook.RONALDO_2017});
        realistic(ModEntities.MESSI, new String[]{"messi", "messi_2012"},
                new FootballerLook[]{FootballerLook.MESSI, FootballerLook.MESSI_2012});
        realistic(ModEntities.ZIDANE, "zidane", FootballerLook.ZIDANE);
        realistic(ModEntities.RONALDINHO, "ronaldinho", FootballerLook.RONALDINHO);
        realistic(ModEntities.NEYMAR, "neymar", FootballerLook.NEYMAR);
        realistic(ModEntities.MBAPPE, "mbappe", FootballerLook.MBAPPE);
        realistic(ModEntities.MODRIC, "modric", FootballerLook.MODRIC);
        realistic(ModEntities.RAMOS, "ramos", FootballerLook.RAMOS);
        realistic(ModEntities.XAVI, "xavi", FootballerLook.XAVI);
        realistic(ModEntities.INIESTA, "iniesta", FootballerLook.INIESTA);
        realistic(ModEntities.SUAREZ, "suarez", FootballerLook.SUAREZ);
        EntityRendererRegistry.register(ModEntities.SOCCER_BALL, SoccerBallRenderer::new);
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.GOAL_NET, RenderLayer.getCutout());
        FootballControls.init();
        realistic(ModEntities.REFEREE, "referee", FootballerLook.REFEREE);
        realistic(ModEntities.GOALKEEPER, "goalkeeper", FootballerLook.KEEPER);
        realistic(ModEntities.COACH, "coach", FootballerLook.COACH);
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
                ru.footballmod.network.AcademyOpenPayload.ID, (payload, ctx) -> AcademyScreen.show(payload));
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
                ru.footballmod.network.BoardStatePayload.ID, (payload, ctx) -> BoardScreen.show(payload));
    }

    /** Футболист с новой моделью; текстура 128×64 (×8) в textures/entity/footballer. */
    private static <T extends FootballerEntity> void realistic(EntityType<T> type, String name, FootballerLook look) {
        realistic(type, new String[]{name}, new FootballerLook[]{look});
    }

    /** То же, но с версиями игрока: names[i] и looks[i] — для версии i. */
    private static <T extends FootballerEntity> void realistic(EntityType<T> type, String[] names, FootballerLook[] looks) {
        Identifier[] textures = new Identifier[names.length];
        EntityModelLayer[] layers = new EntityModelLayer[names.length];
        for (int i = 0; i < names.length; i++) {
            FootballerLook look = looks[i];
            textures[i] = RonaldoMessiMod.id("textures/entity/footballer/" + names[i] + ".png");
            layers[i] = new EntityModelLayer(RonaldoMessiMod.id("footballer_" + names[i]), "main");
            EntityModelLayerRegistry.registerModelLayer(layers[i], () -> FootballerModel.create(look));
        }
        EntityRendererRegistry.register(type, ctx -> new FootballerRenderer<>(ctx, textures, layers, looks));
    }
}
