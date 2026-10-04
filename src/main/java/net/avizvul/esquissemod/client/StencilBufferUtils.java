package net.avizvul.esquissemod.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.opengl.GL11;

public class StencilBufferUtils {

    /**
     * Включает трафаретную маску обрезки.
     * Рисование маски происходит в текущих координатах PoseStack.
     */
    public static void beginMask(GuiGraphics guiGraphics, Runnable drawMaskShape) {
        // 1. Сбрасываем всё, что рисовалось до этого момент (холст, фоны)
        guiGraphics.flush();

        // 2. Включаем Stencil у главного FBO экрана
        Minecraft.getInstance().getMainRenderTarget().enableStencil();

        // 3. Включаем Stencil Test в OpenGL
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glClearStencil(0);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);

        // 4. Отключаем запись цвета и глубины (записываем ТОЛЬКО в Stencil)
        GL11.glColorMask(false, false, false, false);
        GL11.glDepthMask(false);

        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        GL11.glStencilMask(0xFF);

        // Отрисовываем форму маски (без повторного поворота!)
        drawMaskShape.run();

        // КРИТИЧЕСКИ ВАЖНО: сбрасываем буфер, чтобы квад маски записался в Stencil GPU
        guiGraphics.flush();

        // 5. Включаем запись цвета обратно и ограничиваем рендер областью stencil == 1
        GL11.glColorMask(true, true, true, true);
        GL11.glDepthMask(true);

        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        GL11.glStencilMask(0x00);
    }

    /**
     * Выключает буфер трафарета и сбрасывает вершины текста до отключения Stencil
     */
    public static void endMask(GuiGraphics guiGraphics) {
        // Принудительно отрисовываем накопившийся текст до отключения Stencil Test
        guiGraphics.flush();

        GL11.glStencilMask(0xFF);
        GL11.glStencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }
}
