package net.squaremarkers.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarpConfigMigrationTest {
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
}
