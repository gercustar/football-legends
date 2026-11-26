package ru.footballmod.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import ru.footballmod.RonaldoMessiMod;

/**
 * Клиент → сервер: действие игрока с мячом.
 * Силу удара клиент НЕ передаёт: сервер сам считает время между CHARGE_START и SHOT.
 * curve: 1 — закрутка влево, -1 — вправо, 0 — без закрутки.
 */
public record BallActionPayload(int action, int curve) implements CustomPayload {
    public static final int CHARGE_START = 0;
    public static final int CHARGE_CANCEL = 1;
    public static final int SHOT = 2;
    public static final int PASS = 3;

    public static final Id<BallActionPayload> ID = new Id<>(RonaldoMessiMod.id("ball_action"));
    public static final PacketCodec<RegistryByteBuf, BallActionPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, BallActionPayload::action,
            PacketCodecs.VAR_INT, BallActionPayload::curve,
            BallActionPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
