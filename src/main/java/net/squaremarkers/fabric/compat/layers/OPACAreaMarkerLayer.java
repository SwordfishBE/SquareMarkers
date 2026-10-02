package net.squaremarkers.fabric.compat.layers;

import net.minecraft.server.MinecraftServer;
import net.squaremarkers.core.SquareMarkersCore;
import net.squaremarkers.core.MarkerVisibility;
import net.squaremarkers.core.helpers.HtmlHelper;
import net.squaremarkers.core.helpers.PolygonLoops;
import net.squaremarkers.core.interfaces.entities.IMarker;
import net.squaremarkers.core.layers.primitive.MarkerLayer;
import net.squaremarkers.core.markers.AreaMarkerBuilder;
import net.squaremarkers.core.markers.MarkerBuilder;
import net.squaremarkers.core.registries.Layers;
import net.squaremarkers.fabric.FabricMarkersConfig;
import net.squaremarkers.fabric.compat.OpacChunk;
import net.squaremarkers.fabric.compat.OpacClaim;
import net.squaremarkers.fabric.compat.OpacHandler;
import net.squaremarkers.fabric.compat.OpacGeometryWorker;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.marker.Marker;
import xyz.jpenilla.squaremap.api.marker.Polygon;
import java.awt.Color;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import net.minecraft.world.level.ChunkPos;
import java.util.LinkedHashSet;
import java.util.concurrent.Future;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

public final class OPACAreaMarkerLayer extends MarkerLayer<IMarker> {
    private final Map<String, OpacClaim> claims = new HashMap<>();
    private final Map<String, List<String>> renderedKeys = new HashMap<>();
    private final Map<ChunkPos, String> chunkOwners = new HashMap<>();
    private final Map<ChunkPos, OpacChunk> pendingChunks = new HashMap<>();
    private final Set<String> dirtyGroups = new LinkedHashSet<>();
    private final Map<String, Long> revisions = new HashMap<>();
    private final OpacGeometryWorker worker;
    private long revision;
    private GeometryJob geometryJob;
    private RenderPlan renderPlan;
    private boolean geometryFailureLogged;
    private static final int MARKERS_PER_TICK = 128;
    private record GeometryJob(String key, long revision, Future<List<PolygonLoops.Part>> future) {}
    private static final class RenderPlan {
        final String key;
        final long revision;
        final List<PolygonLoops.Part> parts;
        int cursor;
        RenderPlan(String key, long revision, List<PolygonLoops.Part> parts) {
            this.key = key; this.revision = revision; this.parts = parts;
        }
    }
    private boolean initialized;
    private boolean loadFailureLogged;

    public OPACAreaMarkerLayer(MapWorld world) {
        this(world, SquareMarkersCore.visibility(), OpacGeometryWorker.SHARED);
    }

    OPACAreaMarkerLayer(MapWorld world, MarkerVisibility visibility) {
        this(world, visibility, null);
    }

    OPACAreaMarkerLayer(MapWorld world, MarkerVisibility visibility, OpacGeometryWorker worker) {
        super(Layers.Keys.OPAC, Layers.Labels.OPAC, world, FabricMarkersConfig.OPAC_MARKERS_PRIORITY, visibility);
        this.worker = worker;
    }

    @Override
    public void load() {
        clearState();
		MinecraftServer server = getServer();
        OpacHandler.activateLayer(server, this);
        initialized = false;
        loadFailureLogged = false;
    }

    @Override
    public synchronized void tick() {
        MinecraftServer server = getServer();
        if (initialized || !OpacHandler.isActiveLayer(this) || !OpacHandler.isOpacLoaded(server)) {
            return;
        }
        // Register first. Any change delivered while taking the snapshot is de-duplicated by chunk position.
        try {
            OpacHandler.registerListener(server);
            // Retry snapshots from an empty geometry state; queued events remain authoritative.
            Map<ChunkPos, OpacChunk> queued = new HashMap<>(pendingChunks);
            clearState();
            pendingChunks.putAll(queued);
            OpacHandler.load(server, worldIdentifier).forEach(this::addChunk);
            flushPendingChanges();
            initialized = true;
            loadFailureLogged = false;
        } catch (RuntimeException exception) {
            if (!loadFailureLogged) {
                SquareMarkersCore.warn("Failed to initialize Open Parties and Claims markers for " + worldIdentifier, exception);
                loadFailureLogged = true;
            }
        }
    }

    @Override
    public synchronized void close() {
        initialized = false;
        clearState();
        OpacHandler.deactivateLayer(this);
    }

    public synchronized void invalidate() {
        initialized = false;
        clearState();
    }

    private void clearState() {
        if (geometryJob != null) geometryJob.future().cancel(true);
        geometryJob = null;
        renderPlan = null;
        revisions.clear();
        geometryFailureLogged = false;
        claims.clear(); renderedKeys.clear(); chunkOwners.clear(); pendingChunks.clear(); dirtyGroups.clear(); clearMarkers();
    }

    @Override
    public MarkerBuilder<?> createBuilder(IMarker object) {
        return null;
    }

    public synchronized void addChunk(OpacChunk chunk) {
        addChunk(chunk, false);
    }

    public synchronized void addChunk(OpacChunk chunk, boolean render) {
        String key = chunk.groupKey();
        String previous = chunkOwners.put(chunk.pos(), key);
        if (previous != null && !previous.equals(key)) removeFromGroup(previous, chunk.pos());
        OpacClaim claim = claims.computeIfAbsent(key, unused -> new OpacClaim(chunk));
        claim.addChunk(chunk);
        updateGroupMetadata(key, chunk.playerName(), chunk.getName(), chunk.color());
        if (!key.equals(previous)) markDirty(key);
        if (render) flushDirtyGroups();
    }

    public synchronized void removeChunk(int x, int z, boolean render) {
        ChunkPos pos = new ChunkPos(x, z);
        String previous = chunkOwners.remove(pos);
        if (previous != null) removeFromGroup(previous, pos);
        if (render) flushDirtyGroups();
    }

    private void removeFromGroup(String key, ChunkPos pos) {
        OpacClaim claim = claims.get(key);
        if (claim == null) return;
        claim.removeChunk(pos.x(), pos.z());
        markDirty(key);
        if (claim.isEmpty()) claims.remove(key);
    }

    /** Claim callbacks coalesce by chunk; bulk updates rebuild each affected group once per tick. */
    public synchronized void queueChunkChange(int x, int z, OpacChunk chunk) {
        pendingChunks.put(new ChunkPos(x, z), chunk);
    }

    public synchronized void flushPendingChanges() {
        if (pendingChunks.isEmpty() && dirtyGroups.isEmpty() && geometryJob == null && renderPlan == null) return;
        pendingChunks.forEach((pos, chunk) -> {
            if (chunk == null) removeChunk(pos.x(), pos.z(), false); else addChunk(chunk, false);
        });
        pendingChunks.clear();
        flushDirtyGroups();
    }

    private void flushDirtyGroups() {
        if (worker != null) { flushAsyncGeometry(); return; }
        for (String key : dirtyGroups) {
            OpacClaim claim = claims.get(key);
            if (claim == null) {
                List<String> removed = renderedKeys.remove(key);
                if (removed != null) removed.forEach(this::removeMarker);
                revisions.remove(key);
            } else renderClaim(claim);
        }
        dirtyGroups.clear();
    }

    private void markDirty(String key) {
        dirtyGroups.add(key);
        revisions.put(key, ++revision);
    }

    private boolean current(String key, long version) {
        return Long.valueOf(version).equals(revisions.get(key));
    }

    public synchronized boolean hasPendingGeometry() {
        return !pendingChunks.isEmpty() || !dirtyGroups.isEmpty() || geometryJob != null || renderPlan != null;
    }

    private void flushAsyncGeometry() {
        if (renderPlan != null && !current(renderPlan.key, renderPlan.revision)) renderPlan = null;
        if (geometryJob != null && geometryJob.future().isDone()) {
            GeometryJob completed = geometryJob;
            geometryJob = null;
            try {
                List<PolygonLoops.Part> parts = completed.future().get();
                if (current(completed.key(), completed.revision())) {
                    renderPlan = new RenderPlan(completed.key(), completed.revision(), parts);
                    geometryFailureLogged = false;
                }
            } catch (CancellationException ignored) {
                // Invalidated or closing layers discard their snapshots.
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException exception) {
                if (!geometryFailureLogged) SquareMarkersCore.warn("Failed to calculate OPAC claim geometry for " + worldIdentifier, exception.getCause());
                geometryFailureLogged = true;
            }
        }
        if (renderPlan != null) {
            applyPlan();
            if (renderPlan != null) return;
        }
        if (geometryJob != null || dirtyGroups.isEmpty()) return;
        String key = dirtyGroups.iterator().next();
        OpacClaim claim = claims.get(key);
        long version = revisions.get(key);
        if (claim == null) {
            dirtyGroups.remove(key);
            renderPlan = new RenderPlan(key, version, List.of());
            return;
        }
        Future<List<PolygonLoops.Part>> future = worker.trySubmit(() -> List.copyOf(claim.chunks()));
        if (future != null) {
            dirtyGroups.remove(key);
            geometryJob = new GeometryJob(key, version, future);
        }
    }

    /** The worker never touches the squaremap provider; publication is bounded on the server thread. */
    private void applyPlan() {
        RenderPlan plan = renderPlan;
        List<String> keys = renderedKeys.computeIfAbsent(plan.key, unused -> new ArrayList<>());
        OpacClaim claim = claims.get(plan.key);
        int budget = MARKERS_PER_TICK;
        while (budget > 0 && plan.cursor < plan.parts.size()) {
            String key = plan.key + ":" + (plan.cursor + 1);
            addMarker(key, polygonMarker(claim, plan.parts.get(plan.cursor)));
            if (plan.cursor == keys.size()) keys.add(key);
            plan.cursor++;
            budget--;
        }
        while (budget > 0 && keys.size() > plan.parts.size()) {
            removeMarker(keys.removeLast());
            budget--;
        }
        if (plan.cursor == plan.parts.size() && keys.size() == plan.parts.size()) {
            if (keys.isEmpty()) renderedKeys.remove(plan.key);
            if (claim == null) revisions.remove(plan.key);
            renderPlan = null;
        }
    }

    /** One in-memory representative per active group, not a scan of all claimed chunks. */
    public synchronized List<OpacChunk> metadataGroups() {
        return claims.values().stream().map(OpacClaim::representative).toList();
    }

    /** Reuses the existing geometry; unchanged metadata does not touch the provider. */
    public synchronized void updateMetadata(String owner, String name, int color) {
        updateGroupMetadata(OpacClaim.createKey(owner), owner, name, color);
    }

    public synchronized void updateGroupMetadata(String groupKey, String owner, String name, int color) {
        OpacClaim claim = claims.get(groupKey);
        if (claim != null && !migrateLegacyVisibility(claim)) {
            // Keep the last known owner name while storage is unavailable, so the
            // still-persisted legacy exclusion continues to protect the group.
            owner = claim.ownerName;
        }
        if (claim != null && !claim.ownerName.equals(owner)) {
            claim.ownerName = owner;
            refreshVisibility(groupKey);
        }
        if (claim == null || claim.name.equals(name) && claim.color == color) return;
        claim.name = name;
        claim.color = color;
        Color renderedColor = new Color(color & 0x00FFFFFF);
        String label = HtmlHelper.sanitize(name);
        for (String key : renderedKeys.getOrDefault(claim.key, List.of())) {
            if (!(renderedMarker(key) instanceof Polygon previous)) continue;
            var options = previous.markerOptions().asBuilder()
                .fillColor(renderedColor).strokeColor(renderedColor);
            if (FabricMarkersConfig.OPAC_MARKERS_ALWAYS_SHOW_NAME) {
                options.hoverTooltip(label);
            } else {
                options.clickTooltip(label);
            }
            addMarker(key, Marker.polygon(previous.mainPolygon(), previous.negativeSpace()).markerOptions(options));
        }
    }

    private boolean migrateLegacyVisibility(OpacClaim claim) {
        String legacy = OpacClaim.createKey(claim.ownerName);
        return markerVisibility().migrate(
            new MarkerVisibility.Identity(worldIdentifier, getKey(), legacy),
            new MarkerVisibility.Identity(worldIdentifier, getKey(), claim.ownerKey));
    }

    private synchronized void renderClaim(OpacClaim claim) {
        List<String> previousKeys = renderedKeys.remove(claim.key);
        if (previousKeys != null) previousKeys.forEach(this::removeMarker);
        if (claim.isEmpty()) return;
        List<String> keys = new ArrayList<>();
        for (PolygonLoops.Part polygon : PolygonLoops.group(claim.getPolygons())) {
            String key = claim.key + ":" + (keys.size() + 1);
            addMarker(key, polygonMarker(claim, polygon));
            keys.add(key);
        }
        renderedKeys.put(claim.key, keys);
    }

    private Marker polygonMarker(OpacClaim claim, PolygonLoops.Part polygon) {
        AreaMarkerBuilder markerBuilder = AreaMarkerBuilder.newAreaMarker(claim.key, polygon.exterior(), polygon.holes())
                .fill(claim.color)
                .stroke(claim.color);
            if (FabricMarkersConfig.OPAC_MARKERS_ALWAYS_SHOW_NAME) {
                markerBuilder.addPermanentCenteredTooltip(HtmlHelper.sanitize(claim.name));
            } else {
                markerBuilder.addPopup(HtmlHelper.sanitize(claim.name));
            }
        return markerBuilder.build();
    }

    @Override
    protected String visibilityKey(String markerKey) {
        // Strip only the generated polygon counter; retain the stable owner/subconfig identity.
        return markerKey.substring(0, markerKey.lastIndexOf(':'));
    }

    @Override
    protected Set<String> additionalMarkerIdentities() {
        Set<String> identities = new HashSet<>();
        claims.values().forEach(claim -> {
            identities.add(claim.ownerKey);
            String legacy = OpacClaim.createKey(claim.ownerName);
            if (markerVisibility().hidden(worldIdentifier, getKey(), legacy)) identities.add(legacy);
        });
        return identities;
    }

    @Override
    protected boolean additionallyHidden(String identity) {
        OpacClaim claim = claims.get(identity);
        if (claim != null) return markerVisibility().hidden(worldIdentifier, getKey(), claim.ownerKey)
            || markerVisibility().hidden(worldIdentifier, getKey(), OpacClaim.createKey(claim.ownerName));
        return claims.values().stream().anyMatch(group -> group.ownerKey.equals(identity)
            && markerVisibility().hidden(worldIdentifier, getKey(), OpacClaim.createKey(group.ownerName)));
    }

    @Override
    protected boolean affectedByVisibility(String markerIdentity, String changedIdentity) {
        if (markerIdentity.equals(changedIdentity)) return true;
        OpacClaim claim = claims.get(markerIdentity);
        return claim != null && (claim.ownerKey.equals(changedIdentity)
            || OpacClaim.createKey(claim.ownerName).equals(changedIdentity));
    }

    @Override
    protected String identitySummary(String identity) {
        return claims.values().stream().filter(claim -> claim.ownerKey.equals(identity)
            || OpacClaim.createKey(claim.ownerName).equals(identity))
            .map(claim -> "All claims and subclaims of " + claim.ownerName).findFirst().orElse(null);
    }

    public MinecraftServer getServer() {
        return SquareMarkersCore.server();
    }
}
