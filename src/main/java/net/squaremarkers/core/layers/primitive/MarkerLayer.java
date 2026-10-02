package net.squaremarkers.core.layers.primitive;

import net.squaremarkers.core.interfaces.entities.IMarker;
import net.squaremarkers.core.markers.MarkerBuilder;
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.SimpleLayerProvider;
import xyz.jpenilla.squaremap.api.marker.Marker;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.squaremarkers.core.SquareMarkersCore;
import net.squaremarkers.core.MarkerVisibility;

public abstract class MarkerLayer<T> {
    private final String key;
    private final MapWorld world;
    private final SimpleLayerProvider provider;
    private final Map<String, Marker> logicalMarkers = new HashMap<>();
    private final MarkerVisibility visibility;
    public final String worldIdentifier;

    protected MarkerLayer(String key, String label, MapWorld world, int priority) {
        this(key, label, world, priority, SquareMarkersCore.visibility());
    }

    protected MarkerLayer(String key, String label, MapWorld world, int priority, MarkerVisibility visibility) {
        this.visibility = visibility;
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

    /** Called once per second on the server thread. */
    public void tick() {
    }

    /** Releases resources owned by this layer before it is replaced or unloaded. */
    public void close() {
        clearMarkers();
    }

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
        logicalMarkers.put(markerKey, marker);
        if (!isHidden(visibilityKey(markerKey))) provider.addMarker(toKey(markerKey), marker);
        else provider.removeMarker(toKey(markerKey));
    }

    public final void removeMarker(String markerKey) {
        logicalMarkers.remove(markerKey);
        provider.removeMarker(toKey(markerKey));
    }

    public final boolean hasMarker(String markerKey) {
        return logicalMarkers.containsKey(markerKey);
    }

    protected final @Nullable Marker renderedMarker(String markerKey) {
        return logicalMarkers.get(markerKey);
    }

    public final void clearMarkers() {
        logicalMarkers.clear();
        provider.clearMarkers();
    }

    /** A source object may have multiple geometry fragments sharing one visibility identity. */
    protected String visibilityKey(String markerKey) { return markerKey; }

    public final Set<String> markerIdentities() {
        return logicalMarkers.keySet().stream().map(this::visibilityKey).collect(Collectors.toUnmodifiableSet());
    }

    public final boolean isHidden(String identity) {
        return visibility.hidden(worldIdentifier, key, identity);
    }

    public final void refreshVisibility(String identity) {
        boolean hidden = isHidden(identity);
        logicalMarkers.forEach((raw, marker) -> {
            if (!visibilityKey(raw).equals(identity)) return;
            if (hidden) provider.removeMarker(toKey(raw)); else provider.addMarker(toKey(raw), marker);
        });
    }

    public final int logicalCount() { return logicalMarkers.size(); }
    public final int visibleCount() { return provider.registeredMarkers().size(); }

    public final String markerSummary(String identity) {
        return logicalMarkers.entrySet().stream().filter(entry -> visibilityKey(entry.getKey()).equals(identity))
            .map(entry -> {
                var options = entry.getValue().markerOptions();
                String text = options.hoverTooltip() != null ? options.hoverTooltip() : options.clickTooltip();
                if (text == null) return "";
                text = text.replaceAll("<[^>]*>", " ").replaceAll("[\\r\\n\\t]", " ");
                return text.substring(0, Math.min(text.length(), 160));
            }).findFirst().orElse("");
    }

    public final String toMarkerKey(int x, int y, int z) {
        return x + ":" + y + ":" + z;
    }

    private Key toKey(String markerKey) {
        String sanitized = markerKey.replaceAll("[^A-Za-z0-9._-]", "_");
        if (sanitized.equals(markerKey)) {
            return Key.of(sanitized);
        }
        String suffix = UUID.nameUUIDFromBytes(markerKey.getBytes(StandardCharsets.UTF_8)).toString();
        return Key.of(sanitized + "_" + suffix);
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
