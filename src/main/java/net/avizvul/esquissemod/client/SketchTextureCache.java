package net.avizvul.esquissemod.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.avizvul.esquissemod.component.SketchData;
import net.avizvul.esquissemod.component.TextElement;
import net.avizvul.esquissemod.util.ColorUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SketchTextureCache {

    private static final int MAX_CACHE_SIZE = 50;

    private static final Map<Integer, ResourceLocation> CACHE = new LinkedHashMap<>(MAX_CACHE_SIZE + 1, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, ResourceLocation> eldest) {
            if (size() > MAX_CACHE_SIZE) {
                Minecraft.getInstance().getTextureManager().release(eldest.getValue());
                return true;
            }
            return false;
        }
    };

    public static ResourceLocation getOrCreateTexture(SketchData data) {
        if (data == null || data.isEmpty()) return null;

        int hash = data.hashCode();
        if (CACHE.containsKey(hash)) {
            return CACHE.get(hash);
        }

        int w = 126;
        int h = 192;
        int[][] pixels = data.toArray(w, h);

        NativeImage image = new NativeImage(w, h, true);

        // 1. Заполняем рисунок пикселями
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int argb = pixels[x][y];
                if (argb != 0) {
                    image.setPixelRGBA(x, y, argbToAbgr(argb));
                }
            }
        }

        // 2. Запекаем векторный текст поверх рисунка
        List<TextElement> textElements = data.getTextElements();
        if (textElements != null && !textElements.isEmpty()) {
            rasterizeTextOntoImage(image, textElements, w, h);
        }

        DynamicTexture texture = new DynamicTexture(image);
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("esquissemod", "sketch_cache_" + Math.abs(hash));
        Minecraft.getInstance().getTextureManager().register(id, texture);

        CACHE.put(hash, id);
        return id;
    }

    private static void rasterizeTextOntoImage(NativeImage baseImage, List<TextElement> textElements, int width, int height) {
        try {
            Minecraft mc = Minecraft.getInstance();
            Font font = mc.font;

            TextureTarget target = new TextureTarget(width, height, true, Minecraft.ON_OSX);
            target.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            target.clear(Minecraft.ON_OSX);
            target.bindWrite(true);

            RenderSystem.viewport(0, 0, width, height);
            RenderSystem.backupProjectionMatrix();
            RenderSystem.setProjectionMatrix(
                    new Matrix4f().setOrtho(0.0f, (float) width, (float) height, 0.0f, 1000.0f, 3000.0f),
                    VertexSorting.ORTHOGRAPHIC_Z
            );

            GuiGraphics guiGraphics = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, -2000.0f);

            for (TextElement elem : textElements) {
                String[] lines = elem.text().split("\n", -1);
                for (int l = 0; l < lines.length; l++) {
                    if (lines[l].isEmpty()) continue;
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(elem.x(), elem.y(), 0);
                    if (elem.rotation() != 0.0f) {
                        guiGraphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(elem.rotation()));
                    }
                    guiGraphics.pose().scale(elem.scale(), elem.scale(), 1.0f);
                    guiGraphics.drawString(font, lines[l], 0, l * 9, elem.color(), false);
                    guiGraphics.pose().popPose();
                }
            }

            guiGraphics.pose().popPose();

            // ВАЖНО: Принудительный сброс буфера отрисовки перед скачиванием текстуры!
            mc.renderBuffers().bufferSource().endBatch();

            NativeImage textImage = new NativeImage(width, height, false);
            RenderSystem.bindTexture(target.getColorTextureId());
            textImage.downloadTexture(0, false);

            RenderSystem.restoreProjectionMatrix();
            mc.getMainRenderTarget().bindWrite(true);
            RenderSystem.viewport(0, 0, mc.getWindow().getWidth(), mc.getWindow().getHeight());
            target.destroyBuffers();

            // Альфа-смешивание запечённого текста с рисунком
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    int textAbgr = textImage.getPixelRGBA(x, y);
                    int textAlpha = (textAbgr >> 24) & 0xFF;
                    if (textAlpha > 0) {
                        int bgAbgr = baseImage.getPixelRGBA(x, y);
                        int blendedArgb = ColorUtils.blendColors(abgrToArgb(bgAbgr), abgrToArgb(textAbgr));
                        baseImage.setPixelRGBA(x, y, argbToAbgr(blendedArgb));
                    }
                }
            }
            textImage.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static int argbToAbgr(int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    private static int abgrToArgb(int abgr) {
        int a = (abgr >> 24) & 0xFF;
        int b = (abgr >> 16) & 0xFF;
        int g = (abgr >> 8) & 0xFF;
        int r = abgr & 0xFF;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
