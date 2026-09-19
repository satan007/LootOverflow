package com.lootoverflow.compat;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;

/** Small helper mirroring vanilla's own "which way is the other half of this double chest" logic. */
public final class DoubleChestUtil {

    private DoubleChestUtil() {
    }

    public static boolean isDouble(BlockState state) {
        return state.hasProperty(ChestBlock.TYPE) && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE;
    }

    public static Direction directionToOtherHalf(BlockState state) {
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        ChestType type = state.getValue(ChestBlock.TYPE);
        return type == ChestType.LEFT ? facing.getClockWise() : facing.getCounterClockWise();
    }
}
