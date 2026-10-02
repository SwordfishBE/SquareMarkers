package net.squaremarkers.core;

import net.squaremarkers.core.layers.DeathMarkerLayer;
import net.squaremarkers.core.registries.Layers;
import java.util.List;
import java.util.UUID;

/** Runs on the Minecraft server thread; uses the shared once-per-second tick. */
public final class DeathMarkers {
    private static final DeathStore STORE = new DeathStore(SquareMarkersCore.getMainDir().resolve("deaths.json"));
    private DeathMarkers() {}
    public static void start() { STORE.load(System.currentTimeMillis()); }
    public static List<DeathStore.Death> current() { return STORE.current(System.currentTimeMillis()); }
    public static void tick() {
        if (MarkersConfig.DEATH_MARKERS_ENABLED) STORE.expire(System.currentTimeMillis()).forEach(DeathMarkers::remove);
    }
    public static void record(UUID player, String name, String world, int x, int y, int z) {
        if (!MarkersConfig.DEATH_MARKERS_ENABLED) return;
        if (Math.abs((long) x) > 30_000_000 || Math.abs((long) z) > 30_000_000) return;
        var layer = SquareMarkersCore.squaremapHandler().getLayer(world, DeathMarkerLayer.class, Layers.Keys.DEATHS);
        if (layer == null) return; // Do not collect private locations from unmapped worlds.
        long now = System.currentTimeMillis();
        STORE.expire(now).forEach(DeathMarkers::remove);
        var death = new DeathStore.Death(player.toString(), name, world, x, y, z, now,
            now + MarkersConfig.DEATH_MARKERS_LIFETIME * 1000L);
        STORE.add(death).forEach(DeathMarkers::remove);
        layer.loadMarker(death);
    }
    private static void remove(DeathStore.Death death) {
        var layer = SquareMarkersCore.squaremapHandler().getLayer(death.world(), DeathMarkerLayer.class, Layers.Keys.DEATHS);
        if (layer != null) layer.removeMarker(death.player());
    }
    public static void save() { STORE.save(); }
    public static void stop() {
        STORE.expire(System.currentTimeMillis());
        STORE.save();
        STORE.clearRuntime();
    }
}
