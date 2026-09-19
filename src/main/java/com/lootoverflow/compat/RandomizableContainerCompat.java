package com.lootoverflow.compat;

import com.lootoverflow.LootOverflowMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;

import java.lang.reflect.Field;

/**
 * In Minecraft 1.20.1, {@code RandomizableContainerBlockEntity} (the common superclass of
 * chests and barrels) keeps its pending {@code lootTable}/{@code lootTableSeed} in plain
 * protected fields with no public getter/setter (that came in a later version) - so reading
 * and clearing them from outside the class needs reflection rather than a normal method call.
 * Fields are located by type rather than by exact name, since that class declares exactly one
 * {@link ResourceLocation} field and one {@code long} field, which is more resilient to a
 * field being renamed than hard-coding "lootTable"/"lootTableSeed" would be.
 */
public final class RandomizableContainerCompat {

    private static final Field LOOT_TABLE_FIELD = findField(ResourceLocation.class);
    private static final Field LOOT_TABLE_SEED_FIELD = findField(long.class);

    private RandomizableContainerCompat() {
    }

    public static boolean isApplicable(BlockEntity blockEntity) {
        return blockEntity instanceof RandomizableContainerBlockEntity;
    }

    private static Field findField(Class<?> type) {
        for (Field field : RandomizableContainerBlockEntity.class.getDeclaredFields()) {
            if (field.getType() == type) {
                field.setAccessible(true);
                return field;
            }
        }
        LootOverflowMod.LOGGER.warn("LootOverflow: could not find a {} field on "
                + "RandomizableContainerBlockEntity; overflow protection is disabled for chests/barrels "
                + "on this game version.", type.getSimpleName());
        return null;
    }

    @javax.annotation.Nullable
    public static ResourceLocation getLootTable(BlockEntity blockEntity) {
        if (LOOT_TABLE_FIELD == null) {
            return null;
        }
        try {
            return (ResourceLocation) LOOT_TABLE_FIELD.get(blockEntity);
        } catch (ReflectiveOperationException | ClassCastException e) {
            LootOverflowMod.LOGGER.error("LootOverflow: failed to read the loot table field", e);
            return null;
        }
    }

    public static void clearLootTable(BlockEntity blockEntity) {
        if (LOOT_TABLE_FIELD == null) {
            return;
        }
        try {
            LOOT_TABLE_FIELD.set(blockEntity, null);
        } catch (ReflectiveOperationException e) {
            LootOverflowMod.LOGGER.error("LootOverflow: failed to clear the loot table field", e);
        }
    }

    public static long getLootTableSeed(BlockEntity blockEntity) {
        if (LOOT_TABLE_SEED_FIELD == null) {
            return 0L;
        }
        try {
            return LOOT_TABLE_SEED_FIELD.getLong(blockEntity);
        } catch (ReflectiveOperationException e) {
            LootOverflowMod.LOGGER.error("LootOverflow: failed to read the loot table seed field", e);
            return 0L;
        }
    }
}
