package net.squaremarkers.fabric.helpers;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.squaremarkers.core.AreaFeedbackConfig;
import net.squaremarkers.core.FeedbackMessages;
import net.squaremarkers.core.MarkersConfig;
import net.squaremarkers.core.SquareMarkersCore;
import net.squaremarkers.core.interfaces.IBoundary;
import net.squaremarkers.core.layers.primitive.AreaMarkerLayer;
import net.squaremarkers.core.registries.Layers;

/** Server-thread area feedback: cached containment, throttled actionbars, one owned bossbar per player. */
public final class AreaFeedbackHandler {
    private static final Map<UUID, State> STATES = new HashMap<>();
    private AreaFeedbackHandler() {}

    public static void tick(MinecraftServer server) {
        int now = server.getTickCount();
        if (now % 5 != 0) return; // At most four movement checks per second per player.
        if (!MarkersConfig.AREA_MARKERS_ENABLED || !SquareMarkersCore.areFeedbackMessagesEnabled()
            || (MarkersConfig.AREA_ON_ENTER == AreaFeedbackConfig.Transition.NONE
            && MarkersConfig.AREA_ON_LEAVE == AreaFeedbackConfig.Transition.NONE
            && MarkersConfig.AREA_PRESENCE == AreaFeedbackConfig.Presence.NONE)) {
            clear();
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive() || player.isSpectator()) { remove(player.getUUID()); continue; }
            String world = player.level().dimension().identifier().toString();
            var layer = SquareMarkersCore.squaremapHandler().getLayer(world, AreaMarkerLayer.class, Layers.Keys.AREAS);
            State state = STATES.get(player.getUUID());
            if (state != null && state.player != player) { remove(player.getUUID()); state = null; }
            if (state != null) state.changingWorld(world);
            if (layer == null) { remove(player.getUUID()); continue; }
            if (state == null || state.layer != layer) {
                if (state != null) state.close();
                state = new State(player, layer, new PlayerDisplay(player));
                STATES.put(player.getUUID(), state);
            }
            state.lastSeenTick = now;
            state.update(player.blockPosition(), now);
        }
        var iterator = STATES.values().iterator();
        while (iterator.hasNext()) {
            State state = iterator.next();
            if (state.lastSeenTick != now) { state.close(); iterator.remove(); }
        }
    }

    private static void transition(ServerPlayer player, AreaFeedbackConfig.Transition mode, String text, int color) {
        if (mode == AreaFeedbackConfig.Transition.NONE || text.isEmpty()) return;
        if (mode == AreaFeedbackConfig.Transition.ACTIONBAR) {
            FeedbackHelper.sendOverlayMessage(player, text, color);
            return;
        }
        player.connection.send(new ClientboundSetTitlesAnimationPacket(20, 60, 20));
        player.connection.send(new ClientboundSetSubtitleTextPacket(mode == AreaFeedbackConfig.Transition.SUBTITLE
            ? Component.literal(text).withColor(color) : Component.empty()));
        player.connection.send(new ClientboundSetTitleTextPacket(mode == AreaFeedbackConfig.Transition.TITLE
            ? Component.literal(text).withColor(color) : Component.empty()));
    }

    public static void remove(UUID id) {
        State state = STATES.remove(id);
        if (state != null) state.close();
    }

    public static void clear() {
        STATES.values().forEach(State::close);
        STATES.clear();
    }

    /** Small output boundary so transitions and cleanup can be tested without a running client. */
    interface Display {
        void overlay(Component text);
        void transition(AreaFeedbackConfig.Transition mode, String text, int color);
        void boss(Component text);
        void clearBoss();
    }

    private static final class PlayerDisplay implements Display {
        private final ServerPlayer player;
        private ServerBossEvent boss;
        PlayerDisplay(ServerPlayer player) { this.player = player; }
        public void overlay(Component text) { player.sendOverlayMessage(text); }
        public void transition(AreaFeedbackConfig.Transition mode, String text, int color) {
            AreaFeedbackHandler.transition(player, mode, text, color);
        }
        public void boss(Component text) {
            if (boss == null) {
                boss = new ServerBossEvent(UUID.randomUUID(), text, BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);
                boss.setProgress(1.0F);
                boss.addPlayer(player);
            } else boss.setName(text);
        }
        public void clearBoss() { if (boss != null) boss.removeAllPlayers(); boss = null; }
    }

    static final class State {
        final ServerPlayer player;
        final AreaMarkerLayer layer;
        final Display display;
        BlockPos position;
        long revision = Long.MIN_VALUE;
        IBoundary boundary;
        String boundaryKey;
        int nextPresence, lastSeenTick;
        boolean presenceCached, actionbarShown;
        Component presenceText;
        String name;
        int color;
        State(ServerPlayer player, AreaMarkerLayer layer, Display display) {
            this.player = player; this.layer = layer; this.display = display;
        }

        void update(BlockPos pos, int now) {
            if (position == null || pos.getX() != position.getX() || pos.getZ() != position.getZ()
                || layer.revision() != revision) {
                IBoundary previous = boundary;
                IBoundary current = layer.getContaining(pos.getX(), pos.getZ()).orElse(null);
                position = pos;
                revision = layer.revision();
                if (previous != current) {
                    String nextKey = current == null ? null : current.areaMarker().getKey();
                    if (!Objects.equals(boundaryKey, nextKey)) {
                        stopPresence();
                        nextPresence = now + 40;
                        notifyTransition(previous, MarkersConfig.AREA_ON_LEAVE, FeedbackMessages.AREA_LEAVE);
                        notifyTransition(current, MarkersConfig.AREA_ON_ENTER, FeedbackMessages.AREA_ENTER);
                    }
                    boundaryKey = nextKey;
                    boundary = current;
                }
            }
            if (boundary == null || MarkersConfig.AREA_PRESENCE == AreaFeedbackConfig.Presence.NONE) return;
            var area = boundary.areaMarker();
            boolean changed = !presenceCached || !Objects.equals(name, area.getName()) || color != area.getColor();
            if (changed) {
                name = area.getName(); color = area.getColor(); presenceCached = true;
                String text = FeedbackMessages.AREA_PRESENCE.text("name", name);
                presenceText = text.isEmpty() ? null : Component.literal(text).withColor(color);
            }
            if (MarkersConfig.AREA_PRESENCE == AreaFeedbackConfig.Presence.ACTIONBAR) {
                if (now - nextPresence < 0) return; // Subtraction remains valid across tick counter rollover.
                nextPresence = now + 40;
                if (presenceText != null) { display.overlay(presenceText); actionbarShown = true; }
            } else if (changed) {
                if (presenceText == null) display.clearBoss(); else display.boss(presenceText);
            }
        }

        private void notifyTransition(IBoundary area, AreaFeedbackConfig.Transition mode, FeedbackMessages message) {
            if (area == null || mode == AreaFeedbackConfig.Transition.NONE) return;
            String text = message.text("name", area.areaMarker().getName());
            if (!text.isEmpty()) display.transition(mode, text, area.areaMarker().getColor());
        }

        void changingWorld(String world) {
            if (layer.worldIdentifier.equals(world)) return;
            stopPresence();
            notifyTransition(boundary, MarkersConfig.AREA_ON_LEAVE, FeedbackMessages.AREA_LEAVE);
            boundary = null; boundaryKey = null;
        }

        void stopPresence() {
            display.clearBoss();
            presenceCached = false;
            presenceText = null;
            name = null;
            if (actionbarShown) display.overlay(Component.empty());
            actionbarShown = false;
        }
        void close() { stopPresence(); boundary = null; boundaryKey = null; position = null; }
    }
}
