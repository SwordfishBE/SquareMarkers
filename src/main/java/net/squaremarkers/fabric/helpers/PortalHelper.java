package net.squaremarkers.fabric.helpers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.portal.PortalShape;
import net.squaremarkers.fabric.interfaces.NetherPortalInterface;
import net.squaremarkers.core.SquareMarkersCore;
import net.squaremarkers.core.layers.NetherPortalMarkerLayer;
import net.squaremarkers.core.registries.Layers;
import org.jetbrains.annotations.Nullable;
import java.util.function.Predicate;
import java.util.function.Function;

public class PortalHelper {

    @Nullable
    public static BlockPos getEndPortalCenterFromFrame(Level world, BlockPos pos, BlockState state) {
        if (!(world instanceof ServerLevel level)) return null;
        return findActiveEndPortalFromFrame(pos, state.getValue(BlockStateProperties.HORIZONTAL_FACING),
            block -> {
                BlockState loaded = loadedState(level, block);
                return loaded != null && loaded.is(Blocks.END_PORTAL);
            });
    }

    /** At most three candidate centers and nine portal blocks each; never searches the world. */
    @Nullable
    static BlockPos findActiveEndPortalFromFrame(BlockPos frame, Direction inward, Predicate<BlockPos> isPortal) {
        if (inward.getAxis() == Direction.Axis.Y) return null;
        BlockPos middle = frame.relative(inward, 2);
        Direction sideways = inward.getClockWise();
        for (int offset = -1; offset <= 1; offset++) {
            BlockPos center = middle.relative(sideways, offset);
            boolean complete = true;
            for (int x = -1; x <= 1 && complete; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (!isPortal.test(center.offset(x, 0, z))) { complete = false; break; }
                }
            }
            if (complete) return center;
        }
        return null;
    }

    @Nullable
    public static BlockPos findAffectedNetherPortalCenter(LevelAccessor level, BlockPos pos, BlockState state) {
        return scanAffectedNetherPortal(level, pos, state).center();
    }

    public record PortalScan(@Nullable BlockPos center, boolean incomplete) {}

    public static PortalScan scanAffectedNetherPortal(LevelAccessor level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel) || !hasNetherPortalLayer(serverLevel)) return new PortalScan(null, false);
        boolean[] incomplete = {false};
        Function<BlockPos, BlockState> reader = block -> {
            BlockState loaded = loadedState(serverLevel, block);
            if (loaded == null) incomplete[0] = true;
            return loaded;
        };
        if (state.is(Blocks.NETHER_PORTAL)) {
            BlockPos center = netherPortalCenter(pos, state, serverLevel.getMinY(), serverLevel.getHeight(), reader);
            return new PortalScan(center, incomplete[0]);
        }
        if (!state.is(Blocks.OBSIDIAN)) {
            return new PortalScan(null, false);
        }
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = pos.relative(direction);
            BlockState adjacentState = reader.apply(adjacentPos);
            if (adjacentState != null && adjacentState.is(Blocks.NETHER_PORTAL)) {
                BlockPos center = netherPortalCenter(adjacentPos, adjacentState, serverLevel.getMinY(), serverLevel.getHeight(), reader);
                if (center != null) {
                    return new PortalScan(center, incomplete[0]);
                }
            }
        }
        return new PortalScan(null, incomplete[0]);
    }

    @Nullable
    public static BlockPos getLoadedNetherPortalCenter(ServerLevel level, BlockPos portalPos, BlockState portalState) {
        return netherPortalCenter(portalPos, portalState, level.getMinY(), level.getHeight(), pos -> loadedState(level, pos));
    }

    @Nullable
    private static BlockPos netherPortalCenter(BlockPos portalPos, BlockState portalState, int minY, int height,
                                              Function<BlockPos, BlockState> reader) {
        Direction.Axis axis = portalState.getValue(BlockStateProperties.HORIZONTAL_AXIS);
        PortalShape portalShape = loadedShape(portalPos, axis, minY, height, reader);
        return portalShape instanceof NetherPortalInterface portal
            ? portal.squareMarkers$getPortalCenter()
            : null;
    }

    public static boolean hasNetherPortalLayer(ServerLevel level) {
        return SquareMarkersCore.squaremapHandler().getLayer(level.dimension().identifier().toString(),
            NetherPortalMarkerLayer.class, Layers.Keys.NETHER_PORTALS) != null;
    }

    @Nullable
    public static BlockState loadedState(ServerLevel level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos)) return Blocks.AIR.defaultBlockState();
        var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk == null ? null : chunk.getBlockState(pos);
    }

    /** Vanilla geometry, with a reader that cannot load or wait for chunks. Null means incomplete. */
    @Nullable
    static PortalShape loadedShape(BlockPos pos, Direction.Axis axis, int minY, int height,
                                   Function<BlockPos, BlockState> loadedState) {
        BlockGetter view = new BlockGetter() {
            @Override public BlockState getBlockState(BlockPos block) {
                BlockState state = loadedState.apply(block);
                if (state == null) throw MissingChunk.INSTANCE;
                return state;
            }
            @Override public BlockEntity getBlockEntity(BlockPos block) { return null; }
            @Override public FluidState getFluidState(BlockPos block) { return getBlockState(block).getFluidState(); }
            @Override public int getMinY() { return minY; }
            @Override public int getHeight() { return height; }
        };
        try {
            return PortalShape.findAnyShape(view, pos, axis);
        } catch (MissingChunk ignored) {
            // Never infer a smaller portal from the edge of the available chunk data.
            return null;
        }
    }

    private static final class MissingChunk extends RuntimeException {
        static final MissingChunk INSTANCE = new MissingChunk();
        private MissingChunk() { super(null, null, false, false); }
    }

    public static BlockPos getNetherPortalCenter(BlockPos lowerCorner, Direction.Axis axis, int width) {
        if (lowerCorner == null) return null;
        if (axis == Direction.Axis.X) {
            return lowerCorner.offset((int) Math.ceil(width / -2.0), 0, 0);
        } else {
            return lowerCorner.offset(0, 0, (int) Math.floor(width / 2.0));
        }
    }

    @Nullable
    public static BlockPos getEndPortalCenter(Level world, BlockPos pos) {
        if (!(world instanceof ServerLevel level)) return null;
        for (Direction direction : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            BlockState neighbor = loadedState(level, pos.relative(direction));
            if (neighbor == null) return null;
            if (!neighbor.is(Blocks.END_PORTAL)) pos = pos.relative(direction.getOpposite());
        }
        return pos;
    }

}
