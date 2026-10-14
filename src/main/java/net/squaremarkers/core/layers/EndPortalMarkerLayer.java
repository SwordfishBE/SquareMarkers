package net.squaremarkers.core.layers;

import net.squaremarkers.core.MarkersConfig;
import net.squaremarkers.core.FeedbackMessages;
import net.squaremarkers.core.helpers.HtmlHelper;
import net.squaremarkers.core.interfaces.entities.ISimpleMarker;
import net.squaremarkers.core.layers.primitive.NamedPortalMarkerLayer;
import net.squaremarkers.core.objects.InteractionResult;
import net.squaremarkers.core.registries.Icons;
import net.squaremarkers.core.registries.Layers;
import org.jetbrains.annotations.NotNull;
import xyz.jpenilla.squaremap.api.MapWorld;

public class EndPortalMarkerLayer extends NamedPortalMarkerLayer {
    /** A normal End portal occupies the 3x3 surface around its stored center. No world reads required. */
    public InteractionResult removeContainingBlock(int x, int y, int z) {
        var result = InteractionResult.skip();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (hasMarker(toMarkerKey(x + dx, y, z + dz))) result = remove(x + dx, y, z + dz);
            }
        }
        return result;
    }

    @Override protected boolean renameEnabled() { return MarkersConfig.END_PORTAL_MARKERS_RENAME; }
    @Override protected boolean onlyShowNamed() { return MarkersConfig.END_PORTAL_MARKERS_ONLY_SHOW_NAMED; }

    public EndPortalMarkerLayer(@NotNull MapWorld world) {
        super(Icons.Keys.END_PORTAL, Layers.Keys.END_PORTALS, Layers.Labels.END_PORTALS, Layers.Tooltips.END_PORTALS, world, MarkersConfig.END_PORTAL_MARKERS_PRIORITY);
    }

    @Override
    protected String createPopup(ISimpleMarker object) {
        return HtmlHelper.travelPopup(
                createTooltip(object),
                destinationKey(worldIdentifier),
                100, 0,
                FeedbackMessages.PORTAL_GO_TO_END.text()
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
