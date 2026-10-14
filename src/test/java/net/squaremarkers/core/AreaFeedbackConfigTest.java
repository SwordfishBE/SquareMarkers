package net.squaremarkers.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AreaFeedbackConfigTest {
    @Test void legacyOptOutAndCustomSettingsSurviveMigration() {
        String input = "# Keep\r\nsettings:\r\n  feedback:\r\n    area-enter: false # disabled\r\n"
            + "    area-on-enter: Subtitle\r\nmarker-settings:\r\n  nether-portals:\r\n    rename: false\r\n";
        String result = ConfigMigration.addAreaOptions(input);
        var values = MarkersConfig.parse(result.lines().toList());
        assertEquals("Subtitle", values.get("settings.feedback.area-on-enter"));
        assertEquals("None", values.get("settings.feedback.area-on-leave"));
        assertEquals("None", values.get("settings.feedback.area-presence"));
        assertEquals("false", values.get("marker-settings.nether-portals.only-show-named"));
        assertEquals("false", values.get("marker-settings.nether-portals.rename"));
        assertTrue(result.contains("false # disabled\r\n"));
        assertTrue(result.contains("# Options: None, ActionBar, Title, Subtitle.\r\n    area-on-enter: Subtitle"));
        assertTrue(result.contains("# Options: None, ActionBar, Title, Subtitle.\r\n    area-on-leave: None"));
        assertFalse(result.replace("\r\n", "").contains("\n"));
        assertEquals(result, ConfigMigration.addAreaOptions(result));
    }

    @Test void defaultMigrationKeepsTransitionsAndPreservesExplicitOptIns() {
        String result = ConfigMigration.addAreaOptions("settings:\n  feedback:\n    area-presence: BossBar\n"
            + "marker-settings:\n  nether-portals:\n    only-show-named: true\n");
        var values = MarkersConfig.parse(result.lines().toList());
        assertEquals("ActionBar", values.get("settings.feedback.area-on-enter"));
        assertEquals("ActionBar", values.get("settings.feedback.area-on-leave"));
        assertEquals("BossBar", values.get("settings.feedback.area-presence"));
        assertEquals("true", values.get("marker-settings.nether-portals.only-show-named"));
    }

    @Test void modesAreCaseInsensitiveAndInvalidOrMissingModesFallBack() {
        assertEquals(AreaFeedbackConfig.Transition.SUBTITLE, AreaFeedbackConfig.parse(" subtitle ",
            AreaFeedbackConfig.Transition.class, AreaFeedbackConfig.Transition.ACTIONBAR, "test"));
        assertEquals(AreaFeedbackConfig.Presence.BOSSBAR, AreaFeedbackConfig.parse("BossBar",
            AreaFeedbackConfig.Presence.class, AreaFeedbackConfig.Presence.NONE, "test"));
        for (String invalid : new String[]{null, "", "unknown"}) {
            assertEquals(AreaFeedbackConfig.Presence.NONE, AreaFeedbackConfig.parse(invalid,
                AreaFeedbackConfig.Presence.class, AreaFeedbackConfig.Presence.NONE, "test"));
        }
    }
}
