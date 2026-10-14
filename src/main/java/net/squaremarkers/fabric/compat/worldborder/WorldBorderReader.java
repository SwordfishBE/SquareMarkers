package net.squaremarkers.fabric.compat.worldborder;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Reads public in-memory config fields; no third-party classes are linked at startup. */
final class WorldBorderReader {
    private record Fields(String dimension, Field enabled, Field minX, Field minZ, Field maxX, Field maxZ) {}
    private final List<Fields> fields;

    @FunctionalInterface interface ClassResolver { Class<?> find(String name) throws ClassNotFoundException; }

    static WorldBorderReader resolve() throws ReflectiveOperationException {
        return resolve(Class::forName);
    }

    static WorldBorderReader resolve(ClassResolver resolver) throws ReflectiveOperationException {
        var missing = new ClassNotFoundException("World Border config class was not found");
        // Distributed multi-loader JARs relocate common classes; development builds may not.
        for (String name : List.of("com.serilum.worldborder_common_fabric.config.ConfigHandler",
            "com.natamus.worldborder_common_fabric.config.ConfigHandler",
            "com.serilum.worldborder.config.ConfigHandler", "com.natamus.worldborder.config.ConfigHandler")) {
            Class<?> config;
            try { config = resolver.find(name); }
            catch (ClassNotFoundException exception) { missing.addSuppressed(exception); continue; }
            return new WorldBorderReader(config);
        }
        throw missing;
    }

    WorldBorderReader(Class<?> config) throws ReflectiveOperationException {
        fields = List.of(fields(config, "Overworld", "overworld", "minecraft:overworld"),
            fields(config, "Nether", "nether", "minecraft:the_nether"),
            fields(config, "End", "end", "minecraft:the_end"));
    }

    private static Fields fields(Class<?> config, String title, String prefix, String dimension) throws ReflectiveOperationException {
        return new Fields(dimension, field(config, "enableCustom" + title + "Border", boolean.class),
            field(config, prefix + "BorderNegativeX", int.class), field(config, prefix + "BorderNegativeZ", int.class),
            field(config, prefix + "BorderPositiveX", int.class), field(config, prefix + "BorderPositiveZ", int.class));
    }

    private static Field field(Class<?> config, String name, Class<?> type) throws ReflectiveOperationException {
        Field field = config.getField(name);
        if (field.getType() != type || !Modifier.isStatic(field.getModifiers())) throw new NoSuchFieldException(name);
        return field;
    }

    Map<String, BorderBounds> read() throws IllegalAccessException {
        Map<String, BorderBounds> result = new HashMap<>(3);
        for (Fields source : fields) {
            if (source.enabled.getBoolean(null)) result.put(source.dimension, new BorderBounds(source.dimension,
                source.minX.getInt(null), source.minZ.getInt(null), source.maxX.getInt(null), source.maxZ.getInt(null)));
        }
        return Map.copyOf(result);
    }
}
