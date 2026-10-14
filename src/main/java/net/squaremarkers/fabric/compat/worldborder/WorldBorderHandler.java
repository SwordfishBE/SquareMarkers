package net.squaremarkers.fabric.compat.worldborder;

import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.squaremarkers.core.SquareMarkersCore;
import net.squaremarkers.core.registries.Layers;
import net.squaremarkers.fabric.FabricMarkersConfig;
import xyz.jpenilla.squaremap.api.SquaremapProvider;

/** At most three rectangles, checked on the server thread only while integration is enabled. */
public final class WorldBorderHandler {
    private static final boolean INSTALLED = FabricLoader.getInstance().isModLoaded("worldborder");
    private static Map<String, BorderBounds> snapshot = Map.of();
    private static WorldBorderReader reader;
    private static boolean failed;

    private WorldBorderHandler() {}

    public static boolean installed() { return INSTALLED; }
    public static boolean enabled() { return INSTALLED && FabricMarkersConfig.WORLD_BORDER_ENABLED; }

    public static boolean supports(String world) {
        return world.equals("minecraft:overworld") || world.equals("minecraft:the_nether") || world.equals("minecraft:the_end");
    }

    public static Map<String, BorderBounds> current() { return snapshot; }

    public static void refresh() {
        if (!enabled()) { reset(); return; }
        try {
            if (reader == null) reader = WorldBorderReader.resolve();
            accept(reader.read());
            failed = false;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            accept(Map.of()); // Do not leave stale, misleading bounds after a failed read.
            if (!failed) {
                SquareMarkersCore.warn("Could not read World Border settings; its map boundaries are unavailable", exception);
                failed = true;
            }
        }
    }

    private static void accept(Map<String, BorderBounds> next) {
        if (next.equals(snapshot)) return;
        snapshot = next;
        for (var world : SquaremapProvider.get().mapWorlds()) {
            var layer = SquareMarkersCore.squaremapHandler().getLayer(world.identifier().asString(),
                WorldBorderMarkerLayer.class, Layers.Keys.WORLD_BORDER);
            if (layer != null) layer.sync(snapshot);
        }
    }

    public static void reset() { snapshot = Map.of(); reader = null; failed = false; }
}
