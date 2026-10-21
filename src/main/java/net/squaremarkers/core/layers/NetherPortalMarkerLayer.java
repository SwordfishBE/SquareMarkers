package net.squaremarkers.core.layers;

import net.squaremarkers.core.MarkersConfig;
import net.squaremarkers.core.FeedbackMessages;
import net.squaremarkers.core.helpers.HtmlHelper;
import net.squaremarkers.core.helpers.WorldHelpers;
import net.squaremarkers.core.interfaces.entities.ISimpleMarker;
import net.squaremarkers.core.layers.primitive.NamedPortalMarkerLayer;
import net.squaremarkers.core.registries.Icons;
import net.squaremarkers.core.registries.Layers;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import xyz.jpenilla.squaremap.api.MapWorld;
import net.squaremarkers.core.interfaces.entities.IPoint;
import net.squaremarkers.core.json.entities.Point;
import net.squaremarkers.core.helpers.DeferredChecks;
import net.squaremarkers.core.markers.MarkerBuilder;
import net.squaremarkers.core.objects.InteractionResult;
import net.minecraft.world.level.ChunkPos;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class NetherPortalMarkerLayer extends NamedPortalMarkerLayer {
    private final Map<Long, Map<String, IPoint>> positionsByChunk = new HashMap<>();
    private final DeferredChecks<IPoint> checks = new DeferredChecks<>();

    public DeferredChecks<IPoint> validationChecks() { return checks; }

    @Override public void load() { positionsByChunk.clear(); checks.clear(); super.load(); }
    @Override public void close() { positionsByChunk.clear(); checks.clear(); super.close(); }

    @Override public MarkerBuilder<?> createBuilder(ISimpleMarker marker) {
        IPoint pos = marker.getPosition();
        if (pos.y() != Integer.MIN_VALUE) {
            positionsByChunk.computeIfAbsent(chunk(pos), unused -> new HashMap<>()).put(marker.getKey(), pos);
        }
        return super.createBuilder(marker);
    }

    /** Metadata changes keep pending validation and replace the displayed marker in place. */
    @Override public void updateMarker(ISimpleMarker marker) { loadMarker(marker); }

    public void forEachPosition(Consumer<IPoint> action) {
        positionsByChunk.values().forEach(positions -> positions.values().forEach(action));
    }

    /** Portal dimensions are at most 21 blocks; examine only nearby indexed markers. */
    public void forEachNearbyPosition(int x, int y, int z, Consumer<IPoint> action) {
        for (int cx = (x - 21) >> 4; cx <= (x + 21) >> 4; cx++) {
            for (int cz = (z - 21) >> 4; cz <= (z + 21) >> 4; cz++) {
                var positions = positionsByChunk.get(ChunkPos.pack(cx, cz));
                if (positions == null) continue;
                positions.values().forEach(pos -> {
                    if (Math.abs((long) pos.x() - x) <= 21 && Math.abs((long) pos.y() - y) <= 21
                        && Math.abs((long) pos.z() - z) <= 21) action.accept(pos);
                });
            }
        }
    }

    private static long chunk(IPoint pos) { return ChunkPos.pack(pos.x() >> 4, pos.z() >> 4); }

    private void forget(IPoint pos) {
        var positions = positionsByChunk.get(chunk(pos));
        if (positions != null) {
            positions.remove(toMarkerKey(pos.x(), pos.y(), pos.z()));
            if (positions.isEmpty()) positionsByChunk.remove(chunk(pos));
        }
        checks.complete(pos);
    }

    @Override public InteractionResult remove(int x, int y, int z) {
        InteractionResult result = super.remove(x, y, z);
        if (result.state() == InteractionResult.State.REMOVED) forget(new Point(x, y, z));
        return result;
    }

    @Override public void removeMarker(ISimpleMarker marker) {
        forget(marker.getPosition());
        super.removeMarker(marker);
    }
    @Override protected boolean renameEnabled() { return MarkersConfig.NETHER_PORTAL_MARKERS_RENAME; }
    @Override protected boolean onlyShowNamed() { return MarkersConfig.NETHER_PORTAL_MARKERS_ONLY_SHOW_NAMED; }

    public NetherPortalMarkerLayer(@NotNull MapWorld world) {
        super(Icons.Keys.NETHER_PORTAL, Layers.Keys.NETHER_PORTALS, Layers.Labels.NETHER_PORTALS, Layers.Tooltips.NETHER_PORTALS, world, MarkersConfig.NETHER_PORTAL_MARKERS_PRIORITY);
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
