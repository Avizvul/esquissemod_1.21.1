package net.avizvul.esquissemod.network;

import net.avizvul.esquissemod.EsquisseMod;
import net.avizvul.esquissemod.component.SketchData;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SketchbookSavePayload(int pageIndex, SketchData sketchData,
                                    int pencilPixels, int eraserPixels,
                                    int kneadedPixels, int smudgePixels) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SketchbookSavePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(EsquisseMod.MOD_ID, "sketchbook_save"));

    public static final StreamCodec<io.netty.buffer.ByteBuf, SketchbookSavePayload> STREAM_CODEC =
            StreamCodec.composite(
                    net.minecraft.network.codec.ByteBufCodecs.INT, SketchbookSavePayload::pageIndex,
                    SketchData.STREAM_CODEC, SketchbookSavePayload::sketchData,
                    net.minecraft.network.codec.ByteBufCodecs.INT, SketchbookSavePayload::pencilPixels,
                    net.minecraft.network.codec.ByteBufCodecs.INT, SketchbookSavePayload::eraserPixels,
                    net.minecraft.network.codec.ByteBufCodecs.INT, SketchbookSavePayload::kneadedPixels,
                    net.minecraft.network.codec.ByteBufCodecs.INT, SketchbookSavePayload::smudgePixels,
                    SketchbookSavePayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}