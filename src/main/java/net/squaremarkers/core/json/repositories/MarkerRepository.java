package net.squaremarkers.core.json.repositories;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import net.squaremarkers.core.interfaces.IMarkerRepository;
import net.squaremarkers.core.SquareMarkersCore;
import net.squaremarkers.core.json.entities.Marker;
import net.squaremarkers.core.json.entities.Point;
import net.squaremarkers.core.json.serializers.PointSerializer;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

public abstract class MarkerRepository<T extends Marker> implements IMarkerRepository<T> {

	public final String worldIdentifier;
	public final String layerKey;
	private final Gson gson;
	private final Path folderPath;
	private final Path filePath;
	private final Type markerClass;
	private final AtomicBoolean dirty = new AtomicBoolean(false);
	protected HashSet<T> data;

	public MarkerRepository(WorldRepository worldRepository, String layerKey, Class<T> clazz) {
		worldIdentifier = worldRepository.worldIdentifier;
		this.layerKey = layerKey;
		gson = new GsonBuilder()
				.setPrettyPrinting()
				.registerTypeAdapter(Point.class, new PointSerializer())
				.create();
		folderPath = Path.of(worldRepository.getStorage().getConfigPath(), formatWorldIdentifier(worldRepository.worldIdentifier));
		filePath = folderPath.resolve(layerKey + ".json");
		markerClass = TypeToken.getParameterized(HashSet.class, clazz).getType();
		data = new HashSet<>();
		read();
	}

	public String formatWorldIdentifier(String worldIdentifier) {
		return worldIdentifier.replace(":", "_");
	}

	@Override
	public Collection<T> copy() {
		return Set.copyOf(data);
	}

	@Override
	public void foreach(Consumer<T> action) {
		data.forEach(action);
	}

	@Override
	public Stream<T> stream() {
		return data.stream();
	}

	/**
	 * Marks the repository as dirty, indicating that it needs to be written to disk.
	 */
	final public void markDirty() {
		dirty.set(true);
	}

	private boolean fileExists() {
		return Files.isRegularFile(filePath);
	}

	private void ensureStorageDirectory() throws IOException {
		Files.createDirectories(folderPath);
		if (!Files.isDirectory(folderPath)) {
			throw new IOException("Storage path is not a directory: " + folderPath);
		}
		if (Files.exists(filePath) && !Files.isRegularFile(filePath)) {
			throw new IOException("Marker path is not a file: " + filePath);
		}
	}

	final public synchronized void write() {
		if (!dirty.get()) {
			return;
		}
		try {
			if (data == null) {
				return;
			}
			ensureStorageDirectory();
			Path temporaryFile = filePath.resolveSibling(filePath.getFileName() + ".tmp");
			try (Writer writer = Files.newBufferedWriter(temporaryFile, StandardCharsets.UTF_8)) {
				gson.toJson(data, writer);
			}
			try {
				Files.move(temporaryFile, filePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException ignored) {
				Files.move(temporaryFile, filePath, StandardCopyOption.REPLACE_EXISTING);
			}
			dirty.set(false);
		} catch (IOException exception) {
			SquareMarkersCore.warn("Failed to write marker data to " + filePath, exception);
		}
	}

	final public synchronized void read() {
		// only read if not dirty
		if (dirty.get()) {
			return;
		}
		try {
			if (!fileExists()) {
				return;
			}
			HashSet<T> data;
			try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
				data = gson.fromJson(reader, markerClass);
			}
			if (data == null) {
				return;
			}
			assert data instanceof HashSet<T>;
			data.forEach(marker -> marker.SetContext(this));
			this.data = data;
		} catch (JsonParseException exception) {
			backupCorruptFile(exception);
		} catch (IOException exception) {
			SquareMarkersCore.warn("Failed to read marker data from " + filePath, exception);
		}
	}

	private void backupCorruptFile(JsonParseException cause) {
		Path backup = filePath.resolveSibling(filePath.getFileName() + ".broken-" + System.currentTimeMillis());
		try {
			Files.move(filePath, backup, StandardCopyOption.REPLACE_EXISTING);
			SquareMarkersCore.warn("Invalid marker data moved to " + backup, cause);
		} catch (IOException moveException) {
			moveException.addSuppressed(cause);
			SquareMarkersCore.warn("Invalid marker data could not be backed up: " + filePath, moveException);
		}
	}

}
