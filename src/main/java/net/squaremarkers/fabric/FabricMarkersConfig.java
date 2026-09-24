package net.squaremarkers.fabric;

import net.squaremarkers.core.MarkersConfig;

public final class FabricMarkersConfig {
    public static boolean OPAC_MARKERS_ENABLED = true;
    public static int OPAC_MARKERS_PRIORITY = 40;
    public static boolean OPAC_MARKERS_ALWAYS_SHOW_NAME = true;
    public static boolean FABRIC_ESSENTIALS_WARPS_ENABLED = true;
    public static int FABRIC_ESSENTIALS_WARPS_PRIORITY = 50;
    public static boolean ESSENTIAL_COMMANDS_WARPS_ENABLED = true;
    public static int ESSENTIAL_COMMANDS_WARPS_PRIORITY = 50;

    private FabricMarkersConfig() {
    }

    public static void reload() {
        MarkersConfig.reload();
        OPAC_MARKERS_ENABLED = MarkersConfig.getBoolean("marker-settings.open-parties-and-claims.enabled", true);
        OPAC_MARKERS_PRIORITY = MarkersConfig.getInt("marker-settings.open-parties-and-claims.priority", 40);
        OPAC_MARKERS_ALWAYS_SHOW_NAME = MarkersConfig.getBoolean("marker-settings.open-parties-and-claims.always-show-name", true);
        FABRIC_ESSENTIALS_WARPS_ENABLED = MarkersConfig.getBoolean("marker-settings.fabric-essentials-warps.enabled", true);
        FABRIC_ESSENTIALS_WARPS_PRIORITY = MarkersConfig.getInt("marker-settings.fabric-essentials-warps.priority", 50);
        ESSENTIAL_COMMANDS_WARPS_ENABLED = MarkersConfig.getBoolean("marker-settings.essential-commands-warps.enabled", true);
        ESSENTIAL_COMMANDS_WARPS_PRIORITY = MarkersConfig.getInt("marker-settings.essential-commands-warps.priority", 50);
    }
}
