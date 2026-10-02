package net.squaremarkers.fabric.compat.layers;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import net.minecraft.world.level.ChunkPos;
import net.squaremarkers.core.MarkerVisibility;
import net.squaremarkers.core.registries.Layers;
import net.squaremarkers.fabric.compat.OpacChunk;
import net.squaremarkers.fabric.compat.OpacGeometryWorker;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import static org.junit.jupiter.api.Assertions.*;

class OpacAsyncGeometryTest {
    @TempDir Path directory;
    private final OpacGeometryWorker worker = new OpacGeometryWorker();
    private static final UUID OWNER = UUID.fromString("12345678-1234-1234-1234-123456789012");
    private static final String KEY = "OpacClaim:" + OWNER + ":farm";
    @AfterEach void stop() { worker.shutdown(); }

    private OPACAreaMarkerLayer layer(MarkerVisibility visibility) {
        MapWorld world = (MapWorld) Proxy.newProxyInstance(MapWorld.class.getClassLoader(), new Class<?>[]{MapWorld.class},
            (proxy, method, args) -> {
                if (method.getName().equals("identifier")) return WorldIdentifier.parse("minecraft:overworld");
                throw new UnsupportedOperationException(method.getName());
            });
        return new OPACAreaMarkerLayer(world, visibility, worker);
    }
    private OpacChunk chunk(int i) {
        return new OpacChunk(new ChunkPos(i * 2, 0), "Owner", "Farm", 1, OWNER, "farm", 1);
    }
    private void enqueue(OPACAreaMarkerLayer layer, int count) {
        for (int i = 0; i < count; i++) layer.queueChunkChange(i * 2, 0, chunk(i));
    }
    private void drain(OPACAreaMarkerLayer layer) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (layer.hasPendingGeometry() && System.nanoTime() < deadline) {
            int before = layer.logicalCount();
            layer.flushPendingChanges();
            assertTrue(Math.abs(layer.logicalCount() - before) <= 128, "Unbounded publication");
            Thread.sleep(1);
        }
        assertFalse(layer.hasPendingGeometry());
    }

    @Test void publishesAndRemovesLargeGroupsInBoundedBatches() throws Exception {
        var layer = layer(new MarkerVisibility(directory.resolve("hidden.json")));
        enqueue(layer, 3000);
        layer.flushPendingChanges();
        assertEquals(0, layer.logicalCount()); // submission never publishes on the worker
        drain(layer);
        assertEquals(3000, layer.logicalCount());
        for (int i = 0; i < 3000; i++) layer.queueChunkChange(i * 2, 0, null);
        drain(layer);
        assertEquals(0, layer.logicalCount());
        assertTrue(layer.markerIdentities().isEmpty());
    }

    @Test void changedClaimDiscardsOldSnapshotAndUsesLatestMetadataAndVisibility() throws Exception {
        var visibility = new MarkerVisibility(directory.resolve("hidden.json"));
        var layer = layer(visibility);
        enqueue(layer, 300);
        layer.flushPendingChanges();
        // The old snapshot may finish now, but has not been published.
        for (int i = 1; i < 300; i++) layer.queueChunkChange(i * 2, 0, null);
        layer.updateGroupMetadata(KEY, "Owner", "Latest", 0xff0000);
        visibility.setHidden(new MarkerVisibility.Identity("minecraft:overworld", Layers.Keys.OPAC, KEY), true);
        layer.flushPendingChanges();
        assertEquals(0, layer.logicalCount(), "Stale snapshot was published");
        drain(layer);
        assertEquals(1, layer.logicalCount());
        assertEquals(0, layer.visibleCount());
        visibility.setHidden(new MarkerVisibility.Identity("minecraft:overworld", Layers.Keys.OPAC, KEY), false);
        layer.refreshVisibility(KEY);
        var marker = layer.provider().getMarkers().iterator().next();
        assertEquals(java.awt.Color.RED, marker.markerOptions().fillColor());
        assertTrue(layer.markerSummary(KEY).contains("Latest"));
    }

    @Test void invalidationAndRestartCannotPublishOldResults() throws Exception {
        var layer = layer(new MarkerVisibility(directory.resolve("hidden.json")));
        enqueue(layer, 3000);
        layer.flushPendingChanges();
        layer.invalidate();
        assertFalse(layer.hasPendingGeometry());
        worker.shutdown();
        enqueue(layer, 1);
        drain(layer);
        assertEquals(1, layer.logicalCount());
    }

    @Test void workerRefusesExtraSnapshotsWhileReservedAndRecoversAfterFailure() throws Exception {
        assertThrows(IllegalStateException.class, () -> worker.trySubmit(() -> {
            assertNull(worker.trySubmit(() -> { fail("Rejected work must not copy a snapshot"); return java.util.List.of(); }));
            throw new IllegalStateException("Snapshot failed");
        }));
        assertEquals(1, worker.trySubmit(() -> java.util.List.of(chunk(0))).get(5, TimeUnit.SECONDS).size());
    }
}
