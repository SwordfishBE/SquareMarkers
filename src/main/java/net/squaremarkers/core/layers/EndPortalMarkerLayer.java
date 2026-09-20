package net.squaremarkers.core.layers;

import net.squaremarkers.core.MarkersConfig;
import net.squaremarkers.core.helpers.HtmlHelper;
import net.squaremarkers.core.interfaces.entities.ISimpleMarker;
import net.squaremarkers.core.layers.primitive.SimpleMarkerLayer;
import net.squaremarkers.core.registries.Icons;
import net.squaremarkers.core.registries.Layers;
import org.jetbrains.annotations.NotNull;
import xyz.jpenilla.squaremap.api.MapWorld;

public class EndPortalMarkerLayer extends SimpleMarkerLayer {

    public EndPortalMarkerLayer(@NotNull MapWorld world) {
        super(Icons.Keys.END_PORTAL, Layers.Keys.END_PORTALS, Layers.Labels.END_PORTALS, Layers.Tooltips.END_PORTALS, world, MarkersConfig.END_PORTAL_MARKERS_PRIORITY);
    }

    @Override
    protected String createPopup(ISimpleMarker object) {
        return HtmlHelper.travelPopup(
                createTooltip(object),
                destinationKey(worldIdentifier),
                100, 0,
                "Go to The End"
        );
    }

    private String destinationKey(String worldKey) {
        return switch (worldKey) {
            case "minecraft:overworld" -> "minecraft_the_end";
            default -> dynamicDestinationKey(worldKey);
        };
    }

    private String dynamicDestinationKey(String worldKey) {
        return (worldKey + "_the_end").replace(":", "_");
    }

}
