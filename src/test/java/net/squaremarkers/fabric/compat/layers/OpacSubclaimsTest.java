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
import java.util.Map;
import java.util.UUID;
import java.awt.Color;
import static org.junit.jupiter.api.Assertions.*;

class OpacSubclaimsTest {
    @TempDir Path directory;
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String OWNER_KEY = "OpacClaim:" + OWNER;
    private MarkerVisibility visibility() { return new MarkerVisibility(directory.resolve("hidden.json")); }
    private static MapWorld world() {
        return (MapWorld) Proxy.newProxyInstance(MapWorld.class.getClassLoader(), new Class<?>[]{MapWorld.class},
            (proxy, method, args) -> {
                if (method.getName().equals("identifier")) return WorldIdentifier.parse("minecraft:overworld");
                throw new UnsupportedOperationException(method.getName());
            });
    }
    private static OpacChunk chunk(int x, int z, String sub, String name, int color) {
        return new OpacChunk(new ChunkPos(x, z), "Owner", name, color, OWNER, sub, sub.equals("main") ? -1 : 1);
    }
    private static MarkerVisibility.Identity identity(String key) {
        return new MarkerVisibility.Identity("minecraft:overworld", Layers.Keys.OPAC, key);
    }
    @Test void adjacentSubclaimsStaySeparateEvenWithEqualNamesAndColors() {
        var layer = new OPACAreaMarkerLayer(world(), visibility());
        layer.addChunk(chunk(0, 0, "farm", "Home", 0), true);
        layer.addChunk(chunk(1, 0, "town", "Home", 0), true);
        assertEquals(2, layer.logicalCount());
        assertTrue(layer.markerIdentities().containsAll(java.util.Set.of(OWNER_KEY, OWNER_KEY + ":farm", OWNER_KEY + ":town")));
        layer.provider().getMarkers().forEach(marker -> assertEquals(Color.BLACK, marker.markerOptions().fillColor()));
        layer.updateGroupMetadata(OWNER_KEY + ":farm", "Owner", "Farm", 0xff0000);
        assertEquals(1, layer.provider().getMarkers().stream().filter(marker -> Color.RED.equals(marker.markerOptions().fillColor())).count());
        assertEquals(1, layer.provider().getMarkers().stream().filter(marker -> Color.BLACK.equals(marker.markerOptions().fillColor())).count());
    }
    @Test void subclaimNamedMainCannotCollideWithMainConfiguration() {
        var layer = new OPACAreaMarkerLayer(world(), visibility());
        layer.addChunk(chunk(0, 0, "main", "Home", 1), true);
        layer.addChunk(new OpacChunk(new ChunkPos(1, 0), "Owner", "Home", 1, OWNER, "main", 1), true);
        assertEquals(2, layer.logicalCount());
        assertTrue(layer.markerIdentities().containsAll(java.util.Set.of(OWNER_KEY + ":$main", OWNER_KEY + ":main")));
    }
    @Test void legacyOwnerExclusionsStillHideEverySubclaimAndCanBeCleared() {
        var visibility = visibility();
        visibility.setHidden(identity("OpacClaim:Owner"), true);
        var layer = new OPACAreaMarkerLayer(world(), visibility);
        layer.addChunk(chunk(0, 0, "main", "Main", 0x00ff00), true);
        layer.addChunk(chunk(1, 0, "farm", "Farm", 0xff0000), true);
        assertEquals(0, layer.visibleCount());
        assertTrue(layer.isHidden(OWNER_KEY + ":farm"));
        assertTrue(layer.isHidden(OWNER_KEY));
        assertFalse(visibility.exclusions().contains(identity("OpacClaim:Owner")));
        assertTrue(visibility.exclusions().contains(identity(OWNER_KEY)));
        layer.updateGroupMetadata(OWNER_KEY + ":farm", "Owner", "Renamed Farm", 0x0000ff);
        assertEquals(0, layer.visibleCount());
        visibility.setHidden(identity(OWNER_KEY), false);
        layer.refreshVisibility(OWNER_KEY);
        assertEquals(2, layer.visibleCount());
        assertFalse(layer.isHidden(OWNER_KEY + ":farm"));
    }
    @Test void ownerAndSubclaimExclusionsComposeWithoutExposingHiddenSiblings() {
        var visibility = visibility();
        var layer = new OPACAreaMarkerLayer(world(), visibility);
        layer.addChunk(chunk(0, 0, "farm", "Farm", 1), true);
        layer.addChunk(chunk(1, 0, "town", "Town", 2), true);
        visibility.setHidden(identity(OWNER_KEY + ":farm"), true); layer.refreshVisibility(OWNER_KEY + ":farm");
        assertEquals(1, layer.visibleCount());
        visibility.setHidden(identity(OWNER_KEY), true); layer.refreshVisibility(OWNER_KEY);
        assertEquals(0, layer.visibleCount());
        visibility.setHidden(identity(OWNER_KEY), false); layer.refreshVisibility(OWNER_KEY);
        assertEquals(1, layer.visibleCount());
        assertTrue(layer.isHidden(OWNER_KEY + ":farm"));
        assertFalse(layer.isHidden(OWNER_KEY + ":town"));
    }
    @Test void migratedOwnerExclusionSurvivesRenameAndRestart() {
        var visibility = visibility();
        visibility.setHidden(identity("OpacClaim:Owner"), true);
        var layer = new OPACAreaMarkerLayer(world(), visibility);
        var chunk = chunk(0, 0, "farm", "Farm", 1);
        layer.addChunk(chunk, true);
        layer.updateGroupMetadata(chunk.groupKey(), "NewName", "Farm", 1);
        assertEquals(0, layer.visibleCount());
        assertFalse(visibility.exclusions().contains(identity("OpacClaim:Owner")));
        var restored = visibility();
        restored.load();
        var restarted = new OPACAreaMarkerLayer(world(), restored);
        restarted.addChunk(new OpacChunk(chunk.pos(), "NewName", "Farm", 1, OWNER, "farm", 1), true);
        assertEquals(0, restarted.visibleCount());
        restored.setHidden(identity(OWNER_KEY), false);
        restarted.refreshVisibility(OWNER_KEY);
        assertEquals(1, restarted.visibleCount());
    }

    @Test void failedMigrationKeepsLegacyProtectionDuringRename() throws Exception {
        var visibility = visibility();
        var layer = new OPACAreaMarkerLayer(world(), visibility);
        var chunk = chunk(0, 0, "farm", "Farm", 1);
        layer.addChunk(chunk, true);
        visibility.setHidden(identity("OpacClaim:Owner"), true);
        layer.refreshVisibility("OpacClaim:Owner");
        java.nio.file.Files.delete(directory.resolve("hidden.json"));
        java.nio.file.Files.createDirectory(directory.resolve("hidden.json"));
        // Non-empty destination cannot be replaced by a state file.
        java.nio.file.Files.writeString(directory.resolve("hidden.json/blocker"), "blocked");
        layer.updateGroupMetadata(chunk.groupKey(), "NewName", "Updated", 2);
        assertEquals(0, layer.visibleCount());
        assertTrue(visibility.exclusions().contains(identity("OpacClaim:Owner")));
        assertFalse(visibility.exclusions().contains(identity(OWNER_KEY)));
    }
    @Test void reassignmentAndQueuedBulkChangesCleanUpEmptyGroups() throws Exception {
        var layer = new OPACAreaMarkerLayer(world(), visibility());
        for (int i = 0; i < 100; i++) layer.queueChunkChange(i, 0, chunk(i, 0, "farm", "Farm", 1));
        assertEquals(0, layer.logicalCount());
        layer.flushPendingChanges();
        assertEquals(1, layer.logicalCount());
        for (int i = 0; i < 100; i++) {
            layer.queueChunkChange(i, 0, null);
            layer.queueChunkChange(i, 0, chunk(i, 0, "town", "Town", 2));
        }
        layer.flushPendingChanges();
        assertEquals(1, layer.logicalCount());
        assertFalse(layer.markerIdentities().contains(OWNER_KEY + ":farm"));
        assertTrue(layer.markerIdentities().contains(OWNER_KEY + ":town"));
        for (int i = 0; i < 100; i++) layer.queueChunkChange(i, 0, null);
        layer.flushPendingChanges();
        assertEquals(0, layer.logicalCount());
        assertTrue(layer.markerIdentities().isEmpty());
        for (String fieldName : new String[]{"claims", "renderedKeys", "chunkOwners", "pendingChunks"}) {
            var field = OPACAreaMarkerLayer.class.getDeclaredField(fieldName); field.setAccessible(true);
            assertTrue(((Map<?, ?>) field.get(layer)).isEmpty(), fieldName);
        }
    }
    @Test void repeatedChunkNotificationDoesNotRebuildUnchangedGeometry() {
        var layer = new OPACAreaMarkerLayer(world(), visibility());
        var chunk = chunk(0, 0, "farm", "Farm", 1);
        layer.addChunk(chunk, true);
        var original = layer.provider().getMarkers().iterator().next();
        layer.queueChunkChange(0, 0, chunk);
        layer.flushPendingChanges();
        assertSame(original, layer.provider().getMarkers().iterator().next());
        layer.updateGroupMetadata(chunk.groupKey(), "RenamedOwner", "New", 2);
        assertTrue(layer.markerIdentities().contains(chunk.groupKey()));
        assertTrue(layer.markerSummary(OWNER_KEY).contains("RenamedOwner"));
    }
}
