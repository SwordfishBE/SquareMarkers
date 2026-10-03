package net.squaremarkers.core;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Plain-text player feedback. Inserted names are never interpreted as templates. */
public enum FeedbackMessages {
    MARKER_ADD("marker", "add", "Added {type} marker"),
    MARKER_INTERACT("marker", "interact", "{name}"),
    MARKER_RENAME("marker", "rename", "Renamed {type} marker to '{name}'"),
    MARKER_RENAME_FAILED("marker", "rename-failed", "Could not rename {type} marker"),
    MARKER_COLOR("marker", "color", "Colored {type} marker"),
    MARKER_COLOR_FAILED("marker", "color-failed", "Could not color {type} marker"),
    MARKER_REMOVE("marker", "remove", "Removed {type} marker"),
    AREA_CREATE("area", "create", "Created area: {label}"),
    AREA_POINT_ADD("area", "point-add", "Added point to area: {label}"),
    AREA_POINT_ADD_FAILED("area", "point-add-failed", "Could not add point to area: {label}"),
    AREA_REMOVE("area", "remove", "Removed area: {label}"),
    AREA_POINT_REMOVE("area", "point-remove", "Removed point from area: {label}"),
    AREA_ENTER("area", "enter", "[+] {name}"),
    AREA_LEAVE("area", "leave", "[-] {name}"),
    SIGN_INVALID_TEXT("sign", "invalid-text", "Text should be a String array with a size of 4"),
    SIGN_ADD("sign", "add", "Added sign marker"),
    SIGN_EDIT("sign", "edit", "Edited sign marker"),
    SIGN_REMOVE("sign", "remove", "Removed sign marker");

    private static final Pattern PLACEHOLDERS = Pattern.compile("\\{(type|name|label)}");
    private static Map<FeedbackMessages, String> templates = Map.of();
    final String group, key, defaultText;

    FeedbackMessages(String group, String key, String defaultText) {
        this.group = group; this.key = key; this.defaultText = defaultText;
    }

    static void reload(Map<String, String> config) {
        var updated = new EnumMap<FeedbackMessages, String>(FeedbackMessages.class);
        for (var message : values()) updated.put(message,
            config.getOrDefault("messages." + message.group + "." + message.key, message.defaultText));
        templates = Map.copyOf(updated);
    }

    public String text(String... replacements) {
        if (replacements.length % 2 != 0) throw new IllegalArgumentException("Expected placeholder/value pairs");
        String template = templates.getOrDefault(this, defaultText);
        if (replacements.length == 0 || template.indexOf('{') < 0) return template;
        return PLACEHOLDERS.matcher(template).replaceAll(match -> {
            // There are only a few pairs; avoid allocating a map for each message.
            for (int i = replacements.length - 2; i >= 0; i -= 2) {
                if (match.group(1).equals(replacements[i])) {
                    return java.util.regex.Matcher.quoteReplacement(String.valueOf(replacements[i + 1]));
                }
            }
            return java.util.regex.Matcher.quoteReplacement(match.group());
        });
    }

    static String defaultsYaml() {
        String config = "# Plain-text player feedback. Supported placeholders: {type}, {name}, {label}.\nmessages:\n";
        return addMissingOptions(config);
    }

    static String addMissingOptions(String config) {
        String newline = config.contains("\r\n") ? "\r\n" : config.contains("\r") && !config.contains("\n") ? "\r" : "\n";
        Map<String, List<WarpConfigMigration.Option>> groups = new LinkedHashMap<>();
        for (var message : values()) groups.computeIfAbsent(message.group, unused -> new java.util.ArrayList<>())
            .add(new WarpConfigMigration.Option(message.key, "\"" + message.defaultText + "\""));
        for (var group : groups.entrySet()) config = WarpConfigMigration.addSection(config, "messages", group.getKey(), newline, group.getValue());
        return config;
    }
}
