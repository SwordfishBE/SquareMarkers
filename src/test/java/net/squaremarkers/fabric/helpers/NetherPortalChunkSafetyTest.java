package net.squaremarkers.fabric.helpers;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NetherPortalChunkSafetyTest {
    private static java.util.List<net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block>> originalTags;
    private static java.lang.reflect.Method bindTags;
    @BeforeAll static void bootstrap() throws ReflectiveOperationException {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        // Unit bootstrap has no datapacks. Supply vanilla's frame tag for the actual shape algorithm.
        var holder = Blocks.OBSIDIAN.builtInRegistryHolder();
        originalTags = holder.tags().toList();
        bindTags = holder.getClass().getDeclaredMethod("bindTags", java.util.Collection.class);
        bindTags.setAccessible(true);
        var tags = new java.util.ArrayList<>(originalTags);
        tags.add(net.minecraft.tags.BlockTags.NETHER_PORTAL_FRAME);
        bindTags.invoke(holder, tags);
    }

    @AfterAll static void restoreTags() throws ReflectiveOperationException {
        if (bindTags != null && originalTags != null) bindTags.invoke(Blocks.OBSIDIAN.builtInRegistryHolder(), originalTags);
    }

    private BlockState portal(BlockPos pos, Direction.Axis axis) {
        int along = axis == Direction.Axis.X ? pos.getX() : pos.getZ();
        int across = axis == Direction.Axis.X ? pos.getZ() : pos.getX();
        if (across != 0) return Blocks.AIR.defaultBlockState();
        if (along >= 14 && along <= 17 && pos.getY() >= 63 && pos.getY() <= 67) {
            if (along == 14 || along == 17 || pos.getY() == 63 || pos.getY() == 67) {
                return Blocks.OBSIDIAN.defaultBlockState();
            }
            return Blocks.NETHER_PORTAL.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_AXIS, axis);
        }
        return Blocks.AIR.defaultBlockState();
    }

    @Test void vanillaGeometryWorksAcrossLoadedChunkEdgesInBothAxes() {
        for (var axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
            BlockPos entry = axis == Direction.Axis.X ? new BlockPos(15, 65, 0) : new BlockPos(0, 65, 15);
            var shape = PortalHelper.loadedShape(entry, axis, -64, 384, pos -> portal(pos, axis));
            assertNotNull(shape);
            assertTrue(shape.isComplete());
        }
    }

    @Test void unavailableNeighborAbortsInsteadOfReturningPartialGeometry() {
        BlockPos entry = new BlockPos(15, 65, 0);
        assertNull(PortalHelper.loadedShape(entry, Direction.Axis.X, -64, 384,
            pos -> pos.getX() >= 16 ? null : portal(pos, Direction.Axis.X)));
        assertNull(PortalHelper.loadedShape(entry, Direction.Axis.X, -64, 384, pos -> null));
    }
}
