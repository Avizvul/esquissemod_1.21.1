package net.avizvul.esquissemod.client.screen;

import net.avizvul.esquissemod.EsquisseMod;
import net.avizvul.esquissemod.client.ClientRenderUtils;
import net.avizvul.esquissemod.component.ModDataComponents;
import net.avizvul.esquissemod.component.SketchData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class SketchedPageScreen extends Screen {

    private static final ResourceLocation PAGE_TEX =
            ResourceLocation.fromNamespaceAndPath(EsquisseMod.MOD_ID, "textures/gui/sketched_page_gui.png");

    private final int canvasWidth = 63;
    private final int canvasHeight = 96;
    private final int scale = 3;

    private final SketchData data;
    private final InteractionHand hand;
    private final boolean signed;

    public SketchedPageScreen(ItemStack stack, InteractionHand hand) {
        super(Component.literal("Sketched Page"));
        this.data = stack.get(ModDataComponents.PAGE_DATA.get());
        this.hand = hand;
        this.signed = stack.getOrDefault(ModDataComponents.PAGE_SIGNED.get(), false);
    }

    @Override
    protected void init() {
        super.init();
        if (this.signed) return;

        int drawWidth = this.canvasWidth * this.scale;
        int drawHeight = this.canvasHeight * this.scale;
        int renderX = (this.width - drawWidth) / 2;
        int renderY = (this.height - drawHeight) / 2;

        int btnWidth = 80;
        int btnX = Math.min(renderX + drawWidth + 10, this.width - btnWidth - 4);
        int btnY = renderY + drawHeight / 2 - 10;

        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.esquissemod.sign"),
                        b -> this.minecraft.setScreen(new SignPageScreen(this, this.hand)))
                .bounds(btnX, btnY, btnWidth, 20)
                .build());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Пусто, чтобы не было стандартного затемнения
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int drawWidth = this.canvasWidth * this.scale;
        int drawHeight = this.canvasHeight * this.scale;

        int renderX = (this.width - drawWidth) / 2;
        int renderY = (this.height - drawHeight) / 2;

        guiGraphics.blit(PAGE_TEX, renderX, renderY, drawWidth, drawHeight,
                0.0f, 0.0f, this.canvasWidth, this.canvasHeight, this.canvasWidth, this.canvasHeight);

        if (this.data != null && !this.data.isEmpty()) {
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            ClientRenderUtils.renderCachedSketch(guiGraphics, this.data, renderX, renderY, drawWidth, drawHeight);
            com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}