package net.squaremarkers.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class DeathStoreTest {
    @TempDir Path directory;
    private DeathStore.Death death(String id, String world, long at, long expiry) {
        return new DeathStore.Death(id, "Player", world, 1, 65, 2, at, expiry);
    }
    @Test void replacementAcrossDimensionsAndExpiryUseAbsoluteTime() {
        var store = new DeathStore(directory.resolve("deaths.json"));
        String player = UUID.randomUUID().toString();
        var first = death(player, "minecraft:overworld", 100, 200);
        var next = death(player, "minecraft:the_nether", 150, 250);
        assertTrue(store.add(first).isEmpty());
        assertEquals(java.util.List.of(first), store.add(next));
        store.save();
        var reloaded = new DeathStore(directory.resolve("deaths.json"));
        reloaded.load(230);
        assertEquals(java.util.List.of(next), reloaded.current(230));
        assertEquals(java.util.List.of(next), reloaded.expire(250));
        reloaded.save();
        store.load(251);
        assertTrue(store.current(251).isEmpty());
    }
    @Test void expiresOfflineAndLimitsMemory() {
        var store = new DeathStore(directory.resolve("deaths.json"), 2);
        var first = death(UUID.randomUUID().toString(), "a:b", 1, 100);
        var second = death(UUID.randomUUID().toString(), "a:b", 2, 100);
        var third = death(UUID.randomUUID().toString(), "a:b", 3, 100);
        store.add(first); store.add(second);
        assertEquals(java.util.List.of(first), store.add(third));
        store.save(); store.load(100);
        assertTrue(store.current(100).isEmpty());
        store.save(); store.clearRuntime(); store.load(101);
        assertTrue(store.current(101).isEmpty());
    }
    @Test void corruptAndInvalidRecordsAreBackedUp() throws Exception {
        Path path = directory.resolve("deaths.json");
        Files.writeString(path, "[{\"player\":\"invalid\"}]");
        var store = new DeathStore(path);
        assertDoesNotThrow(() -> store.load(0));
        assertTrue(store.current(0).isEmpty());
        assertFalse(Files.exists(path));
        try (var files = Files.list(directory)) {
            assertTrue(files.anyMatch(file -> file.getFileName().toString().startsWith("deaths.json.broken-")));
        }
    }
}
