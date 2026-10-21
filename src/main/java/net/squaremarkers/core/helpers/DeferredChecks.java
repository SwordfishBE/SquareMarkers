package net.squaremarkers.core.helpers;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Server-thread checks that sleep until a required chunk becomes available. */
public final class DeferredChecks<T> {
    private final Set<T> scheduled = new LinkedHashSet<>();
    private final Set<T> ready = new LinkedHashSet<>();
    private final Map<T, Long> waitingFor = new HashMap<>();
    private final Map<Long, Set<T>> waitingByChunk = new HashMap<>();

    public void schedule(T value) {
        if (scheduled.add(value)) ready.add(value);
    }

    public T poll() {
        var iterator = ready.iterator();
        if (!iterator.hasNext()) return null;
        T value = iterator.next();
        iterator.remove();
        return value;
    }

    public void await(T value, long chunk) {
        if (!scheduled.contains(value)) return;
        ready.remove(value);
        removeWait(value);
        waitingFor.put(value, chunk);
        waitingByChunk.computeIfAbsent(chunk, unused -> new LinkedHashSet<>()).add(value);
    }

    public boolean chunkLoaded(long chunk) {
        Set<T> values = waitingByChunk.remove(chunk);
        if (values == null) return false;
        values.forEach(value -> {
            waitingFor.remove(value);
            if (scheduled.contains(value)) ready.add(value);
        });
        return !values.isEmpty();
    }

    public void complete(T value) {
        scheduled.remove(value);
        ready.remove(value);
        removeWait(value);
    }

    private void removeWait(T value) {
        Long chunk = waitingFor.remove(value);
        if (chunk == null) return;
        Set<T> values = waitingByChunk.get(chunk);
        values.remove(value);
        if (values.isEmpty()) waitingByChunk.remove(chunk);
    }

    public boolean hasReady() { return !ready.isEmpty(); }
    public int size() { return scheduled.size(); }
    public void clear() { scheduled.clear(); ready.clear(); waitingFor.clear(); waitingByChunk.clear(); }
}
