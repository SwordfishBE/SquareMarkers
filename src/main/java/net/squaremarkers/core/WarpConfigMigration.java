package net.squaremarkers.core;

import java.util.ArrayList;
import java.util.List;

/** Adds new warp settings without reformatting or replacing existing configuration. */
final class WarpConfigMigration {
    private WarpConfigMigration() {
    }

    static String addMissingOptions(String config) {
        String newline = config.contains("\r\n") ? "\r\n" : config.contains("\n") ? "\n"
            : config.contains("\r") ? "\r" : "\n";
        String updated = addSection(config, "fabric-essentials-warps", newline);
        return addSection(updated, "essential-commands-warps", newline);
    }

    private static String addSection(String config, String section, String newline) {
        List<Line> lines = lines(config);
        int parent = findHeader(lines, 0, lines.size(), 0, "marker-settings");
        if (parent < 0) {
            String block = "marker-settings:" + newline + sectionBlock(section, newline);
            return insert(config, config.length(), block, newline);
        }

        int parentEnd = nextSection(lines, parent + 1, 0);
        int child = findHeader(lines, parent + 1, parentEnd, 2, section);
        if (child < 0) {
            int offset = parentEnd == lines.size() ? config.length() : lines.get(parentEnd).start();
            return insert(config, offset, sectionBlock(section, newline), newline);
        }

        int childEnd = nextSection(lines, child + 1, 2);
        boolean enabled = findKey(lines, child + 1, childEnd, 4, "enabled");
        boolean priority = findKey(lines, child + 1, childEnd, 4, "priority");
        if (enabled && priority) {
            return config;
        }
        String missing = (enabled ? "" : "    enabled: true" + newline)
            + (priority ? "" : "    priority: 50" + newline);
        int offset = childEnd == lines.size() ? config.length() : lines.get(childEnd).start();
        return insert(config, offset, missing, newline);
    }

    private static String sectionBlock(String section, String newline) {
        return "  " + section + ":" + newline
            + "    enabled: true" + newline
            + "    priority: 50" + newline;
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
}
