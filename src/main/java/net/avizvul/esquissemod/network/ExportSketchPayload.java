package net.avizvul.esquissemod.network;

import net.avizvul.esquissemod.EsquisseMod;
import net.avizvul.esquissemod.component.SketchData;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ExportSketchPayload(SketchData sketchData) implements CustomPacketPayload {
    public static final Type<ExportSketchPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(EsquisseMod.MOD_ID, "export_sketch"));
    public static final StreamCodec<io.netty.buffer.ByteBuf, ExportSketchPayload> STREAM_CODEC = StreamCodec.composite(SketchData.STREAM_CODEC, ExportSketchPayload::sketchData, ExportSketchPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}