package net.avizvul.esquissemod.network;

import net.avizvul.esquissemod.EsquisseMod;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Лёгкий пакет износа: отправляется при отпускании кнопки мыши, без данных рисунка. */
public record ToolUsagePayload(int pencilPixels, int eraserPixels, int kneadedPixels, int smudgePixels) implements CustomPacketPayload {

    public static final Type<ToolUsagePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EsquisseMod.MOD_ID, "tool_usage"));

    public static final StreamCodec<io.netty.buffer.ByteBuf, ToolUsagePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ToolUsagePayload::pencilPixels,
            ByteBufCodecs.VAR_INT, ToolUsagePayload::eraserPixels,
            ByteBufCodecs.VAR_INT, ToolUsagePayload::kneadedPixels,
            ByteBufCodecs.VAR_INT, ToolUsagePayload::smudgePixels,
            ToolUsagePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}