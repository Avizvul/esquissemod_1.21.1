package net.avizvul.esquissemod.client;

import net.avizvul.esquissemod.EsquisseMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

@EventBusSubscriber(modid = EsquisseMod.MOD_ID, value = Dist.CLIENT)
public class SketchRenderHooks {
    @SubscribeEvent
    public static void onFrameStart(RenderFrameEvent.Pre event) {
        SketchTextureCache.processPending();
    }
}