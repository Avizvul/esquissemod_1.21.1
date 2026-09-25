package net.avizvul.esquissemod.client.screen;

import net.avizvul.esquissemod.EsquisseMod;
import net.avizvul.esquissemod.menu.PrinterMenu;
import net.avizvul.esquissemod.network.PrinterActionPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class PrinterScreen extends AbstractContainerScreen{
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(EsquisseMod.MOD_ID, "textures/gui/printer_gui.png");

    public PrinterScreen(PrinterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 158;
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelX = 8;
        this.titleLabelY = 5;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // ЛКМ
            int x = (this.width - this.imageWidth) / 2;
            int y = (this.height - this.imageHeight) / 2;

            // Зона стрелки-кнопки (находится в промежутке между X=80 и X=116)
            int btnX = x + 82;
            int btnY = y + 15;
            int btnWidth = 30;
            int btnHeight = 18;

            if (mouseX >= btnX && mouseX < btnX + btnWidth && mouseY >= btnY && mouseY < btnY + btnHeight) {
                // Отправляем сигнал печати/экспорта на сервер
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