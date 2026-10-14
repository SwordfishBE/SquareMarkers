package net.squaremarkers.fabric.compat.worldborder;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.Map;
import net.squaremarkers.core.MarkerVisibility;
import net.squaremarkers.core.SquareMarkersCore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import static org.junit.jupiter.api.Assertions.*;

class WorldBorderTest {
    @TempDir Path directory;

    /** Public fields match the optional config contract without shipping its source. */
    public static class ConfigFixture {
        public static boolean enableCustomOverworldBorder = true, enableCustomNetherBorder = false, enableCustomEndBorder = true;
        public static int overworldBorderNegativeX = -100, overworldBorderNegativeZ = -200,
            overworldBorderPositiveX = 300, overworldBorderPositiveZ = 400;
        public static int netherBorderNegativeX = -625, netherBorderNegativeZ = -625,
            netherBorderPositiveX = 625, netherBorderPositiveZ = 625;
        public static int endBorderNegativeX = -5000, endBorderNegativeZ = -5000,
            endBorderPositiveX = 5000, endBorderPositiveZ = 5000;
    }

    @Test void readerUsesLoadedValuesAndRemovesDisabledDimensions() throws Exception {
        var reader = new WorldBorderReader(ConfigFixture.class);
        var initial = reader.read();
        assertEquals(2, initial.size());
        assertEquals(new BorderBounds("minecraft:overworld", -100, -200, 300, 400), initial.get("minecraft:overworld"));
        assertFalse(initial.containsKey("minecraft:the_nether"));
        try {
            ConfigFixture.enableCustomNetherBorder = true;
            ConfigFixture.enableCustomEndBorder = false;
            ConfigFixture.overworldBorderPositiveX = 600;
            var changed = reader.read();
            assertEquals(600, changed.get("minecraft:overworld").maxX());
            assertTrue(changed.containsKey("minecraft:the_nether"));
            assertFalse(changed.containsKey("minecraft:the_end"));
            assertEquals(300, initial.get("minecraft:overworld").maxX());
            assertThrows(UnsupportedOperationException.class, () -> changed.clear());
        } finally {
            ConfigFixture.enableCustomNetherBorder = false;
            ConfigFixture.enableCustomEndBorder = true;
            ConfigFixture.overworldBorderPositiveX = 300;
        }
    }

    @Test void badBoundsAndUnsupportedConfigAreRejected() {
        assertThrows(NoSuchFieldException.class, () -> new WorldBorderReader(String.class));
        assertThrows(IllegalArgumentException.class, () -> new BorderBounds("test", 1, 0, 1, 20));
        assertThrows(IllegalArgumentException.class, () -> new BorderBounds("test", -1, 20, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new BorderBounds("test", Integer.MIN_VALUE, 0, 1, 20));
    }

    @Test void readerResolvesRelocatedModernAndLegacyFabricPackages() throws Exception {
        for (String expected : new String[]{"com.serilum.worldborder_common_fabric.config.ConfigHandler",
            "com.natamus.worldborder_common_fabric.config.ConfigHandler",
            "com.serilum.worldborder.config.ConfigHandler", "com.natamus.worldborder.config.ConfigHandler"}) {
            var reader = WorldBorderReader.resolve(name -> {
                if (name.equals(expected)) return ConfigFixture.class;
                throw new ClassNotFoundException(name);
            });
            assertEquals(2, reader.read().size());
        }
        assertThrows(ClassNotFoundException.class, () -> WorldBorderReader.resolve(name -> { throw new ClassNotFoundException(name); }));
    }

    @Test void boundariesUpdateIncrementallyAndRespectManualVisibility() {
        String dimension = "minecraft:overworld";
        var world = (MapWorld) Proxy.newProxyInstance(MapWorld.class.getClassLoader(), new Class<?>[]{MapWorld.class},
            (proxy, method, args) -> {
                if (method.getName().equals("identifier")) return WorldIdentifier.parse(dimension);
                throw new UnsupportedOperationException(method.getName());
            });
        var layer = new WorldBorderMarkerLayer(world);
        var initial = new BorderBounds(dimension, -100, -200, 300, 400);
        layer.sync(Map.of(dimension, initial, "minecraft:the_end", new BorderBounds("minecraft:the_end", -1, -1, 1, 1)));
        assertEquals(1, layer.logicalCount());
        var marker = layer.provider().getMarkers().iterator().next();
        assertFalse(marker.markerOptions().fill());
        assertEquals(3, marker.markerOptions().strokeWeight());
        assertTrue(marker.markerOptions().clickTooltip().contains("X: -100 to 300"));
        layer.sync(Map.of(dimension, initial));
        assertSame(marker, layer.provider().getMarkers().iterator().next());
        var identity = new MarkerVisibility.Identity(dimension, layer.getKey(), dimension);
        try {
            SquareMarkersCore.visibility().setHidden(identity, true);
            layer.refreshVisibility(dimension);
            layer.sync(Map.of(dimension, new BorderBounds(dimension, -100, -200, 600, 400)));
            assertEquals(0, layer.visibleCount());
            assertEquals(1, layer.logicalCount());
            layer.sync(Map.of());
            assertEquals(0, layer.logicalCount());
            layer.sync(Map.of(dimension, initial));
            layer.close();
            assertEquals(0, layer.logicalCount());
        } finally { SquareMarkersCore.visibility().setHidden(identity, false); }
    }
}
