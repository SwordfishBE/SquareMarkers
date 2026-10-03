package net.squaremarkers.core;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.Map;
import net.squaremarkers.core.json.JsonStorage;
import net.squaremarkers.core.layers.SignsMarkerLayer;
import net.squaremarkers.core.layers.NetherPortalMarkerLayer;
import net.squaremarkers.core.layers.primitive.AreaMarkerLayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import static org.junit.jupiter.api.Assertions.*;

class FeedbackMessagesTest {
    @TempDir Path directory;
    @AfterEach void resetMessages() { FeedbackMessages.reload(Map.of()); }

    @Test void migrationAddsAllDefaultsAndIsIdempotent() {
        String input = "settings:\n  feedback:\n    messages: false # keep\n";
        String updated = FeedbackMessages.addMissingOptions(input);
        var parsed = MarkersConfig.parse(updated.lines().toList());
        assertEquals("false", parsed.get("settings.feedback.messages"));
        assertEquals(24, parsed.keySet().stream().filter(key -> key.startsWith("messages.")).count());
        for (var message : FeedbackMessages.values()) assertEquals(message.defaultText,
            parsed.get("messages." + message.group + "." + message.key));
        assertTrue(updated.startsWith(input));
        assertEquals(updated, FeedbackMessages.addMissingOptions(updated));
        FeedbackMessages.reload(parsed);
        assertEquals("Renamed Portal marker to 'Home'", FeedbackMessages.MARKER_RENAME.text("type", "Portal", "name", "Home"));
    }

    @Test void partialMigrationPreservesCustomValuesCommentsAndLineEndings() {
        for (int indent : new int[]{1, 2, 4}) {
            String input = "# Custom\r\nmessages:\r\n" + " ".repeat(indent) + "area:\r\n"
                + " ".repeat(indent * 2) + "enter: \"Welcome: {name} #1\" # keep\r\n"
                + " ".repeat(indent * 2) + "leave: \"\"\r\nother:\r\n  enabled: false\r\n";
            String updated = FeedbackMessages.addMissingOptions(input);
            var parsed = MarkersConfig.parse(updated.lines().toList());
            assertEquals("Welcome: {name} #1", parsed.get("messages.area.enter"));
            assertEquals("", parsed.get("messages.area.leave"));
            assertEquals("false", parsed.get("other.enabled"));
            assertTrue(updated.contains("# keep\r\n"));
            assertFalse(updated.replace("\r\n", "").contains("\n"));
            assertEquals(updated, FeedbackMessages.addMissingOptions(updated));
        }
    }

    @Test void placeholdersAreExpandedOnceAndPreserveUnknownTokensAndLiteralCharacters() {
        FeedbackMessages.reload(Map.of("messages.marker.rename", "{type}: {name}, {name}, {unknown}"));
        String name = "Home {type} $1 \\ path";
        assertEquals("Portal: " + name + ", " + name + ", {unknown}",
            FeedbackMessages.MARKER_RENAME.text("type", "Portal", "name", name));
    }

    @Test void quotedTextSupportsApostrophesUnicodeEscapesAndEmptyTemplates() {
        var parsed = MarkersConfig.parse(java.util.List.of("messages:", "  area:",
            "    enter: 'Welcome to John''s {name} #2'", "    leave: \"\"",
            "  sign:", "    add: \"Sign: \\" + "\"home\\" + "\" — café\""));
        FeedbackMessages.reload(parsed);
        assertEquals("Welcome to John's Spawn #2", FeedbackMessages.AREA_ENTER.text("name", "Spawn"));
        assertEquals("", FeedbackMessages.AREA_LEAVE.text("name", "Spawn"));
        assertDoesNotThrow(() -> net.squaremarkers.fabric.helpers.FeedbackHelper.sendOverlayMessage(null, "", 0));
        assertEquals("Sign: \"home\" — café", FeedbackMessages.SIGN_ADD.text());
        assertThrows(IllegalArgumentException.class, () -> MarkersConfig.parse(java.util.List.of("text: \"bad\\q\"")));
        FeedbackMessages.reload(Map.of());
        assertEquals("[-] Spawn", FeedbackMessages.AREA_LEAVE.text("name", "Spawn"));
    }

    @Test void fixedEmptyAndUnresolvedMessagesPreserveTheirText() {
        FeedbackMessages.reload(Map.of("messages.marker.rename", "Fixed message", "messages.area.enter", ""));
        assertEquals("Fixed message", FeedbackMessages.MARKER_RENAME.text("name", "Home"));
        assertEquals("", FeedbackMessages.AREA_ENTER.text("name", "Home"));
        assertThrows(IllegalArgumentException.class, () -> FeedbackMessages.AREA_ENTER.text("name"));
        FeedbackMessages.reload(Map.of("messages.marker.rename", "{name}: {type} {unknown}"));
        assertEquals("{name}: {type} {unknown}", FeedbackMessages.MARKER_RENAME.text());
        assertEquals("Last: {type} {unknown}", FeedbackMessages.MARKER_RENAME.text("name", "First", "name", "Last"));
    }

    @Test void malformedQuotesAreRejectedWithoutRejectingPlainApostrophes() {
        for (String value : new String[]{"\"unfinished", "'unfinished", "\"", "'", "'John''s", "\"closed\" trailing", "'closed' trailing", "\"escaped\\\""}) {
            assertThrows(IllegalArgumentException.class,
                () -> MarkersConfig.parse(java.util.List.of("text: " + value)), value);
        }
        var parsed = MarkersConfig.parse(java.util.List.of(
            "plain: John's area # comment", "single: 'John''s # area' # comment",
            "double: \"Home \\\"quoted\\\" # area\" # comment", "empty: '' # comment"));
        assertEquals("John's area", parsed.get("plain"));
        assertEquals("John's # area", parsed.get("single"));
        assertEquals("Home \"quoted\" # area", parsed.get("double"));
        assertEquals("", parsed.get("empty"));
    }

    @Test void portalInteractionAndUnchangedRenameShowTheNameWithoutChangingMarkers() {
        SquareMarkersCore.onInitialize(new JsonStorage(directory.toString()), null, () -> {});
        var portal = new NetherPortalMarkerLayer(world());
        assertEquals(net.squaremarkers.core.objects.InteractionResult.State.SKIP, portal.interact(0, 64, 0).state());
        assertTrue(portal.getMarker(portal.toMarkerKey(0, 64, 0)).isEmpty());
        portal.add(0, 64, 0);
        assertEquals(net.squaremarkers.core.objects.InteractionResult.State.SKIP, portal.interact(0, 64, 0).state());
        boolean renameEnabled = MarkersConfig.NETHER_PORTAL_MARKERS_RENAME;
        try {
            MarkersConfig.NETHER_PORTAL_MARKERS_RENAME = true;
            assertEquals(net.squaremarkers.core.objects.InteractionResult.State.ADDED, portal.setName(0, 64, 0, "Home $1 {type}").state());
            assertEquals("Home $1 {type}", portal.interact(0, 64, 0).message());
            FeedbackMessages.reload(Map.of("messages.marker.interact", "Portal: {name}"));
            var unchanged = portal.setName(0, 64, 0, "Home $1 {type}");
            assertEquals(net.squaremarkers.core.objects.InteractionResult.State.FEEDBACK, unchanged.state());
            assertEquals("Portal: Home $1 {type}", unchanged.message());
            MarkersConfig.NETHER_PORTAL_MARKERS_RENAME = false;
            var disabledRename = portal.setName(0, 64, 0, "Other");
            assertEquals(net.squaremarkers.core.objects.InteractionResult.State.FEEDBACK, disabledRename.state());
            assertEquals("Portal: Home $1 {type}", disabledRename.message());
            assertEquals(net.squaremarkers.core.objects.InteractionResult.State.FEEDBACK,
                portal.setName(0, 64, 0, "Home $1 {type}").state());
            assertEquals(net.squaremarkers.core.objects.InteractionResult.State.SKIP,
                portal.setName(10, 64, 0, "Other").state());
            assertTrue(portal.getMarker(portal.toMarkerKey(10, 64, 0)).isEmpty());
            assertEquals("Portal: Home $1 {type}", portal.interact(0, 64, 0).message());
            assertEquals("Home $1 {type}", portal.getMarker(portal.toMarkerKey(0, 64, 0)).orElseThrow().getName());
            FeedbackMessages.reload(Map.of("messages.marker.interact", ""));
            assertEquals("", portal.interact(0, 64, 0).message());
        } finally {
            MarkersConfig.NETHER_PORTAL_MARKERS_RENAME = renameEnabled;
        }
    }

    @Test void markerAreaAndSignOperationsUseConfiguredFeedback() {
        SquareMarkersCore.onInitialize(new JsonStorage(directory.toString()), null, () -> {});
        FeedbackMessages.reload(Map.of("messages.marker.add", "Created {type}",
            "messages.marker.rename", "{name} ({type})", "messages.area.create", "Area {label}",
            "messages.area.point-add", "Point {label}", "messages.sign.add", "New sign",
            "messages.sign.edit", "Changed sign", "messages.sign.remove", "Deleted sign"));
        var portal = new NetherPortalMarkerLayer(world());
        assertTrue(portal.add(0, 64, 0).message().startsWith("Created "));
        assertTrue(portal.setName(0, 64, 0, "Home").message().startsWith("Home ("));
        var area = new AreaMarkerLayer(world());
        assertEquals("Area Home", area.addPoint("Home", 1, 0, 64, 0).message());
        assertEquals("Point Home", area.addPoint("Home", 1, 16, 64, 0).message());
        var signs = new SignsMarkerLayer(world());
        assertEquals("New sign", signs.set(0, 64, 0, new String[]{"A", "", "", ""}).message());
        assertEquals("Changed sign", signs.set(0, 64, 0, new String[]{"B", "", "", ""}).message());
        assertEquals("Deleted sign", signs.remove(0, 64, 0).message());
    }

    private static MapWorld world() {
        return (MapWorld) Proxy.newProxyInstance(MapWorld.class.getClassLoader(), new Class<?>[]{MapWorld.class},
            (proxy, method, args) -> {
                if (method.getName().equals("identifier")) return WorldIdentifier.parse("minecraft:overworld");
                throw new UnsupportedOperationException(method.getName());
            });
    }
}
