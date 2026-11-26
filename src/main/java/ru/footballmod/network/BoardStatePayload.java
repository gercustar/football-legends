package ru.footballmod.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;
import ru.footballmod.RonaldoMessiMod;

/** Сервер → клиент: открыть (или обновить) меню табло. myTeam: 0 — зритель, 1 — Реал, 2 — Барса. */
public record BoardStatePayload(BlockPos pos, int home, int away, int myTeam, boolean running, String score) implements CustomPayload {
    public static final Id<BoardStatePayload> ID = new Id<>(RonaldoMessiMod.id("board_state"));
    public static final PacketCodec<RegistryByteBuf, BoardStatePayload> CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC, BoardStatePayload::pos,
            PacketCodecs.VAR_INT, BoardStatePayload::home,
            PacketCodecs.VAR_INT, BoardStatePayload::away,
            PacketCodecs.VAR_INT, BoardStatePayload::myTeam,
            PacketCodecs.BOOL, BoardStatePayload::running,
            PacketCodecs.STRING, BoardStatePayload::score,
            BoardStatePayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
