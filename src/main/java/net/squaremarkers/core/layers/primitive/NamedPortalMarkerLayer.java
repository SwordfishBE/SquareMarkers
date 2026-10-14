package net.squaremarkers.core.layers.primitive;

import java.util.HashSet;
import java.util.Set;
import net.squaremarkers.core.interfaces.entities.ISimpleMarker;
import net.squaremarkers.core.markers.MarkerBuilder;
import net.squaremarkers.core.objects.InteractionResult;
import xyz.jpenilla.squaremap.api.MapWorld;

/** Shared portal naming and visibility; unnamed sources remain stored for later renaming. */
public abstract class NamedPortalMarkerLayer extends SimpleMarkerLayer {
    private final Set<String> unnamedMarkers = new HashSet<>();

    protected NamedPortalMarkerLayer(String icon, String key, String label, String tooltip, MapWorld world, int priority) {
        super(icon, key, label, tooltip, world, priority);
    }

    protected abstract boolean renameEnabled();
    protected abstract boolean onlyShowNamed();

    @Override public void load() { unnamedMarkers.clear(); clearMarkers(); super.load(); }
    @Override public void close() { unnamedMarkers.clear(); super.close(); }

    @Override public MarkerBuilder<?> createBuilder(ISimpleMarker marker) {
        if (marker.getName() == null || marker.getName().isBlank()) unnamedMarkers.add(marker.getKey());
        else unnamedMarkers.remove(marker.getKey());
        return super.createBuilder(marker);
    }

    @Override protected boolean additionallyHidden(String identity) {
        return onlyShowNamed() && unnamedMarkers.contains(identity);
    }

    @Override public InteractionResult remove(int x, int y, int z) {
        unnamedMarkers.remove(toMarkerKey(x, y, z));
        return super.remove(x, y, z);
    }

    @Override public void removeMarker(ISimpleMarker marker) {
        unnamedMarkers.remove(marker.getKey());
        super.removeMarker(marker);
    }

    @Override public InteractionResult setName(int x, int y, int z, String name) {
        return renameEnabled() ? super.setName(x, y, z, name) : interact(x, y, z);
    }

}
