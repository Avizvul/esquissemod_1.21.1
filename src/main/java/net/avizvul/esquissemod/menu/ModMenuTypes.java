package net.avizvul.esquissemod.menu;

import net.avizvul.esquissemod.EsquisseMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, EsquisseMod.MOD_ID);

    public static final Supplier<MenuType<PencilCaseMenu>> PENCIL_CASE_MENU = MENUS.register("pencil_case_menu",
            () -> new MenuType<>(PencilCaseMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));

    public static final Supplier<MenuType<PrinterMenu>> PRINTER_MENU = MENUS.register("printer_menu",
            () -> IMenuTypeExtension.create((containerId, playerInventory, buf) ->
                    new PrinterMenu(containerId, playerInventory, new ItemStackHandler(8), buf.readBlockPos())));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}