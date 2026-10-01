package net.avizvul.esquissemod.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;

import java.util.List;

public class TextRasterizer {

    public static int[][] rasterize(String rawText, int width, int height, float scale, int colorArgb,
                                    boolean isBold, boolean isItalic, boolean isUnderline, boolean isStrikethrough) {
        int[][] textPixels = new int[width][height];
        if (rawText.isEmpty() || width <= 0 || height <= 0) return textPixels;

        StringBuilder formatted = new StringBuilder();
        if (isBold) formatted.append("§l");
        if (isItalic) formatted.append("§o");
        if (isUnderline) formatted.append("§n");
        if (isStrikethrough) formatted.append("§m");
        formatted.append(rawText);

        Font font = Minecraft.getInstance().font;
        Component component = Component.literal(formatted.toString());

        int maxLineWidth = Math.max(10, (int) (width / scale));
        List<FormattedCharSequence> lines = font.split(component, maxLineWidth);

        int fontHeight = font.lineHeight;
        int currentY = 0;

        MultiBufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();

        for (FormattedCharSequence line : lines) {
            if (currentY + fontHeight > height / scale) break;

            final int lineY = currentY;

            font.drawInBatch(
                    line, 0, lineY, colorArgb, false,
                    new Matrix4f(),
                    bufferSource,
                    Font.DisplayMode.NORMAL, 0, 15728880
            );

            currentY += fontHeight + 1;
        }

        return textPixels;
    }
}
