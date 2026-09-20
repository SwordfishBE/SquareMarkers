package net.squaremarkers.core;

import net.squaremarkers.core.interfaces.api.IApi;
import net.squaremarkers.core.interfaces.api.IWorldApi;
import net.squaremarkers.core.layers.primitive.MarkerLayer;
import net.squaremarkers.core.objects.LayerFactory;
import org.jspecify.annotations.Nullable;

public final class Api implements IApi {
    @Override
    public IWorldApi getWorld(String worldIdentifier) {
        return new WorldApi(worldIdentifier);
    }

    @Override
    public void registerMarkerLayer(LayerFactory factory) {
        SquareMarkersCore.squaremapHandler().registerMarkerLayer(factory);
    }

    @Override
    public void registerIconImage(String path, String filename, String filetype) {
        SquareMarkersCore.squaremapHandler().registerIconImage(path, filename, filetype);
    }

    private record WorldApi(String worldIdentifier) implements IWorldApi {
        @Override
        public @Nullable <T extends MarkerLayer<?>> T getLayer(Class<T> layerClass, String layerKey) {
            return SquareMarkersCore.squaremapHandler().getLayer(worldIdentifier, layerClass, layerKey);
        }
    }
}
