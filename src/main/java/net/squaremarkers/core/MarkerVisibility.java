package net.squaremarkers.core;

import net.squaremarkers.core.json.StateFile;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/** Exclusions outlive source deletion, so a recreated location remains private. */
public final class MarkerVisibility {
    public record Identity(String world, String layer, String marker) {
        public Identity {
            if (world == null || layer == null || marker == null || world.isBlank() || layer.isBlank()
                || marker.isBlank() || world.length() > 256 || layer.length() > 256 || marker.length() > 4096) {
                throw new IllegalArgumentException("Invalid marker identity");
            }
        }
    }
    private final Path path;
    private final Set<Identity> hidden = new HashSet<>();

    public MarkerVisibility(Path path) { this.path = path; }
    public void load() {
        hidden.clear();
        hidden.addAll(StateFile.read(path, Identity[].class, unused -> {}));
    }
    public boolean hidden(String world, String layer, String marker) {
        if (marker == null || marker.isBlank() || marker.length() > 4096) return false;
        return hidden.contains(new Identity(world, layer, marker));
    }
    public Set<Identity> exclusions() { return Set.copyOf(hidden); }
    public boolean setHidden(Identity identity, boolean value) {
        boolean previous = hidden.contains(identity);
        if (previous == value) return true;
        if (value && hidden.size() >= 10_000) return false;
        if (value) hidden.add(identity); else hidden.remove(identity);
        if (StateFile.write(path, hidden)) return true;
        if (previous) hidden.add(identity); else hidden.remove(identity);
        return false;
    }
    public void clearRuntime() { hidden.clear(); }
}
