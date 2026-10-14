package net.squaremarkers.core;

import java.util.Locale;

/** Independently configured area transitions and presence, preserving legacy opt-outs. */
public final class AreaFeedbackConfig {
    public enum Transition { NONE, ACTIONBAR, TITLE, SUBTITLE }
    public enum Presence { NONE, ACTIONBAR, BOSSBAR }
    private AreaFeedbackConfig() {}

    static <E extends Enum<E>> E parse(String value, Class<E> type, E fallback, String key) {
        if (value == null) return fallback;
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            SquareMarkersCore.warn("Invalid mode for '" + key + "': " + value + "; using " + fallback);
            return fallback;
        }
    }
}
