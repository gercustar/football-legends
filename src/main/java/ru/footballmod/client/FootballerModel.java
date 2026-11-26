package ru.footballmod.client;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import ru.footballmod.entity.FootballerEntity;

import java.util.EnumSet;
import java.util.Set;

/**
 * Модель футболиста: локти и колени сгибаются, объёмные волосы, нос, уши, рукава, шорты и бутсы.
 * Развёртка текстуры — 128×64 (см. tools/textures/realistic.py).
 */
public class FootballerModel<T extends FootballerEntity> extends BipedEntityModel<T> {
    private final ModelPart rightForearm, leftForearm, rightElbow, leftElbow;
    private final ModelPart rightShin, leftShin, rightKnee, leftKnee;

    public FootballerModel(ModelPart root) {
        super(root);
        rightForearm = rightArm.getChild("forearm");
        rightElbow = rightArm.getChild("elbow");
        leftForearm = leftArm.getChild("forearm");
        leftElbow = leftArm.getChild("elbow");
        rightShin = rightLeg.getChild("shin");
        rightKnee = rightLeg.getChild("knee");
        leftShin = leftLeg.getChild("shin");
        leftKnee = leftLeg.getChild("knee");
    }

    public static TexturedModelData create(FootballerLook look) {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        root.addChild(EntityModelPartNames.HAT, ModelPartBuilder.create(), ModelTransform.NONE);
        head(root, look);
        body(root);
        arm(root, EntityModelPartNames.RIGHT_ARM, -1, 40, 16, 64);
        arm(root, EntityModelPartNames.LEFT_ARM, 1, 32, 48, 80);
        leg(root, EntityModelPartNames.RIGHT_LEG, -1, 0, 16, 64, 64);
        leg(root, EntityModelPartNames.LEFT_LEG, 1, 16, 48, 80, 84);
        return TexturedModelData.of(data, 128, 64);
    }

    private static void head(ModelPartData root, FootballerLook look) {
        ModelPartBuilder b = ModelPartBuilder.create()
                .uv(0, 0).cuboid(-4, -8, -4, 8, 8, 8, new Dilation(-0.75f, -0.45f, -0.55f))
                .uv(96, 0).cuboid(-0.5f, -4.1f, -4.0f, 1, 2, 1, new Dilation(0, -0.05f, 0))                       // нос
                .uv(100, 0).cuboid(-3.75f, -4.6f, -0.8f, 1, 2, 2, new Dilation(-0.1f, 0, -0.2f))  // уши
                .uv(106, 0).cuboid(2.75f, -4.6f, -0.8f, 1, 2, 2, new Dilation(-0.1f, 0, -0.2f));
        switch (look.hair()) {
            case NONE -> { }
            case BUZZ -> b
                    .uv(64, 28).cuboid(-4, -8.05f, -4, 8, 2, 8, new Dilation(-0.68f, 0, -0.48f));
            case SHORT -> {
                cap(b, -8.1f, 0);
                back(b, 0);
            }
            case SHORT_SWEPT -> {
                cap(b, -8.2f, 0.03f);
                back(b, 0.03f);
                b.uv(96, 28).cuboid(-4, -8.45f, -4.05f, 8, 2, 3, new Dilation(-0.85f, 0, 0));
            }
            case QUIFF -> {
                cap(b, -8.3f, 0);
                back(b, 0);
                b.uv(96, 28).cuboid(-4, -8.95f, -3.9f, 8, 2, 3, new Dilation(-1.0f, 0, -0.1f));
            }
            case SLICK -> {
                cap(b, -8.6f, -0.04f);
                back(b, 0);
                b.uv(64, 28).cuboid(-4, -8.95f, -4, 8, 2, 8, new Dilation(-1.1f, 0, -0.75f));
                b.uv(96, 28).cuboid(-4, -9.2f, -3.7f, 8, 2, 3, new Dilation(-1.35f, 0, -0.1f));
            }
            case MOHAWK -> mohawk(b);
            case LONG -> {
                cap(b, -8.15f, 0.05f);
                longHair(b, 0);
            }
            case CURLY -> {
                cap(b, -8.5f, 0.35f);
                b.uv(96, 28).cuboid(-4, -8.9f, -3.2f, 8, 2, 3, new Dilation(-0.8f, 0, 0.2f));
                longHair(b, 0.35f);
            }
        }
        if (look.headband()) {
            float wide = look.hair() == FootballerLook.Hair.CURLY ? 0.42f : 0.12f;
            b.uv(86, 41).cuboid(-4, -6.95f, -4, 8, 1, 8, new Dilation(-0.6f + wide, -0.1f, -0.42f + wide));
        }
        if (look.beard()) {
            b.uv(64, 48).cuboid(-4, -3.2f, -4, 8, 3, 5, new Dilation(-0.65f, 0, -0.45f));
        }
        root.addChild(EntityModelPartNames.HEAD, b, ModelTransform.NONE);
    }

    /** Волосы сверху; grow — пышность. */
    private static void cap(ModelPartBuilder b, float y, float grow) {
        b.uv(64, 28).cuboid(-4, y, -4, 8, 2, 8, new Dilation(-0.65f + grow, 0, -0.45f + grow));
    }

    /** Ирокез: тёмная основа, зубчатые пряди вдоль и поперёк, полоса до шеи. */
    private static void mohawk(ModelPartBuilder b) {
        b.uv(64, 38).cuboid(-1.5f, -8.45f, -3.75f, 3, 2, 8, new Dilation(0.35f, 0, -0.3f));
        b.uv(106, 50).cuboid(-1.5f, -7.9f, 3.1f, 3, 7, 1, new Dilation(0.35f, 0, -0.1f));
        Set<Direction> side = EnumSet.of(Direction.WEST);
        float[] xs = {-1.5f, -0.75f, 0f, 0.75f, 1.5f};
        for (int i = 0; i < xs.length; i++) {
            float y = -11.6f + Math.abs(xs[i]) * 0.6f;
            b.uv(64, i % 2 == 0 ? 40 : 46).cuboid(xs[i], y, -4.2f + i * 0.1f, 0, 5, 8, side);
        }
        Set<Direction> front = EnumSet.of(Direction.NORTH);
        for (int i = 0; i < 8; i++) {
            float z = -3.75f + i * 0.95f;
            float y = -11.5f + 0.06f * (z + 1) * (z + 1);
            b.uv(90 + 5 * (i % 3), 50).cuboid(-2 + (i % 2) * 0.15f, y, z, 4, 5, 0, front);
        }
    }

    /** Короткие волосы на затылке. */
    private static void back(ModelPartBuilder b, float grow) {
        b.uv(96, 34).cuboid(-4, -7.8f, 2.0f, 8, 5, 2, new Dilation(-0.7f + grow, 0, -0.3f + grow));
    }

    /** Длинные волосы до воротника (фамилия на спине видна) и пряди по бокам. */
    private static void longHair(ModelPartBuilder b, float grow) {
        b.uv(90, 50).cuboid(-4, -7.8f, 2.2f, 8, 7, 2, new Dilation(-0.55f + grow, 0, -0.2f + grow));
        b.uv(110, 50).cuboid(-3.95f - grow, -7.6f, -1.3f + grow * 0.6f, 1, 7, 5, new Dilation(0, 0, 0.08f + grow * 0.5f));
        b.uv(110, 50).cuboid(2.95f + grow, -7.6f, -1.3f + grow * 0.6f, 1, 7, 5, new Dilation(0, 0, 0.08f + grow * 0.5f));
    }

    private static void body(ModelPartData root) {
        // Грудь шире, талия уже; нижняя часть продолжает развёртку верхней
        root.addChild(EntityModelPartNames.BODY, ModelPartBuilder.create()
                .uv(16, 16).cuboid(-4, 0, -2, 8, 7, 4, new Dilation(0.3f, 0, 0.15f))
                .uv(16, 23).cuboid(-4, 7, -2, 8, 5, 4, new Dilation(-0.1f, 0, -0.05f))
                .uv(96, 4).cuboid(-2, -1.2f, -1.8f, 4, 2, 4, new Dilation(-0.35f, 0, -0.35f)), // шея
                ModelTransform.NONE);
    }

    private static void arm(ModelPartData root, String name, int side, int u, int v, int sleeveU) {
        float x = side < 0 ? -3 : -1; // как у обычной модели
        float cx = x + 2;
        ModelPartData arm = root.addChild(name, ModelPartBuilder.create()
                .uv(u, v).cuboid(x, -2, -2, 4, 6, 4, new Dilation(-0.5f, 0, -0.45f))
                .uv(sleeveU, 0).cuboid(x, -2.15f, -2, 4, 5, 4, new Dilation(-0.15f, 0, -0.1f)),
                ModelTransform.pivot(5 * side, 2, 0));
        arm.addChild("elbow", ModelPartBuilder.create()
                .uv(u, v + 4).cuboid(-2, -2, -2, 4, 4, 4, new Dilation(-0.65f)), ModelTransform.pivot(cx, 4, 0));
        arm.addChild("forearm", ModelPartBuilder.create()
                .uv(u, v + 6).cuboid(-2, 0, -2, 4, 6, 4, new Dilation(-0.6f, 0, -0.55f)), ModelTransform.pivot(cx, 4, 0));
    }

    private static void leg(ModelPartData root, String name, int side, int u, int v, int shortsU, int footU) {
        ModelPartData leg = root.addChild(name, ModelPartBuilder.create()
                .uv(u, v).cuboid(-2, 0, -2, 4, 6, 4, new Dilation(-0.2f, 0, -0.2f))
                .uv(shortsU, 10).cuboid(-2, -0.3f, -2, 4, 5, 4, new Dilation(0.2f, 0, side < 0 ? 0.3f : 0.28f)),
                ModelTransform.pivot(1.9f * side, 12, 0));
        leg.addChild("knee", ModelPartBuilder.create()
                .uv(u, v + 4).cuboid(-2, -2, -2, 4, 4, 4, new Dilation(-0.45f)), ModelTransform.pivot(0, 6, 0));
        leg.addChild("shin", ModelPartBuilder.create()
                .uv(u, v + 6).cuboid(-2, 0, -2, 4, 5, 4, new Dilation(-0.3f, 0, -0.3f))
                .uv(footU, 20).cuboid(-2, 4, -3.8f, 4, 2, 6, new Dilation(-0.15f, 0, -0.1f)),
                ModelTransform.pivot(0, 6, 0));
    }

    /** Обычный взмах рукой заменён ударом ногой. */
    @Override
    protected void animateArms(T entity, float animationProgress) {}

    @Override
    public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        super.setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
        applyFootballPose(limbAngle, limbDistance, handSwingProgress);
    }

    /** Сгибание локтей и коленей при беге и удар по мячу (kick: 0..1). */
    public void applyFootballPose(float limbAngle, float limbDistance, float kick) {
        float run = Math.min(1f, limbDistance);
        float phase = limbAngle * 0.6662f;

        rightArm.pivotX -= 0.15f;
        leftArm.pivotX += 0.15f;
        rightArm.roll += 0.02f + 0.1f * run;
        leftArm.roll -= 0.02f + 0.1f * run;
        rightForearm.pitch = leftForearm.pitch = -(0.15f + 1.0f * run);

        // Колено сгибается, когда нога идёт вперёд
        rightShin.pitch = 0.05f + 1.1f * run * Math.max(0, MathHelper.sin(phase));
        leftShin.pitch = 0.05f + 1.1f * run * Math.max(0, -MathHelper.sin(phase));

        if (kick > 0f) {
            float leg, shin;
            if (kick < 0.35f) {
                leg = 0.9f * (kick / 0.35f);
                shin = 1.3f * (kick / 0.35f);
            } else if (kick < 0.65f) {
                float k = (kick - 0.35f) / 0.3f;
                leg = MathHelper.lerp(k, 0.9f, -1.3f);
                shin = MathHelper.lerp(k, 1.3f, 0.05f);
            } else {
                leg = MathHelper.lerp((kick - 0.65f) / 0.35f, -1.3f, 0f);
                shin = 0.05f;
            }
            float w = MathHelper.clamp(Math.min(kick, 1 - kick) * 6f, 0f, 1f);
            rightLeg.pitch = MathHelper.lerp(w, rightLeg.pitch, leg);
            rightShin.pitch = MathHelper.lerp(w, rightShin.pitch, shin);
            leftArm.pitch = MathHelper.lerp(w, leftArm.pitch, -0.9f);
            rightArm.pitch = MathHelper.lerp(w, rightArm.pitch, 0.7f);
        }

        rightElbow.pitch = rightForearm.pitch / 2;
        leftElbow.pitch = leftForearm.pitch / 2;
        rightKnee.pitch = rightShin.pitch / 2;
        leftKnee.pitch = leftShin.pitch / 2;
    }
}
