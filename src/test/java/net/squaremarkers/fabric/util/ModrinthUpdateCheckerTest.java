package net.squaremarkers.fabric.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModrinthUpdateCheckerTest {
    @Test
    void selectsNewestListedCompatibleFabricRelease() {
        String response = """
            [
              {"version_number":"1.0.9","version_type":"release","status":"unlisted","loaders":["fabric"],"game_versions":["26.3"]},
              {"version_number":"1.0.8","version_type":"release","status":"listed","loaders":["fabric"],"game_versions":["26.3"]},
              {"version_number":"1.0.7","version_type":"beta","status":"listed","loaders":["fabric"],"game_versions":["26.3"]},
              {"version_number":"1.0.10","version_type":"release","status":"listed","loaders":["fabric"],"game_versions":["26.2"]},
              {"version_number":"2.0.0","version_type":"release","status":"listed","loaders":["forge"],"game_versions":["26.3"]}
            ]
            """;

        assertEquals("1.0.8", ModrinthUpdateChecker.findNewestCompatible(response, "1.0.6", "26.3").orElseThrow());
        assertEquals("1.0.10", ModrinthUpdateChecker.findNewestCompatible(response, "1.0.6", "26.2").orElseThrow());
        assertTrue(ModrinthUpdateChecker.findNewestCompatible(response, "1.0.8", "26.3").isEmpty());
    }

    @Test
    void ignoresUnexpectedResponseAndInvalidVersion() {
        assertTrue(ModrinthUpdateChecker.findNewestCompatible("{}", "1.0.6", "26.3").isEmpty());
        assertTrue(ModrinthUpdateChecker.findNewestCompatible("[]", "invalid version!", "26.3").isEmpty());
    }
}
