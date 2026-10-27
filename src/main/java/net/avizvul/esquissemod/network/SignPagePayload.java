package net.avizvul.esquissemod.network;

import net.avizvul.esquissemod.EsquisseMod;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SignPagePayload(String title, boolean offHand) implements CustomPacketPayload {

    public static final Type<SignPagePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EsquisseMod.MOD_ID, "sign_page"));

    public static final StreamCodec<io.netty.buffer.ByteBuf, SignPagePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), SignPagePayload::title,
            ByteBufCodecs.BOOL, SignPagePayload::offHand,
            SignPagePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}