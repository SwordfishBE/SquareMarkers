package net.squaremarkers.core.layers;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import static org.junit.jupiter.api.Assertions.*;

class PlayerSnapshotTest {
    @Test void stationaryPlayersReuseMarkersAndDepartedPlayersAreRemoved() {
        var world = (MapWorld) Proxy.newProxyInstance(MapWorld.class.getClassLoader(), new Class<?>[]{MapWorld.class},
            (proxy, method, args) -> {
                if (method.getName().equals("identifier")) return WorldIdentifier.parse("minecraft:overworld");
                throw new UnsupportedOperationException(method.getName());
            });
        var layer = new CrossDimensionPlayerMarkerLayer(world);
        UUID id = UUID.randomUUID();
        var first = new CrossDimensionPlayerMarkerLayer.PlayerPoint(id, "minecraft:overworld", "Alex", -8, 16);
        layer.sync(Map.of(id, first));
        var marker = layer.provider().getMarkers().iterator().next();
        layer.sync(Map.of(id, new CrossDimensionPlayerMarkerLayer.PlayerPoint(id, "minecraft:overworld", "Alex", -8, 16)));
        assertSame(marker, layer.provider().getMarkers().iterator().next());
        layer.sync(Map.of(id, new CrossDimensionPlayerMarkerLayer.PlayerPoint(id, "minecraft:overworld", "Alex", 0, 16)));
        assertNotSame(marker, layer.provider().getMarkers().iterator().next());
        layer.sync(Map.of()); // Logout, vanish and leaving the source world all omit the player.
        assertEquals(0, layer.logicalCount());
        layer.sync(Map.of(id, first));
        layer.close();
        layer.sync(Map.of(id, first));
        assertEquals(1, layer.visibleCount());
        layer.close();
    }
}
