package net.squaremarkers.fabric.compat.layers;

import net.minecraft.server.MinecraftServer;
import net.squaremarkers.core.SquareMarkersCore;
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
        SquareMarkersCore.runParallel(() -> {
            while (!OpacHandler.isOpacLoaded(server)) {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            OpacHandler.load(server, worldIdentifier).forEach(this::addChunk);
			if (!OpacHandler.isActiveLayer(this)) {
				return;
			}
            claims.values().forEach(this::renderClaim);
			OpacHandler.registerListener(server);
        });
    }

    @Override
    public MarkerBuilder<?> createBuilder(IMarker object) {
        return null;
    }

    private OpacClaim getOrCreateClaim(String playerName, @Language("HTML") String name, int color) {
        return claims.computeIfAbsent(playerName, ignored -> new OpacClaim(name, color));
    }

    public synchronized void addChunk(OpacChunk chunk) {
        addChunk(chunk, false);
    }

    public synchronized void addChunk(OpacChunk chunk, boolean render) {
        OpacClaim claim = getOrCreateClaim(chunk.playerName(), chunk.getName(), chunk.color());
        claim.addChunk(chunk);
        if (render) {
            renderClaim(claim);
        }
    }

    public synchronized void removeChunk(int x, int z, boolean render) {
        for (OpacClaim claim : claims.values()) {
            if (claim.removeChunk(x, z)) {
                if (render) {
                    renderClaim(claim);
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
                markerBuilder.addPermanentCenteredTooltip(claim.name);
            } else {
                markerBuilder.addPopup(claim.name);
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
