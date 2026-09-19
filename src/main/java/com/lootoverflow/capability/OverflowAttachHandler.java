package com.lootoverflow.capability;

import com.lootoverflow.LootOverflowMod;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Attaches the overflow-storage capability to vanilla chest and barrel block entities.
 * Deliberately does NOT touch anything else - other mods' custom containers (Lootr's own
 * block entity classes included) are left completely untouched, so we can never interfere
 * with how they manage their own inventories.
 */
@Mod.EventBusSubscriber(modid = LootOverflowMod.MOD_ID)
public final class OverflowAttachHandler {

    private OverflowAttachHandler() {
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<BlockEntity> event) {
        BlockEntity blockEntity = event.getObject();
        if (blockEntity instanceof ChestBlockEntity || blockEntity instanceof BarrelBlockEntity) {
            LootOverflowCapabilities.attach(event, blockEntity);
        }
    }
}
