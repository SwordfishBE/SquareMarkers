package net.squaremarkers.core.layers;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import net.squaremarkers.core.MarkerVisibility;
import net.squaremarkers.core.layers.primitive.SnapshotMarkerLayer;
import net.squaremarkers.core.markers.IconMarkerBuilder;
import net.squaremarkers.core.markers.MarkerBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import static org.junit.jupiter.api.Assertions.*;

class SnapshotMarkerLayerTest {
    @TempDir Path directory;
    record Point(Object id, String dimension, int x) {}

    private final class Layer<K> extends SnapshotMarkerLayer<K, Point> {
        int builds;
        Layer(String world, MarkerVisibility visibility) { super("snapshot", "Test", world(world), 50, visibility); }
        @Override public void load() {}
        @Override protected String dimension(Point point) { return point.dimension(); }
        @Override protected String markerKey(Point point) { return point.id().toString(); }
        @Override public MarkerBuilder<?> createBuilder(Point point) {
            builds++;
            return IconMarkerBuilder.newIconMarker(point.id().toString(), "test", point.x(), 0).centerIcon(16, 16);
        }
    }

    @Test void changedSourcesUpdateAndUnchangedSnapshotsDoNotRebuild() {
        var layer = new Layer<String>("minecraft:overworld", new MarkerVisibility(directory.resolve("hidden.json")));
        var first = Map.of("Home", new Point("Home", "minecraft:overworld", 0));
        layer.sync(first);
        var marker = layer.provider().getMarkers().iterator().next();
        layer.sync(Map.of("Home", new Point("Home", "minecraft:overworld", 0)));
        assertEquals(1, layer.builds);
        assertSame(marker, layer.provider().getMarkers().iterator().next());
        layer.sync(Map.of("Home", new Point("Home", "minecraft:overworld", 20)));
        assertEquals(2, layer.builds);
        assertNotSame(marker, layer.provider().getMarkers().iterator().next());
        layer.sync(Map.of());
        assertEquals(0, layer.logicalCount());
        assertEquals(0, layer.visibleCount());
    }

    @Test void uuidSourcesMoveBetweenWorldsAndManualVisibilitySurvivesUpdates() {
        var visibility = new MarkerVisibility(directory.resolve("hidden.json"));
        var overworld = new Layer<UUID>("minecraft:overworld", visibility);
        var end = new Layer<UUID>("minecraft:the_end", visibility);
        UUID id = UUID.randomUUID();
        visibility.setHidden(new MarkerVisibility.Identity("minecraft:the_end", "snapshot", id.toString()), true);
        var first = Map.of(id, new Point(id, "minecraft:overworld", 0));
        overworld.sync(first); end.sync(first);
        assertEquals(1, overworld.visibleCount()); assertEquals(0, end.logicalCount());
        var moved = Map.of(id, new Point(id, "minecraft:the_end", 10));
        overworld.sync(moved); end.sync(moved);
        assertEquals(0, overworld.logicalCount()); assertEquals(1, end.logicalCount());
        assertEquals(0, end.visibleCount());
        end.sync(Map.of(id, new Point(id, "minecraft:the_end", 20)));
        assertEquals(0, end.visibleCount());
        visibility.setHidden(new MarkerVisibility.Identity("minecraft:the_end", "snapshot", id.toString()), false);
        end.refreshVisibility(id.toString());
        assertEquals(1, end.visibleCount());
    }

    @Test void closingClearsSourceCacheSoAnIdenticalSnapshotCanLoadAgain() {
        var layer = new Layer<String>("minecraft:overworld", new MarkerVisibility(directory.resolve("hidden.json")));
        var snapshot = Map.of("Home", new Point("Home", "minecraft:overworld", 0));
        layer.sync(snapshot);
        layer.close();
        assertEquals(0, layer.logicalCount());
        layer.sync(snapshot);
        assertEquals(1, layer.visibleCount());
        assertEquals(2, layer.builds);
    }

    private static MapWorld world(String id) {
        return (MapWorld) Proxy.newProxyInstance(MapWorld.class.getClassLoader(), new Class<?>[]{MapWorld.class},
            (proxy, method, args) -> {
                if (method.getName().equals("identifier")) return WorldIdentifier.parse(id);
                throw new UnsupportedOperationException(method.getName());
            });
    }
}
