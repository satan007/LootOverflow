package com.lootoverflow.compat;

import com.lootoverflow.LootOverflowMod;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link LootTable#getRandomItems} has carried a couple of different overloads across
 * Minecraft versions (with and without an explicit seed). Rather than hard-coding one
 * signature and risking a compile break against whatever exact build of the game is used,
 * this looks the method up once via reflection and calls whichever overload is present.
 */
public final class LootTableCompat {

    private static final Method GET_RANDOM_ITEMS_SEEDED = find("getRandomItems", LootParams.class, long.class);
    private static final Method GET_RANDOM_ITEMS = find("getRandomItems", LootParams.class);

    private LootTableCompat() {
    }

    private static Method find(String name, Class<?>... paramTypes) {
        try {
            Method method = LootTable.class.getMethod(name, paramTypes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static List<ItemStack> getRandomItems(LootTable table, LootParams params, long seed) {
        try {
            if (seed != 0L && GET_RANDOM_ITEMS_SEEDED != null) {
                Object result = GET_RANDOM_ITEMS_SEEDED.invoke(table, params, seed);
                return new ArrayList<>((List<ItemStack>) result);
            }
            if (GET_RANDOM_ITEMS != null) {
                Object result = GET_RANDOM_ITEMS.invoke(table, params);
                return new ArrayList<>((List<ItemStack>) result);
            }
        } catch (ReflectiveOperationException | ClassCastException e) {
            LootOverflowMod.LOGGER.error("LootOverflow: could not generate loot items via reflection", e);
        }
        LootOverflowMod.LOGGER.warn("LootOverflow: no compatible LootTable#getRandomItems overload found; "
                + "loot overflow protection is disabled for this container.");
        return Collections.emptyList();
    }
}
