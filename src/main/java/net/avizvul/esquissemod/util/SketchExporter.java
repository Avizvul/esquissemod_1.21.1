package net.avizvul.esquissemod.util;

import com.mojang.blaze3d.platform.NativeImage;
import net.avizvul.esquissemod.component.SketchData;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SketchExporter {

    public static void exportSketchToScreenshots(SketchData sketchData) {
        if (sketchData == null || sketchData.isEmpty()) return;

        // 1. Гарантируем точные размеры холста (126x192) независимо от исходного массива
        int srcWidth = 126;
        int srcHeight = 192;
        int[][] pixels = sketchData.toArray(srcWidth, srcHeight);

        // 2. Масштабируем картинку в 4 раза (504x768 px), чтобы файл был крупным и четким
        int scale = 4;
        int exportWidth = srcWidth * scale;
        int exportHeight = srcHeight * scale;

        NativeImage image = new NativeImage(exportWidth, exportHeight, false);
        int whiteBg = 0xFFFFFFFF; // Белый цвет бумаги (100% непрозрачный)

        for (int x = 0; x < srcWidth; x++) {
            for (int y = 0; y < srcHeight; y++) {
                int argb = pixels[x][y];

                // 3. Смешиваем полупрозрачный пиксель с белым фоном бумаги (Alpha Blending)
                int blended = ColorUtils.blendColors(whiteBg, argb);

                int a = (blended >> 24) & 0xFF;
                int r = (blended >> 16) & 0xFF;
                int g = (blended >> 8) & 0xFF;
                int b = blended & 0xFF;

                // NativeImage ожидает формат ABGR
                int abgr = (a << 24) | (b << 16) | (g << 8) | r;

                // 4. Заполняем увеличенный блок пикселей для масштаба 4x
                for (int dx = 0; dx < scale; dx++) {
                    for (int dy = 0; dy < scale; dy++) {
                        image.setPixelRGBA(x * scale + dx, y * scale + dy, abgr);
                    }
                }
            }
        }

        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date());
        File screenshotsDir = new File(Minecraft.getInstance().gameDirectory, "screenshots");
        if (!screenshotsDir.exists()) {
            screenshotsDir.mkdirs();
        }

        File outputFile = new File(screenshotsDir, "esquisse_sketch_" + timeStamp + ".png");

        try {
            image.writeToFile(outputFile);
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendSystemMessage(
                        net.minecraft.network.chat.Component.literal("§aРисунок сохранен в screenshots/" + outputFile.getName())
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            image.close();
        }
    }
}
