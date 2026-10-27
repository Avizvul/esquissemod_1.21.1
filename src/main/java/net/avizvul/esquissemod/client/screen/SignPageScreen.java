package net.avizvul.esquissemod.client.screen;

import net.avizvul.esquissemod.network.SignPagePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public class SignPageScreen extends Screen {

    private static final int MAX_TITLE_LENGTH = 32;

    private final Screen parent;
    private final InteractionHand hand;
    private EditBox titleBox;
    private Button confirmButton;

    public SignPageScreen(Screen parent, InteractionHand hand) {
        super(Component.translatable("gui.esquissemod.sign.title"));
        this.parent = parent;
        this.hand = hand;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        this.confirmButton = Button.builder(
                        Component.translatable("gui.esquissemod.sign.confirm"), b -> confirm())
                .bounds(cx - 100, cy + 18, 98, 20)
                .build();
        this.confirmButton.active = false;

        this.titleBox = new EditBox(this.font, cx - 100, cy - 10, 200, 20,
                Component.translatable("gui.esquissemod.sign.field"));
        this.titleBox.setMaxLength(MAX_TITLE_LENGTH);
        this.titleBox.setHint(Component.translatable("gui.esquissemod.sign.hint"));
        this.titleBox.setResponder(text -> this.confirmButton.active = !text.isBlank());

        this.addRenderableWidget(this.titleBox);
        this.addRenderableWidget(this.confirmButton);
        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.esquissemod.sign.cancel"), b -> onClose())
                .bounds(cx + 2, cy + 18, 98, 20)
                .build());

        this.setInitialFocus(this.titleBox);
    }

    private void confirm() {
        String title = this.titleBox.getValue().trim();
        if (title.isEmpty()) return;
        PacketDistributor.sendToServer(new SignPagePayload(title, this.hand == InteractionHand.OFF_HAND));
        this.minecraft.setScreen(null); // закрываем всё: страница уже подписана
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
        this.minecraft.setScreen(this.parent); // отмена или Esc возвращают к странице
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 30, 0xFFFFFF);
    }
}