package net.squaremarkers.fabric.helpers;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.squaremarkers.core.SquareMarkersCore;
import net.squaremarkers.core.interfaces.entities.IPoint;
import net.squaremarkers.core.layers.NetherPortalMarkerLayer;
import net.squaremarkers.core.registries.Layers;
import java.util.function.Function;

/** Checks sleep on missing chunks. Chunk callbacks only wake them; world reads happen after the tick. */
public final class NetherPortalValidation {
    private static final int CHECKS_PER_WORLD = 8;
    private static final int CHECKS_PER_TICK = 16;
    private static boolean workPending;
    private NetherPortalValidation() {}

    public static void reset() { workPending = false; }

    public static void queueAffected(ServerLevel level, BlockPos changed) {
        var layer = layer(level);
        if (layer == null) return;
        layer.forEachNearbyPosition(changed.getX(), changed.getY(), changed.getZ(), layer.validationChecks()::schedule);
        workPending |= layer.validationChecks().hasReady();
    }

    /** Rebuild after startup/reload so interrupted checks also recover from persisted markers. */
    public static void queueAll() {
        SquareMarkersCore.squaremapHandler().forEachLayer(NetherPortalMarkerLayer.class, NetherPortalValidation::queueLayer);
    }

    public static void queueWorld(String worldIdentifier) {
        var layer = SquareMarkersCore.squaremapHandler().getLayer(worldIdentifier,
            NetherPortalMarkerLayer.class, Layers.Keys.NETHER_PORTALS);
        if (layer != null) queueLayer(layer);
    }

    private static void queueLayer(NetherPortalMarkerLayer layer) {
        layer.forEachPosition(layer.validationChecks()::schedule);
        workPending |= layer.validationChecks().hasReady();
    }

    public static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
        var layer = layer(level);
        if (layer != null) workPending |= layer.validationChecks().chunkLoaded(chunk.getPos().pack());
    }

    public static void flush() {
        if (!workPending) return;
        workPending = false;
        int[] remaining = {CHECKS_PER_TICK};
        SquareMarkersCore.squaremapHandler().forEachLayer(NetherPortalMarkerLayer.class, layer -> {
            if (!layer.validationChecks().hasReady()) return;
            if (remaining[0] == 0) { workPending = true; return; }
            var server = SquareMarkersCore.server();
            ServerLevel level = server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                net.minecraft.resources.Identifier.parse(layer.worldIdentifier)));
            if (level == null) return;
            remaining[0] -= flushLayer(layer, point -> validate(point, level.getMinY(), level.getHeight(),
                pos -> PortalHelper.loadedState(level, pos)), Math.min(CHECKS_PER_WORLD, remaining[0]));
            workPending |= layer.validationChecks().hasReady();
        });
    }

    private static NetherPortalMarkerLayer layer(ServerLevel level) {
        return SquareMarkersCore.squaremapHandler().getLayer(level.dimension().identifier().toString(),
            NetherPortalMarkerLayer.class, Layers.Keys.NETHER_PORTALS);
    }

    record Validation(boolean invalid, Long missingChunk) {
        static final Validation VALID = new Validation(false, null);
        static final Validation INVALID = new Validation(true, null);
    }

    static Validation validate(IPoint point, int minY, int height, Function<BlockPos, BlockState> reader) {
        BlockPos center = new BlockPos(point.x(), point.y(), point.z());
        BlockState state = reader.apply(center);
        if (state == null) return new Validation(false, ChunkPos.pack(point.x() >> 4, point.z() >> 4));
        if (!state.is(Blocks.NETHER_PORTAL)) return Validation.INVALID;
        Long[] missing = {null};
        var shape = PortalHelper.loadedShape(center, state.getValue(BlockStateProperties.HORIZONTAL_AXIS), minY, height, pos -> {
            BlockState loaded = reader.apply(pos);
            if (loaded == null) missing[0] = ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4);
            return loaded;
        });
        if (missing[0] != null) return new Validation(false, missing[0]);
        return shape != null && shape.isComplete() ? Validation.VALID : Validation.INVALID;
    }

    static int flushLayer(NetherPortalMarkerLayer layer, Function<IPoint, Validation> validator, int budget) {
        var checks = layer.validationChecks();
        for (int i = 0; i < budget; i++) {
            IPoint point = checks.poll();
            if (point == null) return i;
            Validation result = validator.apply(point);
            if (result.missingChunk() != null) checks.await(point, result.missingChunk());
            else {
                checks.complete(point);
                if (result.invalid()) layer.remove(point.x(), point.y(), point.z());
            }
        }
        return budget;
    }
}
