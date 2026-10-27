package net.avizvul.esquissemod.network;

import net.avizvul.esquissemod.EsquisseMod;
import net.avizvul.esquissemod.component.ModDataComponents;
import net.avizvul.esquissemod.component.SketchData;
import net.avizvul.esquissemod.item.ModItems;
import net.avizvul.esquissemod.menu.PrinterMenu;
import net.avizvul.esquissemod.util.ToolWear;
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
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.avizvul.esquissemod.item.custom.StampItem;

import java.util.ArrayList;
import java.util.List;

public class SketchbookPayloadHandler {

    // Helper для создания 16 полных страниц (126x192)
    private static List<SketchData> createDefaultPages() {
        List<SketchData> list = new ArrayList<>(16);
        SketchData emptyData = SketchData.fromArray(new int[126][192]);
        for (int i = 0; i < 16; i++) {
            list.add(emptyData);
        }
        return list;
    }

    public void handleData(final SketchbookSavePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            ItemStack stack = player.getMainHandItem();
            if (!stack.is(ModItems.SKETCHBOOK.get())) {
                stack = player.getOffhandItem();
            }

            if (stack.is(ModItems.SKETCHBOOK.get())) {
                List<SketchData> pages = new ArrayList<>(stack.getOrDefault(ModDataComponents.SKETCHBOOK_PAGES.get(), new ArrayList<>()));
                if (pages.isEmpty()) {
                    pages = createDefaultPages();
                }

                if (payload.pageIndex() >= 0 && payload.pageIndex() < pages.size()) {
                    pages.set(payload.pageIndex(), payload.sketchData());
                    stack.set(ModDataComponents.SKETCHBOOK_PAGES.get(), pages);
                    stack.set(ModDataComponents.LAST_PAGE.get(), payload.pageIndex());
                }
            }

            if (player.level() instanceof ServerLevel) {
                applyWear(player, payload.pencilPixels(), payload.eraserPixels(),
                        payload.kneadedPixels(), payload.smudgePixels());
            }
        });
    }

    public void handlePaintUsage(final PaintUsagePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player.getAbilities().instabuild) return;
            Item target = paintToolItem(payload.toolId());
            if (target == null) return;
            modifyTool(player, target, stack -> ToolWear.consumePaint(stack, payload.units()));
        });
    }

    private static void applyWear(Player player, int pencil, int eraser, int kneaded, int smudge) {
        damageTool(player, ModItems.PENCIL.get(), pencil / ToolWear.pixelsPerDamage(ModItems.PENCIL.get()));
        damageTool(player, ModItems.ERASER.get(), eraser / ToolWear.pixelsPerDamage(ModItems.ERASER.get()));
        damageTool(player, ModItems.KNEADED_ERASER.get(), kneaded / ToolWear.pixelsPerDamage(ModItems.KNEADED_ERASER.get()));
        damageTool(player, ModItems.SMUDGE.get(), smudge / ToolWear.pixelsPerDamage(ModItems.SMUDGE.get()));
    }

    public void handleToolUsage(final ToolUsagePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> applyWear(context.player(),
                payload.pencilPixels(), payload.eraserPixels(), payload.kneadedPixels(), payload.smudgePixels()));
    }

    private static void damageTool(Player player, Item toolItem, int amount) {
        if (amount <= 0) return;
        if (!ToolWear.WEAR_IN_CREATIVE && player.getAbilities().instabuild) return;

        boolean found = modifyTool(player, toolItem, stack -> {
            int newDamage = stack.getDamageValue() + amount;
            if (newDamage >= stack.getMaxDamage()) {
                stack.shrink(1); // инструмент сломался
                player.level().playSound(null, player.blockPosition(),
                        SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8f, 1.0f);
            } else {
                stack.setDamageValue(newDamage);
            }
        });
        EsquisseMod.LOGGER.info("Wear {} x{} applied={}", toolItem, amount, found);
    }

    /** Ищет первый инструмент в инвентаре (включая левую руку) и внутри пенала и применяет к нему действие. */
    private static boolean modifyTool(Player player, Item target, java.util.function.Consumer<ItemStack> action) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(target)) {
                action.accept(stack);
                return true;
            }
            if (stack.is(ModItems.PENCIL_CASE.get()) && stack.has(DataComponents.CONTAINER)) {
                ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
                if (contents == null) continue;
                NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);
                contents.copyInto(items);
                for (ItemStack inner : items) {
                    if (inner.is(target)) {
                        action.accept(inner); // inner — копия, поэтому ниже перезаписываем контейнер
                        stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void handleTearPage(final TearPagePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            ItemStack stack = player.getMainHandItem();
            if (!stack.is(ModItems.SKETCHBOOK.get())) {
                stack = player.getOffhandItem();
            }

            if (stack.is(ModItems.SKETCHBOOK.get())) {
                List<SketchData> pages = new ArrayList<>(stack.getOrDefault(ModDataComponents.SKETCHBOOK_PAGES.get(), new ArrayList<>()));
                if (pages.isEmpty()) {
                    pages = createDefaultPages();
                }

                if (payload.pageIndex() >= 0 && payload.pageIndex() < pages.size()) {
                    pages.remove(payload.pageIndex());

                    SketchData tornData = payload.sketchData();
                    ItemStack tornPage;

                    if (tornData == null || tornData.isEmpty()) {
                        tornPage = new ItemStack(ModItems.EMPTY_PAGE.get());
                    } else {
                        tornPage = new ItemStack(ModItems.SKETCHED_PAGE.get());
                        tornPage.set(ModDataComponents.PAGE_DATA.get(), tornData);
                        tornPage.set(ModDataComponents.PAGE_AUTHOR.get(), player.getGameProfile().getName());
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

    public void handleChangeColor(final ChangeColorPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            Item targetItem = paintToolItem(payload.toolId());
            if (targetItem == null) return;

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

    public void handleSealStamp(final SealStampPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            ItemStack stack = payload.offHand() ? player.getOffhandItem() : player.getMainHandItem();
            if (!(stack.getItem() instanceof StampItem stamp)) return;
            if (StampItem.isSealed(stack)) return; // после запечатывания менять нельзя

            int size = stamp.getSize();
            if (payload.rows().size() != size) return;

            int mask = size >= 32 ? -1 : (1 << size) - 1;
            List<Integer> clean = new ArrayList<>(size);
            boolean any = false;
            for (int row : payload.rows()) {
                int v = row & mask;
                if (v != 0) any = true;
                clean.add(v);
            }
            if (!any) return; // пустой штамп запечатать нельзя

            stack.set(ModDataComponents.STAMP_BITS.get(), clean);

            String title = payload.name().replace("§", "").trim();
            if (title.length() > 32) title = title.substring(0, 32);
            if (!title.isEmpty()) {
                stack.set(DataComponents.CUSTOM_NAME,
                        Component.literal("\"" + title + "\"").withStyle(ChatFormatting.ITALIC));
            }

            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.INK_SAC_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
        });
    }

    /** Переносит автора и название оригинала на копию. */
    private static void copyPageMeta(ItemStack from, ItemStack to) {
        String author = from.get(ModDataComponents.PAGE_AUTHOR.get());
        if (author != null) to.set(ModDataComponents.PAGE_AUTHOR.get(), author);

        if (from.getOrDefault(ModDataComponents.PAGE_SIGNED.get(), false)) {
            to.set(ModDataComponents.PAGE_SIGNED.get(), true);
            Component name = from.get(DataComponents.CUSTOM_NAME);
            if (name != null) to.set(DataComponents.CUSTOM_NAME, name);
        }
    }

    public void handlePrinterAction(final PrinterActionPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player.containerMenu instanceof PrinterMenu printerMenu) {
                IItemHandler inv = printerMenu.getInventory();
                ItemStack sourcePage = inv.getStackInSlot(0);
                ItemStack paper = inv.getStackInSlot(1);
                ItemStack outputSlot = inv.getStackInSlot(2);
                ItemStack cyanDye = inv.getStackInSlot(3);
                ItemStack magentaDye = inv.getStackInSlot(4);
                ItemStack yellowDye = inv.getStackInSlot(5);
                ItemStack blackDye = inv.getStackInSlot(6);
                ItemStack catalyst = inv.getStackInSlot(7);

                if (sourcePage.isEmpty() || !sourcePage.is(ModItems.SKETCHED_PAGE.get())) return;
                SketchData sketchData = sourcePage.get(ModDataComponents.PAGE_DATA.get());
                if (sketchData == null || sketchData.isEmpty()) return;

                boolean isWarpEssence = !catalyst.isEmpty() && catalyst.is(ModItems.WARP_ESSENCE.get());
                boolean isPaperCatalyst = !catalyst.isEmpty() && catalyst.is(Items.GLOWSTONE_DUST);

                if (!isWarpEssence && !isPaperCatalyst) return;

                if (isPaperCatalyst) {
                    boolean hasPaper = !paper.isEmpty() && (paper.is(ModItems.EMPTY_PAGE.get()) || paper.is(Items.PAPER));
                    if (!hasPaper || !outputSlot.isEmpty()) return;
                }

                boolean hasAllDyes = !cyanDye.isEmpty() && !magentaDye.isEmpty() && !yellowDye.isEmpty() && !blackDye.isEmpty();
                if (!hasAllDyes) return;

                cyanDye.shrink(1);
                magentaDye.shrink(1);
                yellowDye.shrink(1);
                blackDye.shrink(1);
                catalyst.shrink(1);

                if (isWarpEssence) {
                    if (player instanceof ServerPlayer serverPlayer) {
                        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(serverPlayer, new ExportSketchPayload(sketchData));
                    }
                    player.level().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.6f, 1.2f);
                } else {
                    paper.shrink(1);
                    ItemStack printedPage = new ItemStack(ModItems.SKETCHED_PAGE.get());
                    printedPage.set(ModDataComponents.PAGE_DATA.get(), sketchData);
                    copyPageMeta(sourcePage, printedPage);
                    printedPage.set(ModDataComponents.PAGE_COPIED.get(), true);
                    inv.insertItem(2, printedPage, false);
                }
            }
        });
    }

    public void handleSignPage(final SignPagePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            ItemStack stack = payload.offHand() ? player.getOffhandItem() : player.getMainHandItem();
            if (!stack.is(ModItems.SKETCHED_PAGE.get())) return;
            if (stack.getOrDefault(ModDataComponents.PAGE_SIGNED.get(), false)) return;

            // Сервер не доверяет клиенту: чистим форматирующие коды и ограничиваем длину
            String title = payload.title().replace("§", "").trim();
            if (title.isEmpty()) return;
            if (title.length() > 32) title = title.substring(0, 32);

            stack.set(DataComponents.CUSTOM_NAME,
                    Component.literal("\"" + title + "\"").withStyle(ChatFormatting.ITALIC));
            stack.set(ModDataComponents.PAGE_SIGNED.get(), true);

            // Автор обычно записан при отрыве; если нет (старая страница), автором становится подписавший
            if (!stack.has(ModDataComponents.PAGE_AUTHOR.get())) {
                stack.set(ModDataComponents.PAGE_AUTHOR.get(), player.getGameProfile().getName());
            }

            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.INK_SAC_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
        });
    }

    private static Item paintToolItem(int toolId) {
        return switch (toolId) {
            case 0 -> ModItems.COLOR_PENCIL.get();
            case 1 -> ModItems.COLOR_MARKER.get();
            case 2 -> ModItems.STAMP_PAD.get();
            default -> null;
        };
    }

}
