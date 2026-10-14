package net.squaremarkers.fabric.compat.worldborder;

/** Same coordinates used by World Border; not the inset teleport landing location. */
public record BorderBounds(String dimension, int minX, int minZ, int maxX, int maxZ) {
    public BorderBounds {
        if (minX >= maxX || minZ >= maxZ || Math.abs((long) minX) > 30_000_000
            || Math.abs((long) minZ) > 30_000_000 || Math.abs((long) maxX) > 30_000_000
            || Math.abs((long) maxZ) > 30_000_000) {
            throw new IllegalArgumentException("Invalid World Border bounds for " + dimension);
        }
    }
}
