package net.squaremarkers.fabric.compat.worldborder;

import java.util.List;
import net.squaremarkers.core.layers.primitive.SnapshotMarkerLayer;
import net.squaremarkers.core.markers.MarkerBuilder;
import net.squaremarkers.core.markers.AreaMarkerBuilder;
import net.squaremarkers.core.json.entities.Point;
import net.squaremarkers.core.registries.Layers;
import net.squaremarkers.fabric.FabricMarkersConfig;
import xyz.jpenilla.squaremap.api.MapWorld;

public final class WorldBorderMarkerLayer extends SnapshotMarkerLayer<String, BorderBounds> {
    public WorldBorderMarkerLayer(MapWorld world) {
        super(Layers.Keys.WORLD_BORDER, "World Border (Serilum)", world, FabricMarkersConfig.WORLD_BORDER_PRIORITY);
    }

    @Override public void load() { sync(WorldBorderHandler.current()); }
    @Override protected String dimension(BorderBounds bounds) { return bounds.dimension(); }
    @Override protected String markerKey(BorderBounds bounds) { return bounds.dimension(); }

    @Override public MarkerBuilder<?> createBuilder(BorderBounds bounds) {
        return AreaMarkerBuilder.newOutlineMarker(bounds.dimension(), List.of(new Point(bounds.minX(), 0, bounds.minZ()),
            new Point(bounds.maxX(), 0, bounds.minZ()), new Point(bounds.maxX(), 0, bounds.maxZ()),
            new Point(bounds.minX(), 0, bounds.maxZ()), new Point(bounds.minX(), 0, bounds.minZ()))).stroke(0xFF0000, 3);
    }

    @Override protected String createTooltip(BorderBounds bounds) { return "World Border (Serilum)"; }
    @Override protected String createPopup(BorderBounds bounds) {
        return "<b>World Border (Serilum)</b><br>X: " + bounds.minX() + " to " + bounds.maxX()
            + "<br>Z: " + bounds.minZ() + " to " + bounds.maxZ();
    }
}
