package net.squaremarkers.core.objects;

import net.squaremarkers.core.layers.primitive.MarkerLayer;
import xyz.jpenilla.squaremap.api.MapWorld;

import java.util.function.Function;
import java.util.function.Predicate;

public class LayerFactory {

	private final Function<MapWorld, MarkerLayer<?>> builder;
	private final Predicate<MapWorld> predicate;

	public LayerFactory(Function<MapWorld, MarkerLayer<?>> builder, Predicate<MapWorld> predicate) {
		this.builder = builder;
		this.predicate = predicate;
	}

	public MarkerLayer<?> create(MapWorld world) {
		return builder.apply(world);
	}

	public boolean disabledFor(MapWorld world) {
		return !predicate.test(world);
	}

}
