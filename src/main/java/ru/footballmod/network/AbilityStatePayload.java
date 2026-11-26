package ru.footballmod.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import ru.footballmod.RonaldoMessiMod;

/** Сервер → клиент: сколько тиков осталось до готовности финта, рывка, пушки и сколько ещё заряжена пушка. */
public record AbilityStatePayload(int feint, int dash, int cannon, int cannonActive) implements CustomPayload {
    public static final Id<AbilityStatePayload> ID = new Id<>(RonaldoMessiMod.id("ability_state"));
    public static final PacketCodec<RegistryByteBuf, AbilityStatePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, AbilityStatePayload::feint,
            PacketCodecs.VAR_INT, AbilityStatePayload::dash,
            PacketCodecs.VAR_INT, AbilityStatePayload::cannon,
            PacketCodecs.VAR_INT, AbilityStatePayload::cannonActive,
            AbilityStatePayload::new);

    public int cooldown(int ability) {
        return switch (ability) {
            case AbilityPayload.FEINT -> feint;
            case AbilityPayload.DASH -> dash;
            default -> cannon;
        };
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
