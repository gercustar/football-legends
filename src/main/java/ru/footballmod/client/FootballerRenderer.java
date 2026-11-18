package ru.footballmod.client;

import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;
import ru.footballmod.entity.FootballerEntity;

public class FootballerRenderer<T extends FootballerEntity> extends BipedEntityRenderer<T, PlayerEntityModel<T>> {
    private final Identifier texture;

    public FootballerRenderer(EntityRendererFactory.Context ctx, Identifier texture) {
        super(ctx, new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER), false), 0.5f);
        this.texture = texture;
    }

    @Override
    public Identifier getTexture(T entity) {
        return texture;
    }

    /** Имя над головой видно всегда, если игрок рядом. */
    @Override
    protected boolean hasLabel(T entity) {
        return !entity.isInvisible() && this.dispatcher.getSquaredDistanceToCamera(entity) < 24 * 24;
    }
}
