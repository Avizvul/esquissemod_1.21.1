package net.avizvul.esquissemod.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.avizvul.esquissemod.EsquisseMod;
import net.avizvul.esquissemod.item.ModItems;
import net.avizvul.esquissemod.menu.PrinterMenu;
import net.avizvul.esquissemod.network.PrinterActionPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class PrinterScreen extends AbstractContainerScreen<PrinterMenu> {

    private static final ResourceLocation TEXTURE_GUI = ResourceLocation.fromNamespaceAndPath(EsquisseMod.MOD_ID, "textures/gui/printer_gui.png");
    private static final ResourceLocation TEXTURE_BUTTON_PRESS = ResourceLocation.fromNamespaceAndPath(EsquisseMod.MOD_ID, "textures/gui/printer_button_press.png");
    private static final ResourceLocation TEXTURE_BUTTON_HL = ResourceLocation.fromNamespaceAndPath(EsquisseMod.MOD_ID, "textures/gui/printer_button_hl.png");

    private static final ResourceLocation TEXTURE_CYAN = ResourceLocation.fromNamespaceAndPath(EsquisseMod.MOD_ID, "textures/gui/printer_cyan.png");
    private static final ResourceLocation TEXTURE_MAGENTA = ResourceLocation.fromNamespaceAndPath(EsquisseMod.MOD_ID, "textures/gui/printer_magenta.png");
    private static final ResourceLocation TEXTURE_YELLOW = ResourceLocation.fromNamespaceAndPath(EsquisseMod.MOD_ID, "textures/gui/printer_yellow.png");
    private static final ResourceLocation TEXTURE_BLACK = ResourceLocation.fromNamespaceAndPath(EsquisseMod.MOD_ID, "textures/gui/printer_black.png");
    private static final ResourceLocation TEXTURE_GLOWSTONE = ResourceLocation.fromNamespaceAndPath(EsquisseMod.MOD_ID, "textures/gui/printer_glowstone.png");
    private static final ResourceLocation TEXTURE_ESSENCE = ResourceLocation.fromNamespaceAndPath(EsquisseMod.MOD_ID, "textures/gui/printer_essence.png");

    private long buttonPressTime = 0;

    public PrinterScreen(PrinterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 192; // Высота окна под новые координаты инвентаря
        this.inventoryLabelY = 96; // Надпись "Инвентарь" прямо над слотами
        this.titleLabelX = 8;
        this.titleLabelY = 5;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        RenderSystem.enableBlend();

        guiGraphics.blit(TEXTURE_GUI, x, y, 0, 0, this.imageWidth, this.imageHeight);

        if (!this.menu.getSlot(3).getItem().isEmpty()) {
            guiGraphics.blit(TEXTURE_CYAN, x, y, 0, 0, this.imageWidth, this.imageHeight);
        }
        if (!this.menu.getSlot(4).getItem().isEmpty()) {
            guiGraphics.blit(TEXTURE_MAGENTA, x, y, 0, 0, this.imageWidth, this.imageHeight);
        }
        if (!this.menu.getSlot(5).getItem().isEmpty()) {
            guiGraphics.blit(TEXTURE_YELLOW, x, y, 0, 0, this.imageWidth, this.imageHeight);
        }
        if (!this.menu.getSlot(6).getItem().isEmpty()) {
            guiGraphics.blit(TEXTURE_BLACK, x, y, 0, 0, this.imageWidth, this.imageHeight);
        }

        ItemStack catalystStack = this.menu.getSlot(7).getItem();
        if (!catalystStack.isEmpty()) {
            if (catalystStack.is(ModItems.WARP_ESSENCE.get())) {
                guiGraphics.blit(TEXTURE_ESSENCE, x, y, 0, 0, this.imageWidth, this.imageHeight);
            } else {
                guiGraphics.blit(TEXTURE_GLOWSTONE, x, y, 0, 0, this.imageWidth, this.imageHeight);
            }
        }

        int btnX = x + 113;
        int btnY = y + 34;
        int btnWidth = 18;
        int btnHeight = 47;

        boolean isPressed = (System.currentTimeMillis() - this.buttonPressTime) < 500;
        boolean isHovered = mouseX >= btnX && mouseX < btnX + btnWidth && mouseY >= btnY && mouseY < btnY + btnHeight;

        if (isPressed) {
            guiGraphics.blit(TEXTURE_BUTTON_PRESS, x, y, 0, 0, this.imageWidth, this.imageHeight);
        } else if (isHovered) {
            guiGraphics.blit(TEXTURE_BUTTON_HL, x, y, 0, 0, this.imageWidth, this.imageHeight);
        }

        RenderSystem.disableBlend();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int x = (this.width - this.imageWidth) / 2;
            int y = (this.height - this.imageHeight) / 2;

            int btnX = x + 113;
            int btnY = y + 34;
            int btnWidth = 18;
            int btnHeight = 47;

            if (mouseX >= btnX && mouseX < btnX + btnWidth && mouseY >= btnY && mouseY < btnY + btnHeight) {
                this.buttonPressTime = System.currentTimeMillis();
                PacketDistributor.sendToServer(new PrinterActionPayload(this.menu.getPos()));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
