package net.squaremarkers.fabric.helpers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.portal.PortalShape;
import net.squaremarkers.fabric.interfaces.NetherPortalInterface;
import org.jetbrains.annotations.Nullable;

public class PortalHelper {

    @Nullable
    public static BlockPos findAffectedNetherPortalCenter(LevelAccessor level, BlockPos pos, BlockState state) {
        if (state.is(Blocks.NETHER_PORTAL)) {
            return getNetherPortalCenter(level, pos, state);
        }
        if (!state.is(Blocks.OBSIDIAN)) {
            return null;
        }
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = pos.relative(direction);
            BlockState adjacentState = level.getBlockState(adjacentPos);
            if (adjacentState.is(Blocks.NETHER_PORTAL)) {
                BlockPos center = getNetherPortalCenter(level, adjacentPos, adjacentState);
                if (center != null) {
                    return center;
                }
            }
        }
        return null;
    }

    @Nullable
    private static BlockPos getNetherPortalCenter(LevelAccessor level, BlockPos portalPos, BlockState portalState) {
        Direction.Axis axis = portalState.getValue(BlockStateProperties.HORIZONTAL_AXIS);
        PortalShape portalShape = PortalShape.findAnyShape(level, portalPos, axis);
        return portalShape instanceof NetherPortalInterface portal
            ? portal.squareMarkers$getPortalCenter()
            : null;
    }

    public static BlockPos getNetherPortalCenter(BlockPos lowerCorner, Direction.Axis axis, int width) {
        if (lowerCorner == null) return null;
        if (axis == Direction.Axis.X) {
            return lowerCorner.offset((int) Math.ceil(width / -2.0), 0, 0);
        } else {
            return lowerCorner.offset(0, 0, (int) Math.floor(width / 2.0));
        }
    }

    public static BlockPos getEndPortalCenter(Level world, BlockPos pos) {
        // if there is no portal block north, move south
        if (!world.getBlockState(pos.north()).is(Blocks.END_PORTAL)) {
            pos = pos.south();
        }
        // if there is no portal block south, move north
        if (!world.getBlockState(pos.south()).is(Blocks.END_PORTAL)) {
            pos = pos.north();
        }
        // if there is no portal block east, move west
        if (!world.getBlockState(pos.east()).is(Blocks.END_PORTAL)) {
            pos = pos.west();
        }
        // if there is no portal block west, move east
        if (!world.getBlockState(pos.west()).is(Blocks.END_PORTAL)) {
            pos = pos.east();
        }
        return pos;
    }

}
