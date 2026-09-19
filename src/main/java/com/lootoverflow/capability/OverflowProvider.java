package com.lootoverflow.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Attaches an {@link OverflowInventory} to a block entity. Because this implements
 * {@link ICapabilitySerializable}, Forge automatically persists {@link #serializeNBT()}
 * into the block entity's own "ForgeCaps" NBT on save/load - no extra wiring needed.
 */
public class OverflowProvider implements ICapabilitySerializable<CompoundTag> {

    private final OverflowInventory inventory;
    private final LazyOptional<OverflowInventory> lazy;

    public OverflowProvider(BlockEntity owner) {
        this.inventory = new OverflowInventory(owner::setChanged);
        this.lazy = LazyOptional.of(() -> this.inventory);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return LootOverflowCapabilities.OVERFLOW_INVENTORY.orEmpty(cap, this.lazy);
    }

    @Override
    public CompoundTag serializeNBT() {
        return this.inventory.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        this.inventory.deserializeNBT(tag);
    }
}
