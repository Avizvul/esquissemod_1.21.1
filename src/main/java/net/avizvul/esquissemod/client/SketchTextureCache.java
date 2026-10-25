package net.avizvul.esquissemod.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.avizvul.esquissemod.component.SketchData;
import net.avizvul.esquissemod.component.TextElement;
import net.avizvul.esquissemod.util.ColorUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.*;

public class SketchTextureCache {

    private static final int W = 126, H = 192;
    private static final int MAX_CACHE_SIZE = 100;

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

    // Очередь для блоков: их нельзя растеризовать прямо внутри BER
    private static final Map<Integer, SketchData> PENDING = new LinkedHashMap<>();

    /** Безопасно для BER: только возвращает готовую текстуру или null и ставит запрос в очередь. */
    public static ResourceLocation getIfReady(SketchData data) {
        if (data == null || data.isEmpty()) return null;
        int hash = data.hashCode();
        ResourceLocation id = CACHE.get(hash);
        if (id == null) PENDING.putIfAbsent(hash, data);
        return id;
    }

    /** Вызывать из RenderFrameEvent.Pre — основной таргет привязан, мир ещё не рисуется. */
    public static void processPending() {
        if (PENDING.isEmpty()) return;
        List<SketchData> list = new ArrayList<>(PENDING.values());
        PENDING.clear();
        for (SketchData d : list) getOrCreateTexture(d);
    }

    /** Для GUI/тултипов (там можно вызывать сразу). */
    private static ResourceLocation getOrCreateTexture(SketchData data) {
        if (data == null || data.isEmpty()) return null;

        int hash = data.hashCode();
        ResourceLocation cached = CACHE.get(hash);
        if (cached != null) return cached;

        NativeImage image = new NativeImage(W, H, true);
        int[][] pixels = data.toArray(W, H);
        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) {
                int argb = pixels[x][y];
                if (argb != 0) image.setPixelRGBA(x, y, argbToAbgr(argb));
            }
        }

        List<TextElement> texts = data.getTextElements();
        if (texts != null && !texts.isEmpty()) {
            rasterizeText(image, texts);
        }

        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("esquissemod", "sketch_cache_" + Math.abs(hash));
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
        CACHE.put(hash, id);
        return id;
    }

    private static void rasterizeText(NativeImage baseImage, List<TextElement> textElements) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        TextureTarget target = null;
        ByteBufferBuilder bbb = null;
        NativeImage textImage = null;
        try {
            // Размер FBO == размер страницы, поэтому всё, что вылезает за края,
            // аппаратно отсекается ПО ПИКСЕЛЯМ (а не по буквам).
            target = new TextureTarget(W, H, true, Minecraft.ON_OSX);
            target.setClearColor(0f, 0f, 0f, 0f);
            target.clear(Minecraft.ON_OSX);
            target.bindWrite(true);

            RenderSystem.backupProjectionMatrix();
            RenderSystem.setProjectionMatrix(
                    new Matrix4f().setOrtho(0f, W, H, 0f, -1000f, 1000f),
                    VertexSorting.ORTHOGRAPHIC_Z);

            bbb = new ByteBufferBuilder(2048);
            MultiBufferSource.BufferSource buffers = MultiBufferSource.immediate(bbb);
            GuiGraphics g = new GuiGraphics(mc, buffers);

            for (TextElement elem : textElements) {
                if (elem.text() == null || elem.text().isEmpty()) continue;

                int color = elem.color();
                if ((color & 0xFF000000) == 0) color |= 0xFF000000;

                String[] lines = elem.text().split("\n", -1);
                g.pose().pushPose();
                g.pose().translate(elem.x(), elem.y(), 0f);
                if (elem.rotation() != 0f) {
                    g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(elem.rotation()));
                }
                g.pose().scale(elem.scale(), elem.scale(), 1f);
                for (int l = 0; l < lines.length; l++) {
                    if (lines[l].isEmpty()) continue;
                    g.drawString(font, lines[l], 0, l * 9, color, false);
                }
                g.pose().popPose();
            }
            g.flush();

            RenderSystem.restoreProjectionMatrix();

            textImage = new NativeImage(W, H, false);
            RenderSystem.bindTexture(target.getColorTextureId());
            textImage.downloadTexture(0, false);

            for (int x = 0; x < W; x++) {
                for (int y = 0; y < H; y++) {
                    int textAbgr = textImage.getPixelRGBA(x, H - 1 - y); // FBO перевёрнут по Y
                    if (((textAbgr >> 24) & 0xFF) == 0) continue;
                    int bg = abgrToArgb(baseImage.getPixelRGBA(x, y));
                    int blended = ColorUtils.blendColors(bg, abgrToArgb(textAbgr));
                    baseImage.setPixelRGBA(x, y, argbToAbgr(blended));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (textImage != null) textImage.close();
            if (bbb != null) bbb.close();
            if (target != null) target.destroyBuffers();
            // Возвращаем главный таргет и viewport
            RenderTarget main = mc.getMainRenderTarget();
            main.bindWrite(true);
        }
    }

    private static int argbToAbgr(int argb) {
        return (argb & 0xFF00FF00) | ((argb >> 16) & 0xFF) | ((argb & 0xFF) << 16);
    }

    private static int abgrToArgb(int abgr) {
        return argbToAbgr(abgr); // преобразование симметрично
    }
}