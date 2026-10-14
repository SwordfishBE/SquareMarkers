package net.squaremarkers.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigMigrationTest {
    @Test void legacyAreaEnterIsRemovedOnlyAfterItsChoicesAreTransferred() {
        for (String newline : new String[]{"\n", "\r\n", "\r"}) {
            for (boolean legacy : new boolean[]{true, false}) {
                String input = "settings:" + newline + "    feedback:" + newline
                    + "        # My note" + newline
                    + "        # Legacy migration fallback; use area-on-enter/area-on-leave after migration." + newline
                    + "        area-enter: " + legacy + " # keep this note" + newline
                    + "other:" + newline + "    area-enter: false" + newline;
                String result = ConfigMigration.migrate(input);
                var values = MarkersConfig.parse(result.lines().toList());
                assertEquals(legacy ? "ActionBar" : "None", values.get("settings.feedback.area-on-enter"));
                assertEquals(legacy ? "ActionBar" : "None", values.get("settings.feedback.area-on-leave"));
                org.junit.jupiter.api.Assertions.assertFalse(values.containsKey("settings.feedback.area-enter"));
                org.junit.jupiter.api.Assertions.assertFalse(result.contains("Legacy migration fallback"));
                assertTrue(result.contains("# My note"));
                assertTrue(result.contains("# keep this note"));
                assertEquals("false", values.get("other.area-enter"));
                assertEquals(result, ConfigMigration.migrate(result));
            }
        }
    }

    @Test void legacyRemovalPreservesExplicitModernChoicesAndNewConfigsOmitTheOldKey() {
        String input = "settings:\n  feedback:\n    area-enter: false\n"
            + "    area-on-enter: Title\n    area-on-leave: Subtitle\n";
        var values = MarkersConfig.parse(ConfigMigration.migrate(input).lines().toList());
        assertEquals("Title", values.get("settings.feedback.area-on-enter"));
        assertEquals("Subtitle", values.get("settings.feedback.area-on-leave"));
        org.junit.jupiter.api.Assertions.assertFalse(values.containsKey("settings.feedback.area-enter"));
        org.junit.jupiter.api.Assertions.assertFalse(MarkersConfig.defaultConfig().contains("area-enter:"));
    }

    @Test void completeUpgradeValidatesInputAndPreservesChoicesAcrossAllFeatures() {
        String old = "# My config\r\nsettings:\r\n  feedback:\r\n    area-enter: false\r\n"
            + "marker-settings:\r\n  huskhomes-warps:\r\n    enabled: false\r\n"
            + "  end-portals:\r\n    only-show-named: true # keep\r\n"
            + "messages:\r\n  marker:\r\n    interact: \"Custom: {name}\"\r\n";
        String updated = ConfigMigration.migrate(old);
        var values = MarkersConfig.parse(updated.lines().toList());
        assertEquals("false", values.get("marker-settings.huskhomes-warps.enabled"));
        assertEquals("true", values.get("marker-settings.end-portals.only-show-named"));
        assertEquals("None", values.get("settings.feedback.area-on-enter"));
        assertEquals("None", values.get("settings.feedback.area-on-leave"));
        assertEquals("Custom: {name}", values.get("messages.marker.interact"));
        assertEquals("{name}", values.get("messages.area.presence"));
        assertTrue(updated.contains("only-show-named: true # keep\r\n"));
        org.junit.jupiter.api.Assertions.assertFalse(updated.replace("\r\n", "").contains("\n"));
        assertEquals(updated, ConfigMigration.migrate(updated));
        String invalid = "settings:\nsettings:\n";
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> ConfigMigration.migrate(invalid));
    }

    @Test void completeUpgradeIsIdempotentForEmptyConfigsAndDifferentLineEndings() {
        for (String newline : new String[]{"\n", "\r\n", "\r"}) {
            String updated = ConfigMigration.migrate("# Keep" + newline);
            assertEquals(updated, ConfigMigration.migrate(updated));
            assertEquals("true", MarkersConfig.parse(updated.lines().toList()).get("marker-settings.end-gateways.rename"));
        }
        String updated = ConfigMigration.migrate("");
        assertEquals(updated, ConfigMigration.migrate(updated));
    }
    @Test void deathTimezoneMigrationPreservesSelectedZoneAndDefaultsToUtc() {
        String existing = "marker-settings:\n  deaths:\n    timezone: Europe/Brussels # keep\n";
        String updated = ConfigMigration.addIntegrationOptions(existing);
        assertEquals("Europe/Brussels", MarkersConfig.parse(updated.lines().toList()).get("marker-settings.deaths.timezone"));
        assertTrue(updated.contains("timezone: Europe/Brussels # keep"));
        assertEquals(updated, ConfigMigration.addIntegrationOptions(updated));
        assertEquals("UTC", MarkersConfig.parse(ConfigMigration.addIntegrationOptions("").lines().toList())
            .get("marker-settings.deaths.timezone"));
    }
    @Test void timezoneParsingAcceptsStandardZonesAndSafelyRejectsAmbiguousAbbreviations() {
        assertEquals(java.time.ZoneId.of("UTC"), MarkersConfig.parseDeathTimezone("UTC"));
        assertEquals(java.time.ZoneId.of("Europe/Brussels"), MarkersConfig.parseDeathTimezone("Europe/Brussels"));
        assertEquals(java.time.ZoneId.of("UTC+01:00"), MarkersConfig.parseDeathTimezone("UTC+01:00"));
        assertEquals(java.time.ZoneId.of("UTC"), MarkersConfig.parseDeathTimezone("CEST+1"));
        assertEquals(java.time.ZoneId.of("UTC"), MarkersConfig.parseDeathTimezone("BST"));
    }
    @Test void deathLifetimeCommentPreservesValuesCommentsAndLineEndings() {
        String existing = "marker-settings:\r\n    deaths:\r\n        enabled: true\r\n"
            + "        # My custom duration\r\n        lifetime: 120 # preserve me\r\n";
        String updated = ConfigMigration.addIntegrationOptions(existing);
        assertTrue(updated.contains("        # My custom duration\r\n"
            + "        # Marker lifetime in seconds.\r\n        lifetime: 120 # preserve me\r\n"));
        assertEquals("120", MarkersConfig.parse(updated.lines().toList()).get("marker-settings.deaths.lifetime"));
        assertEquals(updated, ConfigMigration.addIntegrationOptions(updated));
        assertTrue(ConfigMigration.addIntegrationOptions("").contains("# Marker lifetime in seconds.\n    lifetime: 1800"));
    }
    @Test void deathOptionsDefaultToPrivateAndPreserveCustomValues() {
        String existing = "marker-settings:\n  deaths:\n    enabled: true\n    lifetime: 90 # keep\n";
        String updated = ConfigMigration.addIntegrationOptions(existing);
        var parsed = MarkersConfig.parse(updated.lines().toList());
        assertEquals("true", parsed.get("marker-settings.deaths.enabled"));
        assertEquals("90", parsed.get("marker-settings.deaths.lifetime"));
        assertEquals("50", parsed.get("marker-settings.deaths.priority"));
        assertEquals(updated, ConfigMigration.addIntegrationOptions(updated));
        parsed = MarkersConfig.parse(ConfigMigration.addIntegrationOptions("").lines().toList());
        assertEquals("false", parsed.get("marker-settings.deaths.enabled"));
        assertEquals("1800", parsed.get("marker-settings.deaths.lifetime"));
    }
    @Test
    void addsOpacMetadataIntervalWithoutChangingExistingChoices() {
        String existing = "marker-settings:\n    open-parties-and-claims:\n"
            + "        enabled: false\n        priority: 17\n";
        String updated = ConfigMigration.addIntegrationOptions(existing);
        var parsed = MarkersConfig.parse(updated.lines().toList());
        assertEquals("false", parsed.get("marker-settings.open-parties-and-claims.enabled"));
        assertEquals("17", parsed.get("marker-settings.open-parties-and-claims.priority"));
        assertEquals("30", parsed.get("marker-settings.open-parties-and-claims.metadata-refresh-interval"));
        assertEquals(updated, ConfigMigration.addIntegrationOptions(updated));
        String customized = updated.replace("metadata-refresh-interval: 30", "metadata-refresh-interval: 120");
        assertEquals(customized, ConfigMigration.addIntegrationOptions(customized));
    }
    @Test
    void preservesDisabledIntegrationsWithDifferentIndentation() {
        for (int width : new int[]{1, 2, 4, 6}) {
            String existing = "marker-settings:\r\n" + " ".repeat(width) + "huskhomes-warps:\r\n"
                + " ".repeat(width * 2) + "enabled: false # private\r\n"
                + " ".repeat(width * 2) + "priority: 12\r\n";
            String updated = ConfigMigration.addIntegrationOptions(existing);
            var parsed = MarkersConfig.parse(updated.lines().toList());
            assertEquals("false", parsed.get("marker-settings.huskhomes-warps.enabled"));
            assertEquals("12", parsed.get("marker-settings.huskhomes-warps.priority"));
            assertTrue(updated.startsWith(existing));
            assertEquals(updated, ConfigMigration.addIntegrationOptions(updated));
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

        String updated = ConfigMigration.addIntegrationOptions(existing);

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
        assertEquals(updated, ConfigMigration.addIntegrationOptions(updated));
    }

    @Test
    void fillsOnlyMissingKeysInPartiallyConfiguredSections() {
        String existing = "marker-settings:\n"
            + "  fabric-essentials-warps:\n    enabled: false\n"
            + "  essential-commands-warps:\n    priority: 72\n";

        String updated = ConfigMigration.addIntegrationOptions(existing);

        assertTrue(updated.contains("fabric-essentials-warps:\n    enabled: false\n    priority: 50\n"));
        assertTrue(updated.contains("essential-commands-warps:\n    priority: 72\n    enabled: true\n"));
        assertEquals(updated, ConfigMigration.addIntegrationOptions(updated));
    }

    @Test
    void addsParentWhenAbsent() {
        String existing = "settings:\n  feedback:\n    sound: false\n";

        String updated = ConfigMigration.addIntegrationOptions(existing);

        assertTrue(updated.startsWith(existing));
        assertTrue(updated.contains("marker-settings:\n  fabric-essentials-warps:"));
        assertEquals(updated, ConfigMigration.addIntegrationOptions(updated));
    }

    @Test
    void preservesPartiallyConfiguredWaystones() {
        String existing = "marker-settings:\n"
            + "  waystones:\n    enabled: false\n    include-undiscovered: true\n";

        String updated = ConfigMigration.addIntegrationOptions(existing);

        assertTrue(updated.contains("waystones:\n    enabled: false\n"
            + "    include-undiscovered: true\n"
            + "    priority: 50\n    include-sharestones: true\n"));
        assertEquals(updated, ConfigMigration.addIntegrationOptions(updated));
    }

    @Test
    void preservesExistingHuskHomesWarpChoices() {
        String existing = "marker-settings:\n"
            + "  huskhomes-warps:\n    enabled: false\n"
            + "other:\n  custom: yes\n";

        String updated = ConfigMigration.addIntegrationOptions(existing);

        assertTrue(updated.contains("huskhomes-warps:\n    enabled: false\n    priority: 50\n"));
        assertTrue(updated.endsWith("other:\n  custom: yes\n"));
        assertEquals(updated, ConfigMigration.addIntegrationOptions(updated));
    }
}
