package net.squaremarkers.fabric.compat.warps;

import net.fabricmc.loader.api.FabricLoader;
import net.squaremarkers.core.SquareMarkersCore;
import net.squaremarkers.core.registries.Layers;
import net.squaremarkers.fabric.FabricMarkersConfig;
import xyz.jpenilla.squaremap.api.SquaremapProvider;

import java.lang.reflect.InvocationTargetException;
import java.util.EnumMap;
import java.util.Map;

/** Synchronizes optional warp mods once per second, on the server thread. */
public final class WarpHandler {
    public enum Source {
        FABRIC_ESSENTIALS("fabric-essentials", Layers.Keys.FABRIC_ESSENTIALS_WARPS, "Fabric Essentials"),
        ESSENTIAL_COMMANDS("essential_commands", Layers.Keys.ESSENTIAL_COMMANDS_WARPS, "Essential Commands");

        private final String modId;
        private final String layerKey;
        private final String label;

        Source(String modId, String layerKey, String label) {
            this.modId = modId;
            this.layerKey = layerKey;
            this.label = label;
        }

        public String label() {
            return label;
        }

        public boolean enabled() {
            return FabricLoader.getInstance().isModLoaded(modId) && (this == FABRIC_ESSENTIALS
                ? FabricMarkersConfig.FABRIC_ESSENTIALS_WARPS_ENABLED
                : FabricMarkersConfig.ESSENTIAL_COMMANDS_WARPS_ENABLED);
        }
    }

    private static final EnumMap<Source, Map<String, WarpPoint>> SNAPSHOTS = new EnumMap<>(Source.class);
    private static final EnumMap<Source, Boolean> FAILED = new EnumMap<>(Source.class);

    private WarpHandler() {
    }

    public static Map<String, WarpPoint> current(Source source) {
        return SNAPSHOTS.getOrDefault(source, Map.of());
    }

    public static void refresh() {
        for (Source source : Source.values()) {
            if (!source.enabled()) {
                SNAPSHOTS.remove(source);
                continue;
            }
            try {
                Map<String, WarpPoint> next = source == Source.FABRIC_ESSENTIALS
                    ? WarpReaders.fabricEssentials() : WarpReaders.essentialCommands();
                if (next == null) {
                    continue;
                }
                FAILED.remove(source);
                if (next.equals(current(source))) {
                    continue;
                }
                SNAPSHOTS.put(source, Map.copyOf(next));
                SquaremapProvider.get().mapWorlds().forEach(world -> {
                    WarpMarkerLayer layer = SquareMarkersCore.squaremapHandler().getLayer(
                        world.identifier().asString(), WarpMarkerLayer.class, source.layerKey);
                    if (layer != null) {
                        layer.sync(next);
                    }
                });
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                if (FAILED.put(source, true) == null) {
                    Throwable cause = exception instanceof InvocationTargetException target
                        ? target.getTargetException() : exception;
                    SquareMarkersCore.warn("Could not read " + source.label + " warps", cause);
                }
            }
        }
    }

    public static void reset() {
        SNAPSHOTS.clear();
        FAILED.clear();
    }
}
