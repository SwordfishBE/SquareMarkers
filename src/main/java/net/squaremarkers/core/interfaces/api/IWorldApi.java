package net.squaremarkers.core.interfaces.api;

import net.squaremarkers.core.layers.primitive.MarkerLayer;
import org.jspecify.annotations.Nullable;

public interface IWorldApi {

	@Nullable
	<T extends MarkerLayer<?>> T getLayer(Class<T> layerClass, String layerKey);

}
