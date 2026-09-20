package net.squaremarkers.core.layers;

import net.squaremarkers.core.MarkersConfig;
import net.squaremarkers.core.layers.primitive.SimpleMarkerLayer;
import net.squaremarkers.core.registries.Icons;
import net.squaremarkers.core.registries.Layers;
import org.jetbrains.annotations.NotNull;
import xyz.jpenilla.squaremap.api.MapWorld;

public class BeaconMarkerLayer extends SimpleMarkerLayer {

	public BeaconMarkerLayer(@NotNull MapWorld world) {
		super(Icons.Keys.BEACON, Layers.Keys.BEACONS, Layers.Labels.BEACONS, Layers.Tooltips.BEACONS, world,
		      MarkersConfig.BEACON_MARKERS_PRIORITY
		);
	}
}
