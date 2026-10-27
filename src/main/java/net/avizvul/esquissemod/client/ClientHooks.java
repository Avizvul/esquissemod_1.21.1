package net.avizvul.esquissemod.client;

import net.avizvul.esquissemod.client.screen.SketchedPageScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class ClientHooks {
    public static void openSketchedPageScreen(ItemStack stack, InteractionHand hand) {
        Minecraft.getInstance().setScreen(new SketchedPageScreen(stack, hand));
    }

    public static void openStampCarveScreen(ItemStack stack, InteractionHand hand) {
        Minecraft.getInstance().setScreen(new net.avizvul.esquissemod.client.screen.StampCarveScreen(stack, hand));
    }

}