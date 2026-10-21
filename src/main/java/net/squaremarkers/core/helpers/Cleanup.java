package net.squaremarkers.core.helpers;

import net.squaremarkers.core.SquareMarkersCore;

/** One failed integration must not prevent unrelated resources from being released. */
public final class Cleanup {
    private Cleanup() {}

    public static void run(String description, Runnable action) {
        try { action.run(); }
        catch (RuntimeException | LinkageError failure) {
            SquareMarkersCore.warn("Failed to " + description, failure);
        }
    }
}
