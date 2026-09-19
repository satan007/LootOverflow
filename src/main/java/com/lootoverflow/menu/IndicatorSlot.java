package com.lootoverflow.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A purely visual/informational slot: it never accepts items and never yields them through
 * normal pickup/drag - {@link OverflowChestMenu#clicked} intercepts clicks on it directly and
 * performs the actual extraction, so vanilla's slot-transfer machinery never touches it.
 */
public class IndicatorSlot extends Slot {

    public IndicatorSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayPickup(Player player) {
        return false;
    }
}
