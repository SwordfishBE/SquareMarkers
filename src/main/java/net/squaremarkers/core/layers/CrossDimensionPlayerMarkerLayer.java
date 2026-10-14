package net.squaremarkers.core.layers;

import java.util.HashMap;
import java.util.UUID;
import net.squaremarkers.core.SquareMarkersCore;
import net.squaremarkers.core.helpers.HtmlHelper;
import net.squaremarkers.core.helpers.WorldHelpers;
import net.squaremarkers.core.layers.primitive.SnapshotMarkerLayer;
import net.squaremarkers.core.markers.IconMarkerBuilder;
import net.squaremarkers.core.markers.MarkerBuilder;
import net.squaremarkers.core.registries.Icons;
import net.squaremarkers.core.registries.Layers;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.SquaremapProvider;

public final class CrossDimensionPlayerMarkerLayer extends SnapshotMarkerLayer<UUID, CrossDimensionPlayerMarkerLayer.PlayerPoint> {
    /** Immutable display state, never retaining a player or server instance. */
    public record PlayerPoint(UUID id, String dimension, String name, int x, int z) {}
    public CrossDimensionPlayerMarkerLayer(MapWorld world) {
        super(
            WorldHelpers.isOverworld(world) ? Layers.Keys.NETHER_PLAYERS : Layers.Keys.OVERWORLD_PLAYERS,
            WorldHelpers.isOverworld(world) ? Layers.Labels.NETHER_PLAYERS : Layers.Labels.OVERWORLD_PLAYERS,
            world,
            21
        );
    }

    @Override
    public void load() {
        update();
    }

    public void update() {
        var snapshot = new HashMap<UUID, PlayerPoint>();
        String sourceWorld = WorldHelpers.isOverworld(worldIdentifier) ? "minecraft:the_nether" : "minecraft:overworld";
        double scale = WorldHelpers.isOverworld(worldIdentifier) ? 8.0D : 1.0D / 8.0D;
        for (var player : SquareMarkersCore.server().getPlayerList().getPlayers()) {
            if (!player.level().dimension().identifier().toString().equals(sourceWorld)) {
                continue;
            }
            if (player.isSpectator() || player.isInvisible() || SquaremapProvider.get().playerManager().hidden(player.getUUID())) {
                continue;
            }
            snapshot.put(player.getUUID(), new PlayerPoint(player.getUUID(), worldIdentifier,
                player.getGameProfile().name(), (int) Math.floor(player.getX() * scale), (int) Math.floor(player.getZ() * scale)));
        }
        sync(snapshot);
    }

    @Override
    protected String dimension(PlayerPoint player) { return player.dimension(); }

    @Override
    protected String markerKey(PlayerPoint player) {
        return player.id().toString();
    }

    @Override
    public MarkerBuilder<?> createBuilder(PlayerPoint player) {
        return IconMarkerBuilder.newIconMarker(markerKey(player), Icons.Keys.CROSS_DIMENSION_PLAYER, player.x(), player.z())
            .centerIcon(16, 16);
    }

    @Override
    protected String createTooltip(PlayerPoint player) {
        return HtmlHelper.sanitize(player.name());
    }
}
