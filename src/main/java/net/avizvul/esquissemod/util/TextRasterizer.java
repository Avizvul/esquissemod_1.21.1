package net.avizvul.esquissemod.util;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;

import java.util.List;

public class TextRasterizer {

    public static int[][] rasterize(String formattedText, int boxWidth, int boxHeight, float fontScale, int colorArgb) {
        if (formattedText == null || formattedText.isEmpty() || boxWidth <= 0 || boxHeight <= 0) {
            return new int[0][0];
        }

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        int targetW = boxWidth;
        int targetH = boxHeight;

        // 1. Буфер кадра под размер текстовой рамки
        TextureTarget target = new TextureTarget(targetW, targetH, true, Minecraft.ON_OSX);
        target.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);

        RenderSystem.viewport(0, 0, targetW, targetH);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(
                new Matrix4f().setOrtho(0.0f, targetW, targetH, 0.0f, 1000.0f, 3000.0f),
                VertexSorting.ORTHOGRAPHIC_Z
        );

        GuiGraphics guiGraphics = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, -2000.0f);
        guiGraphics.pose().scale(fontScale, fontScale, 1.0f);

        // 2. Отрисовка перенесённых строк текста с тегами §
        int maxW = Math.max(10, (int) (boxWidth / fontScale));
        Component comp = Component.literal(formattedText);
        List<FormattedCharSequence> lines = font.split(comp, maxW);

        int lineH = font.lineHeight;
        int currentY = 0;

        for (FormattedCharSequence line : lines) {
            if (currentY + lineH > (int) (boxHeight / fontScale)) break;
            guiGraphics.drawString(font, line, 0, currentY, colorArgb, false);
            currentY += lineH;
        }

        guiGraphics.flush();
        guiGraphics.pose().popPose();

        // 3. Чтение пикселей из текстуры
        NativeImage image = new NativeImage(targetW, targetH, false);
        RenderSystem.bindTexture(target.getColorTextureId());
        image.downloadTexture(0, false);

        RenderSystem.restoreProjectionMatrix();
        mc.getMainRenderTarget().bindWrite(true);
        RenderSystem.viewport(0, 0, mc.getWindow().getWidth(), mc.getWindow().getHeight());
        target.destroyBuffers();

        // 4. Заполнение массива ARGB пикселей
        int dimW = targetW;
        int dimH = targetH;
        int[][] textPixels = new int[dimW][dimH];

        for (int x = 0; x < dimW; x++) {
            for (int y = 0; y < dimH; y++) {
                int abgr = image.getPixelRGBA(x, y);
                int a = (abgr >> 24) & 0xFF;
                int b = (abgr >> 16) & 0xFF;
                int g = (abgr >> 8) & 0xFF;
                int r = abgr & 0xFF;

                if (a > 0) {
                    textPixels[x][y] = (a << 24) | (r << 16) | (g << 8) | b;
                } else {
                    textPixels[x][y] = 0;
                }
            }
        }
        image.close();

        return textPixels;
    }
}
