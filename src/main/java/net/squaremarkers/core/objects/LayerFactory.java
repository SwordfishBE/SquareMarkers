package net.squaremarkers.core.objects;

import net.squaremarkers.core.layers.primitive.MarkerLayer;
import xyz.jpenilla.squaremap.api.MapWorld;

import java.util.function.Function;
import java.util.function.Predicate;

public class LayerFactory {

    private String key;

	private final Function<MapWorld, MarkerLayer<?>> builder;
	private final Predicate<MapWorld> predicate;

	public LayerFactory(Function<MapWorld, MarkerLayer<?>> builder, Predicate<MapWorld> predicate) {
		this.builder = builder;
		this.predicate = predicate;
	}

    /** A stable layer key allows replacement even when no map worlds are currently enabled. */
    public LayerFactory(String key, Function<MapWorld, MarkerLayer<?>> builder, Predicate<MapWorld> predicate) {
        this(builder, predicate);
        if (key == null || key.isBlank()) throw new IllegalArgumentException("Layer key must not be blank");
        this.key = key;
    }

    public String key() { return key; }

	public MarkerLayer<?> create(MapWorld world) {
        MarkerLayer<?> layer = builder.apply(world);
        if (key == null) key = layer.getKey();
        if (!key.equals(layer.getKey())) {
            layer.close();
            throw new IllegalArgumentException("Factory key " + key + " does not match layer key " + layer.getKey());
        }
		return layer;
	}

	public boolean disabledFor(MapWorld world) {
		return !predicate.test(world);
	}

}
