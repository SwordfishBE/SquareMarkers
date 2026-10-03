package net.squaremarkers.core.layers;

import net.squaremarkers.core.*;
import net.squaremarkers.core.helpers.HtmlHelper;
import net.squaremarkers.core.layers.primitive.MarkerLayer;
import net.squaremarkers.core.markers.IconMarkerBuilder;
import net.squaremarkers.core.markers.MarkerBuilder;
import net.squaremarkers.core.registries.Icons;
import net.squaremarkers.core.registries.Layers;
import xyz.jpenilla.squaremap.api.MapWorld;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class DeathMarkerLayer extends MarkerLayer<DeathStore.Death> {
    private static final DateTimeFormatter DEATH_TIME = DateTimeFormatter
        .ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
    private static final DateTimeFormatter ZONE_LABEL = DateTimeFormatter.ofPattern("z", Locale.ENGLISH);
    public DeathMarkerLayer(MapWorld world) {
        super(Layers.Keys.DEATHS, "Player Deaths", world, MarkersConfig.DEATH_MARKERS_PRIORITY);
    }
    @Override public void load() {
        DeathMarkers.current().stream().filter(death -> death.world().equals(worldIdentifier)).forEach(this::loadMarker);
    }
    @Override protected String markerKey(DeathStore.Death death) { return death.player(); }
    @Override public MarkerBuilder<?> createBuilder(DeathStore.Death death) {
        return IconMarkerBuilder.newIconMarker(death.player(), Icons.Keys.DEATH, death.x(), death.z()).centerIcon(16, 16);
    }
    @Override protected String createTooltip(DeathStore.Death death) {
        return HtmlHelper.sanitize(FeedbackMessages.DEATH_TITLE.text("name", death.name()));
    }
    @Override protected String createPopup(DeathStore.Death death) {
        String title = createTooltip(death);
        String position = HtmlHelper.sanitize(FeedbackMessages.DEATH_POSITION.text(
            "x", Integer.toString(death.x()), "y", Integer.toString(death.y()), "z", Integer.toString(death.z())));
        String time = HtmlHelper.sanitize(formatDeathTime(death.diedAt(), MarkersConfig.DEATH_MARKERS_TIMEZONE));
        StringBuilder popup = new StringBuilder();
        if (!title.isEmpty()) popup.append("<b>").append(title).append("</b>");
        for (String line : new String[]{position, time}) {
            if (line.isEmpty()) continue;
            if (!popup.isEmpty()) popup.append("<br>");
            popup.append(line);
        }
        return popup.toString();
    }

    static String formatDeathTime(long timestamp, ZoneId zone) {
        var time = Instant.ofEpochMilli(timestamp).atZone(zone);
        return FeedbackMessages.DEATH_TIME.text("timezone", ZONE_LABEL.format(time), "time", DEATH_TIME.format(time));
    }
}
