package net.avizvul.esquissemod.item.custom;

import net.avizvul.esquissemod.item.ModItems;
import net.avizvul.esquissemod.menu.AlbumMenu;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

public class AlbumItem extends Item {

    public AlbumItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            player.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, p) -> new AlbumMenu(containerId, inventory, stack),
                    Component.translatable("item.esquissemod.album")
            ));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** ПКМ по альбому в инвентаре с рисунком на курсоре вставляет его на первую свободную страницу. */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (action == ClickAction.SECONDARY && slot.allowModification(player) && other.is(ModItems.SKETCHED_PAGE.get())) {
            NonNullList<ItemStack> pages = NonNullList.withSize(AlbumMenu.PAGES, ItemStack.EMPTY);
            ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
            if (contents != null) contents.copyInto(pages);

            for (int i = 0; i < pages.size(); i++) {
                if (pages.get(i).isEmpty()) {
                    pages.set(i, other.split(1));
                    stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(pages));
                    player.playSound(SoundEvents.BOOK_PAGE_TURN, 1.0f, 1.0f);
                    return true;
                }
            }
        }
        return super.overrideOtherStackedOnMe(stack, other, slot, action, player, access);
    }
}