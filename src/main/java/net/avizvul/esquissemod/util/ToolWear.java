package net.avizvul.esquissemod.util;

import net.avizvul.esquissemod.component.ModDataComponents;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class ToolWear {
    private ToolWear() {}

    /** Сколько изменённых пикселей = 1 единица износа (как у карандаша и ластика). */
    public static final int PIXELS_PER_DAMAGE = 100;
    public static final int KNEADED_PIXELS_PER_DAMAGE = 3000;
    public static final int SMUDGE_PIXELS_PER_DAMAGE = 3000;
    /** Ёмкость одного цвета (равна прочности обычных инструментов). */
    public static final int MAX_PAINT = 256;
    public static final int COLOR_COUNT = 16;
    public static final int PAINT_STAGES = 4;

    /** Изнашивать инструменты в творческом режиме (true удобно для отладки, для релиза поставьте false). */
    public static final boolean WEAR_IN_CREATIVE = false;

    public static int pixelsPerDamage(net.minecraft.world.item.Item item) {
        if (item == net.avizvul.esquissemod.item.ModItems.KNEADED_ERASER.get()) return KNEADED_PIXELS_PER_DAMAGE;
        if (item == net.avizvul.esquissemod.item.ModItems.SMUDGE.get()) return SMUDGE_PIXELS_PER_DAMAGE;
        return PIXELS_PER_DAMAGE;
    }
    /** ID активного цвета инструмента или -1, если красок нет. */
    public static int getActiveColorId(ItemStack stack) {
        List<Integer> colors = stack.getOrDefault(ModDataComponents.STORED_COLORS.get(), List.of());
        if (colors.isEmpty()) return -1;
        int idx = stack.getOrDefault(ModDataComponents.ACTIVE_COLOR_INDEX.get(), 0);
        return colors.get(Math.abs(idx) % colors.size());
    }

    /** Остаток краски цвета (0..MAX_PAINT). Если компонента нет, цвет считается полным. */
    public static int getPaintLevel(ItemStack stack, int colorId) {
        if (colorId < 0 || colorId >= COLOR_COUNT) return 0;
        List<Integer> colors = stack.getOrDefault(ModDataComponents.STORED_COLORS.get(), List.of());
        if (!colors.contains(colorId)) return 0;
        List<Integer> levels = stack.get(ModDataComponents.PAINT_LEVELS.get());
        if (levels == null || colorId >= levels.size()) return MAX_PAINT;
        return Math.max(0, Math.min(MAX_PAINT, levels.get(colorId)));
    }

    public static void setPaintLevel(ItemStack stack, int colorId, int level) {
        if (colorId < 0 || colorId >= COLOR_COUNT) return;
        List<Integer> levels = new ArrayList<>(stack.getOrDefault(ModDataComponents.PAINT_LEVELS.get(), List.of()));
        while (levels.size() < COLOR_COUNT) levels.add(MAX_PAINT);
        levels.set(colorId, Math.max(0, Math.min(MAX_PAINT, level)));
        stack.set(ModDataComponents.PAINT_LEVELS.get(), levels);
    }

    /** units[colorId] = сколько единиц краски потрачено. */
    public static void consumePaint(ItemStack stack, List<Integer> units) {
        for (int c = 0; c < Math.min(COLOR_COUNT, units.size()); c++) {
            int u = units.get(c);
            if (u <= 0) continue;
            int cur = getPaintLevel(stack, c);
            if (cur > 0) setPaintLevel(stack, c, cur - u);
        }
    }

    /** 0 = пусто, 1..4 = 25% / 50% / 75% / 100%. */
    public static int getPaintStage(int level) {
        if (level <= 0) return 0;
        return (int) Math.ceil(level * (double) PAINT_STAGES / MAX_PAINT);
    }
}