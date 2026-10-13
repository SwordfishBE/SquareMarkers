package net.squaremarkers.core.layers;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import net.squaremarkers.core.MarkersConfig;
import net.squaremarkers.core.MarkerVisibility;
import net.squaremarkers.core.SquareMarkersCore;
import net.squaremarkers.core.json.JsonStorage;
import net.squaremarkers.core.registries.Layers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import static org.junit.jupiter.api.Assertions.*;

class NamedPortalTest {
    @TempDir Path directory;

    @Test void unnamedPortalsStayStoredAndRenamingRespectsManualVisibility() {
        boolean named = MarkersConfig.NETHER_PORTAL_MARKERS_ONLY_SHOW_NAMED;
        boolean rename = MarkersConfig.NETHER_PORTAL_MARKERS_RENAME;
        try {
            MarkersConfig.NETHER_PORTAL_MARKERS_ONLY_SHOW_NAMED = true;
            MarkersConfig.NETHER_PORTAL_MARKERS_RENAME = true;
            SquareMarkersCore.onInitialize(new JsonStorage(directory.toString()), null, () -> {});
            MapWorld world = (MapWorld) Proxy.newProxyInstance(MapWorld.class.getClassLoader(), new Class<?>[]{MapWorld.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("identifier")) return WorldIdentifier.parse("minecraft:overworld");
                    throw new UnsupportedOperationException(method.getName());
                });
            var layer = new NetherPortalMarkerLayer(world);
            layer.add(0, 64, 0);
            assertEquals(1, layer.logicalCount());
            assertEquals(0, layer.visibleCount());
            layer.refreshVisibility(layer.toMarkerKey(0, 64, 0));
            assertEquals(0, layer.visibleCount());
            layer.setName(0, 64, 0, "Home");
            assertEquals(1, layer.visibleCount());
            var identity = new MarkerVisibility.Identity("minecraft:overworld", Layers.Keys.NETHER_PORTALS,
                layer.toMarkerKey(0, 64, 0));
            SquareMarkersCore.visibility().setHidden(identity, true);
            layer.refreshVisibility(identity.marker());
            layer.setName(0, 64, 0, "New home");
            assertEquals(0, layer.visibleCount());
            layer.add(10, 64, 0);
            layer.load();
            assertEquals(2, layer.logicalCount());
            assertEquals(0, layer.visibleCount());
            MarkersConfig.NETHER_PORTAL_MARKERS_ONLY_SHOW_NAMED = false;
            layer.refreshVisibility(layer.toMarkerKey(10, 64, 0));
            assertEquals(1, layer.visibleCount());
            layer.remove(10, 64, 0);
            assertEquals(1, layer.logicalCount());
            layer.close();
            assertEquals(0, layer.logicalCount());
        } finally {
            MarkersConfig.NETHER_PORTAL_MARKERS_ONLY_SHOW_NAMED = named;
            MarkersConfig.NETHER_PORTAL_MARKERS_RENAME = rename;
        }
    }
}
