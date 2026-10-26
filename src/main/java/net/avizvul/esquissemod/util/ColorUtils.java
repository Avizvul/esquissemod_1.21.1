package net.avizvul.esquissemod.util;

public class ColorUtils {

    /**
     * Смешивает новый полупрозрачный цвет с фоновым пикселем (Alpha Blending).
     * @param bg Фоновый цвет (уже нарисованный пиксель) в формате ARGB
     * @param fg Новый цвет (карандаш) в формате ARGB
     * @return Смешанный цвет в формате ARGB
     */
    public static int blendColors(int bg, int fg) {
        if (bg == 0) return fg; // Если фона нет, просто кладем чистый цвет

        int fgA = (fg >> 24) & 0xFF;
        int fgR = (fg >> 16) & 0xFF;
        int fgG = (fg >> 8) & 0xFF;
        int fgB = fg & 0xFF;

        int bgA = (bg >> 24) & 0xFF;
        int bgR = (bg >> 16) & 0xFF;
        int bgG = (bg >> 8) & 0xFF;
        int bgB = bg & 0xFF;

        // 1. Прямое (линейное) сложение непрозрачности с лимитом в 255 (100%)
        int outA = Math.min(255, bgA + fgA);
        if (outA == 0) return 0;

        // 2. Вычисляем "вес" старого цвета для правильного смешивания RGB
        int bgWeight = outA - fgA;

        // 3. Пропорционально смешиваем цвета в зависимости от их плотности
        int outR = (fgR * fgA + bgR * bgWeight) / outA;
        int outG = (fgG * fgA + bgG * bgWeight) / outA;
        int outB = (fgB * fgA + bgB * bgWeight) / outA;

        return (outA << 24) | (outR << 16) | (outG << 8) | outB;
    }

    /**
     * Линейная интерполяция между двумя ARGB цветами с учётом альфы (premultiplied).
     * Прозрачные пиксели не подмешивают свой RGB, поэтому на границе рисунка нет серых оттенков.
     * @param dest   Цвет назначения (куда сдвигаем)
     * @param source Взятый цвет (откуда сдвигаем)
     * @param t      Коэффициент смешивания (0.0 = только dest, 1.0 = только source)
     */
    public static int lerpColor(int dest, int source, float t) {
        float dA = ((dest >> 24) & 0xFF) / 255.0f;
        float sA = ((source >> 24) & 0xFF) / 255.0f;

        // Альфа интерполируется как обычно
        float outA = dA + (sA - dA) * t;
        if (outA <= 0.0f) return 0;

        // Цвет взвешивается по альфе каждого из пикселей
        float dWeight = dA * (1.0f - t);
        float sWeight = sA * t;
        float total = dWeight + sWeight;
        if (total <= 0.0f) return 0;

        float outR = (((dest >> 16) & 0xFF) * dWeight + ((source >> 16) & 0xFF) * sWeight) / total;
        float outG = (((dest >> 8) & 0xFF) * dWeight + ((source >> 8) & 0xFF) * sWeight) / total;
        float outB = ((dest & 0xFF) * dWeight + (source & 0xFF) * sWeight) / total;

        int a = Math.round(outA * 255.0f);
        int r = Math.round(outR);
        int g = Math.round(outG);
        int b = Math.round(outB);
        if (a <= 0) return 0;

        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}