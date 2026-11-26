package ru.footballmod.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import ru.footballmod.RonaldoMessiMod;

/** Клиент → сервер: игрок нажал способность. side: 1 — влево (A), -1 — вправо (D), 0 — без направления. */
public record AbilityPayload(int ability, int side) implements CustomPayload {
    public static final int FEINT = 0;
    public static final int DASH = 1;
    public static final int CANNON = 2;
    public static final int COUNT = 3;

    public static final Id<AbilityPayload> ID = new Id<>(RonaldoMessiMod.id("ability"));
    public static final PacketCodec<RegistryByteBuf, AbilityPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, AbilityPayload::ability,
            PacketCodecs.VAR_INT, AbilityPayload::side,
            AbilityPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
