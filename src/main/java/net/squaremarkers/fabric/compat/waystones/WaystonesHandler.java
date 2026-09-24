package net.squaremarkers.fabric.compat.waystones;

import net.blay09.mods.waystones.api.Waystone;
import net.blay09.mods.waystones.api.WaystoneKinds;
import net.blay09.mods.waystones.api.WaystonesAPI;
import net.blay09.mods.waystones.api.event.WaystoneInitializedEvent;
import net.blay09.mods.waystones.api.event.WaystoneRemovedEvent;
import net.blay09.mods.waystones.api.event.WaystoneUpdatedEvent;
import net.blay09.mods.waystones.api.event.WaystonesLoadedEvent;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.squaremarkers.core.SquareMarkersCore;
import net.squaremarkers.core.registries.Layers;
import net.squaremarkers.fabric.FabricMarkersConfig;
import xyz.jpenilla.squaremap.api.SquaremapProvider;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Event-driven bridge to the optional Waystones mod. No server references survive shutdown. */
public final class WaystonesHandler {
    private static final boolean INSTALLED = FabricLoader.getInstance().isModLoaded("waystones");
    private static final Map<UUID, WaystonePoint> POINTS = new HashMap<>();
    private static boolean registered;
    private static boolean refreshPending;

    private WaystonesHandler() {
    }

    public static boolean installed() {
        return INSTALLED;
    }

    public static boolean enabled() {
        return installed() && FabricMarkersConfig.WAYSTONES_ENABLED;
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        WaystonesLoadedEvent.EVENT.register(event -> replace(event.waystoneManager().getWaystones()));
        // Waystones can assign a name after this event (notably /waystones place).
        // Read its final state once at the end of the tick instead of polling.
        WaystoneInitializedEvent.EVENT.register(event -> {
            if (enabled()) {
                refreshPending = true;
            }
        });
        WaystoneUpdatedEvent.EVENT.register(event -> upsert(event.waystone()));
        WaystoneRemovedEvent.EVENT.register(event -> remove(event.waystone().getWaystoneUid()));
    }

    public static Map<UUID, WaystonePoint> current() {
        return POINTS;
    }

    public static void flushPending(MinecraftServer server) {
        if (refreshPending) {
            refreshPending = false;
            refresh(server);
        }
    }

    /** Rebuilds after a SquareMarkers config reload, including when support was enabled at runtime. */
    public static void refresh(MinecraftServer server) {
        if (enabled()) {
            replace(WaystonesAPI.getAllWaystones(server));
        } else {
            POINTS.clear();
        }
    }

    public static void reset() {
        POINTS.clear();
        refreshPending = false;
    }

    private static void replace(Collection<Waystone> waystones) {
        if (!enabled()) {
            return;
        }
        Map<UUID, WaystonePoint> next = new HashMap<>();
        for (Waystone waystone : waystones) {
            WaystonePoint point = toPoint(waystone);
            if (point != null) {
                next.put(point.id(), point);
            }
        }
        if (!next.equals(POINTS)) {
            POINTS.clear();
            POINTS.putAll(next);
            publish();
        }
    }

    private static void upsert(Waystone waystone) {
        if (!enabled()) {
            return;
        }
        UUID id = waystone.getWaystoneUid();
        WaystonePoint next = toPoint(waystone);
        WaystonePoint previous = next == null ? POINTS.remove(id) : POINTS.put(id, next);
        if (!Objects.equals(previous, next)) {
            publish();
        }
    }

    private static void remove(UUID id) {
        if (enabled() && POINTS.remove(id) != null) {
            publish();
        }
    }

    private static WaystonePoint toPoint(Waystone waystone) {
        if (waystone == null || !waystone.isValid() || waystone.isTransient()) {
            return null;
        }
        boolean sharestone = WaystoneKinds.isSharestone(waystone.getWaystoneKind());
        if (sharestone ? !FabricMarkersConfig.WAYSTONES_INCLUDE_SHARESTONES
            : !WaystoneKinds.WAYSTONE.equals(waystone.getWaystoneKind())
                || (!waystone.hasName() && !FabricMarkersConfig.WAYSTONES_INCLUDE_UNDISCOVERED)) {
            return null;
        }
        var pos = waystone.getPos();
        if (pos == null || Math.abs((long) pos.getX()) > 30_000_000
            || Math.abs((long) pos.getZ()) > 30_000_000) {
            return null;
        }
        String name = waystone.getName().getString();
        if (name.isBlank()) {
            name = sharestone ? "Sharestone" : "Undiscovered Waystone";
        }
        return new WaystonePoint(waystone.getWaystoneUid(), name,
            waystone.getDimension().identifier().toString(),
            pos.getX(), pos.getY(), pos.getZ(), sharestone);
    }

    private static void publish() {
        SquaremapProvider.get().mapWorlds().forEach(world -> {
            WaystoneMarkerLayer layer = SquareMarkersCore.squaremapHandler().getLayer(
                world.identifier().asString(), WaystoneMarkerLayer.class, Layers.Keys.WAYSTONES);
            if (layer != null) {
                layer.sync(POINTS);
            }
        });
    }
}
