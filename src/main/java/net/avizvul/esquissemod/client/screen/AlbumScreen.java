package net.avizvul.esquissemod.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.avizvul.esquissemod.EsquisseMod;
import net.avizvul.esquissemod.client.ClientRenderUtils;
import net.avizvul.esquissemod.component.ModDataComponents;
import net.avizvul.esquissemod.component.SketchData;
import net.avizvul.esquissemod.menu.AlbumMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class AlbumScreen extends AbstractContainerScreen<AlbumMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(EsquisseMod.MOD_ID, "textures/gui/album_gui.png");
    private static final ResourceLocation PAGE_TEX = ResourceLocation.fromNamespaceAndPath(EsquisseMod.MOD_ID, "textures/gui/sketched_page_gui.png");
    private static final ResourceLocation TAB_BACK_TEX = ResourceLocation.fromNamespaceAndPath(EsquisseMod.MOD_ID, "textures/gui/button_page_b.png");
    private static final ResourceLocation TAB_FWD_TEX = ResourceLocation.fromNamespaceAndPath(EsquisseMod.MOD_ID, "textures/gui/button_page_f.png");

    // ===== Макет (подгоняется под текстуру album_gui.png 176×224) =====
    private static final int GUI_W = 176, GUI_H = 224;
    private static final int PAGE_W = 63, PAGE_H = 96;
    private static final int LEFT_PAGE_X = 24, RIGHT_PAGE_X = 89, PAGE_Y = 10;
    private static final int TEXT_Y = PAGE_Y + PAGE_H + 3;   // начало подписи под рисунком
    private static final int TEXT_MAX_W = 61;                // ширина области подписи
    private static final float TEXT_SCALE = 0.75f;
    private static final int LINE_H = 7;
    private static final int MAX_TITLE_LINES = 3;
    private static final int TEXT_COLOR = 0x404040;

    // Закладки: те же, что в скетчбуке (16×16 текстура в масштабе ×2)
    private static final int TAB_SIZE = 32;
    private static final int TAB_X_OFFSET = 0;   // от правого края GUI
    private static final int TAB_Y_OFFSET = 3;   // от верха GUI
    private static final int TAB_GAP = 3;

    private static final int SPREADS = AlbumMenu.PAGES / 2;

    private static int savedSpread = 0;

    private int spread;
    private boolean skipRelease = false;

    public AlbumScreen(AlbumMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_W;
        this.imageHeight = GUI_H;
    }

    @Override
    protected void init() {
        super.init();
        this.spread = Math.max(0, Math.min(SPREADS - 1, savedSpread));
    }

    // ---------- Закладки ----------

    private int tabX() { return this.leftPos + this.imageWidth + TAB_X_OFFSET; }
    private int backTabY() { return this.topPos + TAB_Y_OFFSET; }
    private int forwardTabY() { return backTabY() + TAB_SIZE + TAB_GAP; }

    private static boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void setSpread(int newSpread) {
        newSpread = Math.max(0, Math.min(SPREADS - 1, newSpread));
        if (newSpread == this.spread) return;
        this.spread = newSpread;
        savedSpread = newSpread;
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.playSound(SoundEvents.BOOK_PAGE_TURN, 1.0f, 1.0f);
        }
    }

    private void drawTab(GuiGraphics g, ResourceLocation tex, int x, int y, boolean hovered) {
        g.blit(tex, x, y, TAB_SIZE, TAB_SIZE, 0.0f, hovered ? 16.0f : 0.0f, 16, 16, 16, 32);
    }

    // ---------- Рендер ----------

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, GUI_W, GUI_H, GUI_W, GUI_H);

        RenderSystem.enableBlend();
        drawPage(g, this.spread * 2, this.leftPos + LEFT_PAGE_X);
        drawPage(g, this.spread * 2 + 1, this.leftPos + RIGHT_PAGE_X);
        RenderSystem.disableBlend();

        if (this.spread > 0) {
            drawTab(g, TAB_BACK_TEX, tabX(), backTabY(), inRect(mouseX, mouseY, tabX(), backTabY(), TAB_SIZE, TAB_SIZE));
        }
        if (this.spread < SPREADS - 1) {
            drawTab(g, TAB_FWD_TEX, tabX(), forwardTabY(), inRect(mouseX, mouseY, tabX(), forwardTabY(), TAB_SIZE, TAB_SIZE));
        }
    }

    private void drawPage(GuiGraphics g, int slotIndex, int pageX) {
        ItemStack stack = this.menu.getSlot(slotIndex).getItem();
        if (stack.isEmpty()) return;

        int y = this.topPos + PAGE_Y;
        g.blit(PAGE_TEX, pageX, y, PAGE_W, PAGE_H, 0.0f, 0.0f, PAGE_W, PAGE_H, PAGE_W, PAGE_H);

        SketchData data = stack.get(ModDataComponents.PAGE_DATA.get());
        if (data != null && !data.isEmpty()) {
            ClientRenderUtils.renderCachedSketch(g, data, pageX, y, PAGE_W, PAGE_H);
        }

        // Название и автор показываются только у подписанных рисунков
        if (stack.getOrDefault(ModDataComponents.PAGE_SIGNED.get(), false)) {
            drawCaption(g, stack, pageX + PAGE_W / 2, this.topPos + TEXT_Y);
        }
    }

    private void drawCaption(GuiGraphics g, ItemStack stack, int centerX, int y) {
        int wrapWidth = (int) (TEXT_MAX_W / TEXT_SCALE);
        int line = 0;

        List<FormattedCharSequence> titleLines = this.font.split(stack.getHoverName(), wrapWidth);
        for (FormattedCharSequence seq : titleLines) {
            if (line >= MAX_TITLE_LINES) break;
            drawScaled(g, seq, centerX, y + line * LINE_H);
            line++;
        }

        String author = stack.get(ModDataComponents.PAGE_AUTHOR.get());
        if (author != null && !author.isEmpty()) {
            String text = Component.translatable("tooltip.esquissemod.author", author).getString();
            if (this.font.width(text) > wrapWidth) {
                text = this.font.plainSubstrByWidth(text, wrapWidth - this.font.width("…")) + "…";
            }
            drawScaled(g, FormattedCharSequence.forward(text, Style.EMPTY), centerX, y + line * LINE_H);
        }
    }

    private void drawScaled(GuiGraphics g, FormattedCharSequence seq, int centerX, int y) {
        float w = this.font.width(seq) * TEXT_SCALE;
        g.pose().pushPose();
        g.pose().translate(centerX - w / 2.0f, y, 0);
        g.pose().scale(TEXT_SCALE, TEXT_SCALE, 1.0f);
        g.drawString(this.font, seq, 0, 0, TEXT_COLOR, false);
        g.pose().popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // Стандартные подписи не нужны: заголовок и надпись «Инвентарь» убраны
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);
    }

    // ---------- Ввод ----------

    /** Индекс слота страницы под курсором или -1. */
    private int pageSlotAt(double mx, double my) {
        int y = this.topPos + PAGE_Y;
        if (inRect(mx, my, this.leftPos + LEFT_PAGE_X, y, PAGE_W, PAGE_H)) return this.spread * 2;
        if (inRect(mx, my, this.leftPos + RIGHT_PAGE_X, y, PAGE_W, PAGE_H)) return this.spread * 2 + 1;
        return -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (this.spread > 0 && inRect(mouseX, mouseY, tabX(), backTabY(), TAB_SIZE, TAB_SIZE)) {
                setSpread(this.spread - 1);
                this.skipRelease = true;
                return true;
            }
            if (this.spread < SPREADS - 1 && inRect(mouseX, mouseY, tabX(), forwardTabY(), TAB_SIZE, TAB_SIZE)) {
                setSpread(this.spread + 1);
                this.skipRelease = true;
                return true;
            }
        }

        if (button == 0 || button == 1) {
            int idx = pageSlotAt(mouseX, mouseY);
            if (idx >= 0) {
                // Слоты страниц спрятаны за экраном, поэтому передаём клик в нужный слот вручную
                ClickType type = Screen.hasShiftDown() ? ClickType.QUICK_MOVE : ClickType.PICKUP;
                this.slotClicked(this.menu.slots.get(idx), idx, button, type);
                this.skipRelease = true;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.skipRelease) {
            this.skipRelease = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY > 0) { setSpread(this.spread - 1); return true; }
        if (scrollY < 0) { setSpread(this.spread + 1); return true; }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT) { setSpread(this.spread - 1); return true; }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) { setSpread(this.spread + 1); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}