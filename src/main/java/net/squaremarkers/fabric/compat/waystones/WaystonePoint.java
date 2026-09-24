package net.squaremarkers.fabric.compat.waystones;

import java.util.UUID;

/** Immutable map data detached from Waystones' mutable database entries. */
public record WaystonePoint(UUID id, String name, String dimension, int x, int y, int z,
                            boolean sharestone) {
}
