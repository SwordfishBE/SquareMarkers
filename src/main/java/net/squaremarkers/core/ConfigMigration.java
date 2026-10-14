package net.squaremarkers.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/** Single config upgrade entry point. Adds missing options/comments while preserving custom content. */
final class ConfigMigration {
    private ConfigMigration() {
    }

    static String migrate(String config) {
        Map<String, String> original = MarkersConfig.parse(config.lines().toList());
        String updated = addIntegrationOptions(config);
        updated = addAreaOptions(updated, original);
        updated = removeOption(updated, "settings", "feedback", "area-enter", List.of(
            "# Legacy migration fallback; use area-on-enter/area-on-leave after migration."));
        updated = migrateLabelModes(updated, original);
        updated = addPortalOptions(updated);
        updated = addMessageOptions(updated);
        return addDocumentedComments(updated);
    }

    /** Reuse the default config as the documentation source; only existing options get comments. */
    private static String addDocumentedComments(String config) {
        String newline = newline(config);
        String root = null, section = null;
        List<String> comments = new ArrayList<>();
        for (Line line : lines(MarkersConfig.defaultConfig())) {
            String text = line.text();
            if (text.startsWith("#")) { comments.add(text); continue; }
            if (line.indent() == 0) {
                root = text.endsWith(":") ? text.substring(0, text.length() - 1) : null;
                section = null;
            } else if (line.indent() == 2 && text.endsWith(":")) {
                section = text.substring(0, text.length() - 1);
            } else if (line.indent() == 4 && section != null && text.contains(":")
                && ("settings".equals(root) || "marker-settings".equals(root))) {
                String key = text.substring(0, text.indexOf(':'));
                for (String comment : comments) config = addOptionComment(config, root, section, key, comment, newline);
            }
            comments.clear();
        }
        return config;
    }

    private static String newline(String config) {
        return config.contains("\r\n") ? "\r\n" : config.contains("\n") ? "\n" : config.contains("\r") ? "\r" : "\n";
    }

    private static String migrateLabelModes(String config, Map<String, String> original) {
        var oldLabelComments = List.of(
            "# Use hover labels instead of click popups; squaremap does not support permanent labels here.",
            "# Legacy label preference; portal names appear on hover in both modes with squaremap.",
            "# Legacy label preference; sign text appears on hover in both modes with squaremap.");
        for (String section : List.of("areas", "open-parties-and-claims")) {
            String mode = "false".equalsIgnoreCase(original.get("marker-settings." + section + ".always-show-name"))
                ? "Click" : "Hover";
            config = addSection(config, section, newline(config), List.of(new Option("label-mode", mode)));
            config = removeOption(config, "marker-settings", section, "always-show-name", oldLabelComments);
        }
        config = removeOption(config, "marker-settings", "nether-portals", "always-show-name", oldLabelComments);
        return removeOption(config, "marker-settings", "signs", "always-show-text", oldLabelComments);
    }

    /** Removes one obsolete option and only its known generated comments; keeps user notes. */
    private static String removeOption(String config, String root, String section, String key, List<String> generatedComments) {
        List<Line> lines = lines(config);
        int parent = findHeader(lines, 0, lines.size(), 0, root);
        if (parent < 0) return config;
        int parentEnd = nextSection(lines, parent + 1, 0);
        int childIndent = contentIndent(lines, parent + 1, parentEnd, 2);
        int child = findHeader(lines, parent + 1, parentEnd, childIndent, section);
        if (child < 0) return config;
        int childEnd = nextSection(lines, child + 1, childIndent);
        int optionIndent = contentIndent(lines, child + 1, childEnd, childIndent * 2);
        for (int index = child + 1; index < childEnd; index++) {
            Line option = lines.get(index);
            if (option.indent() != optionIndent || !option.text().startsWith(key + ":")) continue;
            var removed = new java.util.HashSet<Integer>();
            removed.add(index);
            for (int previous = index - 1; previous > child && lines.get(previous).text().startsWith("#"); previous--) {
                if (generatedComments.contains(lines.get(previous).text())) {
                    removed.add(previous);
                }
            }
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < lines.size(); i++) {
                int end = i + 1 < lines.size() ? lines.get(i + 1).start() : config.length();
                if (!removed.contains(i)) result.append(config, lines.get(i).start(), end);
                else if (i == index) {
                    // Keep a user's inline note as a standalone comment after removing its old key.
                    int comment = option.text().indexOf('#');
                    if (comment >= 0) result.append(" ".repeat(optionIndent)).append(option.text().substring(comment))
                        .append(newline(config));
                }
            }
            return result.toString();
        }
        return config;
    }

    static String addAreaOptions(String config) {
        return addAreaOptions(config, MarkersConfig.parse(config.lines().toList()));
    }

    private static String addAreaOptions(String config, Map<String, String> original) {
        String transition = "false".equalsIgnoreCase(original.get("settings.feedback.area-enter")) ? "None" : "ActionBar";
        String newline = newline(config);
        String updated = addSection(config, "settings", "feedback", newline, List.of(
            new Option("area-on-enter", transition), new Option("area-on-leave", transition),
            new Option("area-presence", "None")));
        for (String key : List.of("area-on-enter", "area-on-leave")) {
            updated = addOptionComment(updated, "settings", "feedback", key,
                "# Options: None, ActionBar, Title, Subtitle.", newline);
        }
        return addSection(updated, "marker-settings", "nether-portals", newline, List.of(new Option("only-show-named", "false")));
    }

    static String addPortalOptions(String config) {
        String newline = newline(config);
        for (String section : List.of("end-portals", "end-gateways")) {
            boolean gateway = section.equals("end-gateways");
            config = addSection(config, "marker-settings", section, newline, List.of(
                new Option("enabled", "true"), new Option("rename", "true"),
                new Option("only-show-named", "false"), new Option("priority", "50")));
            config = addOptionComment(config, "marker-settings", section, "enabled", gateway
                ? "# End gateway markers in End-like dimensions; map links appear once the destination is known."
                : "# End portal markers in overworld-like dimensions; not the End exit portal.", newline);
            config = addOptionComment(config, "marker-settings", section, "rename", gateway
                ? "# Right-click the gateway block with a named name tag to rename it."
                : "# Right-click an active portal block or its frame with a named name tag to rename it.", newline);
            config = addOptionComment(config, "marker-settings", section, "only-show-named", gateway
                ? "# Unnamed gateways remain stored; renaming reveals them unless manually hidden."
                : "# Unnamed portals remain stored; renaming reveals them unless manually hidden.", newline);
        }
        return config;
    }

    static String addMessageOptions(String config) {
        String newline = newline(config);
        Map<String, List<Option>> groups = new LinkedHashMap<>();
        for (var message : FeedbackMessages.values()) groups.computeIfAbsent(message.group, unused -> new ArrayList<>())
            .add(new Option(message.key, "\"" + message.defaultText + "\""));
        for (var group : groups.entrySet()) config = addSection(config, "messages", group.getKey(), newline, group.getValue());
        return config;
    }

    static String addIntegrationOptions(String config) {
        String newline = newline(config);
        String updated = addSection(config, "fabric-essentials-warps", newline, List.of(
            new Option("enabled", "true"), new Option("priority", "50")));
        updated = addSection(updated, "essential-commands-warps", newline, List.of(
            new Option("enabled", "true"), new Option("priority", "50")));
        updated = addSection(updated, "huskhomes-warps", newline, List.of(
            new Option("enabled", "true"), new Option("priority", "50")));
        updated = addSection(updated, "waystones", newline, List.of(
            new Option("enabled", "true"), new Option("priority", "50"),
            new Option("include-sharestones", "true"), new Option("include-undiscovered", "false")));
        updated = addSection(updated, "deaths", newline, List.of(
            new Option("enabled", "false"), new Option("priority", "50"), new Option("lifetime", "1800"),
            new Option("timezone", "UTC")));
        updated = addSection(updated, "world-border", newline, List.of(
            new Option("enabled", "true"), new Option("priority", "50"), new Option("refresh-interval", "30")));
        updated = addOptionComment(updated, "world-border", "enabled",
            "# Optional Serilum World Border mod; displays its enabled vanilla-dimension boundaries.", newline);
        updated = addOptionComment(updated, "world-border", "refresh-interval",
            "# Check loaded config values in memory, in seconds; no chunk or file scans. Range: 5-3600.", newline);
        updated = addSection(updated, "open-parties-and-claims", newline, List.of(
            new Option("metadata-refresh-interval", "30")));
        updated = addOptionComment(updated, "deaths", "lifetime", "# Marker lifetime in seconds.", newline);
        return addOptionComment(updated, "deaths", "timezone",
            "# UTC, Europe/Brussels, Europe/London, or a fixed offset such as UTC+01:00.", newline);
    }

    private static String addOptionComment(String config, String section, String key, String comment, String newline) {
        return addOptionComment(config, "marker-settings", section, key, comment, newline);
    }

    private static String addOptionComment(String config, String root, String section, String key, String comment, String newline) {
        List<Line> lines = lines(config);
        int parent = findHeader(lines, 0, lines.size(), 0, root);
        if (parent < 0) return config;
        int parentEnd = nextSection(lines, parent + 1, 0);
        int childIndent = contentIndent(lines, parent + 1, parentEnd, 2);
        int child = findHeader(lines, parent + 1, parentEnd, childIndent, section);
        if (child < 0) return config;
        int childEnd = nextSection(lines, child + 1, childIndent);
        int optionIndent = contentIndent(lines, child + 1, childEnd, childIndent * 2);
        for (int index = child + 1; index < childEnd; index++) {
            Line line = lines.get(index);
            if (line.indent() != optionIndent || !line.text().startsWith(key + ":")) continue;
            // A preceding comment block may contain both custom and generated explanations.
            for (int previous = index - 1; previous > child && lines.get(previous).text().startsWith("#"); previous--) {
                if (lines.get(previous).text().equals(comment)) return config;
            }
            return insert(config, line.start(), " ".repeat(optionIndent) + comment + newline, newline);
        }
        return config;
    }

    private static String addSection(String config, String section, String newline, List<Option> options) {
        return addSection(config, "marker-settings", section, newline, options);
    }

    private static String addSection(String config, String root, String section, String newline, List<Option> options) {
        List<Line> lines = lines(config);
        int parent = findHeader(lines, 0, lines.size(), 0, root);
        if (parent < 0) {
            String block = root + ":" + newline + sectionBlock(section, options, newline, 2, 4);
            return insert(config, config.length(), block, newline);
        }

        int parentEnd = nextSection(lines, parent + 1, 0);
        int childIndent = contentIndent(lines, parent + 1, parentEnd, 2);
        int child = findHeader(lines, parent + 1, parentEnd, childIndent, section);
        if (child < 0) {
            int offset = parentEnd == lines.size() ? config.length() : lines.get(parentEnd).start();
            return insert(config, offset, sectionBlock(section, options, newline, childIndent, childIndent * 2), newline);
        }

        int childEnd = nextSection(lines, child + 1, childIndent);
        int optionIndent = contentIndent(lines, child + 1, childEnd, childIndent * 2);
        StringBuilder missing = new StringBuilder();
        for (Option option : options) {
            if (!findKey(lines, child + 1, childEnd, optionIndent, option.key())) {
                missing.append(" ".repeat(optionIndent)).append(option.key()).append(": ")
                    .append(option.defaultValue()).append(newline);
            }
        }
        if (missing.isEmpty()) {
            return config;
        }
        int offset = childEnd == lines.size() ? config.length() : lines.get(childEnd).start();
        return insert(config, offset, missing.toString(), newline);
    }

    private static int contentIndent(List<Line> lines, int from, int to, int fallback) {
        return lines.subList(from, to).stream()
            .filter(line -> !line.text().isBlank() && !line.text().startsWith("#"))
            .mapToInt(Line::indent).min().orElse(fallback);
    }

    private static String sectionBlock(String section, List<Option> options, String newline, int childIndent, int optionIndent) {
        StringBuilder block = new StringBuilder(" ".repeat(childIndent)).append(section).append(":").append(newline);
        for (Option option : options) {
            block.append(" ".repeat(optionIndent)).append(option.key()).append(": ")
                .append(option.defaultValue()).append(newline);
        }
        return block.toString();
    }

    private static String insert(String config, int offset, String addition, String newline) {
        boolean needsLineBreak = offset > 0 && config.charAt(offset - 1) != '\n'
            && config.charAt(offset - 1) != '\r';
        return config.substring(0, offset) + (needsLineBreak ? newline : "")
            + addition + config.substring(offset);
    }

    private static int findHeader(List<Line> lines, int from, int to, int indent, String key) {
        for (int index = from; index < to; index++) {
            Line line = lines.get(index);
            if (line.indent() == indent && isHeader(line.text(), key)) {
                return index;
            }
        }
        return -1;
    }

    private static boolean findKey(List<Line> lines, int from, int to, int indent, String key) {
        for (int index = from; index < to; index++) {
            Line line = lines.get(index);
            if (line.indent() == indent && line.text().startsWith(key + ":")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isHeader(String text, String key) {
        if (!text.startsWith(key + ":")) {
            return false;
        }
        String rest = text.substring(key.length() + 1).trim();
        return rest.isEmpty() || rest.startsWith("#");
    }

    private static int nextSection(List<Line> lines, int from, int maxIndent) {
        for (int index = from; index < lines.size(); index++) {
            Line line = lines.get(index);
            if (!line.text().isBlank() && !line.text().startsWith("#")
                && line.indent() <= maxIndent) {
                return index;
            }
        }
        return lines.size();
    }

    private static List<Line> lines(String config) {
        List<Line> result = new ArrayList<>();
        int start = 0;
        while (start < config.length()) {
            int end = start;
            while (end < config.length() && config.charAt(end) != '\n' && config.charAt(end) != '\r') {
                end++;
            }
            String raw = config.substring(start, end);
            int indent = 0;
            while (indent < raw.length() && raw.charAt(indent) == ' ') {
                indent++;
            }
            result.add(new Line(start, indent, raw.substring(indent).trim()));
            start = end;
            if (start < config.length() && config.charAt(start) == '\r') {
                start++;
            }
            if (start < config.length() && config.charAt(start) == '\n') {
                start++;
            }
        }
        return result;
    }

    private record Line(int start, int indent, String text) {
    }

    record Option(String key, String defaultValue) {
    }
}
