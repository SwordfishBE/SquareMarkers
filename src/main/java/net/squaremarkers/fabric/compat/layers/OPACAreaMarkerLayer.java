package net.squaremarkers.fabric.compat.layers;

import net.minecraft.server.MinecraftServer;
import net.squaremarkers.core.SquareMarkersCore;
import net.squaremarkers.core.helpers.HtmlHelper;
import net.squaremarkers.core.interfaces.entities.IMarker;
import net.squaremarkers.core.layers.primitive.MarkerLayer;
import net.squaremarkers.core.markers.AreaMarkerBuilder;
import net.squaremarkers.core.markers.MarkerBuilder;
import net.squaremarkers.core.registries.Layers;
import net.squaremarkers.fabric.FabricMarkersConfig;
import net.squaremarkers.fabric.compat.OpacChunk;
import net.squaremarkers.fabric.compat.OpacClaim;
import net.squaremarkers.fabric.compat.OpacHandler;
import org.intellij.lang.annotations.Language;
import xyz.jpenilla.squaremap.api.MapWorld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class OPACAreaMarkerLayer extends MarkerLayer<IMarker> {
    private final Map<String, OpacClaim> claims = new HashMap<>();
    private final Map<String, List<String>> renderedKeys = new HashMap<>();
    private boolean initialized;
    private boolean loadFailureLogged;

    public OPACAreaMarkerLayer(MapWorld world) {
        super(Layers.Keys.OPAC, Layers.Labels.OPAC, world, FabricMarkersConfig.OPAC_MARKERS_PRIORITY);
    }

    @Override
    public void load() {
        claims.clear();
        renderedKeys.clear();
        clearMarkers();
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
            OpacHandler.load(server, worldIdentifier).forEach(this::addChunk);
            claims.values().forEach(this::renderClaim);
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
        claims.clear();
        renderedKeys.clear();
        clearMarkers();
        OpacHandler.deactivateLayer(this);
    }

    public synchronized void invalidate() {
        initialized = false;
        claims.clear();
        renderedKeys.clear();
        clearMarkers();
    }

    @Override
    public MarkerBuilder<?> createBuilder(IMarker object) {
        return null;
    }

    private OpacClaim getOrCreateClaim(String playerName, @Language("HTML") String name, int color) {
        OpacClaim existing = claims.get(playerName);
        if (existing == null) {
            OpacClaim created = new OpacClaim(playerName, name, color);
            claims.put(playerName, created);
            return created;
        }
        if (existing.name.equals(name) && existing.color == color) {
            return existing;
        }
        renderedKeys.getOrDefault(existing.key, List.of()).forEach(this::removeMarker);
        renderedKeys.remove(existing.key);
        OpacClaim replacement = new OpacClaim(playerName, name, color);
        existing.chunks().forEach(replacement::addChunk);
        claims.put(playerName, replacement);
        return replacement;
    }

    public synchronized void addChunk(OpacChunk chunk) {
        addChunk(chunk, false);
    }

    public synchronized void addChunk(OpacChunk chunk, boolean render) {
        List<OpacClaim> changedClaims = new ArrayList<>();
        claims.values().forEach(claim -> {
            if (claim.removeChunk(chunk.pos().x(), chunk.pos().z())) {
                changedClaims.add(claim);
            }
        });
        OpacClaim claim = getOrCreateClaim(chunk.playerName(), chunk.getName(), chunk.color());
        claim.addChunk(chunk);
        if (render) {
            changedClaims.stream().filter(changed -> changed != claim).forEach(this::renderClaim);
            renderClaim(claim);
            claims.entrySet().removeIf(entry -> entry.getValue() != claim && entry.getValue().isEmpty());
        }
    }

    public synchronized void removeChunk(int x, int z, boolean render) {
		var iterator = claims.entrySet().iterator();
		while (iterator.hasNext()) {
			OpacClaim claim = iterator.next().getValue();
			if (claim.removeChunk(x, z)) {
				if (render) {
					renderClaim(claim);
				}
				if (claim.isEmpty()) {
					iterator.remove();
				}
				return;
			}
        }
    }

    private synchronized void renderClaim(OpacClaim claim) {
        renderedKeys.getOrDefault(claim.key, List.of()).forEach(this::removeMarker);
        List<String> keys = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger();
        claim.getPolygons().forEach(polygon -> {
            String key = claim.key + ":" + counter.incrementAndGet();
            AreaMarkerBuilder markerBuilder = AreaMarkerBuilder.newAreaMarker(key, polygon)
                .fill(claim.color)
                .stroke(claim.color);
            if (FabricMarkersConfig.OPAC_MARKERS_ALWAYS_SHOW_NAME) {
                markerBuilder.addPermanentCenteredTooltip(HtmlHelper.sanitize(claim.name));
            } else {
                markerBuilder.addPopup(HtmlHelper.sanitize(claim.name));
            }
            addMarker(key, markerBuilder.build());
            keys.add(key);
        });
        renderedKeys.put(claim.key, keys);
    }

    public MinecraftServer getServer() {
        return SquareMarkersCore.server();
    }
}
