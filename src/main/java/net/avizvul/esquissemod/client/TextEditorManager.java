package net.avizvul.esquissemod.client;

import net.avizvul.esquissemod.client.screen.SketchbookScreen.TextBoxState;
import net.avizvul.esquissemod.component.TextElement;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class TextEditorManager {

    private TextBoxState activeTextBox = null;
    private boolean isTextModeActive = false;

    private boolean isTextBoxDragging = false;
    private boolean isTextBoxResizing = false;
    private boolean isTextSelectingWithMouse = false;
    private double textBoxDragStartX, textBoxDragStartY;

    public boolean isTextModeActive() {
        return this.isTextModeActive;
    }

    public void setTextModeActive(boolean active) {
        this.isTextModeActive = active;
        if (!active) {
            this.activeTextBox = null;
        }
    }

    public TextBoxState getActiveTextBox() {
        return this.activeTextBox;
    }

    public void setActiveTextBox(TextBoxState box) {
        this.activeTextBox = box;
        this.isTextModeActive = (box != null);
    }

    public void createNewTextBox(int canvasX, int canvasY) {
        this.activeTextBox = new TextBoxState(canvasX, canvasY);
        this.isTextModeActive = true;
    }

    // =========================================================================
    // 1. ОТРИСОВКА ТЕКСТА, ВЫДЕЛЕНИЯ И КАРЕТКИ (Фиксировано относительно экрана)
    // =========================================================================
    public void renderActiveBoxTextContent(GuiGraphics guiGraphics, Font font, double pCell, int canvasScreenLeft, int renderY, int textColor) {
        if (!this.isTextModeActive || this.activeTextBox == null) return;

        TextBoxState box = this.activeTextBox;
        int screenX1 = canvasScreenLeft + (int) (box.x1 * pCell);
        int screenY1 = renderY + (int) (box.y1 * pCell);
        int screenX2 = canvasScreenLeft + (int) (box.x2 * pCell);
        int screenY2 = renderY + (int) (box.y2 * pCell);

        float renderScale = (float) (box.fontScale * pCell);
        int lineH = (int) (9 * renderScale);
        int rotStep = (box.rotation % 4 + 4) % 4;

        boolean isVerticalText = (rotStep == 1 || rotStep == 3);
        int boxLineLength = isVerticalText ? (screenY2 - screenY1) : (screenX2 - screenX1);
        int maxW = Math.max(10, (int) ((boxLineLength - 4) / renderScale));
        List<TextBoxState.TextLine> lines = box.getWrappedLines(font, maxW);

        if (lines.isEmpty()) return;

        // Определяем строку с кареткой
        int caretLineIdx = 0;
        TextBoxState.TextLine caretLine = lines.get(0);
        for (int l = 0; l < lines.size(); l++) {
            TextBoxState.TextLine line = lines.get(l);
            if (box.caret >= line.startCharIndex && (box.caret <= line.endCharIndex || l == lines.size() - 1)) {
                caretLineIdx = l;
                caretLine = line;
                break;
            }
        }

        boolean showCaret = (System.currentTimeMillis() / 500) % 2 == 0;

        for (int l = 0; l < lines.size(); l++) {
            TextBoxState.TextLine line = lines.get(l);

            int lineOriginX = screenX1 + 2;
            int lineOriginY = screenY1 + 2 + l * lineH;

            if (rotStep == 1) {
                lineOriginX = screenX2 - 2 - l * lineH;
                lineOriginY = screenY1 + 2;
            } else if (rotStep == 2) {
                lineOriginX = screenX2 - 2;
                lineOriginY = screenY2 - 2 - l * lineH;
            } else if (rotStep == 3) {
                lineOriginX = screenX1 + 2 + l * lineH;
                lineOriginY = screenY2 - 2;
            }

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(lineOriginX, lineOriginY, 0);

            if (rotStep != 0) {
                guiGraphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rotStep * 90f));
            }
            guiGraphics.pose().scale(renderScale, renderScale, 1.0f);

            // 1.1. Выделение текста (в локальных координатах строки)
            if (box.hasSelection()) {
                int min = box.getSelectionMin();
                int max = box.getSelectionMax();
                if (max > line.startCharIndex && min <= line.endCharIndex) {
                    int selStart = Math.max(min, line.startCharIndex);
                    int selEnd = Math.min(max, line.endCharIndex);
                    String textBefore = box.getFormattedSubstring(line.startCharIndex, selStart);
                    String textSelected = box.getFormattedSubstring(line.startCharIndex, selEnd);
                    int x1 = font.width(textBefore);
                    int x2 = x1 + font.width(textSelected);
                    guiGraphics.fill(x1, 0, x2, 9, 0x802266FF);
                }
            }

            // 1.2. Отрисовка самого текста
            guiGraphics.drawString(font, line.formattedText, 0, 0, textColor, false);

            // 1.3. Синяя каретка (в локальных координатах строки)
            if (showCaret && l == caretLineIdx) {
                String textBeforeCaret = box.getFormattedSubstring(caretLine.startCharIndex, box.caret);
                int caretX = font.width(textBeforeCaret);
                guiGraphics.fill(caretX, 0, caretX + 1, 9, 0xFF007ACC); // Синий цвет каретки
            }

            guiGraphics.pose().popPose();
        }
    }

    // =========================================================================
    // 2. ОТРИСОВКА РАМКИ И РУЧЕК (Фиксировано относительно экрана)
    // =========================================================================
    public void renderActiveBoxHandles(GuiGraphics guiGraphics, Font font, double pCell, int canvasScreenLeft, int renderY) {
        if (!this.isTextModeActive || this.activeTextBox == null) return;

        TextBoxState box = this.activeTextBox;
        int screenX1 = canvasScreenLeft + (int) (box.x1 * pCell);
        int screenY1 = renderY + (int) (box.y1 * pCell);
        int screenX2 = canvasScreenLeft + (int) (box.x2 * pCell);
        int screenY2 = renderY + (int) (box.y2 * pCell);

        int dashLen = 4, dashGap = 2, outlineColor = 0xFF007ACC;
        for (int px = screenX1; px < screenX2; px += dashLen + dashGap) {
            guiGraphics.fill(px, screenY1, Math.min(px + dashLen, screenX2), screenY1 + 1, outlineColor);
            guiGraphics.fill(px, screenY2, Math.min(px + dashLen, screenX2), screenY2 + 1, outlineColor);
        }
        for (int py = screenY1; py < screenY2; py += dashLen + dashGap) {
            guiGraphics.fill(screenX1, py, screenX1 + 1, Math.min(py + dashLen, screenY2), outlineColor);
            guiGraphics.fill(screenX2, py, screenX2 + 1, Math.min(py + dashLen, screenY2), outlineColor);
        }

        // Ручка перемещения "≡"
        guiGraphics.fill(screenX1, screenY1 - 6, screenX2 - 12, screenY1, 0xFF007ACC);
        guiGraphics.drawString(font, "≡", screenX1 + 2, screenY1 - 6, 0xFFFFFFFF, false);

        // Кнопка поворота "↕"
        guiGraphics.fill(screenX2 - 11, screenY1 - 6, screenX2, screenY1, (box.rotation > 0) ? 0xFF0055A0 : 0xFF007ACC);
        guiGraphics.drawString(font, "↕", screenX2 - 8, screenY1 - 6, 0xFFFFFFFF, false);

        // Уголок ресайза
        guiGraphics.fill(screenX2 - 5, screenY2 - 5, screenX2 + 4, screenY2 + 4, 0xFF8B0000);
        guiGraphics.fill(screenX2 - 4, screenY2 - 4, screenX2 + 3, screenY2 + 3, 0xFFFF0000);
    }

    // =========================================================================
    // 3. ПАНЕЛЬ ФОРМАТИРОВАНИЯ (Фиксировано относительно экрана)
    // =========================================================================
    public void renderFormattingToolbar(GuiGraphics guiGraphics, Font font, double pCell, int canvasScreenLeft, int renderY, int screenWidth, int screenHeight, boolean isColorTool) {
        if (!this.isTextModeActive || this.activeTextBox == null) return;

        TextBoxState box = this.activeTextBox;
        int screenX1 = canvasScreenLeft + (int) (box.x1 * pCell);
        int screenX2 = canvasScreenLeft + (int) (box.x2 * pCell);
        int screenY2 = renderY + (int) (box.y2 * pCell);

        int toolbarX = (screenX1 + screenX2) / 2 - 75;
        int toolbarY = screenY2 + 8;
        toolbarX = Math.max(10, Math.min(screenWidth - 160, toolbarX));
        toolbarY = Math.max(10, Math.min(screenHeight - 40, toolbarY));

        guiGraphics.fill(toolbarX, toolbarY, toolbarX + 150, toolbarY + 20, 0xE0000000);

        boolean isBoldActive = box.isStyleActive(TextBoxState.StyleType.BOLD);
        boolean isItalicActive = box.isStyleActive(TextBoxState.StyleType.ITALIC);
        boolean isUnderlineActive = box.isStyleActive(TextBoxState.StyleType.UNDERLINE);
        boolean isStrikethroughActive = box.isStyleActive(TextBoxState.StyleType.STRIKETHROUGH);

        guiGraphics.fill(toolbarX + 4, toolbarY + 3, toolbarX + 16, toolbarY + 17, isBoldActive ? 0xFF007ACC : 0x40FFFFFF);
        guiGraphics.drawString(font, "§lB§r", toolbarX + 7, toolbarY + 5, 0xFFFFFFFF, false);

        guiGraphics.fill(toolbarX + 18, toolbarY + 3, toolbarX + 30, toolbarY + 17, isItalicActive ? 0xFF007ACC : 0x40FFFFFF);
        guiGraphics.drawString(font, "§oI§r", toolbarX + 22, toolbarY + 5, 0xFFFFFFFF, false);

        guiGraphics.fill(toolbarX + 32, toolbarY + 3, toolbarX + 44, toolbarY + 17, isUnderlineActive ? 0xFF007ACC : 0x40FFFFFF);
        guiGraphics.drawString(font, "§nU§r", toolbarX + 36, toolbarY + 5, 0xFFFFFFFF, false);

        guiGraphics.fill(toolbarX + 46, toolbarY + 3, toolbarX + 58, toolbarY + 17, isStrikethroughActive ? 0xFF007ACC : 0x40FFFFFF);
        guiGraphics.drawString(font, "§mS§r", toolbarX + 50, toolbarY + 5, 0xFFFFFFFF, false);

        guiGraphics.fill(toolbarX + 62, toolbarY + 3, toolbarX + 74, toolbarY + 17, 0x40FFFFFF);
        guiGraphics.drawString(font, "-", toolbarX + 66, toolbarY + 5, 0xFFFFFFFF, false);

        guiGraphics.fill(toolbarX + 76, toolbarY + 3, toolbarX + 88, toolbarY + 17, 0x40FFFFFF);
        guiGraphics.drawString(font, "+", toolbarX + 80, toolbarY + 5, 0xFFFFFFFF, false);

        String opacityLabel = (box.textOpacityLevel == 3) ? "H" : (box.textOpacityLevel == 2) ? "M" : "S";
        guiGraphics.fill(toolbarX + 90, toolbarY + 3, toolbarX + 102, toolbarY + 17, 0xFF007ACC);
        guiGraphics.drawString(font, opacityLabel, toolbarX + 94, toolbarY + 5, 0xFFFFFFFF, false);

        guiGraphics.fill(toolbarX + 104, toolbarY + 3, toolbarX + 116, toolbarY + 17, (box.rotation > 0) ? 0xFF007ACC : 0x40FFFFFF);
        guiGraphics.drawString(font, "↕", toolbarX + 108, toolbarY + 5, 0xFFFFFFFF, false);

        guiGraphics.fill(toolbarX + 118, toolbarY + 3, toolbarX + 130, toolbarY + 17, 0xFF228B22);
        guiGraphics.drawString(font, "v", toolbarX + 122, toolbarY + 5, 0xFFFFFFFF, false);

        guiGraphics.fill(toolbarX + 132, toolbarY + 3, toolbarX + 144, toolbarY + 17, 0xFFB22222);
        guiGraphics.drawString(font, "x", toolbarX + 136, toolbarY + 5, 0xFFFFFFFF, false);

        if (isColorTool) {
            int colorBarY = toolbarY + 20;
            guiGraphics.fill(toolbarX, colorBarY, toolbarX + 150, colorBarY + 12, 0xE0000000);
            for (int colorId = 0; colorId < 16; colorId++) {
                int colorX = toolbarX + 6 + colorId * 8;
                int colorY = colorBarY + 3;
                int rgb = net.minecraft.world.item.DyeColor.byId(colorId).getTextureDiffuseColor() | 0xFF000000;
                boolean isSelected = (box.pendingColorId == colorId);
                if (isSelected) {
                    guiGraphics.fill(colorX - 1, colorY - 1, colorX + 6, colorY + 6, 0xFFFFFFFF);
                }
                guiGraphics.fill(colorX, colorY, colorX + 5, colorY + 5, rgb);
            }
        }
    }

    // =========================================================================
    // 4. ОБРАБОТКА КЛИКОВ МЫШИ
    // =========================================================================
    public boolean mouseClicked(double mouseX, double mouseY, double lMouseX, double lMouseY, int button, double pCell, int canvasScreenLeft, int renderY, int screenWidth, int screenHeight, boolean isColorTool, Runnable onCommit, Runnable onCancel) {
        if (!this.isTextModeActive || this.activeTextBox == null) return false;

        TextBoxState box = this.activeTextBox;
        int screenX1 = canvasScreenLeft + (int) (box.x1 * pCell);
        int screenY1 = renderY + (int) (box.y1 * pCell);
        int screenX2 = canvasScreenLeft + (int) (box.x2 * pCell);
        int screenY2 = renderY + (int) (box.y2 * pCell);

        int toolbarX = (screenX1 + screenX2) / 2 - 75;
        int toolbarY = screenY2 + 8;
        toolbarX = Math.max(10, Math.min(screenWidth - 160, toolbarX));
        toolbarY = Math.max(10, Math.min(screenHeight - 40, toolbarY));
        int colorBarY = toolbarY + 20;

        // 4.1. Клик по кнопкам панели форматирования
        if (mouseY >= toolbarY && mouseY <= toolbarY + 20) {
            if (mouseX >= toolbarX + 4 && mouseX <= toolbarX + 16) { box.applyFormattingCode("§l"); return true; }
            if (mouseX >= toolbarX + 18 && mouseX <= toolbarX + 30) { box.applyFormattingCode("§o"); return true; }
            if (mouseX >= toolbarX + 32 && mouseX <= toolbarX + 44) { box.applyFormattingCode("§n"); return true; }
            if (mouseX >= toolbarX + 46 && mouseX <= toolbarX + 58) { box.applyFormattingCode("§m"); return true; }
            if (mouseX >= toolbarX + 62 && mouseX <= toolbarX + 74) { box.fontScale = Math.max(0.5f, box.fontScale - 0.25f); return true; }
            if (mouseX >= toolbarX + 76 && mouseX <= toolbarX + 88) { box.fontScale = Math.min(2.0f, box.fontScale + 0.25f); return true; }
            if (mouseX >= toolbarX + 90 && mouseX <= toolbarX + 102) { box.cycleOpacity(); return true; }
            if (mouseX >= toolbarX + 104 && mouseX <= toolbarX + 116) { box.toggleOrientation(); return true; }
            if (mouseX >= toolbarX + 118 && mouseX <= toolbarX + 130) { onCommit.run(); return true; }
            if (mouseX >= toolbarX + 132 && mouseX <= toolbarX + 144) { onCancel.run(); return true; }
        }

        // 4.2. Клик по красителям
        if (isColorTool && mouseY >= colorBarY && mouseY <= colorBarY + 12) {
            for (int colorId = 0; colorId < 16; colorId++) {
                int colorX = toolbarX + 6 + colorId * 8;
                int colorY = colorBarY + 3;
                if (mouseX >= colorX - 1 && mouseX <= colorX + 6 && mouseY >= colorY - 1 && mouseY <= colorY + 6) {
                    box.applyColor(colorId);
                    return true;
                }
            }
        }

        // 4.3. Клик по ручкам управления рамкой
        if (button == 0) {
            int handleSize = 8;
            if (mouseX >= screenX2 - handleSize && mouseX <= screenX2 + handleSize && mouseY >= screenY2 - handleSize && mouseY <= screenY2 + handleSize) {
                this.isTextBoxResizing = true;
                return true;
            }
            if (mouseX >= screenX2 - 12 && mouseX <= screenX2 + 4 && mouseY >= screenY1 - 6 && mouseY <= screenY1 + 2) {
                box.toggleOrientation();
                return true;
            }
            if (mouseX >= screenX1 && mouseX <= screenX2 - 12 && mouseY >= screenY1 - 6 && mouseY <= screenY1 + 2) {
                this.isTextBoxDragging = true;
                this.textBoxDragStartX = mouseX - screenX1;
                this.textBoxDragStartY = mouseY - screenY1;
                return true;
            }
            if (mouseX >= screenX1 && mouseX <= screenX2 && mouseY >= screenY1 && mouseY <= screenY2) {
                this.isTextSelectingWithMouse = true;
                return true;
            }
        }

        onCommit.run();
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, double pCell, int canvasScreenLeft, int renderY) {
        if (!this.isTextModeActive || this.activeTextBox == null) return false;

        if (this.isTextBoxDragging) {
            int newX1 = (int) ((mouseX - this.textBoxDragStartX - canvasScreenLeft) / pCell);
            int newY1 = (int) ((mouseY - this.textBoxDragStartY - renderY) / pCell);
            int w = this.activeTextBox.getWidth();
            int h = this.activeTextBox.getHeight();
            this.activeTextBox.x1 = newX1;
            this.activeTextBox.y1 = newY1;
            this.activeTextBox.x2 = newX1 + w;
            this.activeTextBox.y2 = newY1 + h;
            return true;
        }

        if (this.isTextBoxResizing) {
            int currentX2 = (int) ((mouseX - canvasScreenLeft) / pCell);
            int currentY2 = (int) ((mouseY - renderY) / pCell);
            this.activeTextBox.x2 = Math.max(this.activeTextBox.x1 + 10, currentX2);
            this.activeTextBox.y2 = Math.max(this.activeTextBox.y1 + 10, currentY2);
            this.activeTextBox.updateText();
            return true;
        }

        return false;
    }

    public void mouseReleased(int button) {
        if (button == 0) {
            this.isTextSelectingWithMouse = false;
            this.isTextBoxDragging = false;
            this.isTextBoxResizing = false;
        }
    }

    public boolean keyPressed(int keyCode, boolean hasShift, boolean hasCtrl, Runnable onCommit) {
        if (!this.isTextModeActive || this.activeTextBox == null) return false;

        TextBoxState box = this.activeTextBox;

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.isTextModeActive = false;
            this.activeTextBox = null;
            return true;
        } else if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && hasCtrl) {
            onCommit.run();
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            box.insertText("\n");
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            box.deleteBack();
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_DELETE) {
            box.deleteForward();
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_LEFT) {
            box.moveCursorLeft(hasShift, hasCtrl);
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            box.moveCursorRight(hasShift, hasCtrl);
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_HOME) {
            box.moveCursorHome(hasShift, hasCtrl);
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_END) {
            box.moveCursorEnd(hasShift, hasCtrl);
            return true;
        }

        return true;
    }

    public boolean charTyped(char codePoint) {
        if (this.isTextModeActive && this.activeTextBox != null) {
            this.activeTextBox.insertText(String.valueOf(codePoint));
            return true;
        }
        return false;
    }

    public TextElement commitTextToCanvas(Font font, double pCell, int canvasScreenLeft, int renderY, int drawWidth, int drawHeight, float rotationAngle, int activeTextColorArgb) {
        if (!this.isTextModeActive || this.activeTextBox == null) return null;

        TextBoxState box = this.activeTextBox;
        int screenX1 = canvasScreenLeft + (int) (box.x1 * pCell);
        int screenY1 = renderY + (int) (box.y1 * pCell);
        int screenX2 = canvasScreenLeft + (int) (box.x2 * pCell);
        int screenY2 = renderY + (int) (box.y2 * pCell);

        float renderScale = (float) (box.fontScale * pCell);
        int rotStep = (box.rotation % 4 + 4) % 4;
        boolean isVerticalText = (rotStep == 1 || rotStep == 3);
        int boxLineLength = isVerticalText ? (screenY2 - screenY1) : (screenX2 - screenX1);
        int maxW = Math.max(10, (int) ((boxLineLength - 4) / renderScale));

        List<TextBoxState.TextLine> lines = box.getWrappedLines(font, maxW);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) sb.append("\n");
            sb.append(lines.get(i).formattedText);
        }

        String formattedString = sb.toString();
        TextElement element = null;

        if (!formattedString.isEmpty()) {
            int baseRgb = activeTextColorArgb & 0xFFFFFF;
            int alpha = switch (box.textOpacityLevel) {
                case 1 -> 84;  // 33%
                case 2 -> 168; // 66%
                default -> 255; // 100%
            };
            int finalArgb = (alpha << 24) | baseRgb;

            int cornerScreenX = (rotStep == 1 || rotStep == 2) ? screenX2 - 2 : screenX1 + 2;
            int cornerScreenY = (rotStep == 2 || rotStep == 3) ? screenY2 - 2 : screenY1 + 2;

            double cx = canvasScreenLeft + drawWidth / 2.0;
            double cy = renderY + drawHeight / 2.0;

            double rad = Math.toRadians(-rotationAngle);
            double cos = Math.cos(rad);
            double sin = Math.sin(rad);

            double dx = cornerScreenX - cx;
            double dy = cornerScreenY - cy;

            double unrotatedX = cx + (dx * cos - dy * sin);
            double unrotatedY = cy + (dx * sin + dy * cos);

            int localX = (int) Math.round((unrotatedX - canvasScreenLeft) / pCell);
            int localY = (int) Math.round((unrotatedY - renderY) / pCell);

            float textScreenAngle = rotStep * 90.0f;

            element = new TextElement(formattedString, localX, localY, box.fontScale, finalArgb, textScreenAngle);
        }

        this.isTextModeActive = false;
        this.activeTextBox = null;
        return element;
    }
}
