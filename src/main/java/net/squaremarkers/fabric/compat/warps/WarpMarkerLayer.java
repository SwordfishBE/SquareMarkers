package net.squaremarkers.fabric.compat.warps;

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

public final class WarpMarkerLayer extends MarkerLayer<WarpPoint> {
    private final WarpHandler.Source source;
    private final Map<String, WarpPoint> rendered = new HashMap<>();

    public WarpMarkerLayer(MapWorld world, WarpHandler.Source source) {
        super(source == WarpHandler.Source.FABRIC_ESSENTIALS
                ? Layers.Keys.FABRIC_ESSENTIALS_WARPS : Layers.Keys.ESSENTIAL_COMMANDS_WARPS,
            source == WarpHandler.Source.FABRIC_ESSENTIALS
                ? Layers.Labels.FABRIC_ESSENTIALS_WARPS : Layers.Labels.ESSENTIAL_COMMANDS_WARPS,
            world,
            source == WarpHandler.Source.FABRIC_ESSENTIALS
                ? FabricMarkersConfig.FABRIC_ESSENTIALS_WARPS_PRIORITY
                : FabricMarkersConfig.ESSENTIAL_COMMANDS_WARPS_PRIORITY);
        this.source = source;
    }

    @Override
    public void load() {
        sync(WarpHandler.current(source));
    }

    public void sync(Map<String, WarpPoint> warps) {
        rendered.keySet().removeIf(name -> {
            WarpPoint point = warps.get(name);
            if (point != null && point.dimension().equals(worldIdentifier)) {
                return false;
            }
            removeMarker(name);
            return true;
        });
        warps.forEach((name, point) -> {
            if (point.dimension().equals(worldIdentifier) && !point.equals(rendered.get(name))) {
                loadMarker(point);
                rendered.put(name, point);
            }
        });
    }

    @Override
    public MarkerBuilder<?> createBuilder(WarpPoint point) {
        return IconMarkerBuilder.newIconMarker(point.name(), Icons.Keys.WARP,
            (int) Math.floor(point.x()), (int) Math.floor(point.z())).centerIcon(16, 16);
    }

    @Override
    protected String markerKey(WarpPoint point) {
        return point.name();
    }

    @Override
    protected String createTooltip(WarpPoint point) {
        return HtmlHelper.sanitize(point.name());
    }

    @Override
    protected String createPopup(WarpPoint point) {
        return "<b>" + HtmlHelper.sanitize(point.name()) + "</b><br>"
            + HtmlHelper.sanitize(source.label()) + "<br>"
            + String.format(java.util.Locale.ROOT, "%.1f, %.1f, %.1f", point.x(), point.y(), point.z());
    }
}
