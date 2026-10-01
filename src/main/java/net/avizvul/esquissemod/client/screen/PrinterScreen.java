package net.avizvul.esquissemod.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.avizvul.esquissemod.EsquisseMod;
import net.avizvul.esquissemod.item.ModItems;
import net.avizvul.esquissemod.menu.PrinterMenu;
import net.avizvul.esquissemod.network.PrinterActionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
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
    private boolean wasButtonPressed = false; // Флаг для отслеживания отжатия кнопки

    public PrinterScreen(PrinterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 192;

        this.titleLabelX = 8;
        this.titleLabelY = 5;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 98;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        RenderSystem.enableBlend();

        // 1. Основной фон GUI
        guiGraphics.blit(TEXTURE_GUI, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 2. Отображение красителей CMYK
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

        // 3. Отображение катализатора
        ItemStack catalystStack = this.menu.getSlot(7).getItem();
        if (!catalystStack.isEmpty()) {
            if (catalystStack.is(ModItems.WARP_ESSENCE.get())) {
                guiGraphics.blit(TEXTURE_ESSENCE, x, y, 0, 0, this.imageWidth, this.imageHeight);
            } else {
                guiGraphics.blit(TEXTURE_GLOWSTONE, x, y, 0, 0, this.imageWidth, this.imageHeight);
            }
        }

        // 4. Кнопка печати (113, 34 | 14x17)
        int btnX = x + 113;
        int btnY = y + 34;
        int btnWidth = 14;
        int btnHeight = 17;

        boolean isPressed = (System.currentTimeMillis() - this.buttonPressTime) < 500;
        boolean isHovered = mouseX >= btnX && mouseX < btnX + btnWidth && mouseY >= btnY && mouseY < btnY + btnHeight;

        // Звук отжатия кнопки (когда 500 мс прошли и кнопка подпрыгивает назад)
        if (this.wasButtonPressed && !isPressed) {
            this.wasButtonPressed = false;
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.STONE_BUTTON_CLICK_OFF, 1.0F)
            );
        }

        if (isPressed) {
            guiGraphics.blit(TEXTURE_BUTTON_PRESS, x, y, 0, 0, this.imageWidth, this.imageHeight);
        } else if (isHovered) {
            guiGraphics.blit(TEXTURE_BUTTON_HL, x, y, 0, 0, this.imageWidth, this.imageHeight);
        }

        RenderSystem.disableBlend();
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int x = (this.width - this.imageWidth) / 2;
            int y = (this.height - this.imageHeight) / 2;

            int btnX = x + 113;
            int btnY = y + 34;
            int btnWidth = 14;
            int btnHeight = 17;

            if (mouseX >= btnX && mouseX < btnX + btnWidth && mouseY >= btnY && mouseY < btnY + btnHeight) {
                this.buttonPressTime = System.currentTimeMillis();
                this.wasButtonPressed = true;

                // Звук утапливания кнопки при клике
                Minecraft.getInstance().getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.STONE_BUTTON_CLICK_ON, 1.0F)
                );

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
