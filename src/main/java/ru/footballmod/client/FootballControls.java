package ru.footballmod.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import ru.footballmod.config.FootballConfig;
import ru.footballmod.network.AbilityPayload;
import ru.footballmod.network.AbilityStatePayload;
import ru.footballmod.network.BallActionPayload;

/** Клавиши управления мячом и полоска силы удара. */
public final class FootballControls {
    private static final String CATEGORY = "key.categories.ronaldomessi";
    private static final int BAR_WIDTH = 92;
    private static final int BAR_HEIGHT = 7;

    private static KeyBinding shotKey;
    private static KeyBinding passKey;
    private static boolean charging;
    private static int chargeAge;

    // Способности (Этап 2): R — финт, V — рывок, G — пушка
    private static final KeyBinding[] abilityKeys = new KeyBinding[AbilityPayload.COUNT];
    private static final String[] ABILITY_LETTER = {"R", "V", "G"};
    private static final int[] ABILITY_COLOR = {0xFF39C5FF, 0xFF7CFF4F, 0xFFFF8A1F};
    /** Остаток перезарядки (тики) и её полная длина — для значков; присылает сервер. */
    private static final int[] cooldownLeft = new int[AbilityPayload.COUNT];
    private static final int[] cooldownTotal = new int[AbilityPayload.COUNT];
    private static int cannonLeft;

    private FootballControls() {}

    public static void init() {
        shotKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.ronaldomessi.shot",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY));
        passKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.ronaldomessi.pass",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Z, CATEGORY));
        int[] keys = {GLFW.GLFW_KEY_R, GLFW.GLFW_KEY_V, GLFW.GLFW_KEY_G};
        String[] names = {"feint", "dash", "cannon"};
        for (int i = 0; i < AbilityPayload.COUNT; i++) {
            abilityKeys[i] = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.ronaldomessi." + names[i],
                    InputUtil.Type.KEYSYM, keys[i], CATEGORY));
        }
        ClientPlayNetworking.registerGlobalReceiver(AbilityStatePayload.ID, (payload, ctx) -> {
            for (int i = 0; i < AbilityPayload.COUNT; i++) {
                cooldownLeft[i] = payload.cooldown(i);
                if (cooldownLeft[i] > 0 && cooldownLeft[i] >= cooldownTotal[i] - 2) cooldownTotal[i] = cooldownLeft[i];
                if (cooldownLeft[i] > cooldownTotal[i]) cooldownTotal[i] = cooldownLeft[i];
            }
            cannonLeft = payload.cannonActive();
        });
        ClientTickEvents.END_CLIENT_TICK.register(FootballControls::tick);
        HudRenderCallback.EVENT.register(FootballControls::renderBar);
        HudRenderCallback.EVENT.register(FootballControls::renderAbilities);
    }

    private static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            charging = false;
            java.util.Arrays.fill(cooldownLeft, 0);
            cannonLeft = 0;
            return;
        }
        for (int i = 0; i < AbilityPayload.COUNT; i++) if (cooldownLeft[i] > 0) cooldownLeft[i]--;
        if (cannonLeft > 0) cannonLeft--;
        boolean canPlay = client.currentScreen == null && client.player.isAlive() && !client.player.isSpectator()
                && ClientPlayNetworking.canSend(BallActionPayload.ID);
        if (!canPlay) {
            if (charging) {
                charging = false;
                send(BallActionPayload.CHARGE_CANCEL, 0);
            }
            while (passKey.wasPressed()) { } // не копим нажатия, сделанные в меню
            while (shotKey.wasPressed()) { }
            for (KeyBinding k : abilityKeys) while (k.wasPressed()) { }
            return;
        }

        for (int i = 0; i < AbilityPayload.COUNT; i++) {
            boolean pressed = false;
            while (abilityKeys[i].wasPressed()) pressed = true;
            if (pressed && cooldownLeft[i] == 0 && ClientPlayNetworking.canSend(AbilityPayload.ID)) {
                ClientPlayNetworking.send(new AbilityPayload(i, i == AbilityPayload.FEINT ? curve(client) : 0));
            }
        }

        boolean tapped = false;
        while (shotKey.wasPressed()) tapped = true;
        if (charging) {
            chargeAge++;
            if (!shotKey.isPressed()) {
                charging = false;
                send(BallActionPayload.SHOT, curve(client));
            }
        } else if (shotKey.isPressed()) {
            charging = true;
            chargeAge = 0;
            send(BallActionPayload.CHARGE_START, 0);
        } else if (tapped) {
            // Нажали и отпустили быстрее одного тика — слабый удар
            send(BallActionPayload.CHARGE_START, 0);
            send(BallActionPayload.SHOT, curve(client));
        }

        boolean pass = false;
        while (passKey.wasPressed()) pass = true;
        if (pass) send(BallActionPayload.PASS, 0);
    }

    /** 1 — A (влево), -1 — D (вправо), 0 — ни одной или обе сразу. */
    private static int curve(MinecraftClient client) {
        return (client.options.leftKey.isPressed() ? 1 : 0) - (client.options.rightKey.isPressed() ? 1 : 0);
    }

    private static void send(int action, int curve) {
        if (ClientPlayNetworking.canSend(BallActionPayload.ID)) {
            ClientPlayNetworking.send(new BallActionPayload(action, curve));
        }
    }

    /** Значки R, V, G справа от хотбара: тёмные с таймером, пока перезаряжаются; у пушки — рамка, пока заряжена. */
    private static void renderAbilities(DrawContext ctx, RenderTickCounter counter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.hudHidden || client.player == null || client.player.isSpectator()
                || !ClientPlayNetworking.canSend(AbilityPayload.ID)) return;
        int size = 16, gap = 3;
        int x0 = ctx.getScaledWindowWidth() / 2 + 91 + 8;
        int y = ctx.getScaledWindowHeight() - 20;
        for (int i = 0; i < AbilityPayload.COUNT; i++) {
            int x = x0 + i * (size + gap);
            boolean ready = cooldownLeft[i] == 0;
            ctx.fill(x, y, x + size, y + size, 0xB0000000);
            ctx.fill(x + 1, y + 1, x + size - 1, y + size - 1, ready ? (ABILITY_COLOR[i] & 0x00FFFFFF) | 0x90000000 : 0x60303030);
            if (!ready && cooldownTotal[i] > 0) {
                int h = Math.round((size - 2) * cooldownLeft[i] / (float) cooldownTotal[i]);
                ctx.fill(x + 1, y + 1, x + size - 1, y + 1 + h, 0x90000000);
            }
            boolean cannonOn = i == AbilityPayload.CANNON && cannonLeft > 0;
            ctx.drawBorder(x, y, size, size, cannonOn ? ((client.player.age / 3) % 2 == 0 ? 0xFFFFD000 : 0xFFFF5000) : 0xFF808080);
            String label = ready ? ABILITY_LETTER[i] : String.valueOf((cooldownLeft[i] + 19) / 20);
            ctx.drawCenteredTextWithShadow(client.textRenderer, label, x + size / 2, y + 4, ready ? 0xFFFFFF : 0xAAAAAA);
            if (cannonOn) {
                ctx.drawCenteredTextWithShadow(client.textRenderer, String.valueOf((cannonLeft + 19) / 20), x + size / 2, y - 9, 0xFFB040);
            }
        }
    }

    private static void renderBar(DrawContext ctx, RenderTickCounter counter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!charging || client.options.hudHidden || client.player == null) return;
        float max = Math.max(1, FootballConfig.get().chargeTicks);
        float charge = Math.min(1f, (chargeAge + counter.getTickDelta(false)) / max);

        int x = (ctx.getScaledWindowWidth() - BAR_WIDTH) / 2;
        int y = ctx.getScaledWindowHeight() - 72; // выше сердец, брони и названия предмета
        ctx.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0xB0000000);
        // Зелёный → жёлтый → красный
        int r = charge < 0.5f ? (int) (charge * 2 * 255) : 255;
        int g = charge < 0.5f ? 220 : (int) ((1 - charge) * 2 * 220);
        ctx.fill(x + 1, y + 1, x + 1 + Math.round((BAR_WIDTH - 2) * charge), y + BAR_HEIGHT - 1,
                0xFF000000 | (r << 16) | (g << 8) | 0x20);
        ctx.drawBorder(x, y, BAR_WIDTH, BAR_HEIGHT, charge >= 1f ? 0xFFFFFFFF : 0xFF808080);

        int curve = curve(client);
        if (curve > 0) ctx.drawCenteredTextWithShadow(client.textRenderer, "<", x - 7, y, 0xFFFFFF);
        if (curve < 0) ctx.drawCenteredTextWithShadow(client.textRenderer, ">", x + BAR_WIDTH + 7, y, 0xFFFFFF);
    }
}
