package net.squaremarkers.core.interfaces.api;

import net.squaremarkers.core.objects.LayerFactory;

public interface IApi {

	IWorldApi getWorld(String worldIdentifier);

	void registerMarkerLayer(LayerFactory factory);

	void registerIconImage(String path, String filename, String filetype);

}
