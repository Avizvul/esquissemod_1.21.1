package net.avizvul.esquissemod.network;

import net.avizvul.esquissemod.EsquisseMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PrinterActionPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<PrinterActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(EsquisseMod.MOD_ID, "printer_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PrinterActionPayload> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, PrinterActionPayload::pos, PrinterActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}