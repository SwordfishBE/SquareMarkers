package net.squaremarkers.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorldBorderConfigTest {
    @Test void upgradePreservesOptOutIntervalPriorityAndComments() {
        String input = "# custom\r\nmarker-settings:\r\n  world-border:\r\n    enabled: false # keep\r\n"
            + "    priority: 80\r\n    refresh-interval: 60\r\n";
        String upgraded = ConfigMigration.migrate(input);
        var values = MarkersConfig.parse(upgraded.lines().toList());
        assertEquals("false", values.get("marker-settings.world-border.enabled"));
        assertEquals("80", values.get("marker-settings.world-border.priority"));
        assertEquals("60", values.get("marker-settings.world-border.refresh-interval"));
        assertTrue(upgraded.contains("enabled: false # keep\r\n"));
        assertTrue(upgraded.contains("# Check loaded config values in memory"));
        assertFalse(upgraded.replace("\r\n", "").contains("\n"));
        assertEquals(upgraded, ConfigMigration.migrate(upgraded));
    }

    @Test void missingSettingsReceiveDocumentedDefaults() {
        var values = MarkersConfig.parse(ConfigMigration.migrate("").lines().toList());
        assertEquals("true", values.get("marker-settings.world-border.enabled"));
        assertEquals("50", values.get("marker-settings.world-border.priority"));
        assertEquals("30", values.get("marker-settings.world-border.refresh-interval"));
    }
}
