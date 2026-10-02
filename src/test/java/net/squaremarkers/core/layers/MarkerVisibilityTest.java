package net.squaremarkers.core.layers;

import net.squaremarkers.core.MarkerVisibility;
import net.squaremarkers.core.layers.primitive.MarkerLayer;
import net.squaremarkers.core.markers.MarkerBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import xyz.jpenilla.squaremap.api.*;
import xyz.jpenilla.squaremap.api.marker.Marker;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class MarkerVisibilityTest {
    @TempDir Path directory;
    private static MapWorld world(String id) {
        return (MapWorld) Proxy.newProxyInstance(MapWorld.class.getClassLoader(), new Class<?>[]{MapWorld.class},
            (proxy, method, args) -> {
                if (method.getName().equals("identifier")) return WorldIdentifier.parse(id);
                throw new UnsupportedOperationException(method.getName());
            });
    }
    private static class Layer extends MarkerLayer<Object> {
        Layer(String world, MarkerVisibility visibility) { super("test", "Test", world(world), 50, visibility); }
        @Override public void load() {}
        @Override public MarkerBuilder<?> createBuilder(Object object) { return null; }
        Marker latest(String key) { return renderedMarker(key); }
    }
    private Marker marker(int x) { return Marker.circle(Point.of(x, 0), 1); }
    private MarkerVisibility.Identity identity(String world, String marker) {
        return new MarkerVisibility.Identity(world, "test", marker);
    }
    @Test void hiddenMarkersRetainLatestStateAndLogicalPresence() {
        var visibility = new MarkerVisibility(directory.resolve("hidden.json"));
        var layer = new Layer("minecraft:overworld", visibility);
        layer.addMarker("a", marker(0));
        assertTrue(visibility.setHidden(identity("minecraft:overworld", "a"), true));
        layer.refreshVisibility("a");
        assertTrue(layer.hasMarker("a"));
        assertEquals(0, layer.visibleCount());
        var next = marker(20);
        layer.addMarker("a", next);
        assertSame(next, layer.latest("a"));
        visibility.setHidden(identity("minecraft:overworld", "a"), false);
        layer.refreshVisibility("a");
        assertSame(next, layer.provider().getMarkers().iterator().next());
        layer.removeMarker("a");
        assertEquals(0, layer.logicalCount());
        layer.refreshVisibility("a");
        assertEquals(0, layer.visibleCount());
    }
    @Test void persistedExclusionsAreDimensionScopedAndSurviveSourceDeletion() {
        var visibility = new MarkerVisibility(directory.resolve("hidden.json"));
        visibility.setHidden(identity("minecraft:overworld", "a"), true);
        var reloaded = new MarkerVisibility(directory.resolve("hidden.json"));
        reloaded.load();
        var hidden = new Layer("minecraft:overworld", reloaded);
        var visible = new Layer("minecraft:the_nether", reloaded);
        hidden.addMarker("a", marker(0)); visible.addMarker("a", marker(0));
        assertEquals(0, hidden.visibleCount()); assertEquals(1, visible.visibleCount());
        hidden.removeMarker("a"); hidden.addMarker("a", marker(1));
        assertEquals(0, hidden.visibleCount());
        hidden.close(); visible.close();
        assertEquals(0, hidden.logicalCount()); assertEquals(0, visible.visibleCount());
    }
    @Test void geometryFragmentsShareOneVisibilityIdentity() {
        var visibility = new MarkerVisibility(directory.resolve("hidden.json"));
        var layer = new Layer("minecraft:overworld", visibility) {
            @Override protected String visibilityKey(String key) { return key.substring(0, key.lastIndexOf(':')); }
        };
        visibility.setHidden(identity("minecraft:overworld", "owner"), true);
        layer.addMarker("owner:1", marker(1)); layer.addMarker("owner:2", marker(2));
        assertEquals(java.util.Set.of("owner"), layer.markerIdentities());
        assertEquals(0, layer.visibleCount());
        layer.removeMarker("owner:1"); layer.addMarker("owner:3", marker(3));
        visibility.setHidden(identity("minecraft:overworld", "owner"), false);
        layer.refreshVisibility("owner");
        assertEquals(2, layer.visibleCount());
        layer.clearMarkers(); assertEquals(0, layer.logicalCount());
    }
    @Test void failedPersistenceDoesNotChangeVisibility() throws Exception {
        Path file = directory.resolve("not-a-directory");
        java.nio.file.Files.writeString(file, "blocked");
        var visibility = new MarkerVisibility(file.resolve("hidden.json"));
        assertFalse(visibility.setHidden(identity("minecraft:overworld", "a"), true));
        assertFalse(visibility.hidden("minecraft:overworld", "test", "a"));
    }
}
