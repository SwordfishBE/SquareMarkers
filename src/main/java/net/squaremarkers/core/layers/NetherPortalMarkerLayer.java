package net.squaremarkers.core.layers;

import net.squaremarkers.core.MarkersConfig;
import net.squaremarkers.core.FeedbackMessages;
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
    private final java.util.Set<String> unnamedMarkers = new java.util.HashSet<>();

    @Override public void load() { unnamedMarkers.clear(); super.load(); }
    @Override public void close() { unnamedMarkers.clear(); super.close(); }

    @Override public net.squaremarkers.core.markers.MarkerBuilder<?> createBuilder(ISimpleMarker marker) {
        if (marker.getName() == null || marker.getName().isBlank()) unnamedMarkers.add(marker.getKey());
        else unnamedMarkers.remove(marker.getKey());
        return super.createBuilder(marker);
    }

    @Override protected boolean additionallyHidden(String identity) {
        return MarkersConfig.NETHER_PORTAL_MARKERS_ONLY_SHOW_NAMED && unnamedMarkers.contains(identity);
    }

    @Override public InteractionResult remove(int x, int y, int z) {
        unnamedMarkers.remove(toMarkerKey(x, y, z));
        return super.remove(x, y, z);
    }

    @Override public void removeMarker(ISimpleMarker marker) {
        unnamedMarkers.remove(marker.getKey());
        super.removeMarker(marker);
    }

    public NetherPortalMarkerLayer(@NotNull MapWorld world) {
        super(Icons.Keys.NETHER_PORTAL, Layers.Keys.NETHER_PORTALS, Layers.Labels.NETHER_PORTALS, Layers.Tooltips.NETHER_PORTALS, world, MarkersConfig.NETHER_PORTAL_MARKERS_PRIORITY);
    }

    @Override
    public InteractionResult setName(int x, int y, int z, String newName) {
        if (!MarkersConfig.NETHER_PORTAL_MARKERS_RENAME) {
            return interact(x, y, z);
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
        return HtmlHelper.sanitize(name != null ? name : tooltip);
    }

    private String buttonText(String worldKey) {
        return (WorldHelpers.isOverworld(worldKey) ? FeedbackMessages.PORTAL_GO_TO_NETHER
            : FeedbackMessages.PORTAL_GO_TO_OVERWORLD).text();
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
