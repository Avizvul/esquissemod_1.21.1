package net.avizvul.esquissemod.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.avizvul.esquissemod.component.SketchData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
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

        // Заполнение растрового рисунка пикселями
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int argb = pixels[x][y];
                if (argb != 0) {
                    image.setPixelRGBA(x, y, argbToAbgr(argb));
                }
            }
        }

        DynamicTexture texture = new DynamicTexture(image);
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("esquissemod", "sketch_cache_" + Math.abs(hash));
        Minecraft.getInstance().getTextureManager().register(id, texture);

        CACHE.put(hash, id);
        return id;
    }

    private static int argbToAbgr(int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return (a << 24) | (b << 16) | (g << 8) | r;
    }
}
