package net.squaremarkers.fabric.helpers;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.squaremarkers.core.AreaFeedbackConfig;
import net.squaremarkers.core.MarkersConfig;
import net.squaremarkers.core.SquareMarkersCore;
import net.squaremarkers.core.json.JsonStorage;
import net.squaremarkers.core.layers.primitive.AreaMarkerLayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import static org.junit.jupiter.api.Assertions.*;

class AreaFeedbackHandlerTest {
    @TempDir Path directory;
    final AreaFeedbackConfig.Transition oldEnter = MarkersConfig.AREA_ON_ENTER, oldLeave = MarkersConfig.AREA_ON_LEAVE;
    final AreaFeedbackConfig.Presence oldPresence = MarkersConfig.AREA_PRESENCE;
    AreaMarkerLayer layer;
    final RecordingDisplay display = new RecordingDisplay();
    AreaFeedbackHandler.State state;
    final BlockPos inside = new BlockPos(5, 64, 0), outside = new BlockPos(100, 64, 0);

    @BeforeEach void setup() {
        MarkersConfig.AREA_ON_ENTER = AreaFeedbackConfig.Transition.NONE;
        MarkersConfig.AREA_ON_LEAVE = AreaFeedbackConfig.Transition.NONE;
        MarkersConfig.AREA_PRESENCE = AreaFeedbackConfig.Presence.NONE;
        SquareMarkersCore.onInitialize(new JsonStorage(directory.toString()), null, () -> {});
        MapWorld world = (MapWorld) Proxy.newProxyInstance(MapWorld.class.getClassLoader(), new Class<?>[]{MapWorld.class},
            (proxy, method, args) -> {
                if (method.getName().equals("identifier")) return WorldIdentifier.parse("minecraft:overworld");
                throw new UnsupportedOperationException(method.getName());
            });
        layer = new AreaMarkerLayer(world);
        layer.addPoint("Home", 123, 0, 64, 0);
        layer.addPoint("Home", 123, 10, 64, 0);
        state = new AreaFeedbackHandler.State(null, layer, display);
    }

    @AfterEach void cleanup() {
        state.close();
        MarkersConfig.AREA_ON_ENTER = oldEnter; MarkersConfig.AREA_ON_LEAVE = oldLeave;
        MarkersConfig.AREA_PRESENCE = oldPresence;
    }

    @Test void dimensionExitSendsLeaveWithEntryDisabledAndRemovesBoss() {
        MarkersConfig.AREA_ON_LEAVE = AreaFeedbackConfig.Transition.SUBTITLE;
        MarkersConfig.AREA_PRESENCE = AreaFeedbackConfig.Presence.BOSSBAR;
        state.update(inside, 0);
        assertNotNull(display.boss);
        assertTrue(display.transitions.isEmpty());
        state.changingWorld("minecraft:overworld");
        assertNotNull(display.boss);
        assertTrue(display.transitions.isEmpty());
        state.changingWorld("minecraft:the_nether");
        assertEquals(List.of("SUBTITLE:[-] Home"), display.transitions);
        assertNull(display.boss);
        state.close();
        assertEquals(1, display.transitions.size());
    }

    @Test void ordinaryExitAndEntryAreIndependent() {
        MarkersConfig.AREA_ON_ENTER = AreaFeedbackConfig.Transition.TITLE;
        state.update(inside, 0);
        state.update(outside, 5);
        assertEquals(List.of("TITLE:[+] Home"), display.transitions);
        display.transitions.clear();
        MarkersConfig.AREA_ON_ENTER = AreaFeedbackConfig.Transition.NONE;
        MarkersConfig.AREA_ON_LEAVE = AreaFeedbackConfig.Transition.ACTIONBAR;
        state.update(inside, 10);
        state.update(outside, 15);
        assertEquals(List.of("ACTIONBAR:[-] Home"), display.transitions);
    }

    @Test void stationaryActionbarIsThrottledAndReusesComponent() {
        MarkersConfig.AREA_PRESENCE = AreaFeedbackConfig.Presence.ACTIONBAR;
        state.update(inside, 0);
        state.update(inside, 35);
        assertTrue(display.overlays.isEmpty());
        state.update(inside, 40);
        state.update(inside, 45);
        assertEquals(1, display.overlays.size());
        state.update(new BlockPos(6, 65, 0), 80);
        assertEquals(2, display.overlays.size());
        assertSame(display.overlays.get(0), display.overlays.get(1));
        state.update(outside, 85);
        assertEquals("", display.overlays.getLast().getString());
    }

    @Test void bossIsNotRepublishedOnMovementAndStationaryAreaRemovalClearsIt() {
        MarkersConfig.AREA_PRESENCE = AreaFeedbackConfig.Presence.BOSSBAR;
        state.update(inside, 0);
        state.update(new BlockPos(6, 64, 0), 5);
        state.update(inside, 10);
        assertEquals(1, display.bossUpdates);
        layer.removePointAt(0, 64, 0);
        layer.removePointAt(10, 64, 0);
        state.update(inside, 15);
        assertNull(display.boss);
    }

    @Test void lifecycleCleanupDoesNotSendArtificialLeaveAndIsIdempotent() {
        MarkersConfig.AREA_ON_LEAVE = AreaFeedbackConfig.Transition.TITLE;
        MarkersConfig.AREA_PRESENCE = AreaFeedbackConfig.Presence.ACTIONBAR;
        state.update(inside, 0);
        state.update(inside, 40);
        state.close();
        state.close();
        assertTrue(display.transitions.isEmpty());
        assertEquals(2, display.overlays.size());
        assertEquals("", display.overlays.getLast().getString());
        assertNull(state.boundary);
        assertNull(state.presenceText);
    }

    @Test void actionbarTimerSurvivesTickCounterRollover() {
        MarkersConfig.AREA_PRESENCE = AreaFeedbackConfig.Presence.ACTIONBAR;
        int start = Integer.MAX_VALUE - 20;
        state.update(inside, start);
        state.update(inside, start + 35);
        assertTrue(display.overlays.isEmpty());
        state.update(inside, start + 40);
        assertEquals(1, display.overlays.size());
    }

    private static final class RecordingDisplay implements AreaFeedbackHandler.Display {
        final List<Component> overlays = new ArrayList<>();
        final List<String> transitions = new ArrayList<>();
        Component boss;
        int bossUpdates;
        public void overlay(Component text) { overlays.add(text); }
        public void transition(AreaFeedbackConfig.Transition mode, String text, int color) {
            transitions.add(mode + ":" + text);
        }
        public void boss(Component text) { boss = text; bossUpdates++; }
        public void clearBoss() { boss = null; }
    }
}
