package net.avizvul.esquissemod.client;

import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.opengl.GL11;

public class StencilBufferUtils {

    /**
     * Включает запись маски обрезки по повернутой области листа
     */
    public static void beginMask(GuiGraphics guiGraphics, double cx, double cy, float rotationAngle, Runnable drawMaskShape) {
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        GL11.glStencilMask(0xFF);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);

        // 1. Рисуем маску листа с учётом поворота
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(cx, cy, 0);
        guiGraphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rotationAngle));
        guiGraphics.pose().translate(-cx, -cy, 0);

        drawMaskShape.run();

        guiGraphics.pose().popPose();

        // 2. Переключаем трафарет в режим ограничения: выводить пиксели ТОЛЬКО внутри маски (где stencil == 1)
        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        GL11.glStencilMask(0x00);
    }

    /**
     * Выключает буфер трафарета и восстанавливает стандартный рендеринг
     */
    public static void endMask() {
        GL11.glStencilMask(0xFF);
        GL11.glStencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }
}
