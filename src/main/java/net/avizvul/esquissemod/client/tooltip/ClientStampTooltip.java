package net.avizvul.esquissemod.client.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

import java.util.List;

public class ClientStampTooltip implements ClientTooltipComponent {

    private final List<Integer> rows;
    private final int size;
    private final int cell;

    public ClientStampTooltip(StampTooltipData data) {
        this.rows = data.rows();
        this.size = this.rows.size();
        this.cell = this.size <= 16 ? 2 : 1; // в обоих случаях миниатюра 32×32
    }

    @Override
    public int getHeight() { return this.size * this.cell + 4; }

    @Override
    public int getWidth(Font font) { return this.size * this.cell + 4; }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics g) {
        int box = this.size * this.cell + 4;
        g.fill(x, y, x + box, y + box, 0xFFE9E1D1);
        for (int yy = 0; yy < this.size; yy++) {
            int row = this.rows.get(yy);
            int run = -1;
            for (int xx = 0; xx <= this.size; xx++) {
                boolean on = xx < this.size && ((row >> xx) & 1) != 0;
                if (on && run < 0) {
                    run = xx;
                } else if (!on && run >= 0) {
                    g.fill(x + 2 + run * this.cell, y + 2 + yy * this.cell,
                            x + 2 + xx * this.cell, y + 2 + (yy + 1) * this.cell, 0xFF2A2420);
                    run = -1;
                }
            }
        }
    }
}