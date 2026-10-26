package net.avizvul.esquissemod.item.custom;

import net.avizvul.esquissemod.component.ModDataComponents;
import net.avizvul.esquissemod.item.custom.base.DrawingToolItem;
import net.avizvul.esquissemod.util.ToolWear;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ColorPencilItem extends DrawingToolItem {

    public ColorPencilItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (action == ClickAction.SECONDARY && slot.allowModification(player) && other.getItem() instanceof DyeItem dye) {
            int colorId = dye.getDyeColor().getId();
            List<Integer> colors = new ArrayList<>(stack.getOrDefault(ModDataComponents.STORED_COLORS.get(), new ArrayList<>()));
            boolean known = colors.contains(colorId);

            // Цвет уже полный, заправлять нечего
            if (known && ToolWear.getPaintLevel(stack, colorId) >= ToolWear.MAX_PAINT) {
                return super.overrideOtherStackedOnMe(stack, other, slot, action, player, access);
            }

            if (!known) {
                if (colors.size() >= ToolWear.COLOR_COUNT) {
                    return super.overrideOtherStackedOnMe(stack, other, slot, action, player, access);
                }
                colors.add(colorId);
                stack.set(ModDataComponents.STORED_COLORS.get(), colors);
            }

            ToolWear.setPaintLevel(stack, colorId, ToolWear.MAX_PAINT);
            stack.set(ModDataComponents.ACTIVE_COLOR_INDEX.get(), colors.indexOf(colorId));

            other.shrink(1);
            player.playSound(SoundEvents.DYE_USE, 1.0f, 1.0f);
            return true;
        }
        return super.overrideOtherStackedOnMe(stack, other, slot, action, player, access);
    }

    // --- Полоска вместо прочности: остаток краски активного цвета ---
    @Override
    public boolean isBarVisible(ItemStack stack) {
        int id = ToolWear.getActiveColorId(stack);
        return id >= 0 && ToolWear.getPaintLevel(stack, id) < ToolWear.MAX_PAINT;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int id = ToolWear.getActiveColorId(stack);
        if (id < 0) return 0;
        return Math.round(13.0f * ToolWear.getPaintLevel(stack, id) / ToolWear.MAX_PAINT);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        int id = ToolWear.getActiveColorId(stack);
        if (id < 0) return 0xFFDDDDDD;
        return net.minecraft.world.item.DyeColor.byId(id).getTextureDiffuseColor() | 0xFF000000;
    }

    @Override
    public net.minecraft.network.chat.Component getName(ItemStack stack) {
        java.util.List<Integer> colors = stack.getOrDefault(ModDataComponents.STORED_COLORS.get(), new java.util.ArrayList<>());

        // Если список цветов пуст, возвращаем ключ перевода для пустого карандаша
        if (colors.isEmpty()) {
            return net.minecraft.network.chat.Component.translatable("item.esquissemod.empty_color_pencil");
        }

        // Иначе возвращаем стандартное имя (item.esquissemod.color_pencil)
        return super.getName(stack);
    }


    @Override
    public net.minecraft.resources.ResourceLocation getGuiTexture() {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(net.avizvul.esquissemod.EsquisseMod.MOD_ID, "textures/gui/button_color_pencil.png");
    }
}