package net.squaremarkers.core;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Independently configured area transitions and presence, preserving legacy opt-outs. */
public final class AreaFeedbackConfig {
    public enum Transition { NONE, ACTIONBAR, TITLE, SUBTITLE }
    public enum Presence { NONE, ACTIONBAR, BOSSBAR }
    private AreaFeedbackConfig() {}

    static String migrate(String config) {
        Map<String, String> values = MarkersConfig.parse(config.lines().toList());
        String transition = "false".equalsIgnoreCase(values.get("settings.feedback.area-enter")) ? "None" : "ActionBar";
        String newline = config.contains("\r\n") ? "\r\n" : config.contains("\r") ? "\r" : "\n";
        String updated = WarpConfigMigration.addSection(config, "settings", "feedback", newline, List.of(
            new WarpConfigMigration.Option("area-on-enter", transition),
            new WarpConfigMigration.Option("area-on-leave", transition),
            new WarpConfigMigration.Option("area-presence", "None")));
        for (String key : List.of("area-on-enter", "area-on-leave")) {
            updated = WarpConfigMigration.addOptionComment(updated, "settings", "feedback", key,
                "# Options: None, ActionBar, Title, Subtitle.", newline);
        }
        return WarpConfigMigration.addSection(updated, "marker-settings", "nether-portals", newline,
            List.of(new WarpConfigMigration.Option("only-show-named", "false")));
    }

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
