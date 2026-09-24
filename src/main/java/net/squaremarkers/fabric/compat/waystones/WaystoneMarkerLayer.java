package net.squaremarkers.fabric.compat.waystones;

import net.squaremarkers.core.helpers.HtmlHelper;
import net.squaremarkers.core.layers.primitive.MarkerLayer;
import net.squaremarkers.core.markers.IconMarkerBuilder;
import net.squaremarkers.core.markers.MarkerBuilder;
import net.squaremarkers.core.registries.Icons;
import net.squaremarkers.core.registries.Layers;
import net.squaremarkers.fabric.FabricMarkersConfig;
import xyz.jpenilla.squaremap.api.MapWorld;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class WaystoneMarkerLayer extends MarkerLayer<WaystonePoint> {
    private final Map<UUID, WaystonePoint> rendered = new HashMap<>();

    public WaystoneMarkerLayer(MapWorld world) {
        super(Layers.Keys.WAYSTONES, Layers.Labels.WAYSTONES, world,
            FabricMarkersConfig.WAYSTONES_PRIORITY);
    }

    @Override
    public void load() {
        sync(WaystonesHandler.current());
    }

    public void sync(Map<UUID, WaystonePoint> waystones) {
        rendered.keySet().removeIf(id -> {
            WaystonePoint point = waystones.get(id);
            if (point != null && point.dimension().equals(worldIdentifier)) {
                return false;
            }
            removeMarker(id.toString());
            return true;
        });
        waystones.forEach((id, point) -> {
            if (point.dimension().equals(worldIdentifier) && !point.equals(rendered.get(id))) {
                loadMarker(point);
                rendered.put(id, point);
            }
        });
    }

    @Override
    public MarkerBuilder<?> createBuilder(WaystonePoint point) {
        return IconMarkerBuilder.newIconMarker(point.id().toString(), Icons.Keys.WARP_STONE,
            point.x(), point.z()).centerIcon(16, 16);
    }

    @Override
    protected String markerKey(WaystonePoint point) {
        return point.id().toString();
    }

    @Override
    protected String createTooltip(WaystonePoint point) {
        return HtmlHelper.sanitize(point.name());
    }

    @Override
    protected String createPopup(WaystonePoint point) {
        return "<b>" + HtmlHelper.sanitize(point.name()) + "</b><br>"
            + (point.sharestone() ? "Sharestone" : "Waystone") + "<br>"
            + point.x() + ", " + point.y() + ", " + point.z();
    }
}
