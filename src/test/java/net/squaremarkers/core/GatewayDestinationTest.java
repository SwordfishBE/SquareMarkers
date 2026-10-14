package net.squaremarkers.core;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.Map;
import net.squaremarkers.core.helpers.HtmlHelper;
import net.squaremarkers.core.json.JsonStorage;
import net.squaremarkers.core.json.entities.Point;
import net.squaremarkers.core.layers.EndGatewayMarkerLayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import static org.junit.jupiter.api.Assertions.*;

class GatewayDestinationTest {
    @TempDir Path directory;
    @AfterEach void resetMessages() { FeedbackMessages.reload(Map.of()); }

    private EndGatewayMarkerLayer layer(JsonStorage storage) {
        SquareMarkersCore.onInitialize(storage, null, () -> {});
        return new EndGatewayMarkerLayer((MapWorld) Proxy.newProxyInstance(MapWorld.class.getClassLoader(),
            new Class<?>[]{MapWorld.class}, (proxy, method, args) -> {
                if (method.getName().equals("identifier")) return WorldIdentifier.parse("minecraft:the_end");
                throw new UnsupportedOperationException(method.getName());
            }));
    }

    @Test void knownDestinationsAddTranslatedLinksAndUnchangedUpdatesKeepTheMarker() {
        boolean onlyNamed = MarkersConfig.END_GATEWAY_MARKERS_ONLY_SHOW_NAMED;
        try {
            MarkersConfig.END_GATEWAY_MARKERS_ONLY_SHOW_NAMED = false;
            var layer = layer(new JsonStorage(directory.toString()));
            layer.add(0, 75, 0);
            assertNull(layer.provider().getMarkers().iterator().next().markerOptions().clickTooltip());
            FeedbackMessages.reload(Map.of("messages.portal.go-to-end-gateway", "Naar gateway <script>bad</script>"));
            layer.setDestination(0, 75, 0, new Point(-1500, 80, 900));
            var rendered = layer.provider().getMarkers().iterator().next();
            String popup = rendered.markerOptions().clickTooltip();
            assertTrue(popup.contains("minecraft_the_end"));
            assertTrue(popup.contains("params.set('x', '-1500')"));
            assertTrue(popup.contains("params.set('z', '900')"));
            assertTrue(popup.contains("Naar gateway &lt;script&gt;bad&lt;/script&gt;"));
            layer.setDestination(0, 75, 0, new Point(-1500, 80, 900));
            assertSame(rendered, layer.provider().getMarkers().iterator().next());
            layer.setDestination(10, 75, 0, new Point(0, 75, 0));
            assertEquals(1, layer.logicalCount());
            layer.setDestination(0, 75, 0, null);
            assertNull(layer.provider().getMarkers().iterator().next().markerOptions().clickTooltip());
        } finally { MarkersConfig.END_GATEWAY_MARKERS_ONLY_SHOW_NAMED = onlyNamed; }
    }

    @Test void destinationsSurviveStorageReloadAndManualHiding() {
        var storage = new JsonStorage(directory.toString());
        var layer = layer(storage);
        layer.add(2, 75, 3);
        layer.setDestination(2, 75, 3, new Point(1000, 70, -2000));
        String key = layer.toMarkerKey(2, 75, 3);
        var hidden = new MarkerVisibility.Identity(layer.worldIdentifier, layer.getKey(), key);
        try {
            SquareMarkersCore.visibility().setHidden(hidden, true);
            layer.refreshVisibility(key);
            layer.refreshDestinations(marker -> new Point(2000, 70, -3000));
            assertEquals(0, layer.visibleCount());
            storage.write();
            layer.close();
            var restored = layer(new JsonStorage(directory.toString()));
            restored.load();
            assertEquals(new Point(2000, 70, -3000), restored.getMarker(key).orElseThrow().getGatewayDestination());
            assertEquals(0, restored.visibleCount());
        } finally { SquareMarkersCore.visibility().setHidden(hidden, false); }
    }

    @Test void removedGatewayDoesNotReturnWithItsStoredDestinationAfterRestart() {
        var storage = new JsonStorage(directory.toString());
        var layer = layer(storage);
        layer.add(2, 75, 3);
        layer.setDestination(2, 75, 3, new Point(1000, 70, -2000));
        layer.remove(2, 75, 3);
        storage.write();
        layer.close();
        var restored = layer(new JsonStorage(directory.toString()));
        restored.load();
        assertEquals(0, restored.logicalCount());
        assertTrue(restored.getMarker(restored.toMarkerKey(2, 75, 3)).isEmpty());
    }

    @Test void travelWorldNamesCannotInjectJavascript() {
        String popup = HtmlHelper.travelPopup("Gateway", "custom_'\"<&/world", 1, -2, "Go");
        assertTrue(popup.contains("decodeURIComponent('custom_%27%22%3C%26%2Fworld')"));
        assertFalse(popup.contains("custom_'\"<&/world"));
    }
}
