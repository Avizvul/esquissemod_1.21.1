package net.avizvul.esquissemod.client;

import net.avizvul.esquissemod.component.SketchData;
import net.avizvul.esquissemod.component.TextElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public class ClientRenderUtils {

    public static void renderCachedSketch(GuiGraphics guiGraphics, SketchData data, int startX, int startY, int drawWidth, int drawHeight) {
        if (data == null || data.isEmpty()) return;

        // 1. Отрисовка растра
        ResourceLocation texture = SketchTextureCache.getOrCreateTexture(data);
        if (texture != null) {
            guiGraphics.blit(texture, startX, startY, 0.0f, 0.0f, drawWidth, drawHeight, drawWidth, drawHeight);
        }

        // 2. Отрисовка векторного текста с обрезкой по краям листа
        List<TextElement> texts = data.getTextElements();
        if (texts != null && !texts.isEmpty()) {
            Font font = Minecraft.getInstance().font;
            double scaleX = (double) drawWidth / 126.0;
            double scaleY = (double) drawHeight / 192.0;

            // Включаем попиксельную обрезку (Scissor Test) строго по краям бумаги
            guiGraphics.enableScissor(startX, startY, startX + drawWidth, startY + drawHeight);

            for (TextElement elem : texts) {
                int elemX = startX + (int) ((elem.x() + 2) * scaleX);
                int elemY = startY + (int) ((elem.y() + 2) * scaleY);

                Component comp = Component.literal(elem.text());
                // Ширина расчитывается от X до края листа (125)
                int maxW = Math.max(10, (int) ((125 - elem.x() - 2) * scaleX / elem.scale()));
                List<FormattedCharSequence> lines = font.split(comp, maxW);
                int lineH = (int) (9 * elem.scale() * scaleY);

                int rotDegrees = (elem.rotation() % 4) * 90;

                for (int l = 0; l < lines.size(); l++) {
                    guiGraphics.pose().pushPose();
                    if (rotDegrees == 90) {
                        guiGraphics.pose().translate(elemX - l * lineH, elemY, 0);
                        guiGraphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90f));
                    } else if (rotDegrees == 180) {
                        guiGraphics.pose().translate(elemX, elemY - l * lineH, 0);
                        guiGraphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180f));
                    } else if (rotDegrees == 270) {
                        guiGraphics.pose().translate(elemX + l * lineH, elemY, 0);
                        guiGraphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(270f));
                    } else {
                        guiGraphics.pose().translate(elemX, elemY + l * lineH, 0);
                    }
                    guiGraphics.pose().scale(elem.scale() * (float) scaleX, elem.scale() * (float) scaleY, 1.0f);
                    guiGraphics.drawString(font, lines.get(l), 0, 0, elem.color(), false);
                    guiGraphics.pose().popPose();
                }
            }

            // Выключаем маску обрезки
            guiGraphics.disableScissor();
        }
    }
}
