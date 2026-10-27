package net.avizvul.esquissemod.client.screen;

import net.avizvul.esquissemod.item.custom.StampItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class StampCarveScreen extends Screen {

    private static final int PANEL = 208;
    private static final int GRID = 192;
    private static final int MARGIN = 8;
    private static final int SIDE_W = 100;

    private static final int BG = 0xFF25201B;        // фон
    private static final int GRID_LINE = 0xFF2E2822; // тонкая сетка
    private static final int RAISED = 0xFF6B5A47;    // выступающие клетки
    private static final int RAISED_HI = 0xFF8F7B62; // светлая кромка

    private final InteractionHand hand;
    private final int size;
    private final int cell;
    private final boolean[][] bits;

    private int panelX, panelY;
    private Button sealButton;

    private boolean dragging = false;
    private boolean paintValue = false;
    private int lastX = -1, lastY = -1;

    public StampCarveScreen(ItemStack stack, InteractionHand hand) {
        super(Component.translatable("gui.esquissemod.stamp.carve"));
        this.hand = hand;
        this.size = ((StampItem) stack.getItem()).getSize();
        this.cell = GRID / this.size;
        this.bits = new boolean[this.size][this.size];
        fillAll(true); // заготовка — сплошной блок, игрок вырезает лишнее
    }

    @Override
    protected void init() {
        int totalW = PANEL + 6 + SIDE_W;
        this.panelX = (this.width - totalW) / 2;
        this.panelY = (this.height - PANEL) / 2;
        int bx = this.panelX + PANEL + 6;

        this.addRenderableWidget(Button.builder(Component.translatable("gui.esquissemod.stamp.invert"), b -> invertAll())
                .bounds(bx, this.panelY, SIDE_W, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.esquissemod.stamp.clear"), b -> fillAll(false))
                .bounds(bx, this.panelY + 24, SIDE_W, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.esquissemod.stamp.fill"), b -> fillAll(true))
                .bounds(bx, this.panelY + 48, SIDE_W, 20).build());

        this.sealButton = Button.builder(Component.translatable("gui.esquissemod.stamp.seal"),
                        b -> this.minecraft.setScreen(new SealStampScreen(this, rows(), this.hand)))
                .bounds(bx, this.panelY + PANEL - 44, SIDE_W, 20).build();
        this.addRenderableWidget(this.sealButton);

        this.addRenderableWidget(Button.builder(Component.translatable("gui.esquissemod.stamp.cancel"), b -> onClose())
                .bounds(bx, this.panelY + PANEL - 20, SIDE_W, 20).build());
    }

    private void fillAll(boolean value) {
        for (int x = 0; x < this.size; x++) for (int y = 0; y < this.size; y++) this.bits[x][y] = value;
    }

    private void invertAll() {
        for (int x = 0; x < this.size; x++) for (int y = 0; y < this.size; y++) this.bits[x][y] = !this.bits[x][y];
    }

    private boolean hasAny() {
        for (int x = 0; x < this.size; x++) for (int y = 0; y < this.size; y++) if (this.bits[x][y]) return true;
        return false;
    }

    private List<Integer> rows() {
        List<Integer> rows = new ArrayList<>(this.size);
        for (int y = 0; y < this.size; y++) {
            int v = 0;
            for (int x = 0; x < this.size; x++) if (this.bits[x][y]) v |= (1 << x);
            rows.add(v);
        }
        return rows;
    }

    // ---------- Рендер ----------

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);

        g.fill(this.panelX - 1, this.panelY - 1, this.panelX + PANEL + 1, this.panelY + PANEL + 1, 0xFF000000);
        g.fill(this.panelX, this.panelY, this.panelX + PANEL, this.panelY + PANEL, BG);

        int gx = this.panelX + MARGIN;
        int gy = this.panelY + MARGIN;

        for (int i = 0; i <= this.size; i++) {
            g.fill(gx + i * this.cell, gy, gx + i * this.cell + 1, gy + GRID, GRID_LINE);
            g.fill(gx, gy + i * this.cell, gx + GRID, gy + i * this.cell + 1, GRID_LINE);
        }

        for (int x = 0; x < this.size; x++) {
            for (int y = 0; y < this.size; y++) {
                if (!this.bits[x][y]) continue;
                int x0 = gx + x * this.cell + 1;
                int y0 = gy + y * this.cell + 1;
                int x1 = gx + (x + 1) * this.cell;
                int y1 = gy + (y + 1) * this.cell;
                g.fill(x0, y0, x1, y1, RAISED);
                g.fill(x0, y0, x1, y0 + 1, RAISED_HI);
                g.fill(x0, y0, x0 + 1, y1, RAISED_HI);
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.sealButton.active = hasAny();
        super.render(g, mouseX, mouseY, partialTick);

        int bx = this.panelX + PANEL + 6;
        g.drawWordWrap(this.font, Component.translatable("gui.esquissemod.stamp.hint"),
                bx, this.panelY + 76, SIDE_W, 0xAAAAAA);
    }

    // ---------- Ввод ----------

    private int cellCoord(double v, int origin) {
        return (int) Math.floor((v - origin) / this.cell);
    }

    private void setCell(int x, int y) {
        if (x >= 0 && x < this.size && y >= 0 && y < this.size) this.bits[x][y] = this.paintValue;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 || button == 1) {
            int cx = cellCoord(mouseX, this.panelX + MARGIN);
            int cy = cellCoord(mouseY, this.panelY + MARGIN);
            if (cx >= 0 && cx < this.size && cy >= 0 && cy < this.size) {
                this.paintValue = (button == 1); // ЛКМ вырезает, ПКМ возвращает
                this.dragging = true;
                this.lastX = cx;
                this.lastY = cy;
                setCell(cx, cy);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.dragging) {
            int cx = cellCoord(mouseX, this.panelX + MARGIN);
            int cy = cellCoord(mouseY, this.panelY + MARGIN);
            int steps = Math.max(Math.abs(cx - this.lastX), Math.abs(cy - this.lastY));
            for (int i = 1; i <= steps; i++) { // заполняем промежуток между кадрами
                setCell(this.lastX + (cx - this.lastX) * i / steps, this.lastY + (cy - this.lastY) * i / steps);
            }
            this.lastX = cx;
            this.lastY = cy;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}