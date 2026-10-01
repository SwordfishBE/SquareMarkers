package net.squaremarkers.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarpConfigMigrationTest {
    @Test
    void preservesDisabledIntegrationsWithDifferentIndentation() {
        for (int width : new int[]{1, 2, 4, 6}) {
            String existing = "marker-settings:\r\n" + " ".repeat(width) + "huskhomes-warps:\r\n"
                + " ".repeat(width * 2) + "enabled: false # private\r\n"
                + " ".repeat(width * 2) + "priority: 12\r\n";
            String updated = WarpConfigMigration.addMissingOptions(existing);
            var parsed = MarkersConfig.parse(updated.lines().toList());
            assertEquals("false", parsed.get("marker-settings.huskhomes-warps.enabled"));
            assertEquals("12", parsed.get("marker-settings.huskhomes-warps.priority"));
            assertTrue(updated.startsWith(existing));
            assertEquals(updated, WarpConfigMigration.addMissingOptions(updated));
        }
    }

    @Test
    void rejectsDuplicateSectionsInsteadOfSilentlyEnablingIntegration() {
        String existing = "marker-settings:\n  huskhomes-warps:\n    enabled: false\n"
            + "  huskhomes-warps:\n    enabled: true\n";
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
            () -> MarkersConfig.parse(existing.lines().toList()));
    }
    @Test
    void preservesExistingSettingsCommentsAndLineEndings() {
        String existing = "# My settings\r\nmarker-settings:\r\n"
            + "  areas:\r\n    enabled: false # keep this\r\n    priority: 17\r\n"
            + "other:\r\n  custom: yes\r\n";

        String updated = WarpConfigMigration.addMissingOptions(existing);

        assertTrue(updated.startsWith("# My settings\r\nmarker-settings:\r\n"
            + "  areas:\r\n    enabled: false # keep this\r\n    priority: 17\r\n"));
        assertTrue(updated.contains("  fabric-essentials-warps:\r\n"
            + "    enabled: true\r\n    priority: 50\r\n"));
        assertTrue(updated.contains("  essential-commands-warps:\r\n"
            + "    enabled: true\r\n    priority: 50\r\n"));
        assertTrue(updated.contains("  huskhomes-warps:\r\n"
            + "    enabled: true\r\n    priority: 50\r\n"));
        assertTrue(updated.contains("  waystones:\r\n"
            + "    enabled: true\r\n    priority: 50\r\n"
            + "    include-sharestones: true\r\n    include-undiscovered: false\r\n"));
        assertTrue(updated.endsWith("other:\r\n  custom: yes\r\n"));
        assertEquals(updated, WarpConfigMigration.addMissingOptions(updated));
    }

    @Test
    void fillsOnlyMissingKeysInPartiallyConfiguredSections() {
        String existing = "marker-settings:\n"
            + "  fabric-essentials-warps:\n    enabled: false\n"
            + "  essential-commands-warps:\n    priority: 72\n";

        String updated = WarpConfigMigration.addMissingOptions(existing);

        assertTrue(updated.contains("fabric-essentials-warps:\n    enabled: false\n    priority: 50\n"));
        assertTrue(updated.contains("essential-commands-warps:\n    priority: 72\n    enabled: true\n"));
        assertEquals(updated, WarpConfigMigration.addMissingOptions(updated));
    }

    @Test
    void addsParentWhenAbsent() {
        String existing = "settings:\n  feedback:\n    sound: false\n";

        String updated = WarpConfigMigration.addMissingOptions(existing);

        assertTrue(updated.startsWith(existing));
        assertTrue(updated.contains("marker-settings:\n  fabric-essentials-warps:"));
        assertEquals(updated, WarpConfigMigration.addMissingOptions(updated));
    }

    @Test
    void preservesPartiallyConfiguredWaystones() {
        String existing = "marker-settings:\n"
            + "  waystones:\n    enabled: false\n    include-undiscovered: true\n";

        String updated = WarpConfigMigration.addMissingOptions(existing);

        assertTrue(updated.contains("waystones:\n    enabled: false\n"
            + "    include-undiscovered: true\n"
            + "    priority: 50\n    include-sharestones: true\n"));
        assertEquals(updated, WarpConfigMigration.addMissingOptions(updated));
    }

    @Test
    void preservesExistingHuskHomesWarpChoices() {
        String existing = "marker-settings:\n"
            + "  huskhomes-warps:\n    enabled: false\n"
            + "other:\n  custom: yes\n";

        String updated = WarpConfigMigration.addMissingOptions(existing);

        assertTrue(updated.contains("huskhomes-warps:\n    enabled: false\n    priority: 50\n"));
        assertTrue(updated.endsWith("other:\n  custom: yes\n"));
        assertEquals(updated, WarpConfigMigration.addMissingOptions(updated));
    }
}
