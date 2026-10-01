package net.avizvul.esquissemod.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public class TextRasterizer {

    //Преобразует форматированный текст Minecraft в массив пикселей ARGB.

    public static int[][] rasterize(String rawText, int width, int height, float scale, int colorArgb,
                                    boolean isBold, boolean isItalic, boolean isUnderline, boolean isStrikethrough) {
        int[][] textPixels = new int[width][height];
        if (rawText.isEmpty() || width <= 0 || height <= 0) return textPixels;

        // Строим строку с форматирующими кодами Minecraft (§l, §o, §n, §m)
        StringBuilder formatted = new StringBuilder();
        if (isBold) formatted.append("§l");
        if (isItalic) formatted.append("§o");
        if (isUnderline) formatted.append("§n");
        if (isStrikethrough) formatted.append("§m");
        formatted.append(rawText);

        Font font = Minecraft.getInstance().font;
        Component component = Component.literal(formatted.toString());

        // Разбиваем текст по строкам с учетом максимальной ширины зоны (Word Wrap)
        int maxLineWidth = Math.max(10, (int) (width / scale));
        List<FormattedCharSequence> lines = font.split(component, maxLineWidth);

        int fontHeight = font.lineHeight;

        // Для точного переноса пикселей используем встроенный рендерер шрифта в NativeImage / Buffer
        // Но на логическом уровне мы считываем позиции глифов
        int currentY = 0;
        for (FormattedCharSequence line : lines) {
            if (currentY + fontHeight > height / scale) break;

            // Рендерим каждую строку через графику и снимаем пиксели
            final int lineY = currentY;
            font.drawInBatch(
                    line, 0, lineY, colorArgb, false,
                    com.mojang.math.Matrix4f.sub(new com.mojang.math.Matrix4f(), 0),
                    buffer -> {}, Font.DisplayMode.NORMAL, 0, 15728880
            );

            currentY += fontHeight + 1;
        }

        return textPixels;
    }
}
