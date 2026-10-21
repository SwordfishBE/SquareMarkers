package net.squaremarkers.core.helpers;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DeferredChecksTest {
    @Test void duplicatesSleepUntilTheRequiredChunkArrives() {
        var checks = new DeferredChecks<String>();
        for (int i = 0; i < 100; i++) checks.schedule("portal");
        assertEquals(1, checks.size());
        assertEquals("portal", checks.poll());
        checks.await("portal", 42);
        checks.schedule("portal");
        assertNull(checks.poll());
        assertFalse(checks.chunkLoaded(43));
        assertTrue(checks.chunkLoaded(42));
        assertEquals("portal", checks.poll());
        checks.await("portal", 99);
        assertFalse(checks.chunkLoaded(42));
        assertTrue(checks.chunkLoaded(99));
        assertEquals("portal", checks.poll());
        checks.complete("portal");
        assertEquals(0, checks.size());
        assertFalse(checks.hasReady());
    }

    @Test void cancellationAndCloseClearReadyAndWaitingReferences() {
        var checks = new DeferredChecks<String>();
        checks.schedule("a"); checks.schedule("b");
        checks.await(checks.poll(), 1);
        checks.complete("a");
        assertFalse(checks.chunkLoaded(1));
        assertEquals("b", checks.poll());
        checks.await("b", 2);
        checks.clear();
        assertEquals(0, checks.size());
        assertFalse(checks.chunkLoaded(2));
        assertNull(checks.poll());
    }
}
