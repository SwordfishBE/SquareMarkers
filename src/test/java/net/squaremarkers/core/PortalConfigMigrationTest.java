package net.squaremarkers.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PortalConfigMigrationTest {
    @Test void migrationPreservesSettingsAndAddsDocumentedDefaultsIdempotently() {
        String config = "# Custom\r\nmarker-settings:\r\n  end-portals:\r\n    enabled: false\r\n"
            + "    rename: false # keep\r\n    priority: 70\r\n  end-gateways:\r\n    only-show-named: true\r\n";
        String updated = ConfigMigration.addPortalOptions(config);
        var values = MarkersConfig.parse(updated.lines().toList());
        assertEquals("false", values.get("marker-settings.end-portals.enabled"));
        assertEquals("false", values.get("marker-settings.end-portals.rename"));
        assertEquals("70", values.get("marker-settings.end-portals.priority"));
        assertEquals("false", values.get("marker-settings.end-portals.only-show-named"));
        assertEquals("true", values.get("marker-settings.end-gateways.only-show-named"));
        assertEquals("true", values.get("marker-settings.end-gateways.rename"));
        assertTrue(updated.contains("rename: false # keep\r\n"));
        assertTrue(updated.contains("# Right-click an active portal block"));
        assertTrue(updated.contains("# Right-click the gateway block"));
        assertFalse(updated.replace("\r\n", "").contains("\n"));
        assertEquals(updated, ConfigMigration.addPortalOptions(updated));
    }
}
