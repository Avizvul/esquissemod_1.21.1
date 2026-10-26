package net.avizvul.esquissemod.network;

import net.avizvul.esquissemod.EsquisseMod;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** units.get(colorId) = сколько единиц краски потрачено с прошлой отправки. */
public record PaintUsagePayload(boolean isMarker, List<Integer> units) implements CustomPacketPayload {

    public static final Type<PaintUsagePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EsquisseMod.MOD_ID, "paint_usage"));

    public static final StreamCodec<io.netty.buffer.ByteBuf, PaintUsagePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, PaintUsagePayload::isMarker,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(16)), PaintUsagePayload::units,
            PaintUsagePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}