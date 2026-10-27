package net.avizvul.esquissemod.client.screen;

import net.avizvul.esquissemod.network.SealStampPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class SealStampScreen extends Screen {

    private static final int MAX_TITLE_LENGTH = 32;

    private final Screen parent;
    private final List<Integer> rows;
    private final InteractionHand hand;
    private EditBox titleBox;

    public SealStampScreen(Screen parent, List<Integer> rows, InteractionHand hand) {
        super(Component.translatable("gui.esquissemod.stamp.seal.title"));
        this.parent = parent;
        this.rows = rows;
        this.hand = hand;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        this.titleBox = new EditBox(this.font, cx - 100, cy - 10, 200, 20,
                Component.translatable("gui.esquissemod.stamp.seal.field"));
        this.titleBox.setMaxLength(MAX_TITLE_LENGTH);
        this.titleBox.setHint(Component.translatable("gui.esquissemod.stamp.seal.hint"));

        this.addRenderableWidget(this.titleBox);
        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.esquissemod.sign.confirm"), b -> confirm())
                .bounds(cx - 100, cy + 18, 98, 20).build());
        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.esquissemod.sign.cancel"), b -> onClose())
                .bounds(cx + 2, cy + 18, 98, 20).build());

        this.setInitialFocus(this.titleBox);
    }

    private void confirm() {
        PacketDistributor.sendToServer(new SealStampPayload(
                this.rows, this.titleBox.getValue().trim(), this.hand == InteractionHand.OFF_HAND));
        this.minecraft.setScreen(null);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            confirm();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int cx = this.width / 2;
        int cy = this.height / 2;
        g.drawCenteredString(this.font, this.title, cx, cy - 56, 0xFFFFFF);
        g.drawWordWrap(this.font, Component.translatable("gui.esquissemod.stamp.seal.warning"),
                cx - 100, cy - 44, 200, 0xFFAA55);
    }
}