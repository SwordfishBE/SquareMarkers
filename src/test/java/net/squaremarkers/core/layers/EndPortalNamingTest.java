package net.squaremarkers.core.layers;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import net.squaremarkers.core.MarkerVisibility;
import net.squaremarkers.core.MarkersConfig;
import net.squaremarkers.core.SquareMarkersCore;
import net.squaremarkers.core.json.JsonStorage;
import net.squaremarkers.core.layers.primitive.NamedPortalMarkerLayer;
import net.squaremarkers.core.objects.InteractionResult.State;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import static org.junit.jupiter.api.Assertions.*;

class EndPortalNamingTest {
    @TempDir Path directory;

    @Test void endPortalsAndGatewaysSupportFilteringNamingLabelsAndManualExclusions() {
        boolean portalRename = MarkersConfig.END_PORTAL_MARKERS_RENAME, gatewayRename = MarkersConfig.END_GATEWAY_MARKERS_RENAME;
        boolean portalOnly = MarkersConfig.END_PORTAL_MARKERS_ONLY_SHOW_NAMED, gatewayOnly = MarkersConfig.END_GATEWAY_MARKERS_ONLY_SHOW_NAMED;
        try {
            SquareMarkersCore.onInitialize(new JsonStorage(directory.toString()), null, () -> {});
            MarkersConfig.END_PORTAL_MARKERS_RENAME = MarkersConfig.END_GATEWAY_MARKERS_RENAME = true;
            MarkersConfig.END_PORTAL_MARKERS_ONLY_SHOW_NAMED = MarkersConfig.END_GATEWAY_MARKERS_ONLY_SHOW_NAMED = true;
            for (NamedPortalMarkerLayer layer : new NamedPortalMarkerLayer[]{new EndPortalMarkerLayer(world("minecraft:overworld")),
                new EndGatewayMarkerLayer(world("minecraft:the_end"))}) {
                layer.add(0, 64, 0);
                assertEquals(1, layer.logicalCount());
                assertEquals(0, layer.visibleCount());
                assertEquals(State.ADDED, layer.setName(0, 64, 0, "Home <script>bad</script>").state());
                assertEquals(1, layer.visibleCount());
                var options = layer.provider().getMarkers().iterator().next().markerOptions();
                assertTrue(options.hoverTooltip().contains("&lt;script&gt;"));
                assertEquals(State.FEEDBACK, layer.setName(0, 64, 0, "Home <script>bad</script>").state());
                var identity = new MarkerVisibility.Identity(layer.worldIdentifier, layer.getKey(), layer.toMarkerKey(0, 64, 0));
                SquareMarkersCore.visibility().setHidden(identity, true);
                layer.refreshVisibility(identity.marker());
                layer.setName(0, 64, 0, "Other");
                assertEquals(0, layer.visibleCount());
                MarkersConfig.END_PORTAL_MARKERS_RENAME = MarkersConfig.END_GATEWAY_MARKERS_RENAME = false;
                assertEquals(State.FEEDBACK, layer.setName(0, 64, 0, "Blocked").state());
                assertEquals("Other", layer.getMarker(identity.marker()).orElseThrow().getName());
                assertEquals(State.SKIP, layer.setName(10, 64, 0, "Missing").state());
                assertEquals(1, layer.logicalCount());
                MarkersConfig.END_PORTAL_MARKERS_RENAME = MarkersConfig.END_GATEWAY_MARKERS_RENAME = true;
                layer.add(10, 64, 0);
                layer.load();
                assertEquals(0, layer.visibleCount());
                MarkersConfig.END_PORTAL_MARKERS_ONLY_SHOW_NAMED = MarkersConfig.END_GATEWAY_MARKERS_ONLY_SHOW_NAMED = false;
                layer.load();
                assertEquals(1, layer.visibleCount());
                layer.remove(10, 64, 0);
                assertEquals(1, layer.logicalCount());
                layer.close();
                assertEquals(0, layer.logicalCount());
                MarkersConfig.END_PORTAL_MARKERS_ONLY_SHOW_NAMED = MarkersConfig.END_GATEWAY_MARKERS_ONLY_SHOW_NAMED = true;
            }
        } finally {
            MarkersConfig.END_PORTAL_MARKERS_RENAME = portalRename; MarkersConfig.END_GATEWAY_MARKERS_RENAME = gatewayRename;
            MarkersConfig.END_PORTAL_MARKERS_ONLY_SHOW_NAMED = portalOnly; MarkersConfig.END_GATEWAY_MARKERS_ONLY_SHOW_NAMED = gatewayOnly;
        }
    }

    private static MapWorld world(String id) {
        return (MapWorld) Proxy.newProxyInstance(MapWorld.class.getClassLoader(), new Class<?>[]{MapWorld.class},
            (proxy, method, args) -> {
                if (method.getName().equals("identifier")) return WorldIdentifier.parse(id);
                throw new UnsupportedOperationException(method.getName());
            });
    }
}
