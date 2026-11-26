package ru.footballmod.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import ru.footballmod.RonaldoMessiMod;

/** Сервер → клиент: открыть меню тренера с лучшими результатами игрока. */
public record AcademyOpenPayload(int coachId, int shooting, int dribbleTicks, int penalties, boolean busy) implements CustomPayload {
    public static final Id<AcademyOpenPayload> ID = new Id<>(RonaldoMessiMod.id("academy_open"));
    public static final PacketCodec<RegistryByteBuf, AcademyOpenPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, AcademyOpenPayload::coachId,
            PacketCodecs.VAR_INT, AcademyOpenPayload::shooting,
            PacketCodecs.VAR_INT, AcademyOpenPayload::dribbleTicks,
            PacketCodecs.VAR_INT, AcademyOpenPayload::penalties,
            PacketCodecs.BOOL, AcademyOpenPayload::busy,
            AcademyOpenPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
