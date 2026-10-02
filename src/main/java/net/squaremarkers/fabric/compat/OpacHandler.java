package net.squaremarkers.fabric.compat;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.squaremarkers.fabric.SquareMarkers;
import net.squaremarkers.fabric.FabricMarkersConfig;
import net.squaremarkers.core.SquareMarkersCore;
import net.squaremarkers.fabric.compat.layers.OPACAreaMarkerLayer;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.claims.ServerClaimsManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class OpacHandler {
	private static final Map<String, OPACAreaMarkerLayer> ACTIVE_LAYERS = new ConcurrentHashMap<>();
	private static MinecraftServer activeServer;
	private static boolean listenerRegistered;
	private static int metadataSeconds;
	private static boolean metadataFailureLogged;

	public static boolean isOpacLoaded(MinecraftServer server) {
		return SquareMarkers.isOpacEnabled()
			&& OpenPACServerAPI.get(server).getServerClaimsManager() instanceof ServerClaimsManager scm
			&& scm.isLoaded();
	}

	public static synchronized void activateLayer(MinecraftServer server, OPACAreaMarkerLayer markerLayer) {
		if (activeServer != server) {
			ACTIVE_LAYERS.clear();
			activeServer = server;
			listenerRegistered = false;
			metadataSeconds = 0;
			metadataFailureLogged = false;
		}
		ACTIVE_LAYERS.put(markerLayer.worldIdentifier, markerLayer);
	}

	public static synchronized void registerListener(MinecraftServer server) {
		if (activeServer != server || listenerRegistered) {
			return;
		}
		listenerRegistered = true;
		try {
			OpenPACServerAPI.get(server)
					.getServerClaimsManager()
					.getTracker().register(new OpacListener());
		} catch (RuntimeException exception) {
			listenerRegistered = false;
			throw exception;
		}
	}

	public static synchronized void deactivateLayer(OPACAreaMarkerLayer markerLayer) {
		ACTIVE_LAYERS.remove(markerLayer.worldIdentifier, markerLayer);
	}

	public static synchronized void reset() {
		ACTIVE_LAYERS.clear();
		activeServer = null;
		listenerRegistered = false;
		metadataSeconds = 0;
		metadataFailureLogged = false;
	}

	/** Called once per second; reads metadata once per server, not once per world. */
	public static void tickMetadata(MinecraftServer server) {
		if (activeServer != server || ACTIVE_LAYERS.isEmpty()) {
			metadataSeconds = 0;
			return;
		}
		if (++metadataSeconds < FabricMarkersConfig.OPAC_METADATA_REFRESH_INTERVAL) {
			return;
		}
		metadataSeconds = 0;
		try {
			if (!isOpacLoaded(server)) return;
			var layers = java.util.List.copyOf(ACTIVE_LAYERS.values());
			OpenPACServerAPI.get(server).getServerClaimsManager().getPlayerInfoStream().forEach(player -> {
				String owner = player.getPlayerUsername();
				String name = player.getClaimsName();
				int color = player.getClaimsColor();
				String label = (name.isEmpty() ? "" : name + " - ") + owner + "'s claim";
				layers.forEach(layer -> layer.updateMetadata(owner, label, color));
			});
			metadataFailureLogged = false;
		} catch (RuntimeException exception) {
			if (!metadataFailureLogged) {
				SquareMarkersCore.warn("Failed to refresh Open Parties and Claims names/colors", exception);
				metadataFailureLogged = true;
			}
		}
	}

	public static boolean isActiveLayer(OPACAreaMarkerLayer markerLayer) {
		return ACTIVE_LAYERS.get(markerLayer.worldIdentifier) == markerLayer;
	}

	static OPACAreaMarkerLayer activeLayer(String worldIdentifier) {
		return ACTIVE_LAYERS.get(worldIdentifier);
	}

    public static Collection<OpacChunk> load(MinecraftServer server, String worldIdentifier) {
        var chunks = new HashSet<OpacChunk>();
        OpenPACServerAPI.get(server)
                .getServerClaimsManager()
                .getPlayerInfoStream()
                .forEach(p -> {
                    var dimensionManager = p.getDimension(Identifier.parse(worldIdentifier));
	                if (dimensionManager == null) {
		                return;
	                }
                    dimensionManager.getStream().forEach(claim ->
                        claim.getStream().forEach(chunk -> chunks.add(
                            new OpacChunk(chunk, p.getPlayerUsername(), p.getClaimsName(), p.getClaimsColor())
                        ))
                    );
                });
        return chunks;
    }

	public static Collection<OpacChunk> getClaimedChunks(MinecraftServer server, Identifier world, UUID uuid) {
		var playerInfo = OpenPACServerAPI.get(server)
								 .getServerClaimsManager()
								 .getPlayerInfo(uuid);
		if (playerInfo == null) {
			return new ArrayList<>();
		}
		var pdc = playerInfo.getDimension(world);
		if (pdc == null) {
			return new ArrayList<>();
		}
		return pdc.getStream().flatMap(claim ->
				claim.getStream().map(chunk ->
				  new OpacChunk(chunk, playerInfo.getPlayerUsername(), playerInfo.getClaimsName(), playerInfo.getClaimsColor()
	    ))).toList();
	}

	public static OpacChunk getChunk(MinecraftServer server, UUID uuid, int x, int z) {
		var playerInfo = OpenPACServerAPI.get(server)
									 .getServerClaimsManager()
									 .getPlayerInfo(uuid);
		if (playerInfo == null) {
			return null;
		}
		return new OpacChunk(new ChunkPos(x, z), playerInfo.getPlayerUsername(), playerInfo.getClaimsName(), playerInfo.getClaimsColor());
	}

}
