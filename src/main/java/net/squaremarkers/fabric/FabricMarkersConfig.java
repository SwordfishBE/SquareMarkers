package net.squaremarkers.fabric;

import net.squaremarkers.core.MarkersConfig;

public final class FabricMarkersConfig {
    public static boolean OPAC_MARKERS_ENABLED = true;
    public static int OPAC_MARKERS_PRIORITY = 40;
    public static int OPAC_METADATA_REFRESH_INTERVAL = 30;
    public static MarkersConfig.LabelMode OPAC_MARKERS_LABEL_MODE = MarkersConfig.LabelMode.HOVER;
    public static boolean FABRIC_ESSENTIALS_WARPS_ENABLED = true;
    public static int FABRIC_ESSENTIALS_WARPS_PRIORITY = 50;
    public static boolean ESSENTIAL_COMMANDS_WARPS_ENABLED = true;
    public static int ESSENTIAL_COMMANDS_WARPS_PRIORITY = 50;
    public static boolean HUSKHOMES_WARPS_ENABLED = true;
    public static int HUSKHOMES_WARPS_PRIORITY = 50;
    public static boolean WAYSTONES_ENABLED = true;
    public static int WAYSTONES_PRIORITY = 50;
    public static boolean WAYSTONES_INCLUDE_SHARESTONES = true;
    public static boolean WAYSTONES_INCLUDE_UNDISCOVERED = false;
    public static boolean WORLD_BORDER_ENABLED = true;
    public static int WORLD_BORDER_PRIORITY = 50;
    public static int WORLD_BORDER_REFRESH_INTERVAL = 30;

    private FabricMarkersConfig() {
    }

    public static void reload() {
        net.squaremarkers.fabric.helpers.AreaFeedbackHandler.clear();
        MarkersConfig.reload();
        OPAC_MARKERS_ENABLED = MarkersConfig.getBoolean("marker-settings.open-parties-and-claims.enabled", true);
        OPAC_MARKERS_PRIORITY = MarkersConfig.getInt("marker-settings.open-parties-and-claims.priority", 40);
        OPAC_METADATA_REFRESH_INTERVAL = MarkersConfig.getInt("marker-settings.open-parties-and-claims.metadata-refresh-interval", 30, 5, 3600);
        OPAC_MARKERS_LABEL_MODE = MarkersConfig.getLabelMode("marker-settings.open-parties-and-claims.label-mode");
        FABRIC_ESSENTIALS_WARPS_ENABLED = MarkersConfig.getBoolean("marker-settings.fabric-essentials-warps.enabled", true);
        FABRIC_ESSENTIALS_WARPS_PRIORITY = MarkersConfig.getInt("marker-settings.fabric-essentials-warps.priority", 50);
        ESSENTIAL_COMMANDS_WARPS_ENABLED = MarkersConfig.getBoolean("marker-settings.essential-commands-warps.enabled", true);
        ESSENTIAL_COMMANDS_WARPS_PRIORITY = MarkersConfig.getInt("marker-settings.essential-commands-warps.priority", 50);
        HUSKHOMES_WARPS_ENABLED = MarkersConfig.getBoolean("marker-settings.huskhomes-warps.enabled", true);
        HUSKHOMES_WARPS_PRIORITY = MarkersConfig.getInt("marker-settings.huskhomes-warps.priority", 50);
        WAYSTONES_ENABLED = MarkersConfig.getBoolean("marker-settings.waystones.enabled", true);
        WAYSTONES_PRIORITY = MarkersConfig.getInt("marker-settings.waystones.priority", 50);
        WAYSTONES_INCLUDE_SHARESTONES = MarkersConfig.getBoolean("marker-settings.waystones.include-sharestones", true);
        WAYSTONES_INCLUDE_UNDISCOVERED = MarkersConfig.getBoolean("marker-settings.waystones.include-undiscovered", false);
        WORLD_BORDER_ENABLED = MarkersConfig.getBoolean("marker-settings.world-border.enabled", true);
        WORLD_BORDER_PRIORITY = MarkersConfig.getInt("marker-settings.world-border.priority", 50);
        WORLD_BORDER_REFRESH_INTERVAL = MarkersConfig.getInt("marker-settings.world-border.refresh-interval", 30, 5, 3600);
    }
}
