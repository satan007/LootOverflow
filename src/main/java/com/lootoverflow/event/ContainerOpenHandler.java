package com.lootoverflow.event;

import com.lootoverflow.LootOverflowMod;
import com.lootoverflow.capability.LootOverflowCapabilities;
import com.lootoverflow.capability.OverflowInventory;
import com.lootoverflow.compat.ChestContainerCompat;
import com.lootoverflow.compat.DoubleChestUtil;
import com.lootoverflow.menu.LootOverflowMenus;
import com.lootoverflow.menu.OverflowChestMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Vanilla has no extension point for adding a slot to {@link ChestMenu}, and that menu
 * doesn't expose the {@link Container} it wraps, so we can't build our replacement menu
 * ahead of time. Instead we let vanilla open its normal chest/barrel menu exactly as it
 * always would (respecting every other mod's access checks along the way), and the instant
 * {@link PlayerContainerEvent.Open} tells us that succeeded, we immediately reopen our own
 * menu wrapping the same underlying container. Opening a new menu automatically replaces
 * the player's current one, so there's no separate "close" step needed.
 */
@Mod.EventBusSubscriber(modid = LootOverflowMod.MOD_ID)
public final class ContainerOpenHandler {

    private static final long PENDING_TIMEOUT_MS = 2000L;
    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    private ContainerOpenHandler() {
    }

    private record Pending(BlockPos pos, long timestamp) {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // Runs at default priority, strictly to remember "this player is about to open a
        // container here" - LootFillHandler (LOW priority) has already dealt with loot by
        // the time this fires relative to other mods, but recording the position doesn't
        // depend on that ordering.
        if (event.isCanceled()) {
            return;
        }
        Level level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }
        Block block = level.getBlockState(event.getPos()).getBlock();
        if (block instanceof ChestBlock || block instanceof BarrelBlock) {
            PENDING.put(event.getEntity().getUUID(), new Pending(event.getPos(), System.currentTimeMillis()));
        }
    }

    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        Player player = event.getEntity();
        Pending pending = PENDING.remove(player.getUUID());
        if (pending == null || player.level().isClientSide()) {
            return;
        }
        if (System.currentTimeMillis() - pending.timestamp() > PENDING_TIMEOUT_MS) {
            return;
        }
        AbstractContainerMenu opened = event.getContainer();
        if (!(opened instanceof ChestMenu)) {
            // Not a plain vanilla chest/barrel menu - either unrelated, or already our own
            // OverflowChestMenu (which doesn't extend ChestMenu), so there's nothing to replace.
            return;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        Level level = player.level();
        BlockPos pos = pending.pos();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);
        if (blockId != null && "lootr".equals(blockId.getNamespace())) {
            return;
        }

        Container container;
        Component title;
        List<OverflowInventory> sources = new ArrayList<>();
        int rows;

        if (block instanceof ChestBlock) {
            container = ChestContainerCompat.getMergedContainer(block, state, level, pos, true);
            BlockEntity primary = level.getBlockEntity(pos);
            boolean isDouble = DoubleChestUtil.isDouble(state);
            if (container == null) {
                // Reflection fallback couldn't find getContainer() - degrade to single-half only.
                if (!(primary instanceof Container primaryContainer)) {
                    return;
                }
                container = primaryContainer;
            }
            rows = container.getContainerSize() / 9;
            addSource(sources, primary);
            if (isDouble) {
                Direction toNeighbor = DoubleChestUtil.directionToOtherHalf(state);
                addSource(sources, level.getBlockEntity(pos.relative(toNeighbor)));
                title = Component.translatable("container.chestDouble");
            } else {
                title = primary instanceof MenuProvider mp ? mp.getDisplayName() : Component.translatable("container.chest");
            }
        } else if (block instanceof BarrelBlock) {
            BlockEntity barrelEntity = level.getBlockEntity(pos);
            if (!(barrelEntity instanceof Container barrelContainer)) {
                return;
            }
            container = barrelContainer;
            rows = 3;
            addSource(sources, barrelEntity);
            title = barrelEntity instanceof MenuProvider mp ? mp.getDisplayName() : Component.translatable("container.barrel");
        } else {
            return;
        }

        Container finalContainer = container;
        Component finalTitle = title;
        int finalRows = rows;
        List<OverflowInventory> finalSources = sources;
        boolean hasOverflow = sources.stream().anyMatch(s -> !s.isEmpty());

        NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return finalTitle;
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player p) {
                return OverflowChestMenu.createServer(LootOverflowMenus.OVERFLOW_CHEST.get(), id, playerInventory,
                        finalContainer, finalRows, finalSources);
            }
        }, buf -> {
            buf.writeByte(finalRows);
            buf.writeBoolean(hasOverflow);
        });
    }

    private static void addSource(List<OverflowInventory> list, BlockEntity blockEntity) {
        OverflowInventory inventory = LootOverflowCapabilities.get(blockEntity);
        if (inventory != null) {
            list.add(inventory);
        }
    }
}
