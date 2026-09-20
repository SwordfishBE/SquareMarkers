package net.squaremarkers.core.layers.primitive;

import net.squaremarkers.core.interfaces.entities.IMarker;
import net.squaremarkers.core.markers.MarkerBuilder;
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.SimpleLayerProvider;
import xyz.jpenilla.squaremap.api.marker.Marker;

public abstract class MarkerLayer<T> {
    private final String key;
    private final MapWorld world;
    private final SimpleLayerProvider provider;
    public final String worldIdentifier;

    protected MarkerLayer(String key, String label, MapWorld world, int priority) {
        this.key = key;
        this.world = world;
        this.worldIdentifier = world.identifier().asString();
        this.provider = SimpleLayerProvider.builder(label)
            .showControls(true)
            .defaultHidden(false)
            .layerPriority(priority)
            .zIndex(100 + priority)
            .build();
    }

    public abstract void load();

    public abstract MarkerBuilder<?> createBuilder(T object);

    public final void loadMarker(T markerEntity) {
        MarkerBuilder<?> builder = createBuilder(markerEntity);
        if (builder == null) {
            return;
        }
        @Language("HTML") String popup = createPopup(markerEntity);
        if (popup != null) {
            builder.addPopup(popup);
        }
        boolean labelled = false;
        @Language("HTML") String bottom = createPermanentBottomTooltip(markerEntity);
        if (bottom != null) {
            builder.addPermanentBottomTooltip(bottom);
            labelled = true;
        }
        @Language("HTML") String centered = createPermanentCenteredTooltip(markerEntity);
        if (centered != null && !labelled) {
            builder.addPermanentCenteredTooltip(centered);
            labelled = true;
        }
        @Language("HTML") String tooltip = createTooltip(markerEntity);
        if (tooltip != null && !labelled) {
            builder.addTooltip(tooltip);
        }
        addMarker(markerKey(markerEntity), builder.build());
    }

    protected String markerKey(T markerEntity) {
        if (markerEntity instanceof IMarker marker) {
            return marker.getKey();
        }
        throw new IllegalArgumentException("Marker entity does not expose a key: " + markerEntity);
    }

    protected @Nullable String createPopup(T object) {
        return null;
    }

    protected @Nullable String createTooltip(T object) {
        return null;
    }

    protected @Nullable String createPermanentCenteredTooltip(T object) {
        return null;
    }

    protected @Nullable String createPermanentBottomTooltip(T object) {
        return null;
    }

    public final void addMarker(String markerKey, Marker marker) {
        provider.addMarker(toKey(markerKey), marker);
    }

    public final void removeMarker(String markerKey) {
        provider.removeMarker(toKey(markerKey));
    }

    public final boolean hasMarker(String markerKey) {
        return provider.hasMarker(toKey(markerKey));
    }

    public final void clearMarkers() {
        provider.clearMarkers();
    }

    public final String toMarkerKey(int x, int y, int z) {
        return x + ":" + y + ":" + z;
    }

    private Key toKey(String markerKey) {
        return Key.of(markerKey.replaceAll("[^A-Za-z0-9._-]", "_"));
    }

    public final String getKey() {
        return key;
    }

    public final MapWorld getWorld() {
        return world;
    }

    public final SimpleLayerProvider provider() {
        return provider;
    }
}
