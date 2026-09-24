package net.squaremarkers.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MarkersConfig {
    private static final String DEFAULT_CONFIG = """
        # SquareMarkers configuration
        settings:
          feedback:
            messages: true
            sound: true
            area-enter: true
        marker-settings:
          players:
            nether-to-overworld: true
            overworld-to-nether: true
          areas:
            enabled: true
            priority: 50
            always-show-name: true
            show-area-size: false
            size: 512
          nether-portals:
            enabled: true
            rename: true
            always-show-name: true
            priority: 50
          beacons:
            enabled: true
            priority: 50
          end-portals:
            enabled: true
            priority: 50
          end-gateways:
            enabled: true
            priority: 50
          signs:
            enabled: true
            priority: 50
            always-show-text: true
            fill-lines: false
          lightning:
            enabled: true
            priority: 50
            # Marker lifetime in seconds.
            lifetime: 3
          open-parties-and-claims:
            enabled: true
            priority: 40
            always-show-name: true
          fabric-essentials-warps:
            enabled: true
            priority: 50
          essential-commands-warps:
            enabled: true
            priority: 50
          waystones:
            enabled: true
            priority: 50
            include-sharestones: true
            include-undiscovered: false
        """;

    private static Map<String, String> values = Map.of();

    public static boolean FEEDBACK_MESSAGES_ENABLED = true;
    public static boolean FEEDBACK_SOUNDS_ENABLED = true;
    public static boolean FEEDBACK_AREA_ENTER_ENABLED = true;
    public static boolean PLAYERS_NETHER_IN_OVERWORLD = true;
    public static boolean PLAYERS_OVERWORLD_IN_NETHER = true;
    public static boolean AREA_MARKERS_ENABLED = true;
    public static int AREA_MARKERS_PRIORITY = 50;
    public static boolean AREA_MARKERS_MARKERS_ALWAYS_SHOW_NAME = true;
    public static boolean AREA_MARKERS_SHOW_SIZE = false;
    public static int AREA_MARKERS_MAX_SIZE = 512;
    public static boolean NETHER_PORTAL_MARKERS_ENABLED = true;
    public static boolean NETHER_PORTAL_MARKERS_RENAME = true;
    public static boolean NETHER_PORTAL_MARKERS_ALWAYS_SHOW_NAME = true;
    public static int NETHER_PORTAL_MARKERS_PRIORITY = 50;
    public static boolean BEACON_MARKERS_ENABLED = true;
    public static int BEACON_MARKERS_PRIORITY = 50;
    public static boolean END_PORTAL_MARKERS_ENABLED = true;
    public static int END_PORTAL_MARKERS_PRIORITY = 50;
    public static boolean END_GATEWAY_MARKERS_ENABLED = true;
    public static int END_GATEWAY_MARKERS_PRIORITY = 50;
    public static boolean SIGN_MARKERS_ENABLED = true;
    public static int SIGN_MARKERS_PRIORITY = 50;
    public static boolean SIGN_MARKERS_ALWAYS_SHOW_TEXT = true;
    public static boolean SIGN_MARKERS_FILL_LINES = false;
    public static boolean LIGHTNING_MARKERS_ENABLED = true;
    public static int LIGHTNING_MARKERS_PRIORITY = 50;
    public static int LIGHTNING_MARKERS_LIFETIME = 3;

    private MarkersConfig() {
    }

    public static synchronized void reload() {
        Path path = SquareMarkersCore.getMainDir().resolve("config.yml");
        try {
            Files.createDirectories(path.getParent());
            if (Files.notExists(path)) {
                Files.writeString(path, DEFAULT_CONFIG, StandardCharsets.UTF_8);
            }
            String config = Files.readString(path, StandardCharsets.UTF_8);
            String migrated = WarpConfigMigration.addMissingOptions(config);
            if (!migrated.equals(config)) {
                writeMigratedConfig(path, migrated);
            }
            values = parse(migrated.lines().toList());
            FEEDBACK_MESSAGES_ENABLED = getBoolean("settings.feedback.messages", true);
            FEEDBACK_SOUNDS_ENABLED = getBoolean("settings.feedback.sound", true);
            FEEDBACK_AREA_ENTER_ENABLED = getBoolean("settings.feedback.area-enter", true);
            PLAYERS_NETHER_IN_OVERWORLD = getBoolean("marker-settings.players.nether-to-overworld", true);
            PLAYERS_OVERWORLD_IN_NETHER = getBoolean("marker-settings.players.overworld-to-nether", true);
            AREA_MARKERS_ENABLED = getBoolean("marker-settings.areas.enabled", true);
            AREA_MARKERS_PRIORITY = getInt("marker-settings.areas.priority", 50);
            AREA_MARKERS_MARKERS_ALWAYS_SHOW_NAME = getBoolean("marker-settings.areas.always-show-name", true);
            AREA_MARKERS_SHOW_SIZE = getBoolean("marker-settings.areas.show-area-size", false);
            AREA_MARKERS_MAX_SIZE = getInt("marker-settings.areas.size", 512, 1, 30_000_000);
            NETHER_PORTAL_MARKERS_ENABLED = getBoolean("marker-settings.nether-portals.enabled", true);
            NETHER_PORTAL_MARKERS_RENAME = getBoolean("marker-settings.nether-portals.rename", true);
            NETHER_PORTAL_MARKERS_ALWAYS_SHOW_NAME = getBoolean("marker-settings.nether-portals.always-show-name", true);
            NETHER_PORTAL_MARKERS_PRIORITY = getInt("marker-settings.nether-portals.priority", 50);
            BEACON_MARKERS_ENABLED = getBoolean("marker-settings.beacons.enabled", true);
            BEACON_MARKERS_PRIORITY = getInt("marker-settings.beacons.priority", 50);
            END_PORTAL_MARKERS_ENABLED = getBoolean("marker-settings.end-portals.enabled", true);
            END_PORTAL_MARKERS_PRIORITY = getInt("marker-settings.end-portals.priority", 50);
            END_GATEWAY_MARKERS_ENABLED = getBoolean("marker-settings.end-gateways.enabled", true);
            END_GATEWAY_MARKERS_PRIORITY = getInt("marker-settings.end-gateways.priority", 50);
            SIGN_MARKERS_ENABLED = getBoolean("marker-settings.signs.enabled", true);
            SIGN_MARKERS_PRIORITY = getInt("marker-settings.signs.priority", 50);
            SIGN_MARKERS_ALWAYS_SHOW_TEXT = getBoolean("marker-settings.signs.always-show-text", true);
            SIGN_MARKERS_FILL_LINES = getBoolean("marker-settings.signs.fill-lines", false);
            LIGHTNING_MARKERS_ENABLED = getBoolean("marker-settings.lightning.enabled", true);
            LIGHTNING_MARKERS_PRIORITY = getInt("marker-settings.lightning.priority", 50);
            LIGHTNING_MARKERS_LIFETIME = getInt("marker-settings.lightning.lifetime", 3, 0, 86_400);
        } catch (IOException exception) {
            SquareMarkersCore.warn("Failed to load config", exception);
        }
    }

    private static void writeMigratedConfig(Path path, String config) throws IOException {
        Path temporary = Files.createTempFile(path.getParent(), "config.yml.", ".tmp");
        try {
            Files.writeString(temporary, config, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public static boolean getBoolean(String key, boolean fallback) {
        String value = values.get(key);
        if (value == null) {
            return fallback;
        }
        if (value.equalsIgnoreCase("true")) {
            return true;
        }
        if (value.equalsIgnoreCase("false")) {
            return false;
        }
        SquareMarkersCore.warn("Invalid boolean for '" + key + "': " + value);
        return fallback;
    }

    public static int getInt(String key, int fallback) {
        try {
            return Integer.parseInt(values.getOrDefault(key, Integer.toString(fallback)));
        } catch (NumberFormatException exception) {
            SquareMarkersCore.warn("Invalid integer for '" + key + "': " + values.get(key), exception);
            return fallback;
        }
    }

    public static int getInt(String key, int fallback, int minimum, int maximum) {
        int value = getInt(key, fallback);
        if (value < minimum || value > maximum) {
            SquareMarkersCore.warn("Value for '" + key + "' must be between " + minimum + " and " + maximum
                + "; using " + Math.clamp(value, minimum, maximum));
            return Math.clamp(value, minimum, maximum);
        }
        return value;
    }

    private static Map<String, String> parse(List<String> lines) {
        Map<String, String> parsed = new HashMap<>();
        List<String> sections = new ArrayList<>();
        for (String rawLine : lines) {
            String withoutComment = stripComment(rawLine);
            if (withoutComment.isBlank()) {
                continue;
            }
            int spaces = 0;
            while (spaces < withoutComment.length() && withoutComment.charAt(spaces) == ' ') {
                spaces++;
            }
            int depth = spaces / 2;
            String line = withoutComment.trim();
            int separator = line.indexOf(':');
            if (separator < 0) {
                continue;
            }
            String key = line.substring(0, separator).trim();
            String value = line.substring(separator + 1).trim();
            while (sections.size() > depth) {
                sections.removeLast();
            }
            if (value.isEmpty()) {
                while (sections.size() < depth) {
                    sections.add("");
                }
                sections.add(key);
                continue;
            }
            List<String> path = new ArrayList<>(sections);
            path.add(key);
            parsed.put(String.join(".", path), unquote(value));
        }
        return parsed;
    }

    private static String stripComment(String line) {
        boolean singleQuoted = false;
        boolean doubleQuoted = false;
        boolean escaped = false;
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (character == '\\' && doubleQuoted) {
                escaped = true;
                continue;
            }
            if (character == '\'' && !doubleQuoted) {
                singleQuoted = !singleQuoted;
            } else if (character == '"' && !singleQuoted) {
                doubleQuoted = !doubleQuoted;
            } else if (character == '#' && !singleQuoted && !doubleQuoted) {
                return line.substring(0, index);
            }
        }
        return line;
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\""))
            || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
