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
import xaero.pac.common.claims.player.api.IPlayerChunkClaimAPI;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class OpacHandler {
	private static final Map<String, OPACAreaMarkerLayer> ACTIVE_LAYERS = new ConcurrentHashMap<>();
	private static MinecraftServer activeServer;
	private static boolean listenerRegistered;
	private static int metadataSeconds;
	private static boolean metadataFailureLogged;
	private static int geometryWorldCursor;

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
		OpacGeometryWorker.SHARED.shutdown();
		geometryWorldCursor = 0;
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
            var manager = OpenPACServerAPI.get(server).getServerClaimsManager();
            Map<MetadataKey, Metadata> cache = new HashMap<>();
            for (var layer : java.util.List.copyOf(ACTIVE_LAYERS.values())) {
                Identifier world = Identifier.parse(layer.worldIdentifier);
                for (OpacChunk representative : layer.metadataGroups()) {
                    var state = manager.get(world, representative.pos());
                    if (state == null) { layer.invalidate(); break; }
                    Metadata next = cache.computeIfAbsent(new MetadataKey(world, state.getPlayerId(), state.getSubConfigIndex()),
                        unused -> metadata(server, world, state));
                    OpacChunk chunk = next.chunk(representative.pos());
                    if (!chunk.groupKey().equals(representative.groupKey())) { layer.invalidate(); break; }
                    layer.updateGroupMetadata(chunk.groupKey(), chunk.playerName(), chunk.getName(), chunk.color());
                }
            }
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

    public static void flushPending() {
        var layers = java.util.List.copyOf(ACTIVE_LAYERS.values());
        if (layers.isEmpty()) return;
        int start = Math.floorMod(geometryWorldCursor++, layers.size());
        for (int i = 0; i < layers.size(); i++) layers.get((start + i) % layers.size()).flushPendingChanges();
    }

    private record MetadataKey(Identifier world, UUID owner, int subIndex) {}
    private record Metadata(UUID owner, String ownerName, String subId, int subIndex, String name, int color) {
        OpacChunk chunk(ChunkPos pos) { return new OpacChunk(pos, ownerName, name, color, owner, subId, subIndex); }
    }

    private static Metadata metadata(MinecraftServer server, Identifier world, IPlayerChunkClaimAPI state) {
        var api = OpenPACServerAPI.get(server);
        var manager = api.getServerClaimsManager();
        var player = manager.getPlayerInfo(state.getPlayerId());
        var config = api.getPlayerConfigManager().getLoadedConfig(state.getPlayerId());
        var subConfig = config == null ? null : config.getEffectiveSubConfig(state.getSubConfigIndex());
        boolean main = subConfig == null || subConfig.getSubId() == null;
        String subId = main ? "main" : subConfig.getSubId();
        String name = manager.getCustomName(state, world);
        return new Metadata(state.getPlayerId(), player.getPlayerUsername(), subId, main ? -1 : state.getSubConfigIndex(),
            name == null ? "" : name, manager.getColor(state, world));
    }

    public static Collection<OpacChunk> load(MinecraftServer server, String worldIdentifier) {
        var chunks = new HashSet<OpacChunk>();
        Identifier world = Identifier.parse(worldIdentifier);
        Map<MetadataKey, Metadata> cache = new HashMap<>();
        OpenPACServerAPI.get(server)
                .getServerClaimsManager()
                .getPlayerInfoStream()
                .forEach(p -> {
                    var dimensionManager = p.getDimension(world);
	                if (dimensionManager == null) {
		                return;
	                }
                    dimensionManager.getStream().forEach(claim -> {
                        var state = claim.getClaimState();
                        Metadata detail = cache.computeIfAbsent(new MetadataKey(world, state.getPlayerId(), state.getSubConfigIndex()),
                            unused -> metadata(server, world, state));
                        claim.getStream().forEach(pos -> chunks.add(detail.chunk(pos)));
                    });
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
        return pdc.getStream().flatMap(claim -> {
            Metadata detail = metadata(server, world, claim.getClaimState());
            return claim.getStream().map(detail::chunk);
        }).toList();
	}

	public static OpacChunk getChunk(MinecraftServer server, Identifier world, IPlayerChunkClaimAPI state, int x, int z) {
        return metadata(server, world, state).chunk(new ChunkPos(x, z));
	}

}
