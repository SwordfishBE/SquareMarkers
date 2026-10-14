package net.squaremarkers.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LabelModeTest {
    @org.junit.jupiter.api.io.TempDir java.nio.file.Path directory;

    @Test void areaLabelsUseOnlyTheSelectedTooltipAndRetainTheirDetails() {
        var previous = MarkersConfig.AREA_MARKERS_LABEL_MODE;
        try {
            SquareMarkersCore.onInitialize(new net.squaremarkers.core.json.JsonStorage(directory.toString()), null, () -> {});
            var world = (xyz.jpenilla.squaremap.api.MapWorld) java.lang.reflect.Proxy.newProxyInstance(
                xyz.jpenilla.squaremap.api.MapWorld.class.getClassLoader(),
                new Class<?>[]{xyz.jpenilla.squaremap.api.MapWorld.class}, (proxy, method, args) -> {
                    if (method.getName().equals("identifier")) return xyz.jpenilla.squaremap.api.WorldIdentifier.parse("minecraft:overworld");
                    throw new UnsupportedOperationException(method.getName());
                });
            for (var mode : MarkersConfig.LabelMode.values()) {
                MarkersConfig.AREA_MARKERS_LABEL_MODE = mode;
                var layer = new net.squaremarkers.core.layers.primitive.AreaMarkerLayer(world);
                layer.addPoint("Farm", 0x00ff00, 0, 64, 0);
                layer.load();
                var options = layer.provider().getMarkers().iterator().next().markerOptions();
                if (mode == MarkersConfig.LabelMode.HOVER) {
                    assertTrue(options.hoverTooltip().contains("Farm"));
                    assertNull(options.clickTooltip());
                } else {
                    assertTrue(options.clickTooltip().contains("Farm"));
                    assertNull(options.hoverTooltip());
                }
                layer.close();
            }
        } finally { MarkersConfig.AREA_MARKERS_LABEL_MODE = previous; }
    }

    @Test void legacyModesMigrateAndRedundantSettingsDisappearWithoutLosingNotes() {
        for (String newline : new String[]{"\n", "\r\n", "\r"}) {
            String input = "marker-settings:" + newline
                + "    areas:" + newline + "        always-show-name: false # my choice" + newline
                + "    open-parties-and-claims:" + newline + "        always-show-name: true" + newline
                + "    nether-portals:" + newline + "        always-show-name: false" + newline
                + "    signs:" + newline + "        always-show-text: true" + newline;
            String migrated = ConfigMigration.migrate(input);
            var values = MarkersConfig.parse(migrated.lines().toList());
            assertEquals("Click", values.get("marker-settings.areas.label-mode"));
            assertEquals("Hover", values.get("marker-settings.open-parties-and-claims.label-mode"));
            assertFalse(values.keySet().stream().anyMatch(key -> key.endsWith("always-show-name") || key.endsWith("always-show-text")));
            assertTrue(migrated.contains("# my choice"));
            assertEquals(migrated, ConfigMigration.migrate(migrated));
        }
    }

    @Test void modernChoicesOverrideOldFlagsAndMissingModesDefaultToHover() {
        String input = "marker-settings:\n  areas:\n    label-mode: Click\n    always-show-name: true\n"
            + "  open-parties-and-claims:\n    label-mode: Hover\n    always-show-name: false\n";
        var values = MarkersConfig.parse(ConfigMigration.migrate(input).lines().toList());
        assertEquals("Click", values.get("marker-settings.areas.label-mode"));
        assertEquals("Hover", values.get("marker-settings.open-parties-and-claims.label-mode"));
        var defaults = MarkersConfig.parse(ConfigMigration.migrate("").lines().toList());
        assertEquals("Hover", defaults.get("marker-settings.areas.label-mode"));
        assertEquals("Hover", defaults.get("marker-settings.open-parties-and-claims.label-mode"));
        assertFalse(MarkersConfig.defaultConfig().contains("always-show-"));
    }

    @Test void parsingAcceptsBothModesAndRejectsInvalidModesSafely() {
        assertEquals(MarkersConfig.LabelMode.CLICK, MarkersConfig.parseLabelMode(" click ", "test"));
        assertEquals(MarkersConfig.LabelMode.HOVER, MarkersConfig.parseLabelMode("hover", "test"));
        assertEquals(MarkersConfig.LabelMode.HOVER, MarkersConfig.parseLabelMode("Always", "test"));
    }
}
