package com.lootoverflow.compat;

import com.lootoverflow.LootOverflowMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Method;

/**
 * Vanilla's {@code ChestBlock} exposes a public {@code getContainer(state, level, pos, allowBlocked)}
 * method that correctly merges a double chest into one {@link Container} (or returns null if the
 * chest is blocked). It's resolved via reflection here purely so a subtle signature difference
 * between game versions degrades to "single-half overflow support" instead of a compile failure.
 */
public final class ChestContainerCompat {

    private static volatile Method getContainerMethod;
    private static volatile boolean resolved;

    private ChestContainerCompat() {
    }

    public static Container getMergedContainer(Block chestBlock, BlockState state, Level level, BlockPos pos, boolean allowBlocked) {
        Method method = resolve(chestBlock);
        if (method == null) {
            return null;
        }
        try {
            return (Container) method.invoke(chestBlock, state, level, pos, allowBlocked);
        } catch (ReflectiveOperationException | ClassCastException e) {
            LootOverflowMod.LOGGER.error("LootOverflow: failed to resolve merged chest container", e);
            return null;
        }
    }

    private static Method resolve(Block chestBlock) {
        if (!resolved) {
            synchronized (ChestContainerCompat.class) {
                if (!resolved) {
                    try {
                        getContainerMethod = chestBlock.getClass().getMethod(
                                "getContainer", BlockState.class, Level.class, BlockPos.class, boolean.class);
                    } catch (NoSuchMethodException e) {
                        LootOverflowMod.LOGGER.warn("LootOverflow: ChestBlock#getContainer not found on this game "
                                + "version; double chests will only expose the overflow GUI on the half that "
                                + "was directly right-clicked.");
                    }
                    resolved = true;
                }
            }
        }
        return getContainerMethod;
    }
}
