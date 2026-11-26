package ru.footballmod.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;
import ru.footballmod.RonaldoMessiMod;

/** Клиент → сервер: кнопка в меню табло. */
public record BoardActionPayload(BlockPos pos, int action) implements CustomPayload {
    public static final int JOIN_HOME = 0, JOIN_AWAY = 1, SPECTATE = 2, START_3 = 3, START_5 = 4, START_10 = 5, STOP = 6, PENALTIES = 7;

    public static final Id<BoardActionPayload> ID = new Id<>(RonaldoMessiMod.id("board_action"));
    public static final PacketCodec<RegistryByteBuf, BoardActionPayload> CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC, BoardActionPayload::pos,
            PacketCodecs.VAR_INT, BoardActionPayload::action,
            BoardActionPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
