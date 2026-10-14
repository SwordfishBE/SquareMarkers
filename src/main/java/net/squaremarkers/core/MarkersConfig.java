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
import java.time.DateTimeException;
import java.time.ZoneId;

public final class MarkersConfig {
    private static final com.google.gson.Gson GSON = new com.google.gson.Gson();
    private static final String DEFAULT_CONFIG = """
        # SquareMarkers configuration
        settings:
          feedback:
            # Show in-game feedback messages for marker interactions and area notifications.
            messages: true
            # Play sounds for marker interactions.
            sound: true
            # Show area feedback when entering an area.
            # Options: None, ActionBar, Title, Subtitle.
            area-on-enter: ActionBar
            # Show area feedback when leaving an area.
            # Options: None, ActionBar, Title, Subtitle.
            area-on-leave: ActionBar
            # Persistent area name: None, ActionBar, BossBar.
            area-presence: None
        marker-settings:
          players:
            # Show Nether players at their scaled positions on the Overworld map.
            nether-to-overworld: true
            # Show Overworld players at their scaled positions on the Nether map.
            overworld-to-nether: true
          areas:
            # Show player-created areas defined by named banners on lodestones.
            enabled: true
            # Layer order in the map menu and overlays; higher values are placed above lower values.
            priority: 50
            # Display area details on mouse hover (Hover) or in a click popup (Click).
            label-mode: Hover
            # Include the area size in the map label or popup.
            show-area-size: false
            # Area size limit in blocks; distant points are excluded when building the boundary.
            size: 512
          nether-portals:
            # Show Nether portal markers in Overworld-like and Nether-like dimensions.
            enabled: true
            # Right-click a portal block with a named name tag to rename it.
            rename: true
            # Unnamed portals remain stored and become visible when renamed.
            only-show-named: false
            # Layer order in the map menu and overlays; higher values are placed above lower values.
            priority: 50
          beacons:
            # Show active beacon markers; inactive or removed beacons are hidden.
            enabled: true
            # Layer order in the map menu and overlays; higher values are placed above lower values.
            priority: 50
          end-portals:
            # End portal markers in overworld-like dimensions; not the End exit portal.
            enabled: true
            # Right-click an active portal block or its frame with a named name tag to rename it.
            rename: true
            # Unnamed portals remain stored; renaming reveals them unless manually hidden.
            only-show-named: false
            # Layer order in the map menu and overlays; higher values are placed above lower values.
            priority: 50
          end-gateways:
            # End gateway markers in End-like dimensions; map links appear once the destination is known.
            enabled: true
            # Right-click the gateway block with a named name tag to rename it.
            rename: true
            # Unnamed gateways remain stored; renaming reveals them unless manually hidden.
            only-show-named: false
            # Layer order in the map menu and overlays; higher values are placed above lower values.
            priority: 50
          signs:
            # Show sign markers from standing signs placed on lodestones.
            enabled: true
            # Layer order in the map menu and overlays; higher values are placed above lower values.
            priority: 50
            # Preserve empty sign lines; false removes blank lines from the displayed text.
            fill-lines: false
          lightning:
            # Show temporary lightning-strike markers in Overworld-like dimensions.
            enabled: true
            # Layer order in the map menu and overlays; higher values are placed above lower values.
            priority: 50
            # Marker lifetime in seconds.
            lifetime: 3
          deaths:
            # Death locations are public on squaremap. Opt in explicitly.
            enabled: false
            # Layer order in the map menu and overlays; higher values are placed above lower values.
            priority: 50
            # Marker lifetime in seconds.
            lifetime: 1800
            # UTC, Europe/Brussels, Europe/London, or a fixed offset such as UTC+01:00.
            timezone: UTC
          open-parties-and-claims:
            # Show claims and subclaims when Open Parties and Claims is installed.
            enabled: true
            # Layer order in the map menu and overlays; higher values are placed above lower values.
            priority: 40
            # Display claim names on mouse hover (Hover) or in a click popup (Click).
            label-mode: Hover
            # Check in-memory claim names/colors every this many seconds.
            metadata-refresh-interval: 30
          fabric-essentials-warps:
            # Show warp points when Fabric Essentials is installed; absent/disabled support does not poll.
            enabled: true
            # Layer order in the map menu and overlays; higher values are placed above lower values.
            priority: 50
          essential-commands-warps:
            # Show warp points when Essential Commands is installed; absent/disabled support does not poll.
            enabled: true
            # Layer order in the map menu and overlays; higher values are placed above lower values.
            priority: 50
          huskhomes-warps:
            # Show this server's HuskHomes warps; cross-server warps are excluded.
            enabled: true
            # Layer order in the map menu and overlays; higher values are placed above lower values.
            priority: 50
          waystones:
            # Show Waystones markers when the Waystones mod is installed.
            enabled: true
            # Layer order in the map menu and overlays; higher values are placed above lower values.
            priority: 50
            # Include sharestones as well as regular waystones.
            include-sharestones: true
            # Include unnamed waystones; locations become visible on the public map.
            include-undiscovered: false
          world-border:
            # Optional Serilum World Border mod; displays its enabled vanilla-dimension boundaries.
            enabled: true
            # Layer order in the map menu and overlays; higher values are placed above lower values.
            priority: 50
            # Check loaded config values in memory, in seconds; no chunk or file scans. Range: 5-3600.
            refresh-interval: 30
        """ + FeedbackMessages.defaultsYaml();

    private static Map<String, String> values = Map.of();

    public static boolean FEEDBACK_MESSAGES_ENABLED = true;
    public static boolean FEEDBACK_SOUNDS_ENABLED = true;
    public static AreaFeedbackConfig.Transition AREA_ON_ENTER = AreaFeedbackConfig.Transition.ACTIONBAR;
    public static AreaFeedbackConfig.Transition AREA_ON_LEAVE = AreaFeedbackConfig.Transition.ACTIONBAR;
    public static AreaFeedbackConfig.Presence AREA_PRESENCE = AreaFeedbackConfig.Presence.NONE;
    public static boolean PLAYERS_NETHER_IN_OVERWORLD = true;
    public static boolean PLAYERS_OVERWORLD_IN_NETHER = true;
    public static boolean AREA_MARKERS_ENABLED = true;
    public static int AREA_MARKERS_PRIORITY = 50;
    public enum LabelMode { HOVER, CLICK }
    public static LabelMode AREA_MARKERS_LABEL_MODE = LabelMode.HOVER;
    public static boolean AREA_MARKERS_SHOW_SIZE = false;
    public static int AREA_MARKERS_MAX_SIZE = 512;
    public static boolean NETHER_PORTAL_MARKERS_ENABLED = true;
    public static boolean NETHER_PORTAL_MARKERS_RENAME = true;
    public static boolean NETHER_PORTAL_MARKERS_ONLY_SHOW_NAMED = false;
    public static int NETHER_PORTAL_MARKERS_PRIORITY = 50;
    public static boolean BEACON_MARKERS_ENABLED = true;
    public static int BEACON_MARKERS_PRIORITY = 50;
    public static boolean END_PORTAL_MARKERS_ENABLED = true;
    public static boolean END_PORTAL_MARKERS_RENAME = true;
    public static boolean END_PORTAL_MARKERS_ONLY_SHOW_NAMED = false;
    public static int END_PORTAL_MARKERS_PRIORITY = 50;
    public static boolean END_GATEWAY_MARKERS_ENABLED = true;
    public static boolean END_GATEWAY_MARKERS_RENAME = true;
    public static boolean END_GATEWAY_MARKERS_ONLY_SHOW_NAMED = false;
    public static int END_GATEWAY_MARKERS_PRIORITY = 50;
    public static boolean SIGN_MARKERS_ENABLED = true;
    public static int SIGN_MARKERS_PRIORITY = 50;
    public static boolean SIGN_MARKERS_FILL_LINES = false;
    public static boolean LIGHTNING_MARKERS_ENABLED = true;
    public static int LIGHTNING_MARKERS_PRIORITY = 50;
    public static int LIGHTNING_MARKERS_LIFETIME = 3;
    public static boolean DEATH_MARKERS_ENABLED = false;
    public static int DEATH_MARKERS_PRIORITY = 50;
    public static int DEATH_MARKERS_LIFETIME = 1800;
    public static ZoneId DEATH_MARKERS_TIMEZONE = ZoneId.of("UTC");

    private MarkersConfig() {
    }

    static String defaultConfig() { return DEFAULT_CONFIG; }

    public static synchronized void reload() {
        Path path = SquareMarkersCore.getMainDir().resolve("config.yml");
        try {
            Files.createDirectories(path.getParent());
            if (Files.notExists(path)) {
                Files.writeString(path, DEFAULT_CONFIG, StandardCharsets.UTF_8);
            }
            String config = Files.readString(path, StandardCharsets.UTF_8);
            String migrated = ConfigMigration.migrate(config); // Validates original input before upgrading.
            Map<String, String> parsed = parse(migrated.lines().toList());
            if (!migrated.equals(config)) {
                writeMigratedConfig(path, migrated);
            }
            values = parsed;
            FeedbackMessages.reload(parsed);
            FEEDBACK_MESSAGES_ENABLED = getBoolean("settings.feedback.messages", true);
            FEEDBACK_SOUNDS_ENABLED = getBoolean("settings.feedback.sound", true);
            AREA_ON_ENTER = AreaFeedbackConfig.parse(values.get("settings.feedback.area-on-enter"),
                AreaFeedbackConfig.Transition.class, AreaFeedbackConfig.Transition.ACTIONBAR, "settings.feedback.area-on-enter");
            AREA_ON_LEAVE = AreaFeedbackConfig.parse(values.get("settings.feedback.area-on-leave"),
                AreaFeedbackConfig.Transition.class, AreaFeedbackConfig.Transition.ACTIONBAR, "settings.feedback.area-on-leave");
            AREA_PRESENCE = AreaFeedbackConfig.parse(values.get("settings.feedback.area-presence"),
                AreaFeedbackConfig.Presence.class, AreaFeedbackConfig.Presence.NONE, "settings.feedback.area-presence");
            PLAYERS_NETHER_IN_OVERWORLD = getBoolean("marker-settings.players.nether-to-overworld", true);
            PLAYERS_OVERWORLD_IN_NETHER = getBoolean("marker-settings.players.overworld-to-nether", true);
            AREA_MARKERS_ENABLED = getBoolean("marker-settings.areas.enabled", true);
            AREA_MARKERS_PRIORITY = getInt("marker-settings.areas.priority", 50);
            AREA_MARKERS_LABEL_MODE = getLabelMode("marker-settings.areas.label-mode");
            AREA_MARKERS_SHOW_SIZE = getBoolean("marker-settings.areas.show-area-size", false);
            AREA_MARKERS_MAX_SIZE = getInt("marker-settings.areas.size", 512, 1, 30_000_000);
            NETHER_PORTAL_MARKERS_ENABLED = getBoolean("marker-settings.nether-portals.enabled", true);
            NETHER_PORTAL_MARKERS_RENAME = getBoolean("marker-settings.nether-portals.rename", true);
            NETHER_PORTAL_MARKERS_ONLY_SHOW_NAMED = getBoolean("marker-settings.nether-portals.only-show-named", false);
            NETHER_PORTAL_MARKERS_PRIORITY = getInt("marker-settings.nether-portals.priority", 50);
            BEACON_MARKERS_ENABLED = getBoolean("marker-settings.beacons.enabled", true);
            BEACON_MARKERS_PRIORITY = getInt("marker-settings.beacons.priority", 50);
            END_PORTAL_MARKERS_ENABLED = getBoolean("marker-settings.end-portals.enabled", true);
            END_PORTAL_MARKERS_RENAME = getBoolean("marker-settings.end-portals.rename", true);
            END_PORTAL_MARKERS_ONLY_SHOW_NAMED = getBoolean("marker-settings.end-portals.only-show-named", false);
            END_PORTAL_MARKERS_PRIORITY = getInt("marker-settings.end-portals.priority", 50);
            END_GATEWAY_MARKERS_ENABLED = getBoolean("marker-settings.end-gateways.enabled", true);
            END_GATEWAY_MARKERS_RENAME = getBoolean("marker-settings.end-gateways.rename", true);
            END_GATEWAY_MARKERS_ONLY_SHOW_NAMED = getBoolean("marker-settings.end-gateways.only-show-named", false);
            END_GATEWAY_MARKERS_PRIORITY = getInt("marker-settings.end-gateways.priority", 50);
            SIGN_MARKERS_ENABLED = getBoolean("marker-settings.signs.enabled", true);
            SIGN_MARKERS_PRIORITY = getInt("marker-settings.signs.priority", 50);
            SIGN_MARKERS_FILL_LINES = getBoolean("marker-settings.signs.fill-lines", false);
            LIGHTNING_MARKERS_ENABLED = getBoolean("marker-settings.lightning.enabled", true);
            LIGHTNING_MARKERS_PRIORITY = getInt("marker-settings.lightning.priority", 50);
            LIGHTNING_MARKERS_LIFETIME = getInt("marker-settings.lightning.lifetime", 3, 0, 86_400);
            DEATH_MARKERS_ENABLED = getBoolean("marker-settings.deaths.enabled", false);
            DEATH_MARKERS_PRIORITY = getInt("marker-settings.deaths.priority", 50);
            DEATH_MARKERS_LIFETIME = getInt("marker-settings.deaths.lifetime", 1800, 1, 604_800);
            DEATH_MARKERS_TIMEZONE = parseDeathTimezone(values.getOrDefault("marker-settings.deaths.timezone", "UTC"));
        } catch (IOException | IllegalArgumentException exception) {
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

    public static LabelMode getLabelMode(String key) {
        return parseLabelMode(values.getOrDefault(key, "Hover"), key);
    }

    static LabelMode parseLabelMode(String value, String key) {
        for (LabelMode mode : LabelMode.values()) if (mode.name().equalsIgnoreCase(value.trim())) return mode;
        SquareMarkersCore.warn("Invalid label mode for '" + key + "': " + value + "; using Hover (options: Hover, Click)");
        return LabelMode.HOVER;
    }

    static ZoneId parseDeathTimezone(String value) {
        try {
            return ZoneId.of(value);
        } catch (DateTimeException exception) {
            SquareMarkersCore.warn("Invalid timezone for 'marker-settings.deaths.timezone': " + value + "; using UTC");
            return ZoneId.of("UTC");
        }
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

    static Map<String, String> parse(List<String> lines) {
        Map<String, String> parsed = new HashMap<>();
        record Section(int indent, String name) {}
        List<Section> sections = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (String rawLine : lines) {
            String withoutComment = stripComment(rawLine);
            if (withoutComment.isBlank()) {
                continue;
            }
            int spaces = 0;
            while (spaces < withoutComment.length() && withoutComment.charAt(spaces) == ' ') {
                spaces++;
            }
            if (withoutComment.charAt(spaces) == '\t') {
                throw new IllegalArgumentException("Use spaces for configuration indentation");
            }
            String line = withoutComment.trim();
            int separator = line.indexOf(':');
            if (separator <= 0) {
                throw new IllegalArgumentException("Invalid configuration line: " + line);
            }
            String key = line.substring(0, separator).trim();
            String value = line.substring(separator + 1).trim();
            while (!sections.isEmpty() && sections.getLast().indent() >= spaces) {
                sections.removeLast();
            }
            List<String> path = new ArrayList<>(sections.stream().map(Section::name).toList());
            path.add(key);
            String fullKey = String.join(".", path);
            if (!seen.add(fullKey)) {
                throw new IllegalArgumentException("Duplicate configuration key: " + fullKey);
            }
            if (value.isEmpty()) {
                sections.add(new Section(spaces, key));
                continue;
            }
            parsed.put(fullKey, unquote(value));
        }
        return parsed;
    }

    private static String stripComment(String line) {
        int separator = line.indexOf(':');
        int comment = line.indexOf('#');
        if (separator < 0 || (comment >= 0 && comment < separator)) {
            return comment < 0 ? line : line.substring(0, comment);
        }
        int start = separator + 1;
        while (start < line.length() && Character.isWhitespace(line.charAt(start))) start++;
        if (start == line.length()) return line;
        char quote = line.charAt(start);
        if (quote != '\'' && quote != '"') {
            return comment < 0 ? line : line.substring(0, comment);
        }
        boolean escaped = false;
        for (int index = start + 1; index < line.length(); index++) {
            char character = line.charAt(index);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (character == '\\' && quote == '"') {
                escaped = true;
                continue;
            }
            if (character == quote) {
                if (quote == '\'' && index + 1 < line.length() && line.charAt(index + 1) == '\'') {
                    index++;
                    continue;
                }
                String remainder = line.substring(index + 1).stripLeading();
                if (!remainder.isEmpty() && !remainder.startsWith("#")) {
                    throw new IllegalArgumentException("Unexpected text after quoted configuration value: " + line.trim());
                }
                return line.substring(0, index + 1);
            }
        }
        throw new IllegalArgumentException("Unterminated quoted configuration value: " + line.trim());
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\""))
            || (value.startsWith("'") && value.endsWith("'")))) {
            if (value.startsWith("'")) return value.substring(1, value.length() - 1).replace("''", "'");
            try {
                return GSON.fromJson(value, String.class);
            } catch (com.google.gson.JsonParseException exception) {
                throw new IllegalArgumentException("Invalid quoted configuration value: " + value, exception);
            }
        }
        return value;
    }
}
