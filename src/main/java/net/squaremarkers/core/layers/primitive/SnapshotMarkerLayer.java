package net.squaremarkers.core.layers.primitive;

import java.util.HashMap;
import java.util.Map;
import net.squaremarkers.core.MarkerVisibility;
import xyz.jpenilla.squaremap.api.MapWorld;

/** Synchronizes immutable source snapshots, rebuilding only changed markers in this world. */
public abstract class SnapshotMarkerLayer<K, T> extends MarkerLayer<T> {
    private final Map<K, T> rendered = new HashMap<>();

    protected SnapshotMarkerLayer(String key, String label, MapWorld world, int priority) {
        super(key, label, world, priority);
    }

    protected SnapshotMarkerLayer(String key, String label, MapWorld world, int priority, MarkerVisibility visibility) {
        super(key, label, world, priority, visibility);
    }

    protected abstract String dimension(T point);

    public final void sync(Map<K, T> snapshot) {
        rendered.keySet().removeIf(id -> {
            T point = snapshot.get(id);
            if (point != null && worldIdentifier.equals(dimension(point))) return false;
            removeMarker(id.toString());
            return true;
        });
        snapshot.forEach((id, point) -> {
            if (worldIdentifier.equals(dimension(point)) && !point.equals(rendered.get(id))) {
                loadMarker(point);
                rendered.put(id, point);
            }
        });
    }

    @Override public void close() {
        rendered.clear();
        super.close();
    }
}
