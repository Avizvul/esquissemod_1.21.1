package net.avizvul.esquissemod.menu;

import net.avizvul.esquissemod.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class PrinterMenu extends AbstractContainerMenu {

    private final IItemHandler inventory;
    private final BlockPos pos;

    public PrinterMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new ItemStackHandler(8), BlockPos.ZERO);
    }

    public PrinterMenu(int containerId, Inventory playerInventory, IItemHandler handler, BlockPos pos) {
        super(ModMenuTypes.PRINTER_MENU.get(), containerId);
        this.inventory = handler;
        this.pos = pos;

        // Слот 0: Рисунок (Оригинал) — Рамка: (79, 34)
        this.addSlot(new SlotItemHandler(handler, 0, 80, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.SKETCHED_PAGE.get());
            }
        });

        // Слот 1: Чистые листы (Бумага) — Рамка: (79, 14)
        this.addSlot(new SlotItemHandler(handler, 1, 80, 15) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.EMPTY_PAGE.get()) || stack.is(Items.PAPER);
            }
        });

        // Слот 2: Выходной результат — Рамка: (79, 63)
        this.addSlot(new SlotItemHandler(handler, 2, 80, 64) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Слот 3: Cyan — Рамка: (41, 6)
        this.addSlot(new SlotItemHandler(handler, 3, 42, 7) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof DyeItem dye && (dye.getDyeColor() == DyeColor.CYAN || dye.getDyeColor() == DyeColor.LIGHT_BLUE);
            }
        });

        // Слот 4: Magenta — Рамка: (41, 24)
        this.addSlot(new SlotItemHandler(handler, 4, 42, 25) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof DyeItem dye && dye.getDyeColor() == DyeColor.MAGENTA;
            }
        });

        // Слот 5: Yellow — Рамка: (41, 42)
        this.addSlot(new SlotItemHandler(handler, 5, 42, 43) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof DyeItem dye && dye.getDyeColor() == DyeColor.YELLOW;
            }
        });

        // Слот 6: Black — Рамка: (41, 60)
        this.addSlot(new SlotItemHandler(handler, 6, 42, 61) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof DyeItem dye && dye.getDyeColor() == DyeColor.BLACK;
            }
        });

        // Слот 7: Катализатор (Варп-эссенция / Глоустоун) — Рамка: (113, 6)
        this.addSlot(new SlotItemHandler(handler, 7, 114, 7) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.WARP_ESSENCE.get()) || stack.is(Items.GLOWSTONE_DUST);
            }
        });

        // Инвентарь игрока (3x9) — Рамка начинается с (7, 108)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 109 + row * 18));
            }
        }

        // Хотбар игрока (1x9) — Y = 167 (109 + 58)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 167));
        }
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public IItemHandler getInventory() {
        return this.inventory;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();
            if (index < 8) {
                if (!this.moveItemStackTo(stackInSlot, 8, this.slots.size(), true)) return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(stackInSlot, 0, 8, false)) {
                return ItemStack.EMPTY;
            }
            if (stackInSlot.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
