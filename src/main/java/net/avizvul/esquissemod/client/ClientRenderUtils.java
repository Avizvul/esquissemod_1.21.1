package net.avizvul.esquissemod.client;

import net.avizvul.esquissemod.component.SketchData;
import net.avizvul.esquissemod.component.TextElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

public class ClientRenderUtils {

    public static void renderCachedSketch(GuiGraphics guiGraphics, SketchData data, int startX, int startY, int drawWidth, int drawHeight) {
        if (data == null || data.isEmpty()) return;

        // 1. Отрисовка растра
        net.minecraft.resources.ResourceLocation texture = SketchTextureCache.getOrCreateTexture(data);
        if (texture != null) {
            guiGraphics.blit(texture, startX, startY, 0.0f, 0.0f, drawWidth, drawHeight, drawWidth, drawHeight);
        }

        // 2. Отрисовка векторного текста с обрезкой по краям листа
        List<TextElement> texts = data.getTextElements();
        if (texts != null && !texts.isEmpty()) {
            Font font = Minecraft.getInstance().font;
            double scaleX = (double) drawWidth / 126.0;
            double scaleY = (double) drawHeight / 192.0;

            guiGraphics.enableScissor(startX, startY, startX + drawWidth, startY + drawHeight);

            for (TextElement elem : texts) {
                int elemX = startX + (int) ((elem.x() + 2) * scaleX);
                int elemY = startY + (int) ((elem.y() + 2) * scaleY);

                String[] lines = elem.text().split("\n", -1);

                for (int l = 0; l < lines.length; l++) {
                    if (lines[l].isEmpty()) continue;
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(elemX, elemY, 0);
                    if (elem.rotation() != 0.0f) {
                        guiGraphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(elem.rotation()));
                    }
                    guiGraphics.pose().scale(elem.scale() * (float) scaleX, elem.scale() * (float) scaleY, 1.0f);
                    guiGraphics.drawString(font, lines[l], 0, l * 9, elem.color(), false);
                    guiGraphics.pose().popPose();
                }
            }

            guiGraphics.disableScissor();
        }
    }
}
