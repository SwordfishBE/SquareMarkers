package net.squaremarkers.fabric.compat.warps;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RefreshGateTest {
    @Test void burstsCoalesceWithoutInvalidatingAnActiveResult() {
        var gate = new RefreshGate();
        var first = gate.begin();
        for (int i = 0; i < 1000; i++) assertNull(gate.begin());
        assertTrue(gate.current(first));
        assertTrue(gate.finish(first));
        var followUp = gate.begin();
        assertNotNull(followUp);
        assertFalse(gate.finish(first)); // A late duplicate must not release the next query.
        assertFalse(gate.finish(followUp));
    }

    @Test void disableAndRestartDiscardResultsButKeepRealWorkBounded() {
        var gate = new RefreshGate();
        var old = gate.begin();
        gate.begin();
        gate.invalidate();
        assertFalse(gate.current(old));
        assertNull(gate.begin()); // Re-enabled while old JDBC work is still running.
        assertTrue(gate.finish(old));
        var next = gate.begin();
        assertTrue(gate.current(next));
        gate.invalidate();
        assertFalse(gate.finish(next)); // No follow-up when disabled with nothing new requested.
    }

    @Test void failedOrSuccessfulCompletionReleasesCapacity() {
        var gate = new RefreshGate();
        var first = gate.begin();
        assertFalse(gate.finish(first));
        assertNotNull(gate.begin());
    }
}
