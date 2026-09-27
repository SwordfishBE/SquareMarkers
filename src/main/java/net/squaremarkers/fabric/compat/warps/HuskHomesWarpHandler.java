package net.squaremarkers.fabric.compat.warps;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionResult;
import net.squaremarkers.core.SquareMarkersCore;
import net.william278.huskhomes.api.FabricHuskHomesAPI;
import net.william278.huskhomes.event.DeleteAllWarpsCallback;
import net.william278.huskhomes.event.WarpCreateCallback;
import net.william278.huskhomes.event.WarpDeleteCallback;
import net.william278.huskhomes.event.WarpEditCallback;
import net.william278.huskhomes.position.Warp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/** Event-driven HuskHomes bridge. Database reads are asynchronous and never run each second. */
public final class HuskHomesWarpHandler {
    private static final WarpHandler.Source SOURCE = WarpHandler.Source.HUSKHOMES;
    private static final AtomicBoolean QUEUED = new AtomicBoolean();
    private static final AtomicBoolean FAILED = new AtomicBoolean();
    private static final AtomicLong GENERATION = new AtomicLong();
    private static final AtomicLong REQUEST = new AtomicLong();
    private static volatile MinecraftServer server;
    private static boolean registered;

    private HuskHomesWarpHandler() {
    }

    public static boolean installed() {
        return SOURCE.installed();
    }

    public static void register() {
        if (registered || !installed()) {
            return;
        }
        registered = true;
        WarpCreateCallback.EVENT.register(event -> {
            scheduleRefresh();
            return InteractionResult.PASS;
        });
        WarpEditCallback.EVENT.register(event -> {
            scheduleRefresh();
            return InteractionResult.PASS;
        });
        WarpDeleteCallback.EVENT.register(event -> {
            scheduleRefresh();
            return InteractionResult.PASS;
        });
        DeleteAllWarpsCallback.EVENT.register(event -> {
            scheduleRefresh();
            return InteractionResult.PASS;
        });
    }

    public static void start(MinecraftServer activeServer) {
        server = activeServer;
        refreshAsync();
    }

    public static void reload() {
        if (SOURCE.enabled()) {
            refreshAsync();
        } else {
            REQUEST.incrementAndGet();
            WarpHandler.clear(SOURCE);
        }
    }

    public static void reset() {
        GENERATION.incrementAndGet();
        REQUEST.incrementAndGet();
        QUEUED.set(false);
        FAILED.set(false);
        server = null;
        WarpHandler.clear(SOURCE);
    }

    /** Also reconciles API or cross-server changes that bypass HuskHomes command events. */
    public static void refreshAsync() {
        if (!SOURCE.enabled() || server == null) {
            return;
        }
        long request = REQUEST.incrementAndGet();
        try {
            FabricHuskHomesAPI.getInstance().getLocalWarps().whenComplete((warps, error) -> {
                if (error != null) {
                    warnOnce(error);
                    return;
                }
                Map<String, WarpPoint> next;
                try {
                    next = toPoints(warps);
                } catch (RuntimeException exception) {
                    warnOnce(exception);
                    return;
                }
                MinecraftServer target = server;
                if (target != null) {
                    target.execute(() -> {
                        if (server == target && request == REQUEST.get() && SOURCE.enabled()) {
                            FAILED.set(false);
                            WarpHandler.accept(SOURCE, next);
                        }
                    });
                }
            });
        } catch (RuntimeException | LinkageError exception) {
            warnOnce(exception);
        }
    }

    private static void scheduleRefresh() {
        if (!SOURCE.enabled() || server == null || !QUEUED.compareAndSet(false, true)) {
            return;
        }
        long generation = GENERATION.get();
        CompletableFuture.runAsync(() -> {
            if (generation == GENERATION.get()) {
                QUEUED.set(false);
                refreshAsync();
            }
        }, CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS));
    }

    private static Map<String, WarpPoint> toPoints(List<Warp> warps) {
        Map<String, WarpPoint> points = new HashMap<>();
        for (Warp warp : warps) {
            String name = warp.getName();
            String world = warp.getWorld().getName();
            double x = warp.getX();
            double y = warp.getY();
            double z = warp.getZ();
            if (name == null || name.isBlank() || world == null || world.isBlank()
                || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || Math.abs(x) > 30_000_000 || Math.abs(z) > 30_000_000) {
                continue;
            }
            String dimension = world.contains(":") ? world : "minecraft:" + world;
            points.put(name, new WarpPoint(name, dimension, x, y, z));
        }
        return points;
    }

    private static void warnOnce(Throwable error) {
        if (FAILED.compareAndSet(false, true)) {
            SquareMarkersCore.warn("Could not read HuskHomes warps", error);
        }
    }
}
