package net.squaremarkers.fabric.compat.layers;

import net.minecraft.world.level.ChunkPos;
import net.squaremarkers.core.MarkerVisibility;
import net.squaremarkers.core.registries.Layers;
import net.squaremarkers.fabric.compat.OpacChunk;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.awt.Color;
import static org.junit.jupiter.api.Assertions.*;

class OpacVisibilityTest {
    @TempDir Path directory;
    @Test void hiddenOwnerRemainsHiddenThroughMetadataAndGeometryChanges() {
        MapWorld world = (MapWorld) Proxy.newProxyInstance(MapWorld.class.getClassLoader(), new Class<?>[]{MapWorld.class},
            (proxy, method, args) -> {
                if (method.getName().equals("identifier")) return WorldIdentifier.parse("minecraft:overworld");
                throw new UnsupportedOperationException(method.getName());
            });
        var visibility = new MarkerVisibility(directory.resolve("hidden.json"));
        var layer = new OPACAreaMarkerLayer(world, visibility);
        layer.addChunk(new OpacChunk(new ChunkPos(0, 0), "Owner", "Claim", 0x00ff00), true);
        var identity = new MarkerVisibility.Identity("minecraft:overworld", Layers.Keys.OPAC, "OpacClaim:Owner");
        assertEquals(java.util.Set.of("OpacClaim:Owner"), layer.markerIdentities());
        visibility.setHidden(identity, true); layer.refreshVisibility(identity.marker());
        layer.updateMetadata("Owner", "Renamed", 0xff0000);
        assertEquals(0, layer.visibleCount());
        layer.addChunk(new OpacChunk(new ChunkPos(5, 5), "Owner", "Claim", 0x00ff00), true);
        layer.updateMetadata("Owner", "Newest", 0xff0000);
        assertEquals(0, layer.visibleCount());
        assertEquals(2, layer.logicalCount());
        visibility.setHidden(identity, false); layer.refreshVisibility(identity.marker());
        assertEquals(2, layer.visibleCount());
        layer.provider().getMarkers().forEach(marker -> {
            assertEquals(Color.RED, marker.markerOptions().fillColor());
            String label = marker.markerOptions().hoverTooltip();
            if (label == null) label = marker.markerOptions().clickTooltip();
            assertEquals("Newest", label);
        });
        layer.removeChunk(0, 0, true); layer.removeChunk(5, 5, true);
        assertEquals(0, layer.logicalCount());
        // Listener cleanup belongs to the installed-OPAC server smoke test, not this geometry-only test.
        layer.clearMarkers();
    }
}
