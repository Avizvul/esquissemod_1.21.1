package net.avizvul.esquissemod.item.custom;

import net.avizvul.esquissemod.EsquisseMod;
import net.avizvul.esquissemod.client.ClientHooks;
import net.avizvul.esquissemod.client.tooltip.StampTooltipData;
import net.avizvul.esquissemod.component.ModDataComponents;
import net.avizvul.esquissemod.item.custom.base.DrawingToolItem;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class StampItem extends DrawingToolItem {

    private final int size;

    public StampItem(Properties properties, int size) {
        super(properties);
        this.size = size;
    }

    public int getSize() {
        return this.size;
    }

    public static boolean isSealed(ItemStack stack) {
        return stack.has(ModDataComponents.STAMP_BITS.get());
    }

    @Override
    public ResourceLocation getGuiTexture() {
        return ResourceLocation.fromNamespaceAndPath(EsquisseMod.MOD_ID, "textures/gui/button_stamp_base.png");
    }

    @Override
    public Component getName(ItemStack stack) {
        if (!isSealed(stack)) {
            return Component.translatable(this.size <= 16
                    ? "item.esquissemod.empty_stamp_small"
                    : "item.esquissemod.empty_stamp_large");
        }
        return super.getName(stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isSealed(stack)) return InteractionResultHolder.pass(stack);
        if (level.isClientSide()) {
            ClientHooks.openStampCarveScreen(stack, hand);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        List<Integer> rows = stack.get(ModDataComponents.STAMP_BITS.get());
        if (rows == null || rows.isEmpty()) return Optional.empty();
        return Optional.of(new StampTooltipData(rows));
    }
}