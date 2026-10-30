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
    private static final int MAX_PIXEL_CACHE = 64;
    private static final int MAX_TEXT_CACHE = 16;
    /** Во сколько раз текстура блока с текстом крупнее холста. */
    private static final int TEXT_SS = 4;

    private static final Map<Integer, ResourceLocation> PIXEL_CACHE = lru(MAX_PIXEL_CACHE);
    private static final Map<Integer, ResourceLocation> TEXT_CACHE = lru(MAX_TEXT_CACHE);

    // Очереди: блоки нельзя растеризовать прямо внутри BER
    private static final Map<Integer, SketchData> PENDING_PIXEL = new LinkedHashMap<>();
    private static final Map<Integer, SketchData> PENDING_TEXT = new LinkedHashMap<>();

    private static Map<Integer, ResourceLocation> lru(int max) {
        return new LinkedHashMap<>(max + 1, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, ResourceLocation> eldest) {
                if (size() > max) {
                    Minecraft.getInstance().getTextureManager().release(eldest.getValue());
                    return true;
                }
                return false;
            }
        };
    }
    // Задачи, которым нужен безопасный момент в начале кадра (например, экспорт со шрифтом)
    private static final List<Runnable> TASKS = new ArrayList<>();

    public static void runAtFrameStart(Runnable task) {
        TASKS.add(task);
    }
    /** Только пиксели рисунка. Текст в GUI дорисовывается живым шрифтом (ClientRenderUtils). */
    public static ResourceLocation getIfReady(SketchData data) {
        if (data == null || data.isEmpty()) return null;
        int hash = data.hashCode();
        ResourceLocation id = PIXEL_CACHE.get(hash);
        if (id == null) PENDING_PIXEL.putIfAbsent(hash, data);
        return id;
    }

    /** Пиксели и текст в одной текстуре: для блока в мире, где живой шрифт с пиксельной обрезкой использовать нельзя. */
    public static ResourceLocation getIfReadyWithText(SketchData data) {
        if (data == null || data.isEmpty()) return null;
        if (data.getTextElements().isEmpty()) return getIfReady(data);
        int hash = data.hashCode();
        ResourceLocation id = TEXT_CACHE.get(hash);
        if (id == null) PENDING_TEXT.putIfAbsent(hash, data);
        return id;
    }

    /** Вызывать из RenderFrameEvent.Pre: основной таргет привязан, мир ещё не рисуется. */
    public static void processPending() {
        if (!TASKS.isEmpty()) {
            List<Runnable> tasks = new ArrayList<>(TASKS);
            TASKS.clear();
            for (Runnable r : tasks) r.run();
        }
        if (PENDING_PIXEL.isEmpty() && PENDING_TEXT.isEmpty()) return;
        List<SketchData> px = new ArrayList<>(PENDING_PIXEL.values());
        List<SketchData> tx = new ArrayList<>(PENDING_TEXT.values());
        PENDING_PIXEL.clear();
        PENDING_TEXT.clear();
        for (SketchData d : px) createTexture(d, false);
        for (SketchData d : tx) createTexture(d, true);
    }

    private static void createTexture(SketchData data, boolean withText) {
        int hash = data.hashCode();
        Map<Integer, ResourceLocation> cache = withText ? TEXT_CACHE : PIXEL_CACHE;
        if (cache.containsKey(hash)) return;

        int k = withText ? TEXT_SS : 1;
        NativeImage image = new NativeImage(W * k, H * k, true);
        int[][] pixels = data.toArray(W, H);
        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) {
                int argb = pixels[x][y];
                if (argb == 0) continue;
                int abgr = argbToAbgr(argb);
                for (int dx = 0; dx < k; dx++) {
                    for (int dy = 0; dy < k; dy++) {
                        image.setPixelRGBA(x * k + dx, y * k + dy, abgr);
                    }
                }
            }
        }

        if (withText) {
            rasterizeText(image, data.getTextElements(), k);
        }

        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("esquissemod",
                (withText ? "sketch_text_" : "sketch_px_") + Integer.toUnsignedString(hash));
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
        cache.put(hash, id);
    }

    public static void rasterizeText(NativeImage baseImage, List<TextElement> textElements, int k) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int tw = W * k, th = H * k;

        TextureTarget target = null;
        ByteBufferBuilder bbb = null;
        NativeImage textImage = null;
        try {
            // Размер буфера == размер страницы, поэтому всё, что вылезает за края,
            // аппаратно отсекается ПО ПИКСЕЛЯМ (а не по буквам).
            target = new TextureTarget(tw, th, true, Minecraft.ON_OSX);
            target.setClearColor(0f, 0f, 0f, 0f);
            target.clear(Minecraft.ON_OSX);
            target.bindWrite(true);

            RenderSystem.backupProjectionMatrix();
            // Координаты остаются в пикселях холста, поэтому текст сам рисуется в k раз чётче
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

            textImage = new NativeImage(tw, th, false);
            RenderSystem.bindTexture(target.getColorTextureId());
            textImage.downloadTexture(0, false);

            for (int x = 0; x < tw; x++) {
                for (int y = 0; y < th; y++) {
                    int textAbgr = textImage.getPixelRGBA(x, th - 1 - y); // буфер перевёрнут по Y
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