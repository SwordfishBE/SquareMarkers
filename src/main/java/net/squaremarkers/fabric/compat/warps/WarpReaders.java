package net.squaremarkers.fabric.compat.warps;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Reads the live warp collections. Invoked only when the matching optional mod is installed. */
final class WarpReaders {
    private WarpReaders() {
    }

    static Map<String, WarpPoint> fabricEssentials() throws ReflectiveOperationException {
        Class<?> storageClass = Class.forName("me.drex.essentials.storage.DataStorage");
        Object serverData = storageClass.getMethod("serverData").invoke(null);
        Map<?, ?> warps = (Map<?, ?>) serverData.getClass().getMethod("getWarps").invoke(serverData);
        Map<String, WarpPoint> result = new HashMap<>();
        for (Map.Entry<?, ?> entry : warps.entrySet()) {
            Object location = entry.getValue().getClass().getMethod("location").invoke(entry.getValue());
            Vec3 pos = (Vec3) location.getClass().getMethod("pos").invoke(location);
            Identifier dimension = (Identifier) location.getClass().getMethod("dimension").invoke(location);
            add(result, entry.getKey(), dimension.toString(), pos.x, pos.y, pos.z);
        }
        return result;
    }

    static Map<String, WarpPoint> essentialCommands() throws ReflectiveOperationException {
        Class<?> locatorClass = Class.forName("com.fibermc.essentialcommands.ManagerLocator");
        Object locator = locatorClass.getMethod("getInstance").invoke(null);
        Object manager = locatorClass.getMethod("getWorldDataManager").invoke(locator);
        if (manager == null) {
            return null; // Essential Commands has not loaded its world data yet.
        }
        Set<?> entries = (Set<?>) manager.getClass().getMethod("getWarpEntries").invoke(manager);
        Map<String, WarpPoint> result = new HashMap<>();
        for (Object object : entries) {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) object;
            Object location = entry.getValue();
            Class<?> type = location.getClass();
            Method x = type.getMethod("x");
            Method y = type.getMethod("y");
            Method z = type.getMethod("z");
            ResourceKey<?> dimension = (ResourceKey<?>) type.getMethod("dim").invoke(location);
            add(result, entry.getKey(), dimension.identifier().toString(),
                ((Number) x.invoke(location)).doubleValue(),
                ((Number) y.invoke(location)).doubleValue(),
                ((Number) z.invoke(location)).doubleValue());
        }
        return result;
    }

    private static void add(Map<String, WarpPoint> result, Object name, String dimension,
                            double x, double y, double z) {
        if (name instanceof String warpName && !warpName.isBlank()
            && Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)
            && Math.abs(x) <= 30_000_000 && Math.abs(z) <= 30_000_000) {
            result.put(warpName, new WarpPoint(warpName, dimension, x, y, z));
        }
    }
}
