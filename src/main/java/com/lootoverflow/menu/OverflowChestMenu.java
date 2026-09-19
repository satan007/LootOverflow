package com.lootoverflow.menu;

import com.lootoverflow.capability.OverflowInventory;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * A vanilla-style chest/barrel menu (built directly on {@link AbstractContainerMenu} rather than
 * extending the vanilla {@code ChestMenu}, whose constructor is not available for subclassing)
 * with one extra, non-standard slot appended after the player's inventory. That slot shows a
 * preview of the next hidden item plus how many are waiting, and clicking it pulls the last
 * hidden item out into a free chest slot (or the player's inventory if the chest is full).
 * Whenever a real chest slot empties out - by hand, by shift-click, or by this same extraction -
 * the next hidden item is automatically pulled into it.
 *
 * <p>The extra slot only exists at all when there is overflow loot to show, so a chest with no
 * overflow (or one whose overflow has just been fully drained on a previous visit) opens with a
 * layout identical to a normal chest.</p>
 */
public class OverflowChestMenu extends AbstractContainerMenu {

    private final Container container;
    private final int rows;
    private final List<OverflowInventory> overflowSources;
    private final Container indicatorContainer;
    private final int indicatorIndex;

    private OverflowChestMenu(MenuType<?> type, int id, Inventory playerInventory, Container container, int rows,
                               List<OverflowInventory> overflowSources, boolean hasOverflow) {
        super(type, id);
        checkContainerSize(container, rows * 9);
        this.container = container;
        this.rows = rows;
        this.overflowSources = overflowSources;
        container.startOpen(playerInventory.player);

        int playerInvOffset = (rows - 4) * 18;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(container, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 103 + row * 18 + playerInvOffset));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 161 + playerInvOffset));
        }

        if (hasOverflow) {
            SimpleContainer indicator = new SimpleContainer(1);
            this.indicatorContainer = indicator;
            this.indicatorIndex = this.slots.size();
            int indicatorY = 161 + playerInvOffset + 18 + 16;
            this.addSlot(new IndicatorSlot(indicator, 0, 8, indicatorY));
            indicator.setItem(0, buildPreviewStack());
        } else {
            this.indicatorContainer = null;
            this.indicatorIndex = -1;
        }
    }

    public static OverflowChestMenu createServer(MenuType<?> type, int id, Inventory playerInventory,
                                                  Container container, int rows, List<OverflowInventory> sources) {
        boolean hasOverflow = sources.stream().anyMatch(source -> !source.isEmpty());
        return new OverflowChestMenu(type, id, playerInventory, container, rows, sources, hasOverflow);
    }

    public static OverflowChestMenu fromNetwork(int id, Inventory playerInventory, FriendlyByteBuf buf) {
        int rows = buf.readByte();
        boolean hasOverflow = buf.readBoolean();
        Container placeholder = new SimpleContainer(rows * 9);
        return new OverflowChestMenu(LootOverflowMenus.OVERFLOW_CHEST.get(), id, playerInventory, placeholder, rows,
                List.of(), hasOverflow);
    }

    public int getRows() {
        return this.rows;
    }

    public boolean hasIndicator() {
        return this.indicatorIndex >= 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }

    private ItemStack buildPreviewStack() {
        int total = 0;
        ItemStack next = ItemStack.EMPTY;
        for (OverflowInventory source : this.overflowSources) {
            total += source.size();
            if (next.isEmpty() && !source.isEmpty()) {
                next = source.peekNext();
            }
        }
        if (total <= 0 || next.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack preview = next.copy();
        preview.setCount(Math.max(1, Math.min(total, preview.getMaxStackSize())));
        preview.setHoverName(Component.literal("Скрытый лут (" + total + ")").withStyle(ChatFormatting.GOLD));
        return preview;
    }

    private void updateIndicator() {
        if (this.indicatorContainer != null) {
            this.indicatorContainer.setItem(0, buildPreviewStack());
        }
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (hasIndicator() && slotId == this.indicatorIndex) {
            if (clickType != ClickType.QUICK_CRAFT) {
                handleIndicatorClick(player);
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
        autoRefillEmptySlots();
    }

    private void handleIndicatorClick(Player player) {
        for (OverflowInventory source : this.overflowSources) {
            if (source.isEmpty()) {
                continue;
            }
            ItemStack stack = source.removeNext();
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack leftover = placeSomewhere(stack, player);
            if (!leftover.isEmpty()) {
                source.addBack(leftover);
            }
            break;
        }
        updateIndicator();
    }

    /** Tries a free real chest slot first, then the player's inventory; returns what didn't fit. */
    private ItemStack placeSomewhere(ItemStack stack, Player player) {
        int chestSlots = this.rows * 9;
        for (int i = 0; i < chestSlots; i++) {
            Slot slot = this.slots.get(i);
            if (slot.getItem().isEmpty() && slot.mayPlace(stack)) {
                slot.set(stack.copy());
                return ItemStack.EMPTY;
            }
        }
        ItemStack copy = stack.copy();
        player.getInventory().add(copy);
        return copy;
    }

    private void autoRefillEmptySlots() {
        if (this.overflowSources.isEmpty()) {
            return;
        }
        int chestSlots = this.rows * 9;
        boolean changed = false;
        for (int i = 0; i < chestSlots; i++) {
            Slot slot = this.slots.get(i);
            if (!slot.getItem().isEmpty()) {
                continue;
            }
            ItemStack next = pollNextOverflow();
            if (next.isEmpty()) {
                break;
            }
            slot.set(next);
            changed = true;
        }
        if (changed) {
            updateIndicator();
        }
    }

    private ItemStack pollNextOverflow() {
        for (OverflowInventory source : this.overflowSources) {
            if (!source.isEmpty()) {
                return source.removeNext();
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (hasIndicator() && index == this.indicatorIndex) {
            return ItemStack.EMPTY;
        }
        Slot slot = index >= 0 && index < this.slots.size() ? this.slots.get(index) : null;
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        int chestSlots = this.rows * 9;
        int playerInventoryEnd = chestSlots + 36;

        if (index < chestSlots) {
            if (!this.moveItemStackTo(original, chestSlots, playerInventoryEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (index < playerInventoryEnd) {
            if (!this.moveItemStackTo(original, 0, chestSlots, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (original.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (original.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, original);
        autoRefillEmptySlots();
        return copy;
    }
}
