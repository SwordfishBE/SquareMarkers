package net.squaremarkers.core.layers;

import net.squaremarkers.core.MarkersConfig;
import net.squaremarkers.core.helpers.HtmlHelper;
import net.squaremarkers.core.helpers.WorldHelpers;
import net.squaremarkers.core.interfaces.entities.ISimpleMarker;
import net.squaremarkers.core.layers.primitive.SimpleMarkerLayer;
import net.squaremarkers.core.objects.InteractionResult;
import net.squaremarkers.core.registries.Icons;
import net.squaremarkers.core.registries.Layers;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import xyz.jpenilla.squaremap.api.MapWorld;

public class NetherPortalMarkerLayer extends SimpleMarkerLayer {

    public NetherPortalMarkerLayer(@NotNull MapWorld world) {
        super(Icons.Keys.NETHER_PORTAL, Layers.Keys.NETHER_PORTALS, Layers.Labels.NETHER_PORTALS, Layers.Tooltips.NETHER_PORTALS, world, MarkersConfig.NETHER_PORTAL_MARKERS_PRIORITY);
    }

    @Override
    public InteractionResult setName(int x, int y, int z, String newName) {
        if (!MarkersConfig.NETHER_PORTAL_MARKERS_RENAME) {
            return InteractionResult.skip();
        }
        return super.setName(x, y, z, newName);
    }

    @Override
    protected String createPopup(ISimpleMarker object) {
        var pos = object.getPosition();
        String worldKey = worldIdentifier;
        boolean isOverworld = WorldHelpers.isOverworld(worldKey);
        // Define the destination
        int relativeX = isOverworld ? pos.x() / 8 : pos.x() * 8;
        int relativeZ = isOverworld ? pos.z() / 8 : pos.z() * 8;
        // Build the pop-up
        return HtmlHelper.travelPopup(
                createTooltip(object),
                destinationKey(worldKey),
                relativeX, relativeZ,
                buttonText(worldKey)
        );
    }

    @Override
    protected @Nullable String createPermanentBottomTooltip(ISimpleMarker object) {
        if (MarkersConfig.NETHER_PORTAL_MARKERS_ALWAYS_SHOW_NAME && object.getName() != null) {
            return createTooltip(object);
        }
        return null;
    }

    @Override
    protected String createTooltip(ISimpleMarker markerEntity) {
        @Language("HTML") var name = markerEntity.getName();
        return name != null ? name : tooltip;
    }

    private String buttonText(String worldKey) {
        return WorldHelpers.isOverworld(worldKey) ? "Go to Nether" : "Go to Overworld";
    }

    private String destinationKey(String worldKey) {
        return switch (worldKey) {
            case "minecraft:the_nether" -> "minecraft_overworld";
            case "minecraft:overworld" -> "minecraft_the_nether";
            default -> dynamicDestinationKey(worldKey);
        };
    }

    private String dynamicDestinationKey(String worldKey) {
        String destination;
        if (WorldHelpers.isOverworld(worldKey)) {
            destination = worldKey + "_nether";
        } else {
            destination = worldKey.replace("_nether", "");
        }
        return destination.replace(":", "_");
    }

}
