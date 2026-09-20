package net.squaremarkers.core.markers;

import net.squaremarkers.core.interfaces.entities.IPoint;
import xyz.jpenilla.squaremap.api.marker.Marker;

import java.awt.Color;
import java.util.List;

public final class AreaMarkerBuilder extends MarkerBuilder<Marker> {
    private AreaMarkerBuilder(Marker marker) {
        super(marker);
    }

    public static AreaMarkerBuilder newAreaMarker(String ignoredKey, List<IPoint> points) {
        return new AreaMarkerBuilder(Marker.polygon(points.stream().map(IPoint::toMapPoint).toList()));
    }

    public static AreaMarkerBuilder newAreaMarker(String ignoredKey, IPoint center, int radius) {
        return new AreaMarkerBuilder(Marker.circle(center.toMapPoint(), radius));
    }

    public AreaMarkerBuilder fill(int color) {
        return fill(color, 96);
    }

    public AreaMarkerBuilder fill(int color, int alpha) {
        options.fill(true).fillColor(new Color(color & 0x00FFFFFF)).fillOpacity((alpha & 0xFF) / 255.0D);
        return this;
    }

    public AreaMarkerBuilder stroke(int color) {
        return stroke(color, 2);
    }

    public AreaMarkerBuilder stroke(int color, int weight) {
        options.stroke(true).strokeColor(new Color(color & 0x00FFFFFF)).strokeWeight(weight).strokeOpacity(1.0D);
        return this;
    }
}
