package ru.footballmod.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import ru.footballmod.RonaldoMessiMod;

/** Клиент → сервер: начать (или отменить) задание тренера. drill: 0 — точность, 1 — дриблинг, 2 — пенальти. */
public record AcademyStartPayload(int coachId, int drill) implements CustomPayload {
    public static final Id<AcademyStartPayload> ID = new Id<>(RonaldoMessiMod.id("academy_start"));
    public static final PacketCodec<RegistryByteBuf, AcademyStartPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, AcademyStartPayload::coachId,
            PacketCodecs.VAR_INT, AcademyStartPayload::drill,
            AcademyStartPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
