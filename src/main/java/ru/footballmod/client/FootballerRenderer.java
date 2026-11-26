package ru.footballmod.client;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import ru.footballmod.entity.FootballerEntity;

/** Рисует футболиста; у каждой версии игрока (CR7 2008 и т.п.) своя модель, текстура и рост. */
public class FootballerRenderer<T extends FootballerEntity> extends BipedEntityRenderer<T, BipedEntityModel<T>> {
    private final Identifier[] textures;
    private final BipedEntityModel<T>[] models;
    private final FootballerLook[] looks;

    @SuppressWarnings("unchecked")
    public FootballerRenderer(EntityRendererFactory.Context ctx, Identifier[] textures, EntityModelLayer[] layers, FootballerLook[] looks) {
        super(ctx, new FootballerModel<>(ctx.getPart(layers[0])), 0.45f);
        this.textures = textures;
        this.looks = looks;
        this.models = new BipedEntityModel[layers.length];
        models[0] = model;
        for (int i = 1; i < layers.length; i++) models[i] = new FootballerModel<>(ctx.getPart(layers[i]));
    }

    private int variant(T entity) {
        int v = entity.getVariant();
        return v >= 0 && v < models.length ? v : 0;
    }

    @Override
    public void render(T entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        model = models[variant(entity)];
        super.render(entity, yaw, tickDelta, matrices, consumers, light);
    }

    @Override
    public Identifier getTexture(T entity) {
        return textures[variant(entity)];
    }

    @Override
    protected void scale(T entity, MatrixStack matrices, float amount) {
        FootballerLook look = looks[variant(entity)];
        matrices.scale(look.width(), look.height(), look.width());
    }

    /** Имя над головой видно всегда, если игрок рядом. */
    @Override
    protected boolean hasLabel(T entity) {
        return !entity.isInvisible() && this.dispatcher.getSquaredDistanceToCamera(entity) < 24 * 24;
    }
}
