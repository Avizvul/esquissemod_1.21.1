package net.avizvul.esquissemod.client.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.List;

public record StampTooltipData(List<Integer> rows) implements TooltipComponent {
}