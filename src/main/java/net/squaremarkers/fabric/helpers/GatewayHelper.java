package net.squaremarkers.fabric.helpers;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.squaremarkers.core.MarkersConfig;
import net.squaremarkers.core.SquareMarkersCore;
import net.squaremarkers.core.json.entities.Point;
import net.squaremarkers.core.layers.EndGatewayMarkerLayer;
import net.squaremarkers.core.registries.Layers;
import net.squaremarkers.fabric.interfaces.EndGatewayInterface;

/** Event-driven destination capture: never loads chunks or invokes teleport generation. */
public final class GatewayHelper {
    private GatewayHelper() {}

    private static EndGatewayMarkerLayer layer(ServerLevel level) {
        if (!MarkersConfig.END_GATEWAY_MARKERS_ENABLED) return null;
        return SquareMarkersCore.squaremapHandler().getLayer(level.dimension().identifier().toString(),
            EndGatewayMarkerLayer.class, Layers.Keys.END_GATEWAYS);
    }

    public static void refresh(ServerLevel level, BlockPos pos) {
        var layer = layer(level);
        if (layer == null || !layer.hasMarker(layer.toMarkerKey(pos.getX(), pos.getY(), pos.getZ()))) return;
        // Unlike hasChunkAt + level.getBlockEntity, this cannot wait for a chunk upgrade.
        var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk != null && chunk.getBlockEntities().get(pos) instanceof TheEndGatewayBlockEntity gateway) {
            capture(layer, gateway);
        }
    }

    /** For lifecycle/mixin callbacks: use the supplied entity, never re-enter chunk loading. */
    public static void refresh(ServerLevel level, TheEndGatewayBlockEntity gateway) {
        var layer = layer(level);
        if (layer != null) capture(layer, gateway);
    }

    private static Point destination(TheEndGatewayBlockEntity gateway) {
        var exit = ((EndGatewayInterface) gateway).squareMarkers$getExitPortal();
        return exit == null ? null : new Point(exit.getX(), exit.getY(), exit.getZ());
    }

    private static void capture(EndGatewayMarkerLayer layer, TheEndGatewayBlockEntity gateway) {
        var pos = gateway.getBlockPos();
        if (!layer.hasMarker(layer.toMarkerKey(pos.getX(), pos.getY(), pos.getZ()))) return;
        layer.setDestination(pos.getX(), pos.getY(), pos.getZ(), destination(gateway));
    }

    public static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
        var layer = layer(level);
        if (layer == null) return;
        // Only already instantiated block entities in the chunk being loaded, not a block scan.
        for (var entity : chunk.getBlockEntities().values()) {
            if (entity instanceof TheEndGatewayBlockEntity gateway) capture(layer, gateway);
        }
    }

    public static void refreshLoaded(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            var layer = layer(level);
            if (layer == null) continue;
            layer.refreshDestinations(marker -> {
                var point = marker.getPosition();
                var pos = new BlockPos(point.x(), point.y(), point.z());
                var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
                if (chunk == null) return marker.getGatewayDestination();
                // Do not instantiate a block entity or consult the world while chunks are loading.
                var entity = chunk.getBlockEntities().get(pos);
                if (entity instanceof TheEndGatewayBlockEntity gateway) return destination(gateway);
                // Packed/lazily instantiated entities are captured by later lifecycle/use events.
                return marker.getGatewayDestination();
            });
        }
    }
}
