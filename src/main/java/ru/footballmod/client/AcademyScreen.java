package ru.footballmod.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.footballmod.network.AcademyOpenPayload;
import ru.footballmod.network.AcademyStartPayload;

/** Меню тренера: три задания и ваши лучшие результаты. */
public class AcademyScreen extends Screen {
    private final AcademyOpenPayload state;

    public AcademyScreen(AcademyOpenPayload state) {
        super(Text.translatable("screen.ronaldomessi.academy"));
        this.state = state;
    }

    public static void show(AcademyOpenPayload state) {
        MinecraftClient.getInstance().setScreen(new AcademyScreen(state));
    }

    private void start(int drill) {
        if (ClientPlayNetworking.canSend(AcademyStartPayload.ID)) ClientPlayNetworking.send(new AcademyStartPayload(state.coachId(), drill));
        close();
    }

    @Override
    protected void init() {
        int cx = width / 2, y = height / 2 - 30, w = 220;
        if (state.busy()) {
            addDrawableChild(ButtonWidget.builder(Text.translatable("screen.ronaldomessi.drill_cancel").formatted(Formatting.RED), b -> start(0))
                    .dimensions(cx - w / 2, y, w, 20).build());
        } else {
            String dribble = state.dribbleTicks() == 0 ? "—" : String.format("%d:%02d", state.dribbleTicks() / 20 / 60, state.dribbleTicks() / 20 % 60);
            addDrawableChild(ButtonWidget.builder(Text.translatable("screen.ronaldomessi.drill_shooting", state.shooting()), b -> start(0))
                    .dimensions(cx - w / 2, y, w, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.translatable("screen.ronaldomessi.drill_dribble", dribble), b -> start(1))
                    .dimensions(cx - w / 2, y + 24, w, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.translatable("screen.ronaldomessi.drill_penalties", state.penalties()), b -> start(2))
                    .dimensions(cx - w / 2, y + 48, w, 20).build());
        }
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> close())
                .dimensions(cx - 50, y + 80, 100, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int cx = width / 2, y = height / 2 - 72;
        ctx.drawCenteredTextWithShadow(textRenderer, title.copy().formatted(Formatting.GOLD, Formatting.BOLD), cx, y, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.ronaldomessi.academy_hint"), cx, y + 16, 0xCCCCCC);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.ronaldomessi.academy_space"), cx, y + 28, 0x999999);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
