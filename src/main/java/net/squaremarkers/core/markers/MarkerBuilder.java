package net.squaremarkers.core.markers;

import org.intellij.lang.annotations.Language;
import xyz.jpenilla.squaremap.api.marker.Marker;
import xyz.jpenilla.squaremap.api.marker.MarkerOptions;

public class MarkerBuilder<T extends Marker> {
    protected final T marker;
    protected final MarkerOptions.Builder options = MarkerOptions.builder();

    protected MarkerBuilder(T marker) {
        this.marker = marker;
    }

    public Marker build() {
        marker.markerOptions(options);
        return marker;
    }

    public MarkerBuilder<T> addTooltip(@Language("HTML") String content) {
        options.hoverTooltip(content);
        return this;
    }

    public MarkerBuilder<T> addPermanentBottomTooltip(@Language("HTML") String content) {
        options.hoverTooltip(content);
        return this;
    }

    public MarkerBuilder<T> addPermanentCenteredTooltip(@Language("HTML") String content) {
        options.hoverTooltip(content);
        return this;
    }

    public MarkerBuilder<T> addPopup(@Language("HTML") String content) {
        options.clickTooltip(content);
        return this;
    }
}
