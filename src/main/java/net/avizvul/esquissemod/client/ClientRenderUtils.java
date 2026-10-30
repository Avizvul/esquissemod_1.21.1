package net.avizvul.esquissemod.client;

import com.mojang.math.Axis;
import net.avizvul.esquissemod.component.SketchData;
import net.avizvul.esquissemod.component.TextElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.systems.RenderSystem;

import java.util.List;

public class ClientRenderUtils {

    public static void renderCachedSketch(GuiGraphics guiGraphics, SketchData data, int startX, int startY, int drawWidth, int drawHeight) {
        if (data == null || data.isEmpty()) return;

        // Текст соседних элементов сбрасывает blend, поэтому включаем его перед каждым рисунком
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Пиксели: если текстуры ещё нет, она появится со следующего кадра
        ResourceLocation texture = SketchTextureCache.getIfReady(data);
        if (texture != null) {
            guiGraphics.blit(texture, startX, startY, 0.0f, 0.0f, drawWidth, drawHeight, drawWidth, drawHeight);
        }

        renderSketchText(guiGraphics, data, startX, startY, drawWidth, drawHeight);
    }

    /** Текст рисуется живым шрифтом по тем же формулам, что в SketchbookScreen, с обрезкой по границам страницы. */
    private static void renderSketchText(GuiGraphics g, SketchData data, int startX, int startY, int drawWidth, int drawHeight) {
        List<TextElement> texts = data.getTextElements();
        if (texts == null || texts.isEmpty()) return;

        Font font = Minecraft.getInstance().font;
        double pCell = drawWidth / 126.0; // пикселей экрана на один пиксель холста

        g.enableScissor(startX, startY, startX + drawWidth, startY + drawHeight);
        for (TextElement elem : texts) {
            if (elem.text() == null || elem.text().isEmpty()) continue;

            int elemX = startX + (int) (elem.x() * pCell);
            int elemY = startY + (int) (elem.y() * pCell);
            float renderScale = (float) (elem.scale() * pCell);

            String[] lines = elem.text().split("\n", -1);
            for (int l = 0; l < lines.length; l++) {
                if (lines[l].isEmpty()) continue;
                g.pose().pushPose();
                g.pose().translate(elemX, elemY, 0);
                if (elem.rotation() != 0.0f) {
                    g.pose().mulPose(Axis.ZP.rotationDegrees(elem.rotation()));
                }
                g.pose().scale(renderScale, renderScale, 1.0f);
                g.drawString(font, lines[l], 0, l * 9, elem.color(), false);
                g.pose().popPose();
            }
        }
        g.disableScissor(); // заодно сбрасывает накопленный текст внутрь обрезки
    }
}