package net.squaremarkers.fabric.compat.warps;

/** Bounds asynchronous work to one active request and one coalesced follow-up. */
final class RefreshGate {
    record Ticket(long generation) {}
    private long generation;
    private Ticket active;
    private boolean pending;

    synchronized Ticket begin() {
        if (active != null) { pending = true; return null; }
        active = new Ticket(generation);
        return active;
    }

    synchronized boolean current(Ticket ticket) { return ticket.generation() == generation; }

    synchronized boolean finish(Ticket ticket) {
        if (active != ticket) return false;
        active = null;
        boolean again = pending;
        pending = false;
        return again;
    }

    synchronized void invalidate() {
        generation++;
        pending = false;
        // Keep the reservation until the real query completes: cancelling a future does not stop JDBC.
    }
}
