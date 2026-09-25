package net.avizvul.esquissemod.network;

import net.avizvul.esquissemod.EsquisseMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PrinterActionPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type
}