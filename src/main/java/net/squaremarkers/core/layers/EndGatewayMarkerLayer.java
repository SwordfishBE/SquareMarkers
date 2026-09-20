package net.squaremarkers.core.layers;

import net.squaremarkers.core.MarkersConfig;
import net.squaremarkers.core.layers.primitive.SimpleMarkerLayer;
import net.squaremarkers.core.registries.Icons;
import net.squaremarkers.core.registries.Layers;
import org.jetbrains.annotations.NotNull;
import xyz.jpenilla.squaremap.api.MapWorld;

public class EndGatewayMarkerLayer extends SimpleMarkerLayer {

    public EndGatewayMarkerLayer(@NotNull MapWorld world) {
        super(Icons.Keys.END_GATEWAY, Layers.Keys.END_GATEWAYS, Layers.Labels.END_GATEWAYS, Layers.Tooltips.END_GATEWAYS, world, MarkersConfig.END_GATEWAY_MARKERS_PRIORITY);
    }
}
