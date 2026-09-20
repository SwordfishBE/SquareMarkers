package net.squaremarkers.core.layers;

import net.squaremarkers.core.MarkersConfig;
import net.squaremarkers.core.interfaces.entities.IMarker;
import net.squaremarkers.core.layers.primitive.MarkerLayer;
import net.squaremarkers.core.markers.IconMarkerBuilder;
import net.squaremarkers.core.markers.MarkerBuilder;
import net.squaremarkers.core.registries.Icons;
import net.squaremarkers.core.registries.Layers;
import xyz.jpenilla.squaremap.api.MapWorld;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class LightningMarkerLayer extends MarkerLayer<IMarker> {
    private final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1, runnable -> {
        Thread thread = new Thread(runnable, "SquareMarkers-Lightning");
        thread.setDaemon(true);
        return thread;
    });

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
        executor.schedule(() -> removeMarker(key), MarkersConfig.LIGHTNING_MARKERS_LIFETIME, TimeUnit.SECONDS);
    }
}
