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

        int[][] pixels = sketchData.getRawPixels();
        int width = pixels.length;
        int height = pixels.length;

        // Создаем изображение NativeImage в формате RGBA
        NativeImage image = new NativeImage(width, height, false);

        int whiteBg = 0xFFFFFFFF; // Белый цвет фона бумаги (100% непрозрачный белый)

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int argb = pixels[x][y];

                // Накладываем цвет пикселя на белый фон с учётом прозрачности (Alpha Blending)
                int blended = ColorUtils.blendColors(whiteBg, argb);

                int a = (blended >> 24) & 0xFF;
                int r = (blended >> 16) & 0xFF;
                int g = (blended >> 8) & 0xFF;
                int b = blended & 0xFF;

                // NativeImage использует формат ABGR
                image.setPixelRGBA(x, y, (a << 24) | (b << 16) | (g << 8) | r);
            }
        }

        // Формируем имя файла с временной меткой
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
