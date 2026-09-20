package net.squaremarkers.core.interfaces;

import net.squaremarkers.core.interfaces.entities.IAreaMarker;
import org.jspecify.annotations.Nullable;

public interface IAreaMarkerRepository<T extends IAreaMarker> extends IMarkerRepository<T> {

	@SuppressWarnings("Unused")
	@Nullable T get(String name, int color);

	@SuppressWarnings("Unused")
	T getOrCreate(String name, int color);

	@SuppressWarnings("Unused")
	boolean remove(String name, int color);

}
