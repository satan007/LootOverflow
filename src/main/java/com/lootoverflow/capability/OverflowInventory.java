package com.lootoverflow.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the loot that didn't fit into a container's visible slots.
 * Persisted through the owning block entity's own NBT via {@link OverflowProvider}.
 */
public class OverflowInventory implements INBTSerializable<CompoundTag> {

    /** Hard safety cap so a pathological loot table can't grow this list without bound. */
    private static final int MAX_ITEMS = 1000;

    private final List<ItemStack> items = new ArrayList<>();
    private final Runnable onChange;

    public OverflowInventory(Runnable onChange) {
        this.onChange = onChange;
    }

    public boolean isEmpty() {
        return this.items.isEmpty();
    }

    public int size() {
        return this.items.size();
    }

    public void addAll(List<ItemStack> stacks) {
        if (stacks.isEmpty()) {
            return;
        }
        for (ItemStack stack : stacks) {
            if (this.items.size() >= MAX_ITEMS) {
                break;
            }
            if (!stack.isEmpty()) {
                this.items.add(stack);
            }
        }
        this.onChange.run();
    }

    /** Returns the item that would be handed out next, without removing it. */
    public ItemStack peekNext() {
        return this.items.isEmpty() ? ItemStack.EMPTY : this.items.get(this.items.size() - 1);
    }

    /** Removes and returns the last hidden item, or {@link ItemStack#EMPTY} if there is none. */
    public ItemStack removeNext() {
        if (this.items.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = this.items.remove(this.items.size() - 1);
        this.onChange.run();
        return stack;
    }

    /** Puts an item back (used when it couldn't be placed anywhere after being pulled out). */
    public void addBack(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        this.items.add(stack);
        this.onChange.run();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (ItemStack stack : this.items) {
            list.add(stack.save(new CompoundTag()));
        }
        tag.put("Items", list);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        this.items.clear();
        ListTag list = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = ItemStack.of(list.getCompound(i));
            if (!stack.isEmpty()) {
                this.items.add(stack);
            }
        }
    }
}
