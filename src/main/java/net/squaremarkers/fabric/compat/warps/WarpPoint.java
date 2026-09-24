package net.squaremarkers.fabric.compat.warps;

/** A warp snapshot detached from the owning mod's mutable storage. */
public record WarpPoint(String name, String dimension, double x, double y, double z) {
}
