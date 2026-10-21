package net.squaremarkers.fabric.helpers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.squaremarkers.core.MarkersConfig;
import net.squaremarkers.core.SquareMarkersCore;
import net.squaremarkers.core.json.JsonStorage;
import net.squaremarkers.core.json.entities.Point;
import net.squaremarkers.core.layers.NetherPortalMarkerLayer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

class NetherPortalValidationTest {
    @TempDir Path directory;
    @BeforeAll static void bootstrap() throws ReflectiveOperationException { NetherPortalChunkSafetyTest.bootstrap(); }
    @AfterAll static void restoreTags() throws ReflectiveOperationException { NetherPortalChunkSafetyTest.restoreTags(); }

    private MapWorld world() {
        return (MapWorld) Proxy.newProxyInstance(MapWorld.class.getClassLoader(), new Class<?>[]{MapWorld.class},
            (proxy, method, args) -> {
                if (method.getName().equals("identifier")) return WorldIdentifier.parse("minecraft:overworld");
                throw new UnsupportedOperationException(method.getName());
            });
    }

    private BlockState portal(BlockPos pos, int offset) {
        int x = pos.getX() - offset;
        if (pos.getZ() == 0 && x >= 14 && x <= 17 && pos.getY() >= 63 && pos.getY() <= 67) {
            if (x == 14 || x == 17 || pos.getY() == 63 || pos.getY() == 67) return Blocks.OBSIDIAN.defaultBlockState();
            return Blocks.NETHER_PORTAL.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_AXIS, Direction.Axis.X);
        }
        return Blocks.AIR.defaultBlockState();
    }

    @Test void incompleteRemovalWaitsForChunkThenRemovesOnlyTheBrokenStoredPortal() {
        JsonStorage storage = new JsonStorage(directory.toString());
        SquareMarkersCore.onInitialize(storage, null, () -> {});
        var layer = new NetherPortalMarkerLayer(world());
        var broken = new Point(15, 64, 0);
        var intact = new Point(25, 64, 0);
        var repository = storage.getWorldRepository("minecraft:overworld").getSimpleMarkerRepository("nether_portals");
        repository.getOrCreate(broken.x(), broken.y(), broken.z()).setName("Broken");
        repository.getOrCreate(intact.x(), intact.y(), intact.z()).setName("Keep me");
        boolean previous = MarkersConfig.NETHER_PORTAL_MARKERS_ONLY_SHOW_NAMED;
        try {
            MarkersConfig.NETHER_PORTAL_MARKERS_ONLY_SHOW_NAMED = true;
            layer.load();
            layer.forEachNearbyPosition(14, 65, 0, layer.validationChecks()::schedule);
            assertEquals(2, layer.validationChecks().size());
            NetherPortalValidation.flushLayer(layer, point -> NetherPortalValidation.validate(point, -64, 384,
                pos -> pos.getX() >= 16 ? null : portal(pos, 0)), 8);
            assertFalse(layer.validationChecks().hasReady());
            assertEquals(2, repository.copy().size(), "Missing data must never remove a marker");
            assertFalse(layer.validationChecks().chunkLoaded(ChunkPos.pack(2, 0)));
            assertTrue(layer.validationChecks().chunkLoaded(ChunkPos.pack(1, 0)));
            NetherPortalValidation.flushLayer(layer, point -> NetherPortalValidation.validate(point, -64, 384, pos -> {
                if (pos.equals(new BlockPos(17, 65, 0))) return Blocks.AIR.defaultBlockState();
                return pos.getX() >= 24 ? portal(pos, 10) : portal(pos, 0);
            }), 8);
            assertEquals(1, repository.copy().size());
            assertEquals("Keep me", repository.get(25, 64, 0).getName());
            assertEquals(0, layer.validationChecks().size());
            storage.write();
            assertEquals(1, new JsonStorage(directory.toString()).getWorldRepository("minecraft:overworld")
                .getSimpleMarkerRepository("nether_portals").copy().size());
        } finally { layer.close(); MarkersConfig.NETHER_PORTAL_MARKERS_ONLY_SHOW_NAMED = previous; }
    }

    @Test void workBudgetAndLayerCleanupAreBounded() {
        SquareMarkersCore.onInitialize(new JsonStorage(directory.toString()), null, () -> {});
        var layer = new NetherPortalMarkerLayer(world());
        for (int x = 0; x < 30; x++) layer.add(x, 64, -16);
        layer.forEachPosition(layer.validationChecks()::schedule);
        int[] visits = {0};
        assertEquals(8, NetherPortalValidation.flushLayer(layer, point -> {
            visits[0]++; return NetherPortalValidation.Validation.VALID;
        }, 8));
        assertEquals(8, visits[0]);
        assertEquals(22, layer.validationChecks().size());
        var pending = layer.validationChecks().poll();
        layer.remove(pending.x(), pending.y(), pending.z());
        assertEquals(21, layer.validationChecks().size());
        layer.close();
        assertEquals(0, layer.validationChecks().size());
        var positions = new ArrayList<>();
        layer.forEachPosition(positions::add);
        assertTrue(positions.isEmpty());
    }

    @Test void renameKeepsWaitingValidationAndReloadCanRebuildIt() {
        SquareMarkersCore.onInitialize(new JsonStorage(directory.toString()), null, () -> {});
        var layer = new NetherPortalMarkerLayer(world());
        var position = new Point(-16, 64, -17);
        boolean rename = MarkersConfig.NETHER_PORTAL_MARKERS_RENAME;
        try {
            MarkersConfig.NETHER_PORTAL_MARKERS_RENAME = true;
            layer.add(position.x(), position.y(), position.z());
            layer.forEachPosition(layer.validationChecks()::schedule);
            layer.validationChecks().await(layer.validationChecks().poll(), ChunkPos.pack(-2, -2));
            layer.setName(position.x(), position.y(), position.z(), "Renamed");
            assertEquals(1, layer.validationChecks().size());
            assertTrue(layer.validationChecks().chunkLoaded(ChunkPos.pack(-2, -2)));
            assertEquals(position, layer.validationChecks().poll());
            layer.load();
            assertEquals(0, layer.validationChecks().size());
            layer.forEachPosition(layer.validationChecks()::schedule);
            assertEquals(position, layer.validationChecks().poll());
        } finally { layer.close(); MarkersConfig.NETHER_PORTAL_MARKERS_RENAME = rename; }
    }
}
