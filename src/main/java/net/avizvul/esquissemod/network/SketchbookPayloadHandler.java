package net.avizvul.esquissemod.network;

import net.avizvul.esquissemod.component.ModDataComponents;
import net.avizvul.esquissemod.component.SketchData;
import net.avizvul.esquissemod.item.ModItems;
import net.avizvul.esquissemod.menu.PrinterMenu;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public class SketchbookPayloadHandler {

    // --- 1. ОБРАБОТКА СОХРАНЕНИЯ СТРАНИЦЫ И ИЗНОСА ИНСТРУМЕНТОВ ---
    public void handleData(final SketchbookSavePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            ItemStack stack = player.getMainHandItem();
            if (!stack.is(ModItems.SKETCHBOOK.get())) {
                stack = player.getOffhandItem();
            }

            if (stack.is(ModItems.SKETCHBOOK.get())) {
                List<SketchData> pages = new ArrayList<>(stack.getOrDefault(ModDataComponents.SKETCHBOOK_PAGES.get(), new ArrayList<>()));
                if (payload.pageIndex() >= 0 && payload.pageIndex() < pages.size()) {
                    pages.set(payload.pageIndex(), payload.sketchData());
                    stack.set(ModDataComponents.SKETCHBOOK_PAGES.get(), pages);
                    stack.set(ModDataComponents.LAST_PAGE.get(), payload.pageIndex());
                }
            }

            if (player.level() instanceof ServerLevel serverLevel) {
                damageTool(player, serverLevel, ModItems.PENCIL.get(), payload.pencilPixels() / 100);
                damageTool(player, serverLevel, ModItems.ERASER.get(), payload.eraserPixels() / 100);
            }
        });
    }

    // --- 2. ОБРАБОТКА ОТРЫВА СТРАНИЦЫ ИЗ СКЕТЧБУКА ---
    public void handleTearPage(final TearPagePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            ItemStack stack = player.getMainHandItem();
            if (!stack.is(ModItems.SKETCHBOOK.get())) {
                stack = player.getOffhandItem();
            }

            if (stack.is(ModItems.SKETCHBOOK.get())) {
                List<SketchData> pages = new ArrayList<>(stack.getOrDefault(ModDataComponents.SKETCHBOOK_PAGES.get(), new ArrayList<>()));
                if (payload.pageIndex() >= 0 && payload.pageIndex() < pages.size()) {
                    SketchData tornData = pages.remove(payload.pageIndex());
                    ItemStack tornPage;

                    if (tornData.isEmpty()) {
                        tornPage = new ItemStack(ModItems.EMPTY_PAGE.get());
                    } else {
                        tornPage = new ItemStack(ModItems.SKETCHED_PAGE.get());
                        tornPage.set(ModDataComponents.PAGE_DATA.get(), tornData);
                    }

                    if (!player.getInventory().add(tornPage)) {
                        player.drop(tornPage, false);
                    }

                    if (pages.isEmpty()) {
                        stack.shrink(1);
                    } else {
                        stack.set(ModDataComponents.SKETCHBOOK_PAGES.get(), pages);
                        int lastPage = stack.getOrDefault(ModDataComponents.LAST_PAGE.get(), 0);
                        if (lastPage >= pages.size()) {
                            stack.set(ModDataComponents.LAST_PAGE.get(), pages.size() - 1);
                        }
                    }
                }
            }
        });
    }

    // --- 3. ОБРАБОТКА ПЕРЕКЛЮЧЕНИЯ ЦВЕТА (В РУКАХ ИЛИ В ПЕНАЛЕ) ---
    public void handleChangeColor(final ChangeColorPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            Item targetItem = payload.isMarker() ? ModItems.COLOR_MARKER.get() : ModItems.COLOR_PENCIL.get();

            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.is(targetItem)) {
                    stack.set(ModDataComponents.ACTIVE_COLOR_INDEX.get(), payload.colorIndex());
                    return;
                }

                if (stack.is(ModItems.PENCIL_CASE.get()) && stack.has(DataComponents.CONTAINER)) {
                    ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
                    if (contents != null) {
                        NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);
                        contents.copyInto(items);
                        for (int j = 0; j < items.size(); j++) {
                            ItemStack innerStack = items.get(j);
                            if (innerStack.is(targetItem)) {
                                innerStack.set(ModDataComponents.ACTIVE_COLOR_INDEX.get(), payload.colorIndex());
                                stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
                                return;
                            }
                        }
                    }
                }
            }
        });
    }

    // --- 4. ОБРАБОТКА НАСТРОЕК КИСТИ (РАЗМЕР, ЖЕСТКОСТЬ, УГОЛ МАРКЕРА) ---
    public void handleToolSettings(final ToolSettingsPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            Item targetItem = switch (payload.toolType()) {
                case 0 -> ModItems.PENCIL.get();
                case 1 -> ModItems.COLOR_PENCIL.get();
                case 2 -> ModItems.ERASER.get();
                case 3 -> ModItems.SMUDGE.get();
                case 4 -> ModItems.KNEADED_ERASER.get();
                case 5 -> ModItems.COLOR_MARKER.get();
                default -> null;
            };

            if (targetItem == null) return;

            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.is(targetItem)) {
                    stack.set(ModDataComponents.BRUSH_SIZE.get(), payload.size());
                    stack.set(ModDataComponents.BRUSH_HARDNESS.get(), payload.hardness());
                    if (targetItem == ModItems.COLOR_MARKER.get()) {
                        stack.set(ModDataComponents.MARKER_ROTATION.get(), payload.rotation());
                    }
                    return;
                }

                if (stack.is(ModItems.PENCIL_CASE.get()) && stack.has(DataComponents.CONTAINER)) {
                    ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
                    if (contents != null) {
                        NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);
                        contents.copyInto(items);
                        for (int j = 0; j < items.size(); j++) {
                            ItemStack innerStack = items.get(j);
                            if (innerStack.is(targetItem)) {
                                innerStack.set(ModDataComponents.BRUSH_SIZE.get(), payload.size());
                                innerStack.set(ModDataComponents.BRUSH_HARDNESS.get(), payload.hardness());
                                if (targetItem == ModItems.COLOR_MARKER.get()) {
                                    innerStack.set(ModDataComponents.MARKER_ROTATION.get(), payload.rotation());
                                }
                                stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
                                return;
                            }
                        }
                    }
                }
            }
        });
    }

    // --- 5. ОБРАБОТКА ПЕЧАТИ В ПРИНТЕРЕ (БУМАГА, КАТАЛИЗАТОРЫ, CMYK И ЗВУКИ) ---
    public void handlePrinterAction(final PrinterActionPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player.containerMenu instanceof PrinterMenu printerMenu) {
                IItemHandler inv = printerMenu.getInventory();

                ItemStack sourcePage = inv.getStackInSlot(0); // Слот 0: Оригинал
                ItemStack paper = inv.getStackInSlot(1);      // Слот 1: Бумага
                ItemStack outputSlot = inv.getStackInSlot(2); // Слот 2: Результат

                ItemStack cyanDye = inv.getStackInSlot(3);    // Слот 3: Cyan
                ItemStack magentaDye = inv.getStackInSlot(4); // Слот 4: Magenta
                ItemStack yellowDye = inv.getStackInSlot(5);  // Слот 5: Yellow
                ItemStack blackDye = inv.getStackInSlot(6);   // Слот 6: Black
                ItemStack catalyst = inv.getStackInSlot(7);   // Слот 7: Катализатор (Редстоун / Глоустоун / Жемчуг Эндера)

                boolean hasPaper = !paper.isEmpty() && (paper.is(ModItems.EMPTY_PAGE.get()) || paper.is(Items.PAPER));
                boolean hasCatalyst = !catalyst.isEmpty() && (
                        catalyst.is(Items.REDSTONE) ||
                                catalyst.is(Items.GLOWSTONE_DUST) ||
                                catalyst.is(Items.ENDER_PEARL)
                );

                if (!sourcePage.isEmpty() && sourcePage.is(ModItems.SKETCHED_PAGE.get())
                        && hasPaper && hasCatalyst && outputSlot.isEmpty()) {

                    SketchData sketchData = sourcePage.get(ModDataComponents.PAGE_DATA.get());
                    if (sketchData != null && !sketchData.isEmpty()) {

                        boolean needsCyan = false;
                        boolean needsMagenta = false;
                        boolean needsYellow = false;
                        boolean needsBlack = false;

                        int[][] pixels = sketchData.getRawPixels();
                        for (int x = 0; x < pixels.length; x++) {
                            for (int y = 0; y < pixels[x].length; y++) {
                                int argb = pixels[x][y];
                                if (argb != 0) {
                                    int r = (argb >> 16) & 0xFF;
                                    int g = (argb >> 8) & 0xFF;
                                    int b = argb & 0xFF;

                                    if (r < 80 && g < 80 && b < 80) {
                                        needsBlack = true;
                                    } else {
                                        if (b > r && b > g) needsCyan = true;
                                        if (r > g && b > g) needsMagenta = true;
                                        if (r > b && g > b) needsYellow = true;
                                    }
                                }
                            }
                        }

                        boolean dyesSatisfied = (!needsCyan || !cyanDye.isEmpty())
                                && (!needsMagenta || !magentaDye.isEmpty())
                                && (!needsYellow || !yellowDye.isEmpty())
                                && (!needsBlack || !blackDye.isEmpty());

                        if (dyesSatisfied) {
                            ItemStack printedPage = new ItemStack(ModItems.SKETCHED_PAGE.get());
                            printedPage.set(ModDataComponents.PAGE_DATA.get(), sketchData);

                            paper.shrink(1);
                            catalyst.shrink(1);

                            if (needsCyan && !cyanDye.isEmpty()) cyanDye.shrink(1);
                            if (needsMagenta && !magentaDye.isEmpty()) magentaDye.shrink(1);
                            if (needsYellow && !yellowDye.isEmpty()) yellowDye.shrink(1);
                            if (needsBlack && !blackDye.isEmpty()) blackDye.shrink(1);

                            inv.insertItem(2, printedPage, false);

                            if (catalyst.is(Items.ENDER_PEARL)) {
                                player.level().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.6f, 1.2f);
                            } else {
                                player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0f, 1.0f);
                            }
                        }
                    }
                }
            }
        });
    }

    // --- ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ И ИЗНОС В ПЕНАЛЕ ---
    private static List<SketchData> createEmptyPages() {
        List<SketchData> pages = new ArrayList<>();
        SketchData emptyData = SketchData.fromArray(new int);
        for (int i = 0; i < 16; i++) {
            pages.add(emptyData);
        }
        return pages;
    }

    private static void damageTool(Player player, ServerLevel level, Item toolItem, int damageAmount) {
        if (damageAmount <= 0) return;

        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(toolItem)) {
                stack.hurtAndBreak(damageAmount, level, (ServerPlayer) player, p -> {});
                return;
            }
            if (damageInPencilCase(stack, toolItem, damageAmount, level, player)) return;
        }

        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.is(toolItem)) {
                stack.hurtAndBreak(damageAmount, level, (ServerPlayer) player, p -> {});
                return;
            }
            if (damageInPencilCase(stack, toolItem, damageAmount, level, player)) return;
        }
    }

    private static boolean damageInPencilCase(ItemStack containerStack, Item toolItem, int damageAmount, ServerLevel level, Player player) {
        if (containerStack.is(ModItems.PENCIL_CASE.get()) && containerStack.has(DataComponents.CONTAINER)) {
            ItemContainerContents contents = containerStack.get(DataComponents.CONTAINER);
            if (contents != null) {
                NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);
                contents.copyInto(items);
                boolean foundAndDamaged = false;

                for (int i = 0; i < items.size(); i++) {
                    ItemStack innerStack = items.get(i);
                    if (innerStack.is(toolItem)) {
                        innerStack.hurtAndBreak(damageAmount, level, (ServerPlayer) player, p -> {});
                        items.set(i, innerStack);
                        foundAndDamaged = true;
                        break;
                    }
                }

                if (foundAndDamaged) {
                    containerStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
                    return true;
                }
            }
        }
        return false;
    }
}