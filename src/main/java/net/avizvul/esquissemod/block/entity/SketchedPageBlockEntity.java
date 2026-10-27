package net.avizvul.esquissemod.block.entity;

import net.avizvul.esquissemod.component.ModDataComponents;
import net.avizvul.esquissemod.component.SketchData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SketchedPageBlockEntity extends BlockEntity {

    private String author = null;
    private boolean signed = false;
    private boolean copied = false;
    private Component customName = null;

    /** Забирает подпись, автора и пометку копии с предмета при размещении. */
    public void readMetaFrom(ItemStack stack) {
        this.author = stack.get(ModDataComponents.PAGE_AUTHOR.get());
        this.signed = stack.getOrDefault(ModDataComponents.PAGE_SIGNED.get(), false);
        this.copied = stack.getOrDefault(ModDataComponents.PAGE_COPIED.get(), false);
        this.customName = stack.get(DataComponents.CUSTOM_NAME);
        setChanged();
    }

    /** Возвращает всё обратно на предмет при снятии со стены. */
    public void writeMetaTo(ItemStack stack) {
        if (this.author != null) stack.set(ModDataComponents.PAGE_AUTHOR.get(), this.author);
        if (this.signed) stack.set(ModDataComponents.PAGE_SIGNED.get(), true);
        if (this.copied) stack.set(ModDataComponents.PAGE_COPIED.get(), true);
        if (this.customName != null) stack.set(DataComponents.CUSTOM_NAME, this.customName);
    }

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
        if (this.author != null) tag.putString("Author", this.author);
        tag.putBoolean("Signed", this.signed);
        tag.putBoolean("Copied", this.copied);
        if (this.customName != null) {
            tag.putString("PageName", Component.Serializer.toJson(this.customName, registries));
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
        this.author = tag.contains("Author") ? tag.getString("Author") : null;
        this.signed = tag.getBoolean("Signed");
        this.copied = tag.getBoolean("Copied");
        this.customName = tag.contains("PageName")
                ? Component.Serializer.fromJson(tag.getString("PageName"), registries) : null;
    }
}
