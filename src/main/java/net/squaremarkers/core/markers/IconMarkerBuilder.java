package net.squaremarkers.core.markers;

import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.Point;
import xyz.jpenilla.squaremap.api.marker.Icon;
import xyz.jpenilla.squaremap.api.marker.Marker;

public final class IconMarkerBuilder extends MarkerBuilder<Icon> {
    private IconMarkerBuilder(Icon marker) {
        super(marker);
    }

    public static IconMarkerBuilder newIconMarker(String ignoredKey, String icon, int x, int z) {
        return new IconMarkerBuilder(Marker.icon(Point.of(x, z), Key.of(icon), 16));
    }

    public IconMarkerBuilder centerIcon(int width, int height) {
        marker.sizeX(width);
        marker.sizeZ(height);
        marker.anchor(Point.of(width / 2.0D, height / 2.0D));
        return this;
    }
}
