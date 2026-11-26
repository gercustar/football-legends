package ru.footballmod.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.footballmod.network.BoardActionPayload;
import ru.footballmod.network.BoardStatePayload;

/** Меню табло: выбрать команду, начать или остановить матч. */
public class BoardScreen extends Screen {
    private BoardStatePayload state;

    public BoardScreen(BoardStatePayload state) {
        super(Text.translatable("screen.ronaldomessi.board"));
        this.state = state;
    }

    /** Открыть меню или обновить уже открытое. */
    public static void show(BoardStatePayload state) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen instanceof BoardScreen s && s.state.pos().equals(state.pos())) {
            s.state = state;
            s.clearAndInit();
        } else {
            mc.setScreen(new BoardScreen(state));
        }
    }

    private void send(int action) {
        if (ClientPlayNetworking.canSend(BoardActionPayload.ID)) ClientPlayNetworking.send(new BoardActionPayload(state.pos(), action));
    }

    @Override
    protected void init() {
        int cx = width / 2, y = height / 2 - 20, w = 98;
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.ronaldomessi.join_home"), b -> send(BoardActionPayload.JOIN_HOME))
                .dimensions(cx - w - w / 2 - 4, y, w, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.ronaldomessi.spectate"), b -> send(BoardActionPayload.SPECTATE))
                .dimensions(cx - w / 2, y, w, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.ronaldomessi.join_away"), b -> send(BoardActionPayload.JOIN_AWAY))
                .dimensions(cx + w / 2 + 4, y, w, 20).build());
        y += 30;
        if (state.running()) {
            addDrawableChild(ButtonWidget.builder(Text.translatable("screen.ronaldomessi.stop").formatted(Formatting.RED), b -> send(BoardActionPayload.STOP))
                    .dimensions(cx - w / 2, y, w, 20).build());
        } else {
            addDrawableChild(ButtonWidget.builder(Text.translatable("screen.ronaldomessi.start", 3), b -> send(BoardActionPayload.START_3))
                    .dimensions(cx - w - w / 2 - 4, y, w, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.translatable("screen.ronaldomessi.start_default"), b -> send(BoardActionPayload.START_5))
                    .dimensions(cx - w / 2, y, w, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.translatable("screen.ronaldomessi.start", 10), b -> send(BoardActionPayload.START_10))
                    .dimensions(cx + w / 2 + 4, y, w, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.translatable("screen.ronaldomessi.penalties").formatted(Formatting.GOLD), b -> send(BoardActionPayload.PENALTIES))
                    .dimensions(cx - w / 2, y + 24, w, 20).build());
        }
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> close())
                .dimensions(cx - w / 2, y + 54, w, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int cx = width / 2, y = height / 2 - 72;
        ctx.drawCenteredTextWithShadow(textRenderer, title.copy().formatted(Formatting.GOLD, Formatting.BOLD), cx, y, 0xFFFFFF);
        Text status = state.running() ? Text.literal(state.score()).formatted(Formatting.YELLOW)
                : Text.translatable("screen.ronaldomessi.waiting").formatted(Formatting.GRAY);
        ctx.drawCenteredTextWithShadow(textRenderer, status, cx, y + 16, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.ronaldomessi.players", state.home(), state.away()), cx, y + 30, 0xDDDDDD);
        String my = switch (state.myTeam()) { case 1 -> "match.ronaldomessi.home"; case 2 -> "match.ronaldomessi.away"; default -> "screen.ronaldomessi.spectator"; };
        ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.ronaldomessi.you", Text.translatable(my)), cx, y + 42, 0xAAFFAA);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
