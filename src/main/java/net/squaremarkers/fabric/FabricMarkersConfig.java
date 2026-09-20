package net.squaremarkers.fabric;

import net.squaremarkers.core.MarkersConfig;

public final class FabricMarkersConfig {
    public static boolean OPAC_MARKERS_ENABLED = true;
    public static int OPAC_MARKERS_PRIORITY = 40;
    public static boolean OPAC_MARKERS_ALWAYS_SHOW_NAME = true;

    private FabricMarkersConfig() {
    }

    public static void reload() {
        MarkersConfig.reload();
        OPAC_MARKERS_ENABLED = MarkersConfig.getBoolean("marker-settings.open-parties-and-claims.enabled", true);
        OPAC_MARKERS_PRIORITY = MarkersConfig.getInt("marker-settings.open-parties-and-claims.priority", 40);
        OPAC_MARKERS_ALWAYS_SHOW_NAME = MarkersConfig.getBoolean("marker-settings.open-parties-and-claims.always-show-name", true);
    }
}
