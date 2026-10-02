package net.squaremarkers.fabric.compat;

import net.squaremarkers.core.helpers.PolygonLoops;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/** One global geometry job at a time. Snapshots contain no server/world references. */
public final class OpacGeometryWorker {
    public static final OpacGeometryWorker SHARED = new OpacGeometryWorker();
    private ExecutorService executor;
    private AtomicBoolean busy;

    public synchronized Future<List<PolygonLoops.Part>> trySubmit(Supplier<List<OpacChunk>> snapshot) {
        if (executor == null) {
            executor = Executors.newSingleThreadExecutor(task -> {
                Thread thread = new Thread(task, "SquareMarkers-OPAC-geometry");
                thread.setDaemon(true);
                return thread;
            });
            busy = new AtomicBoolean();
        }
        AtomicBoolean reservation = busy;
        if (!reservation.compareAndSet(false, true)) return null;
        try {
            // Called by the server thread only after capacity has been reserved.
            List<OpacChunk> chunks = snapshot.get();
            FutureTask<List<PolygonLoops.Part>> task = new FutureTask<>(() -> {
                if (chunks.isEmpty()) return List.of();
                OpacClaim detached = new OpacClaim(chunks.getFirst());
                for (OpacChunk chunk : chunks) {
                    if (Thread.currentThread().isInterrupted()) throw new CancellationException();
                    detached.addChunk(chunk);
                }
                return PolygonLoops.group(detached.getPolygons());
            }) {
                @Override public void run() {
                    try { super.run(); }
                    finally { reservation.set(false); }
                }
            };
            executor.execute(task);
            return task;
        } catch (RuntimeException exception) {
            reservation.set(false);
            throw exception;
        }
    }

    public synchronized void shutdown() {
        if (executor != null) executor.shutdownNow();
        executor = null;
        busy = null;
    }
}
