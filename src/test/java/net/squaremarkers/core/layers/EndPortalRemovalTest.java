package net.squaremarkers.core.layers;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import net.squaremarkers.core.MarkersConfig;
import net.squaremarkers.core.SquareMarkersCore;
import net.squaremarkers.core.json.JsonStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import static org.junit.jupiter.api.Assertions.*;

class EndPortalRemovalTest {
    @TempDir Path directory;

    @Test void removingAnySurfaceBlockRemovesTheStoredCenterAcrossChunkEdges() {
        var storage = new JsonStorage(directory.toString());
        SquareMarkersCore.onInitialize(storage, null, () -> {});
        var layer = new EndPortalMarkerLayer(world());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                layer.add(15, 64, -16);
                layer.removeContainingBlock(15 + dx, 64, -16 + dz);
                assertTrue(layer.getMarker(layer.toMarkerKey(15, 64, -16)).isEmpty());
                assertEquals(0, layer.logicalCount());
            }
        }
        storage.write();
        SquareMarkersCore.onInitialize(new JsonStorage(directory.toString()), null, () -> {});
        var reloaded = new EndPortalMarkerLayer(world());
        reloaded.load();
        assertEquals(0, reloaded.logicalCount());
    }

    @Test void removalIncludesFilteredMarkersButDoesNotTouchNearbyOrOtherHeightPortals() {
        boolean previous = MarkersConfig.END_PORTAL_MARKERS_ONLY_SHOW_NAMED;
        try {
            MarkersConfig.END_PORTAL_MARKERS_ONLY_SHOW_NAMED = true;
            SquareMarkersCore.onInitialize(new JsonStorage(directory.toString()), null, () -> {});
            var layer = new EndPortalMarkerLayer(world());
            layer.add(0, 64, 0);
            layer.add(4, 64, 0);
            layer.add(0, 70, 0);
            assertEquals(0, layer.visibleCount());
            layer.removeContainingBlock(1, 64, 1);
            assertEquals(2, layer.logicalCount());
            assertTrue(layer.getMarker(layer.toMarkerKey(4, 64, 0)).isPresent());
            assertTrue(layer.getMarker(layer.toMarkerKey(0, 70, 0)).isPresent());
        } finally { MarkersConfig.END_PORTAL_MARKERS_ONLY_SHOW_NAMED = previous; }
    }

    private MapWorld world() {
        return (MapWorld) Proxy.newProxyInstance(MapWorld.class.getClassLoader(), new Class<?>[]{MapWorld.class},
            (proxy, method, args) -> {
                if (method.getName().equals("identifier")) return WorldIdentifier.parse("minecraft:overworld");
                throw new UnsupportedOperationException(method.getName());
            });
    }
}
