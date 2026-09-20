package net.squaremarkers.core.layers.primitive;

import net.squaremarkers.core.interfaces.IMarkerRepository;
import net.squaremarkers.core.interfaces.entities.IMarker;
import net.squaremarkers.core.markers.MarkerBuilder;
import org.jetbrains.annotations.NotNull;
import xyz.jpenilla.squaremap.api.MapWorld;

import java.util.Optional;

public abstract class StoredMarkerLayer<T extends IMarker, R extends IMarkerRepository<? extends T>> extends MarkerLayer<T> {

	public StoredMarkerLayer(String key, String label, @NotNull MapWorld world, int priority) {
		super(key, label, world, priority);
	}

	public abstract Optional<? extends T> getMarker(String key);

	public abstract Optional<String> getClosestMarker(int x, int y, int z);

	public void updateMarker(T markerEntity) {
		removeMarker(markerEntity);
		loadMarker(markerEntity);
	}

	public void removeMarker(T markerEntity) {
		super.removeMarker(markerEntity.getKey());
	}

	public boolean hasMarker(T markerEntity) {
		return super.hasMarker(markerEntity.getKey());
	}

	protected abstract R getRepository();
}
