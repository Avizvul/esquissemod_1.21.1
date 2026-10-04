package net.avizvul.esquissemod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.avizvul.esquissemod.EsquisseMod;
import net.avizvul.esquissemod.block.SketchedPageBlock;
import net.avizvul.esquissemod.block.entity.SketchedPageBlockEntity;
import net.avizvul.esquissemod.component.SketchData;
import net.avizvul.esquissemod.component.TextElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class SketchedPageBlockEntityRenderer implements BlockEntityRenderer<SketchedPageBlockEntity> {

    private static final ResourceLocation PAGE_TEX = ResourceLocation.fromNamespaceAndPath(EsquisseMod.MOD_ID, "textures/gui/sketched_page_gui.png");

    public SketchedPageBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(SketchedPageBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        SketchData data = blockEntity.getSketchData();
        if (data == null || data.isEmpty()) return;

        Direction facing = blockEntity.getBlockState().getValue(SketchedPageBlock.FACING);

        poseStack.pushPose();
        poseStack.translate(0.5f, 0.5f, 0.5f);

        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180f));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180f));
            case EAST  -> {
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90f));
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180f));
            }
            case WEST  -> {
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90f));
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180f));
            }
            case UP    -> poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90f));
            case DOWN  -> {
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90f));
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180f));
            }
        }

        poseStack.translate(0.0f, 0.0f, 0.495f);
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(blockEntity.getRotation() * 90f));

        float scale = 0.8f / 192f;
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-63.0f, -96.0f, 0.0f);

        PoseStack.Pose pose = poseStack.last();

        // 1. Бумага
        VertexConsumer bgConsumer = bufferSource.getBuffer(RenderType.entityCutout(PAGE_TEX));
        drawQuad(pose, bgConsumer, 0, 0, 0.0f, 126, 192, 0.0f, 0.0f, 1.0f, 1.0f, 0xFFFFFFFF, packedLight);

        // 2. Пиксельный рисунок
        ResourceLocation sketchTexture = net.avizvul.esquissemod.client.SketchTextureCache.getOrCreateTexture(data);
        if (sketchTexture != null) {
            VertexConsumer pixelConsumer = bufferSource.getBuffer(RenderType.entityTranslucentCull(sketchTexture));
            drawQuad(pose, pixelConsumer, 0, 0, -1.0f, 126, 192, 0.0f, 0.0f, 1.0f, 1.0f, 0xFFFFFFFF, packedLight);
        }

        // 3. Векторный текст на 3D блоке в мире
        List<TextElement> texts = data.getTextElements();
        if (texts != null && !texts.isEmpty()) {
            Font font = Minecraft.getInstance().font;
            for (TextElement elem : texts) {
                String[] lines = elem.text().split("\n", -1);
                for (int l = 0; l < lines.length; l++) {
                    if (lines[l].isEmpty()) continue;
                    poseStack.pushPose();
                    poseStack.translate(elem.x(), elem.y(), -1.5f);
                    if (elem.rotation() != 0.0f) {
                        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(elem.rotation()));
                    }
                    poseStack.scale(elem.scale(), elem.scale(), 1.0f);
                    font.drawInBatch(
                            lines[l],
                            0,
                            l * 9,
                            elem.color(),
                            false,
                            poseStack.last().pose(),
                            bufferSource,
                            Font.DisplayMode.NORMAL,
                            0,
                            packedLight
                    );
                    poseStack.popPose();
                }
            }
        }

        poseStack.popPose();
    }

    private void drawQuad(PoseStack.Pose pose, VertexConsumer consumer, float x, float y, float z, float width, float height, float u0, float v0, float u1, float v1, int argb, int light) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        int a = (argb >> 24) & 0xFF;
        int overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;

        consumer.addVertex(pose, x, y + height, z).setColor(r, g, b, a).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(pose, 0.0f, 0.0f, -1.0f);
        consumer.addVertex(pose, x + width, y + height, z).setColor(r, g, b, a).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(pose, 0.0f, 0.0f, -1.0f);
        consumer.addVertex(pose, x + width, y, z).setColor(r, g, b, a).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(pose, 0.0f, 0.0f, -1.0f);
        consumer.addVertex(pose, x, y, z).setColor(r, g, b, a).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(pose, 0.0f, 0.0f, -1.0f);
    }
}
