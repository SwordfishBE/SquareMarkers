package net.squaremarkers.core.layers;

import net.minecraft.world.level.ChunkPos;
import net.squaremarkers.core.SquareMarkersCore;
import net.squaremarkers.core.json.JsonStorage;
import net.squaremarkers.core.layers.primitive.AreaMarkerLayer;
import net.squaremarkers.fabric.compat.OpacChunk;
import net.squaremarkers.fabric.compat.OpacClaim;
import net.squaremarkers.core.helpers.PolygonLoops;
import net.squaremarkers.core.interfaces.entities.IPoint;
import net.squaremarkers.fabric.compat.layers.OPACAreaMarkerLayer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import net.squaremarkers.fabric.compat.OpacGeometryWorker;
import org.junit.jupiter.api.io.TempDir;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.api.marker.Polygon;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.Map;
import java.util.List;
import java.awt.geom.Path2D;
import static org.junit.jupiter.api.Assertions.*;

class MarkerLayerRegressionTest {
    @TempDir Path directory;

    @AfterEach void stopWorker() { OpacGeometryWorker.SHARED.shutdown(); }

    private static void awaitGeometry(OPACAreaMarkerLayer layer) throws Exception {
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
        while (layer.hasPendingGeometry() && System.nanoTime() < deadline) {
            layer.flushPendingChanges();
            Thread.sleep(1);
        }
        assertFalse(layer.hasPendingGeometry(), "Geometry did not complete");
    }

    @Test
    void opacMetadataUpdatesPreserveGeometryAndLeaveUnchangedMarkersAlone() throws Exception {
        var layer = new OPACAreaMarkerLayer(world());
        for (int x = 0; x < 3; x++) for (int z = 0; z < 3; z++) {
            if (x != 1 || z != 1) layer.addChunk(chunk(x, z, "Owner"), true);
        }
        awaitGeometry(layer);
        Polygon original = (Polygon) layer.provider().getMarkers().iterator().next();
        layer.updateMetadata("Owner", "Claim - Owner's claim", 0x00ff00);
        assertSame(original, layer.provider().getMarkers().iterator().next());
        layer.updateMetadata("Other", "Unrelated", 0xff0000);
        assertSame(original, layer.provider().getMarkers().iterator().next());
        layer.updateMetadata("Owner", "New <script>name</script>", 0xff0000);
        Polygon updated = (Polygon) layer.provider().getMarkers().iterator().next();
        assertNotSame(original, updated);
        assertEquals(original.mainPolygon(), updated.mainPolygon());
        assertEquals(original.negativeSpace(), updated.negativeSpace());
        assertEquals(java.awt.Color.RED, updated.markerOptions().fillColor());
        assertEquals(java.awt.Color.RED, updated.markerOptions().strokeColor());
        assertTrue(updated.markerOptions().hoverTooltip().contains("&lt;script&gt;"));
        layer.updateMetadata("Owner", "New <script>name</script>", 0xff0000);
        assertSame(updated, layer.provider().getMarkers().iterator().next());
        layer.removeChunk(0, 0, true);
        awaitGeometry(layer);
        assertEquals(java.awt.Color.RED, layer.provider().getMarkers().iterator().next().markerOptions().fillColor());
    }

    @Test
    void opacGeometryMatchesEveryThreeByThreeClaimPattern() {
        for (int mask = 0; mask < 512; mask++) {
            var claim = new OpacClaim("Test", "Test", 0);
            for (int index = 0; index < 9; index++) {
                if ((mask & (1 << index)) != 0) claim.addChunk(chunk(index % 3, index / 3, "Test"));
            }
            var parts = PolygonLoops.group(claim.getPolygons());
            for (int index = 0; index < 9; index++) {
                int x = index % 3 * 16 + 8, z = index / 3 * 16 + 8;
                boolean filled = parts.stream().anyMatch(part -> path(part.exterior()).contains(x, z)
                    && part.holes().stream().noneMatch(hole -> path(hole).contains(x, z)));
                assertEquals((mask & (1 << index)) != 0, filled, "Pattern " + mask + ", chunk " + index);
            }
        }
    }

    private static Path2D path(List<IPoint> points) {
        var path = new Path2D.Double();
        path.moveTo(points.getFirst().x(), points.getFirst().z());
        points.forEach(point -> path.lineTo(point.x(), point.z()));
        path.closePath();
        return path;
    }

    @Test
    void coordinateRemovalClearsStoredAreaWithoutBannerMetadata() {
        JsonStorage storage = new JsonStorage(directory.toString());
        SquareMarkersCore.onInitialize(storage, null, () -> {});
        var layer = new AreaMarkerLayer(world());
        layer.addPoint("Home", 123, 10, 65, 20);
        layer.addPoint("Home", 123, 20, 65, 20);
        layer.removePointAt(10, 65, 20);
        var repository = storage.getWorldRepository("minecraft:overworld").getAreaMarkerRepository("areas");
        assertEquals(1, repository.get("Home", 123).getPoints().size());
        layer.removePointAt(20, 65, 20);
        assertTrue(repository.copy().isEmpty());
        assertTrue(layer.provider().getMarkers().isEmpty());
        assertDoesNotThrow(() -> layer.removePointAt(20, 65, 20));
    }

    @Test
    void opacRingHasHoleAndReleasedClaimsLeaveNoBookkeeping() throws Exception {
        var layer = new OPACAreaMarkerLayer(world());
        for (int x = 0; x < 3; x++) for (int z = 0; z < 3; z++) {
            if (x != 1 || z != 1) layer.addChunk(chunk(x, z, "Ring"), true);
        }
        awaitGeometry(layer);
        assertEquals(1, layer.provider().getMarkers().size());
        Polygon polygon = (Polygon) layer.provider().getMarkers().iterator().next();
        assertEquals(1, polygon.negativeSpace().size());
        for (int x = 0; x < 3; x++) for (int z = 0; z < 3; z++) layer.removeChunk(x, z, true);
        for (int i = 0; i < 100; i++) {
            layer.addChunk(chunk(0, 0, "Player" + i), true);
            layer.removeChunk(0, 0, true);
        }
        awaitGeometry(layer);
        assertTrue(layer.provider().getMarkers().isEmpty());
        var field = OPACAreaMarkerLayer.class.getDeclaredField("renderedKeys");
        field.setAccessible(true);
        assertTrue(((Map<?, ?>) field.get(layer)).isEmpty());
    }

    private static OpacChunk chunk(int x, int z, String name) {
        return new OpacChunk(new ChunkPos(x, z), name, "Claim", 0x00ff00);
    }

    private static MapWorld world() {
        return (MapWorld) Proxy.newProxyInstance(MapWorld.class.getClassLoader(), new Class<?>[]{MapWorld.class},
            (proxy, method, args) -> {
                if (method.getName().equals("identifier")) return WorldIdentifier.parse("minecraft:overworld");
                throw new UnsupportedOperationException(method.getName());
            });
    }
}
