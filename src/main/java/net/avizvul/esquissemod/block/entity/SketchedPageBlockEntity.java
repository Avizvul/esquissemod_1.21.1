package net.avizvul.esquissemod.block.entity;

import net.avizvul.esquissemod.component.SketchData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SketchedPageBlockEntity extends BlockEntity {

    private SketchData sketchData = SketchData.fromArray(new int[][]{});
    private int rotation = 0;

    public SketchedPageBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SKETCHED_PAGE_BE.get(), pos, state);
    }

    public void setSketchData(SketchData data) {
        this.sketchData = data;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public SketchData getSketchData() {
        return sketchData;
    }

    public int getRotation() {
        return this.rotation;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // === ОБЯЗАТЕЛЬНЫЕ МЕТОДЫ ДЛЯ СИНХРОНИЗАЦИИ С КЛИЕНТОМ ===
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Rotation", this.rotation);
        if (this.sketchData != null) {
            SketchData.CODEC.encodeStart(NbtOps.INSTANCE, this.sketchData)
                    .resultOrPartial()
                    .ifPresent(encoded -> tag.put("SketchData", encoded));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.rotation = tag.getInt("Rotation");
        if (tag.contains("SketchData")) {
            SketchData.CODEC.parse(NbtOps.INSTANCE, tag.get("SketchData"))
                    .resultOrPartial()
                    .ifPresent(decoded -> this.sketchData = decoded);
        }
    }
}
