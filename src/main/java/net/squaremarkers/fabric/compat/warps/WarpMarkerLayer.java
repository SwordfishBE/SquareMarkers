package net.squaremarkers.fabric.compat.warps;

import net.squaremarkers.core.helpers.HtmlHelper;
import net.squaremarkers.core.layers.primitive.SnapshotMarkerLayer;
import net.squaremarkers.core.markers.IconMarkerBuilder;
import net.squaremarkers.core.markers.MarkerBuilder;
import net.squaremarkers.core.registries.Icons;
import xyz.jpenilla.squaremap.api.MapWorld;


public final class WarpMarkerLayer extends SnapshotMarkerLayer<String, WarpPoint> {
    private final WarpHandler.Source source;

    public WarpMarkerLayer(MapWorld world, WarpHandler.Source source) {
        super(source.layerKey(), source.label() + " Warps", world, source.priority());
        this.source = source;
    }

    @Override
    public void load() {
        sync(WarpHandler.current(source));
    }

    protected String dimension(WarpPoint point) { return point.dimension(); }

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
