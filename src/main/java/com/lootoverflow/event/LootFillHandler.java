package com.lootoverflow.event;

import com.lootoverflow.LootOverflowMod;
import com.lootoverflow.capability.LootOverflowCapabilities;
import com.lootoverflow.capability.OverflowInventory;
import com.lootoverflow.compat.DoubleChestUtil;
import com.lootoverflow.compat.LootTableCompat;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Pre-empts vanilla's own loot-table unpacking for chests and barrels so that when a loot
 * table would generate more stacks than the container has slots for, the extra stacks land
 * in an {@link OverflowInventory} instead of being silently discarded.
 *
 * <p>This runs at LOW priority on {@link PlayerInteractEvent.RightClickBlock} and bails out
 * immediately if the event is already cancelled - that way any protection/claim mod that
 * denies access to the chest still works exactly as it would without this mod installed.
 * We never cancel the event ourselves; vanilla's own chest-opening flow proceeds completely
 * unmodified afterwards; the GUI itself is swapped out separately in
 * {@link ContainerOpenHandler}.</p>
 */
@Mod.EventBusSubscriber(modid = LootOverflowMod.MOD_ID)
public final class LootFillHandler {

    private LootFillHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled()) {
            return;
        }
        Level level = event.getLevel();
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        boolean isChest = block instanceof ChestBlock;
        boolean isBarrel = block instanceof BarrelBlock;
        if (!isChest && !isBarrel) {
            return;
        }
        if (isLootrOwned(block)) {
            // Lootr generates independent loot per player from this same block and must
            // remain in full control of its loot table / seed fields.
            return;
        }

        Player player = event.getEntity();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        unpackIfNeeded(blockEntity, serverLevel, pos, player);

        if (isChest && DoubleChestUtil.isDouble(state)) {
            Direction toNeighbor = DoubleChestUtil.directionToOtherHalf(state);
            BlockPos neighborPos = pos.relative(toNeighbor);
            if (level.getBlockState(neighborPos).getBlock() == block) {
                unpackIfNeeded(level.getBlockEntity(neighborPos), serverLevel, neighborPos, player);
            }
        }
    }

    private static boolean isLootrOwned(Block block) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
        return id != null && "lootr".equals(id.getNamespace());
    }

    private static void unpackIfNeeded(BlockEntity blockEntity, ServerLevel level, BlockPos pos, Player player) {
        if (!(blockEntity instanceof RandomizableContainer container)) {
            return;
        }
        ResourceLocation lootTableId = container.getLootTable();
        if (lootTableId == null) {
            return;
        }
        long seed = container.getLootTableSeed();
        container.setLootTable(null);

        if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.GENERATE_LOOT.trigger(serverPlayer, lootTableId);
        }

        LootTable table = level.getServer().getLootData().getLootTable(lootTableId);
        LootParams.Builder builder = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos));
        if (player != null) {
            builder.withLuck(player.getLuck()).withParameter(LootContextParams.THIS_ENTITY, player);
        }
        LootParams params = builder.create(LootContextParamSets.CHEST);

        List<ItemStack> generated = LootTableCompat.getRandomItems(table, params, seed);
        if (generated.isEmpty()) {
            blockEntity.setChanged();
            return;
        }

        List<ItemStack> merged = mergeStacks(generated);
        Container realContainer = (Container) blockEntity;
        int realSize = realContainer.getContainerSize();

        List<ItemStack> overflow = new ArrayList<>();
        for (int i = 0; i < merged.size(); i++) {
            ItemStack stack = merged.get(i);
            if (i < realSize) {
                realContainer.setItem(i, stack);
            } else {
                overflow.add(stack);
            }
        }

        if (!overflow.isEmpty()) {
            OverflowInventory inventory = LootOverflowCapabilities.get(blockEntity);
            if (inventory != null) {
                inventory.addAll(overflow);
            } else {
                LootOverflowMod.LOGGER.warn("LootOverflow: {} at {} has overflow loot but no overflow "
                        + "capability attached; those items will be lost.", blockEntity.getType(), pos);
            }
        }
        blockEntity.setChanged();
    }

    /**
     * Combines stacks of the same item into as few stacks as possible (up to max stack size),
     * mirroring what vanilla's own loot-filling does to make the most of limited slots.
     */
    private static List<ItemStack> mergeStacks(List<ItemStack> input) {
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

    private static boolean sameKind(ItemStack a, ItemStack b) {
        return !a.isEmpty() && !b.isEmpty() && a.getItem() == b.getItem() && ItemStack.tagMatches(a, b);
    }
}
