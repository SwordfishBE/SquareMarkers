package net.squaremarkers.core;

import net.squaremarkers.core.json.StateFile;
import java.nio.file.Path;
import java.util.*;

/** Bounded, entity-reference-free state for the last death of each player. */
public final class DeathStore {
    public record Death(String player, String name, String world, int x, int y, int z, long diedAt, long expiresAt) {
        public Death {
            if (player == null || name == null || world == null || name.isBlank() || name.length() > 256
                || world.isBlank() || world.length() > 256 || Math.abs((long) x) > 30_000_000
                || Math.abs((long) z) > 30_000_000 || diedAt < 0 || expiresAt <= diedAt) {
                throw new IllegalArgumentException("Invalid death marker");
            }
            UUID.fromString(player);
        }
    }
    private final Path path;
    private final Map<String, Death> deaths = new HashMap<>();
    private boolean dirty;
    private final int limit;

    public DeathStore(Path path) { this(path, 10_000); }
    DeathStore(Path path, int limit) { this.path = path; this.limit = limit; }

    public void load(long now) {
        deaths.clear();
        dirty = false;
        for (Death death : StateFile.read(path, Death[].class, unused -> {})) {
            Death previous = deaths.get(death.player());
            if (previous == null || previous.diedAt() < death.diedAt()) deaths.put(death.player(), death);
        }
        expire(now);
    }
    public List<Death> current(long now) {
        return deaths.values().stream().filter(death -> death.expiresAt() > now).toList();
    }
    public List<Death> add(Death death) {
        List<Death> removed = new ArrayList<>();
        Death previous = deaths.remove(death.player());
        if (previous != null) removed.add(previous);
        if (deaths.size() >= limit) {
            Death oldest = Collections.min(deaths.values(), Comparator.comparingLong(Death::diedAt));
            deaths.remove(oldest.player());
            removed.add(oldest);
        }
        deaths.put(death.player(), death);
        dirty = true;
        return List.copyOf(removed);
    }
    public List<Death> expire(long now) {
        if (deaths.isEmpty()) return List.of();
        List<Death> removed = new ArrayList<>();
        deaths.values().removeIf(death -> {
            if (death.expiresAt() > now) return false;
            removed.add(death);
            return true;
        });
        if (!removed.isEmpty()) dirty = true;
        return List.copyOf(removed);
    }
    public void save() {
        if (dirty && StateFile.write(path, deaths.values())) dirty = false;
    }
    public void clearRuntime() { deaths.clear(); dirty = false; }
}
