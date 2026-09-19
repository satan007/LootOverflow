package com.lootoverflow.api;

import com.lootoverflow.compat.LootTableCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Public, reusable pieces of LootOverflow's core logic, meant for a separate addon/bridge mod
 * that wants to give some OTHER mod's containers the same "don't lose loot that overflows the
 * slots" treatment (for example, a container that isn't a plain vanilla chest/barrel and so
 * isn't covered by {@code LootFillHandler} out of the box).
 *
 * <p>This class only deals with generating and splitting a list of {@link ItemStack}s - it has
 * no idea what a "chest" or a "player-specific inventory" is, so it never needs to know anything
 * about the container it's being used for. Pair it with
 * {@link com.lootoverflow.capability.LootOverflowCapabilities#attach} to persist the overflow,
 * and with {@link com.lootoverflow.menu.OverflowChestMenu#createServer} to show/extract it
 * through the same indicator-slot GUI this mod already ships.
 *
 * <p><b>Note for anyone bridging to a mod with per-player loot (e.g. Lootr):</b> everything here
 * is a plain, stateless function - it doesn't assume there is exactly one roll of loot per
 * container. A bridge for a per-player system should call {@link #generateMergedLoot} /
 * {@link #splitForCapacity} once per player and keep a separate {@code OverflowInventory} per
 * player (e.g. a {@code Map<UUID, OverflowInventory>} on its own capability), rather than the
 * single per-block-entity {@code OverflowInventory} this mod uses for vanilla chests.</p>
 */
public final class LootOverflowApi {

    private LootOverflowApi() {
    }

    /** The result of splitting a generated loot list against a container's real capacity. */
    public static final class Split {
        private final List<ItemStack> visible;
        private final List<ItemStack> overflow;

        public Split(List<ItemStack> visible, List<ItemStack> overflow) {
            this.visible = visible;
            this.overflow = overflow;
        }

        /** Items that fit within the real container's slots, in slot order. */
        public List<ItemStack> visible() {
            return this.visible;
        }

        /** Items beyond the real container's capacity - hand these to an overflow inventory. */
        public List<ItemStack> overflow() {
            return this.overflow;
        }
    }

    /** Builds the same {@link LootParams} vanilla uses to unpack a chest's loot table. */
    public static LootParams buildChestLootParams(ServerLevel level, BlockPos pos, @Nullable Player player) {
        LootParams.Builder builder = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos));
        if (player != null) {
            builder.withLuck(player.getLuck()).withParameter(LootContextParams.THIS_ENTITY, player);
        }
        return builder.create(LootContextParamSets.CHEST);
    }

    /**
     * Generates the loot table's items and combines same-item stacks (up to max stack size),
     * exactly like this mod does for vanilla chests/barrels. Returns an empty list (never null)
     * if generation isn't possible on this game version - see {@link LootTableCompat}.
     */
    public static List<ItemStack> generateMergedLoot(LootTable table, LootParams params, long seed) {
        List<ItemStack> generated = LootTableCompat.getRandomItems(table, params, seed);
        return mergeStacks(generated);
    }

    /** Combines stacks of the same item into as few stacks as possible, up to max stack size. */
    public static List<ItemStack> mergeStacks(List<ItemStack> input) {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack original : input) {
            if (original.isEmpty()) {
                continue;
            }
            ItemStack remaining = original.copy();
            for (ItemStack existing : result) {
                if (remaining.isEmpty()) {
                    break;
                }
                if (sameKind(existing, remaining)) {
                    int space = existing.getMaxStackSize() - existing.getCount();
                    if (space > 0) {
                        int move = Math.min(space, remaining.getCount());
                        existing.grow(move);
                        remaining.shrink(move);
                    }
                }
            }
            if (!remaining.isEmpty()) {
                result.add(remaining);
            }
        }
        return result;
    }

    /** Splits an already-merged item list into what fits in {@code visibleCapacity} slots and the rest. */
    public static Split splitForCapacity(List<ItemStack> mergedItems, int visibleCapacity) {
        List<ItemStack> visible = new ArrayList<>();
        List<ItemStack> overflow = new ArrayList<>();
        for (int i = 0; i < mergedItems.size(); i++) {
            ItemStack stack = mergedItems.get(i);
            if (i < visibleCapacity) {
                visible.add(stack);
            } else {
                overflow.add(stack);
            }
        }
        return new Split(visible, overflow);
    }

    private static boolean sameKind(ItemStack a, ItemStack b) {
        if (a.isEmpty() || b.isEmpty() || a.getItem() != b.getItem()) {
            return false;
        }
        return Objects.equals(a.getTag(), b.getTag());
    }
}
