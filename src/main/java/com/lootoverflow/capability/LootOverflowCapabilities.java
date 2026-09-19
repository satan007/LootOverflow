package com.lootoverflow.capability;

import com.lootoverflow.LootOverflowMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.event.AttachCapabilitiesEvent;

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

    /**
     * Attaches a fresh, NBT-persisted {@link OverflowInventory} to {@code owner} in response to an
     * {@link AttachCapabilitiesEvent}. Exposed so a separate addon mod can give some other block
     * entity type (e.g. a different mod's own container class) the same persisted overflow storage
     * this mod uses for vanilla chests/barrels, from the addon's own event listener, without having
     * to reimplement {@link OverflowInventory}'s NBT (de)serialization itself.
     */
    public static void attach(AttachCapabilitiesEvent<BlockEntity> event, BlockEntity owner) {
        event.addCapability(new ResourceLocation(LootOverflowMod.MOD_ID, "overflow_inventory"),
                new OverflowProvider(owner));
    }
}
