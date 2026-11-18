package ru.footballmod.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.RenderLayer;
import ru.footballmod.ModBlocks;
import ru.footballmod.ModEntities;
import ru.footballmod.RonaldoMessiMod;

public class RonaldoMessiClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.RONALDO,
                ctx -> new FootballerRenderer<>(ctx, RonaldoMessiMod.id("textures/entity/ronaldo.png")));
        EntityRendererRegistry.register(ModEntities.MESSI,
                ctx -> new FootballerRenderer<>(ctx, RonaldoMessiMod.id("textures/entity/messi.png")));
        EntityRendererRegistry.register(ModEntities.ZIDANE,
                ctx -> new FootballerRenderer<>(ctx, RonaldoMessiMod.id("textures/entity/zidane.png")));
        EntityRendererRegistry.register(ModEntities.RONALDINHO,
                ctx -> new FootballerRenderer<>(ctx, RonaldoMessiMod.id("textures/entity/ronaldinho.png")));
        EntityRendererRegistry.register(ModEntities.NEYMAR,
                ctx -> new FootballerRenderer<>(ctx, RonaldoMessiMod.id("textures/entity/neymar.png")));
        EntityRendererRegistry.register(ModEntities.MBAPPE,
                ctx -> new FootballerRenderer<>(ctx, RonaldoMessiMod.id("textures/entity/mbappe.png")));
        EntityRendererRegistry.register(ModEntities.MODRIC,
                ctx -> new FootballerRenderer<>(ctx, RonaldoMessiMod.id("textures/entity/modric.png")));
        EntityRendererRegistry.register(ModEntities.RAMOS,
                ctx -> new FootballerRenderer<>(ctx, RonaldoMessiMod.id("textures/entity/ramos.png")));
        EntityRendererRegistry.register(ModEntities.SOCCER_BALL, SoccerBallRenderer::new);
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.GOAL_NET, RenderLayer.getCutout());
    }
}
