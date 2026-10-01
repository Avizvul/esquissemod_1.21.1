package net.avizvul.esquissemod.network;

import net.avizvul.esquissemod.EsquisseMod;
import net.avizvul.esquissemod.component.SketchData;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TearPagePayload(int pageIndex, SketchData sketchData) implements CustomPacketPayload {

    public static final Type<TearPagePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(EsquisseMod.MOD_ID, "tear_page"));

    public static final StreamCodec<io.netty.buffer.ByteBuf, TearPagePayload> STREAM_CODEC = StreamCodec.composite(
            net.minecraft.network.codec.ByteBufCodecs.INT, TearPagePayload::pageIndex,
            SketchData.STREAM_CODEC, TearPagePayload::sketchData,
            TearPagePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
