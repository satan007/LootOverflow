package com.lootoverflow.capability;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public final class LootOverflowCapabilities {

    public static final Capability<OverflowInventory> OVERFLOW_INVENTORY =
            CapabilityManager.get(new CapabilityToken<>() {});

    private LootOverflowCapabilities() {
    }

    /** Convenience accessor; returns {@code null} if the block entity has no overflow capability attached. */
    public static OverflowInventory get(BlockEntity blockEntity) {
        if (blockEntity == null) {
            return null;
        }
        return blockEntity.getCapability(OVERFLOW_INVENTORY).resolve().orElse(null);
    }
}
