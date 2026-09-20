package net.squaremarkers.core.layers;

import net.minecraft.server.level.ServerPlayer;
import net.squaremarkers.core.SquareMarkersCore;
import net.squaremarkers.core.helpers.HtmlHelper;
import net.squaremarkers.core.helpers.WorldHelpers;
import net.squaremarkers.core.layers.primitive.MarkerLayer;
import net.squaremarkers.core.markers.IconMarkerBuilder;
import net.squaremarkers.core.markers.MarkerBuilder;
import net.squaremarkers.core.registries.Icons;
import net.squaremarkers.core.registries.Layers;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.SquaremapProvider;

public final class CrossDimensionPlayerMarkerLayer extends MarkerLayer<ServerPlayer> {
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
        clearMarkers();
        String sourceWorld = WorldHelpers.isOverworld(worldIdentifier) ? "minecraft:the_nether" : "minecraft:overworld";
        for (ServerPlayer player : SquareMarkersCore.server().getPlayerList().getPlayers()) {
            if (!player.level().dimension().identifier().toString().equals(sourceWorld)) {
                continue;
            }
            if (player.isSpectator() || player.isInvisible() || SquaremapProvider.get().playerManager().hidden(player.getUUID())) {
                continue;
            }
            loadMarker(player);
        }
    }

    @Override
    protected String markerKey(ServerPlayer player) {
        return player.getUUID().toString();
    }

    @Override
    public MarkerBuilder<?> createBuilder(ServerPlayer player) {
        double scale = WorldHelpers.isOverworld(worldIdentifier) ? 8.0D : 1.0D / 8.0D;
        int x = (int) Math.floor(player.getX() * scale);
        int z = (int) Math.floor(player.getZ() * scale);
        return IconMarkerBuilder.newIconMarker(markerKey(player), Icons.Keys.CROSS_DIMENSION_PLAYER, x, z)
            .centerIcon(16, 16);
    }

    @Override
    protected String createTooltip(ServerPlayer player) {
        return HtmlHelper.sanitize(player.getGameProfile().name());
    }
}
