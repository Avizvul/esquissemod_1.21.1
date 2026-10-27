package net.avizvul.esquissemod.network;

import net.avizvul.esquissemod.EsquisseMod;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record SealStampPayload(List<Integer> rows, String name, boolean offHand) implements CustomPacketPayload {

    public static final Type<SealStampPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EsquisseMod.MOD_ID, "seal_stamp"));

    public static final StreamCodec<io.netty.buffer.ByteBuf, SealStampPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT.apply(ByteBufCodecs.list(32)), SealStampPayload::rows,
            ByteBufCodecs.stringUtf8(64), SealStampPayload::name,
            ByteBufCodecs.BOOL, SealStampPayload::offHand,
            SealStampPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}