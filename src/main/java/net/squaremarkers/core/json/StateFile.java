package net.squaremarkers.core.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.squaremarkers.core.SquareMarkersCore;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.function.Consumer;

/** Small server-thread-owned state files, with validation before publication. */
public final class StateFile {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private StateFile() {}

    public static <T> List<T> read(Path path, Class<T[]> type, Consumer<T> validator) {
        if (Files.notExists(path)) return List.of();
        try (var reader = Files.newBufferedReader(path)) {
            if (Files.size(path) > 64L * 1024 * 1024) throw new JsonParseException("State file exceeds size limit");
            T[] values = GSON.fromJson(reader, type);
            if (values == null || values.length > 10_000) throw new JsonParseException("Invalid state array size");
            for (T value : values) {
                if (value == null) throw new JsonParseException("Null state entry");
                validator.accept(value);
            }
            return List.of(values);
        } catch (RuntimeException exception) {
            Path backup = path.resolveSibling(path.getFileName() + ".broken-" + System.currentTimeMillis());
            try {
                Files.move(path, backup);
                SquareMarkersCore.warn("Invalid state moved to " + backup, exception);
            } catch (IOException failure) {
                failure.addSuppressed(exception);
                SquareMarkersCore.warn("Could not back up invalid state " + path, failure);
            }
        } catch (IOException exception) {
            SquareMarkersCore.warn("Could not read state " + path, exception);
        }
        return List.of();
    }

    public static boolean write(Path path, Object values) {
        Path temporary = null;
        try {
            Files.createDirectories(path.getParent());
            temporary = Files.createTempFile(path.getParent(), path.getFileName() + ".", ".tmp");
            try (var writer = Files.newBufferedWriter(temporary)) {
                GSON.toJson(values, writer);
            }
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException exception) {
            SquareMarkersCore.warn("Could not save state " + path, exception);
            return false;
        } finally {
            if (temporary != null) try { Files.deleteIfExists(temporary); }
            catch (IOException exception) { SquareMarkersCore.debug("Could not remove temporary state file " + temporary); }
        }
    }
}
