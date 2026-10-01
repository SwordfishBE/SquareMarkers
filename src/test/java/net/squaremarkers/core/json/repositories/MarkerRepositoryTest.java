package net.squaremarkers.core.json.repositories;

import net.squaremarkers.core.json.JsonStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MarkerRepositoryTest {
    @TempDir Path directory;

    @Test
    void invalidSimpleMarkersAreBackedUpWithoutCrashing() throws Exception {
        int index = 0;
        for (String json : List.of("[null]", "null", "[{}]", "[{\"pos\":[1]}]",
            "[{\"pos\":[1,2.5,3]}]", "[{\"pos\":[1,2,999999999999]}]",
            "[{\"pos\":[30000001,64,0]}]", "[{\"pos\":[1,null,3]}]")) {
            String key = "simple_" + index++;
            Path file = write(key, json);
            var repo = assertDoesNotThrow(() -> new SimpleMarkerRepository(world(), key));
            assertTrue(repo.copy().isEmpty());
            assertBackedUp(file, json);
        }
    }

    @Test
    void invalidSignsAndAreasAreBackedUp() throws Exception {
        Path sign = write("signs", "[{\"pos\":[1,64,2],\"text\":[\"one\",null,\"three\",\"four\"]}]");
        assertDoesNotThrow(() -> new SignMarkerRepository(world(), "signs"));
        assertFalse(Files.exists(sign));
        int index = 0;
        for (String json : List.of("[{\"name\":null,\"points\":[[1,2,3]]}]",
            "[{\"name\":\"test\",\"points\":null}]", "[{\"name\":\"test\",\"points\":[null]}]")) {
            String key = "areas_" + index++;
            Path file = write(key, json);
            assertDoesNotThrow(() -> new AreaMarkerRepository(world(), key));
            assertBackedUp(file, json);
        }
    }

    @Test
    void validMarkersRoundTripAndFailedReadPreservesExistingData() throws Exception {
        var repo = new SimpleMarkerRepository(world(), "portals");
        repo.getOrCreate(12, 64, -34).setName("Portal & home");
        repo.write();
        var loaded = new SimpleMarkerRepository(world(), "portals");
        assertEquals("Portal & home", loaded.get(12, 64, -34).getName());
        Path file = write("portals", "[null]");
        assertDoesNotThrow(loaded::read);
        assertEquals(1, loaded.copy().size());
        assertBackedUp(file, "[null]");
    }

    private WorldRepository world() {
        return new WorldRepository(new JsonStorage(directory.toString()), "minecraft:overworld");
    }

    private Path write(String key, String json) throws Exception {
        Path file = directory.resolve("minecraft_overworld").resolve(key + ".json");
        Files.createDirectories(file.getParent());
        Files.writeString(file, json);
        return file;
    }

    private void assertBackedUp(Path file, String expected) throws Exception {
        assertFalse(Files.exists(file));
        try (var files = Files.list(file.getParent())) {
            Path backup = files.filter(path -> path.getFileName().toString()
                .startsWith(file.getFileName() + ".broken-")).findFirst().orElseThrow();
            assertEquals(expected, Files.readString(backup));
        }
    }
}
