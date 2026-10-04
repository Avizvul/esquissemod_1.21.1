package net.avizvul.esquissemod.client;

import net.avizvul.esquissemod.component.SketchData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class ClientRenderUtils {

    public static void renderCachedSketch(GuiGraphics guiGraphics, SketchData data, int startX, int startY, int drawWidth, int drawHeight) {
        if (data == null || data.isEmpty()) return;

        // Отрисовка текстуры, содержащей и пиксели, и запечённый текст
        ResourceLocation texture = SketchTextureCache.getOrCreateTexture(data);
        if (texture != null) {
            guiGraphics.blit(texture, startX, startY, 0.0f, 0.0f, drawWidth, drawHeight, drawWidth, drawHeight);
        }
    }
}
