package net.avizvul.esquissemod.menu;

import net.avizvul.esquissemod.item.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

public class AlbumMenu extends AbstractContainerMenu {

    public static final int PAGES = 16;

    // Макет инвентаря игрока (должен совпадать с текстурой album_gui.png)
    private static final int INV_X = 8, INV_Y = 142, HOTBAR_Y = 200;

    private final SimpleContainer container;
    private final ItemStack albumStack;

    // Конструктор для клиента
    public AlbumMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ItemStack.EMPTY);
    }

    // Конструктор для сервера
    public AlbumMenu(int containerId, Inventory playerInventory, ItemStack albumStack) {
        super(ModMenuTypes.ALBUM_MENU.get(), containerId);
        this.albumStack = albumStack;

        this.container = new SimpleContainer(PAGES) {
            @Override
            public void setChanged() {
                super.setChanged();
                saveToItem();
            }
        };

        if (!albumStack.isEmpty() && albumStack.has(DataComponents.CONTAINER)) {
            albumStack.get(DataComponents.CONTAINER).copyInto(this.container.getItems());
        }

        // 16 страниц. Слоты спрятаны за экраном: рисунки рисует AlbumScreen, а клики он перенаправляет сам
        for (int i = 0; i < PAGES; i++) {
            this.addSlot(new Slot(this.container, i, -2000, -2000) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.is(ModItems.SKETCHED_PAGE.get());
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, INV_X + col * 18, HOTBAR_Y));
        }
    }

    private void saveToItem() {
        if (!this.albumStack.isEmpty()) {
            this.albumStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.container.getItems()));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack inSlot = slot.getItem();
            result = inSlot.copy();

            if (index < PAGES) {
                if (!this.moveItemStackTo(inSlot, PAGES, this.slots.size(), true)) return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(inSlot, 0, PAGES, false)) {
                return ItemStack.EMPTY;
            }

            if (inSlot.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getMainHandItem() == this.albumStack
                || player.getOffhandItem() == this.albumStack
                || this.albumStack.isEmpty();
    }
}