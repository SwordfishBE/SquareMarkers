package net.squaremarkers.core.layers;

import net.squaremarkers.core.MarkersConfig;
import net.squaremarkers.core.interfaces.entities.IMarker;
import net.squaremarkers.core.layers.primitive.MarkerLayer;
import net.squaremarkers.core.markers.IconMarkerBuilder;
import net.squaremarkers.core.markers.MarkerBuilder;
import net.squaremarkers.core.registries.Icons;
import net.squaremarkers.core.registries.Layers;
import xyz.jpenilla.squaremap.api.MapWorld;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class LightningMarkerLayer extends MarkerLayer<IMarker> {
    private final Map<String, Integer> remainingLifetime = new HashMap<>();

    public LightningMarkerLayer(MapWorld world) {
        super(Layers.Keys.LIGHTNING, Layers.Labels.LIGHTNING, world, MarkersConfig.LIGHTNING_MARKERS_PRIORITY);
    }

    @Override
    public void load() {
    }

    @Override
    public MarkerBuilder<?> createBuilder(IMarker object) {
        return null;
    }

    public void show(int x, int y, int z) {
        String key = toMarkerKey(x, y, z);
        addMarker(key, IconMarkerBuilder.newIconMarker(key, Icons.Keys.LIGHTNING, x, z)
            .centerIcon(16, 16)
            .build());
        int lifetime = MarkersConfig.LIGHTNING_MARKERS_LIFETIME;
        if (lifetime <= 0) {
            removeMarker(key);
        } else {
            remainingLifetime.put(key, lifetime);
        }
    }

    @Override
    public void tick() {
        Iterator<Map.Entry<String, Integer>> iterator = remainingLifetime.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Integer> entry = iterator.next();
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                removeMarker(entry.getKey());
                iterator.remove();
            } else {
                entry.setValue(remaining);
            }
        }
    }

    @Override
    public void close() {
        remainingLifetime.clear();
        clearMarkers();
    }
}
