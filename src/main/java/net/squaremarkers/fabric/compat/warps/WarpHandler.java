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
        ESSENTIAL_COMMANDS("essential_commands", Layers.Keys.ESSENTIAL_COMMANDS_WARPS, "Essential Commands"),
        HUSKHOMES("huskhomes", Layers.Keys.HUSKHOMES_WARPS, "HuskHomes");

        private final String modId;
        private final String layerKey;
        private final String label;
        private final boolean installed;

        Source(String modId, String layerKey, String label) {
            this.modId = modId;
            this.layerKey = layerKey;
            this.label = label;
            this.installed = FabricLoader.getInstance().isModLoaded(modId);
        }

        public String label() {
            return label;
        }

        public boolean installed() {
            return installed;
        }

        public String layerKey() {
            return layerKey;
        }

        public int priority() {
            return switch (this) {
                case FABRIC_ESSENTIALS -> FabricMarkersConfig.FABRIC_ESSENTIALS_WARPS_PRIORITY;
                case ESSENTIAL_COMMANDS -> FabricMarkersConfig.ESSENTIAL_COMMANDS_WARPS_PRIORITY;
                case HUSKHOMES -> FabricMarkersConfig.HUSKHOMES_WARPS_PRIORITY;
            };
        }

        public boolean enabled() {
            return installed && switch (this) {
                case FABRIC_ESSENTIALS -> FabricMarkersConfig.FABRIC_ESSENTIALS_WARPS_ENABLED;
                case ESSENTIAL_COMMANDS -> FabricMarkersConfig.ESSENTIAL_COMMANDS_WARPS_ENABLED;
                case HUSKHOMES -> FabricMarkersConfig.HUSKHOMES_WARPS_ENABLED;
            };
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
            if (source == Source.HUSKHOMES) {
                continue; // HuskHomes has an asynchronous database API and its own refresh schedule.
            }
            try {
                Map<String, WarpPoint> next = source == Source.FABRIC_ESSENTIALS
                    ? WarpReaders.fabricEssentials() : WarpReaders.essentialCommands();
                if (next == null) {
                    continue;
                }
                FAILED.remove(source);
                accept(source, next);
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

    static void accept(Source source, Map<String, WarpPoint> next) {
        if (!source.enabled() || next.equals(current(source))) {
            return;
        }
        Map<String, WarpPoint> snapshot = Map.copyOf(next);
        SNAPSHOTS.put(source, snapshot);
        SquaremapProvider.get().mapWorlds().forEach(world -> {
            WarpMarkerLayer layer = SquareMarkersCore.squaremapHandler().getLayer(
                world.identifier().asString(), WarpMarkerLayer.class, source.layerKey());
            if (layer != null) {
                layer.sync(snapshot);
            }
        });
    }

    static void clear(Source source) {
        SNAPSHOTS.remove(source);
    }
}
